package flare.client.app.data.parser

import android.util.Log
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object V2RayConfigConverter {

    fun convertIfNeeded(json: String): String {
        val trimmed = json.trim()
        return try {
            val obj = JSONObject(trimmed)
            when {
                isSingBoxFormat(obj) -> fixSingBox(obj)
                isV2RayFormat(obj) -> convertV2RayToSingBox(obj)
                else -> trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun isSingBoxFormat(obj: JSONObject): Boolean {
        val outbounds = obj.optJSONArray("outbounds")
        if (outbounds != null && outbounds.length() > 0) {
            val first = outbounds.optJSONObject(0)
            if (first?.has("type") == true) return true
        }
        return obj.has("route") && !obj.has("routing")
    }

    private fun isV2RayFormat(obj: JSONObject): Boolean {
        val outbounds = obj.optJSONArray("outbounds")
        if (outbounds != null && outbounds.length() > 0) {
            val first = outbounds.optJSONObject(0)
            if (first?.has("protocol") == true) return true
        }
        return obj.has("routing") || obj.has("outbounds")
    }

    fun convertV2RayToSingBox(xray: JSONObject): String {
        val sb = JSONObject()

        sb.put(
                "log",
                JSONObject().apply {
                    put("level", "info")
                    put("timestamp", true)
                }
        )

        val xrayOutbounds = xray.optJSONArray("outbounds") ?: JSONArray()
        val sbOutbounds = convertOutbounds(xrayOutbounds)

        ensureOutbound(sbOutbounds, "direct")
        ensureOutbound(sbOutbounds, "block")

        val proxyDomainsSet = linkedSetOf("raw.githubusercontent.com")
        for (i in 0 until sbOutbounds.length()) {
            val ob = sbOutbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type")
            if (type == "direct" || type == "block") continue
            val server = ob.optString("server", "")
            if (server.isNotEmpty() && !server[0].isDigit()) {
                proxyDomainsSet.add(server)
            }
        }
        val proxyDomains = JSONArray()
        proxyDomainsSet.forEach { proxyDomains.put(it) }

        val xrayRouting = xray.optJSONObject("routing")
        val xrayRules = xrayRouting?.optJSONArray("rules")
        val routingRulesObjects = mutableListOf<JSONObject>()
        val requiredRuleSets = mutableSetOf<String>()
        val directRuleSets = mutableSetOf<String>()
        val directDomains = JSONArray()

        val xrayBalancers = xrayRouting?.optJSONArray("balancers")
        val balancerTags = mutableMapOf<String, String>()
        var firstBalancerTag = ""
        if (xrayBalancers != null) {
            for (i in 0 until xrayBalancers.length()) {
                val b = xrayBalancers.optJSONObject(i) ?: continue
                val bTag = b.optString("tag", "")
                if (bTag.isEmpty()) continue
                val selectors = b.optJSONArray("selector")
                val matchedOutbounds = linkedSetOf<String>()
                if (selectors != null) {
                    for (j in 0 until selectors.length()) {
                        val sel = selectors.optString(j, "")
                        if (sel.isEmpty()) continue
                        for (k in 0 until sbOutbounds.length()) {
                            val ob = sbOutbounds.optJSONObject(k) ?: continue
                            val obTag = ob.optString("tag", "")
                            if (obTag.contains(sel)) {
                                matchedOutbounds.add(obTag)
                            }
                        }
                    }
                }
                if (matchedOutbounds.isNotEmpty()) {
                    var finalBTag = bTag
                    for (k in 0 until sbOutbounds.length()) {
                        val ob = sbOutbounds.optJSONObject(k)
                        if (ob?.optString("tag") == finalBTag) {
                            finalBTag = "${bTag}-urltest"
                            break
                        }
                    }
                    val urltestOb = JSONObject().apply {
                        put("type", "urltest")
                        put("tag", finalBTag)
                        put("outbounds", JSONArray().apply {
                            matchedOutbounds.forEach { put(it) }
                        })
                        put("url", "http://www.gstatic.com/generate_204")
                        put("interval", "3m")
                        put("tolerance", 50)
                    }
                    sbOutbounds.put(urltestOb)
                    balancerTags[bTag] = finalBTag
                    if (firstBalancerTag.isEmpty()) {
                        firstBalancerTag = finalBTag
                    }
                }
            }
        }

        if (xrayRules != null) {
            for (i in 0 until xrayRules.length()) {
                val xRule = xrayRules.optJSONObject(i) ?: continue
                val outboundTag = xRule.optString("outboundTag", xRule.optString("outbound", ""))
                val balancerTag = xRule.optString("balancerTag", "")
                val rawActualOutTag = if (balancerTag.isNotEmpty() && balancerTags.containsKey(balancerTag)) {
                    balancerTags[balancerTag] ?: balancerTag
                } else if (balancerTag.isNotEmpty()) {
                    balancerTag
                } else {
                    outboundTag
                }
                if (rawActualOutTag.isEmpty()) continue
                val actualOutTag = when {
                    rawActualOutTag.equals("direct", ignoreCase = true) -> "direct"
                    rawActualOutTag.equals("block", ignoreCase = true) -> "block"
                    rawActualOutTag.equals("dns", ignoreCase = true) -> "dns"
                    else -> rawActualOutTag
                }

                val sbRule = JSONObject()
                var hasContent = false

                val domains = xRule.optJSONArray("domain")
                if (domains != null && domains.length() > 0) {
                    val domainSuffixes  = JSONArray()
                    val domainExact     = JSONArray()
                    val domainRegex     = JSONArray()
                    val domainKeywords  = JSONArray()

                    for (j in 0 until domains.length()) {
                        val d = domains.optString(j, "")
                        when {
                            d.startsWith("geosite:") -> {
                                val gs = d.removePrefix("geosite:")
                                if (gs == "category-ru" || gs == "ru") {
                                    requiredRuleSets.add("geosite-ru")
                                    routingRulesObjects.add(JSONObject().apply {
                                        put("rule_set", "geosite-ru"); put("outbound", actualOutTag)
                                    })
                                    if (actualOutTag == "direct" || actualOutTag == "block") directRuleSets.add("geosite-ru")
                                } else {
                                    Log.d("V2RayConfigConverter", "Skipping unbundled geosite: $gs")
                                }
                            }
                            d.startsWith("domain:") -> {
                                val dom = d.removePrefix("domain:")
                                if (dom.isNotEmpty()) {
                                    domainSuffixes.put(dom)
                                    if (actualOutTag == "direct" || actualOutTag == "block") directDomains.put(dom)
                                }
                            }
                            d.startsWith("full:") -> {
                                val dom = d.removePrefix("full:")
                                if (dom.isNotEmpty()) domainExact.put(dom)
                            }
                            d.startsWith("regexp:") -> {
                                val dom = d.removePrefix("regexp:")
                                if (dom.isNotEmpty()) domainRegex.put(dom)
                            }
                            d.startsWith("keyword:") -> {
                                val dom = d.removePrefix("keyword:")
                                if (dom.isNotEmpty()) domainKeywords.put(dom)
                            }
                            d.isNotEmpty() -> {
                                domainSuffixes.put(d)
                                if (actualOutTag == "direct" || actualOutTag == "block") directDomains.put(d)
                            }
                        }
                    }
                    if (domainSuffixes.length()  > 0) { sbRule.put("domain_suffix",  domainSuffixes);  hasContent = true }
                    if (domainExact.length()     > 0) { sbRule.put("domain",          domainExact);     hasContent = true }
                    if (domainRegex.length()     > 0) { sbRule.put("domain_regex",    domainRegex);     hasContent = true }
                    if (domainKeywords.length()  > 0) { sbRule.put("domain_keyword",  domainKeywords);  hasContent = true }
                }

                val ips = xRule.optJSONArray("ip")
                if (ips != null && ips.length() > 0) {
                    val rawIps = JSONArray()
                    var hasPrivate = false
                    for (j in 0 until ips.length()) {
                        val ip = ips.optString(j, "")
                        when {
                            ip == "geoip:private" -> hasPrivate = true
                            ip.startsWith("geoip:") -> {
                                val gi = ip.removePrefix("geoip:")
                                if (gi == "ru") {
                                    requiredRuleSets.add("geoip-ru")
                                    routingRulesObjects.add(JSONObject().apply {
                                        put("rule_set", "geoip-ru"); put("outbound", actualOutTag)
                                    })
                                } else {
                                    Log.d("V2RayConfigConverter", "Skipping unbundled geoip: $gi")
                                }
                            }
                            ip.isNotEmpty() -> rawIps.put(ip)
                        }
                    }
                    if (hasPrivate)           { sbRule.put("ip_is_private", true); hasContent = true }
                    if (rawIps.length() > 0)  { sbRule.put("ip_cidr", rawIps);     hasContent = true }
                }

                val port = xRule.optString("port", "")
                if (port.isNotEmpty()) {
                    val portInts   = JSONArray()
                    val portRanges = JSONArray()
                    for (p in port.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
                        if (p.contains("-")) portRanges.put(p)
                        else p.toIntOrNull()?.let { portInts.put(it) }
                    }
                    if (portInts.length()   > 0) { sbRule.put("port",       portInts);   hasContent = true }
                    if (portRanges.length() > 0) { sbRule.put("port_range", portRanges); hasContent = true }
                }

                val network = xRule.optString("network", "")
                if (network.isNotEmpty()) {
                    if (network.contains(",")) {
                        val netArray = JSONArray()
                        network.split(",").forEach { netArray.put(it.trim()) }
                        sbRule.put("network", netArray)
                    } else {
                        sbRule.put("network", network.trim())
                    }
                    hasContent = true
                }

                val protocol = xRule.optString("protocol", "")
                if (protocol.isNotEmpty()) {
                    if (protocol.trim().startsWith("[")) {
                        
                        try {
                            sbRule.put("protocol", JSONArray(protocol))
                        } catch (e: Exception) {
                    sbRule.put("protocol", JSONArray(protocol.split(",").map { it.trim() }))
                        }
                    } else {
                        sbRule.put("protocol", JSONArray(protocol.split(",").map { it.trim() }))
                    }
                    hasContent = true
                }

                if (hasContent) {
                    sbRule.put("outbound", actualOutTag)
                    routingRulesObjects.add(sbRule)
                }
            }
        }

        var primaryDns = "https://1.1.1.1/dns-query"
        var directDns = "8.8.8.8"
        var strategy = "prefer_ipv4"
        val xrayDns = xray.optJSONObject("dns")
        if (xrayDns != null) {
            strategy = when (xrayDns.optString("queryStrategy", "")) {
                "UseIPv4" -> "ipv4_only"
                "UseIPv6" -> "ipv6_only"
                "UseIP"   -> "prefer_ipv4"
                else      -> "prefer_ipv4"
            }
            val servers = xrayDns.optJSONArray("servers")
            if (servers != null && servers.length() > 0) {
                fun extractAddr(s: Any?): String = when (s) {
                    is JSONObject -> s.optString("address", "")
                    is String    -> s
                    else         -> ""
                }
                val first = extractAddr(servers.opt(0))
                if (first.isNotEmpty()) primaryDns = first.replace("+local://", "://")
                for (i in 1 until servers.length()) {
                    val addr = extractAddr(servers.opt(i))
                    if (addr.isNotEmpty() && !addr.startsWith("localhost") && !addr.replace("+local://", "://").startsWith("https://")) {
                        directDns = addr.replace("+local://", "://")
                        break
                    }
                }
            }
        }

        val sbDnsServers = JSONArray()
        
        sbDnsServers.put(migrateDnsServerObject(JSONObject().apply {
            put("tag", "dns-remote")
            put("address", primaryDns)
            put("domain_resolver", "dns-direct")
            put("detour", findPrimaryProxyTag(sbOutbounds))
        }))
        
        sbDnsServers.put(migrateDnsServerObject(JSONObject().apply {
            put("tag", "dns-direct")
            put("address", directDns)
            put("detour", "direct")
        }))

        val sbDnsRules = JSONArray()

        sbDnsRules.put(JSONObject().apply {
            put("outbound", JSONArray().put("direct"))
            put("server", "dns-direct")
        })

        val servers = xrayDns?.optJSONArray("servers")
        if (servers != null) {
            for (i in 0 until servers.length()) {
                val s = servers.opt(i)
                if (s is JSONObject) {
                    val addr = s.optString("address", "").replace("+local://", "://")
                    val port = s.optInt("port", 53)
                    val domains = s.optJSONArray("domains")
                    if (domains != null && domains.length() > 0) {
                        val tag = "dns-custom-$i"
                        sbDnsServers.put(migrateDnsServerObject(JSONObject().apply {
                            put("tag", tag)
                            put("address", addr)
                            if (port != 53 && port > 0) put("port", port)
                            put("detour", "direct")
                        }))
                        
                        val dnsRule = JSONObject().apply { put("server", tag) }
                        val dnsDomainExact = JSONArray()
                        val dnsDomainSuffixes = JSONArray()
                        val dnsRuleSets = JSONArray()
                        for (j in 0 until domains.length()) {
                            val d = domains.optString(j, "")
                            when {
                                d.startsWith("geosite:") -> {
                                    val gs = d.removePrefix("geosite:")
                                    if (gs == "category-ru" || gs == "ru") {
                                        requiredRuleSets.add("geosite-ru")
                                        dnsRuleSets.put("geosite-ru")
                                    }
                                }
                                d.startsWith("domain:") -> {
                                    val dom = d.removePrefix("domain:")
                                    if (dom.isNotEmpty()) dnsDomainSuffixes.put(dom)
                                }
                                d.startsWith("full:") -> {
                                    val dom = d.removePrefix("full:")
                                    if (dom.isNotEmpty()) dnsDomainExact.put(dom)
                                }
                                d.isNotEmpty() -> {
                                    dnsDomainSuffixes.put(d)
                                }
                            }
                        }
                        if (dnsDomainExact.length() > 0) dnsRule.put("domain", dnsDomainExact)
                        if (dnsDomainSuffixes.length() > 0) dnsRule.put("domain_suffix", dnsDomainSuffixes)
                        if (dnsRuleSets.length() > 0) dnsRule.put("rule_set", dnsRuleSets)
                        if (dnsDomainExact.length() > 0 || dnsDomainSuffixes.length() > 0 || dnsRuleSets.length() > 0) {
                            sbDnsRules.put(dnsRule)
                        }
                    }
                }
            }
        }

        val dnsDirectDomains = JSONArray()
        for (i in 0 until proxyDomains.length()) {
            dnsDirectDomains.put(proxyDomains.getString(i))
        }
        for (i in 0 until directDomains.length()) {
            dnsDirectDomains.put(directDomains.getString(i))
        }
        if (dnsDirectDomains.length() > 0) {
            sbDnsRules.put(JSONObject().apply {
                put("domain_suffix", dnsDirectDomains)
                put("server", "dns-direct")
            })
        }

        for (rs in directRuleSets) {
            sbDnsRules.put(JSONObject().apply {
                put("rule_set", rs)
                put("server", "dns-direct")
            })
        }

        val sbDns = JSONObject().apply {
            put("servers", sbDnsServers)
            put("rules", sbDnsRules)
            put("final", "dns-remote")
            put("strategy", strategy)
            put("independent_cache", true)
        }
        sb.put("dns", sbDns)

        val sbInbounds = JSONArray()
        sbInbounds.put(createTunInbound(xray))
        sb.put("inbounds", sbInbounds)

        sb.put("outbounds", sbOutbounds)


        val sbRoute =
                JSONObject().apply {
                    put("auto_detect_interface", false)
                    val primaryProxyTag = findPrimaryProxyTag(sbOutbounds)
                    put("final", primaryProxyTag)
                    val sbRules = JSONArray().apply {
                        put(JSONObject().apply { put("protocol", "dns"); put("action", "hijack-dns") })
                        put(JSONObject().apply { put("port", 53); put("action", "hijack-dns") })
                        put(JSONObject().apply { put("action", "sniff") })
                    }
                    for (rule in routingRulesObjects) {
                        sbRules.put(rule)
                    }

                    if (proxyDomains.length() > 0) {
                        sbRules.put(JSONObject().apply { put("domain", proxyDomains); put("outbound", "direct") })
                    }
                    val sbRuleSets = JSONArray()
                    for (rs in requiredRuleSets) {
                        sbRuleSets.put(
                                JSONObject().apply {
                                    put("tag", rs)
                                    put("type", "local")
                                    put("format", "binary")
                                    put("path", "$rs.srs")
                                }
                        )
                    }

                    put("rules", sbRules)
                    if (sbRuleSets.length() > 0) {
                        put("rule_set", sbRuleSets)
                    }
                }
        
        
        val finalPrimaryTag = findPrimaryProxyTag(sbOutbounds)
        val dns = sb.optJSONObject("dns")
        if (dns != null) {
            val servers = dns.optJSONArray("servers")
            if (servers != null) {
                for (i in 0 until servers.length()) {
                    val server = servers.optJSONObject(i) ?: continue
                    if (server.optString("tag") == "dns-remote") {
                        server.put("detour", finalPrimaryTag)
                    }
                }
            }
        }

        sb.put("route", sbRoute)

        return sb.toString(2).replace("\\/", "/")
    }

    fun convertOutboundsPublic(xrayOutbounds: JSONArray): JSONArray = convertOutbounds(xrayOutbounds)

    private fun convertOutbounds(xrayOutbounds: JSONArray): JSONArray {
        val sbOutbounds = JSONArray()
        val extraOutbounds = mutableListOf<JSONObject>()
        for (i in 0 until xrayOutbounds.length()) {
            val xrayOb = xrayOutbounds.optJSONObject(i) ?: continue
            val protocol = xrayOb.optString("protocol", "").lowercase(Locale.ROOT)
            val rawTag = xrayOb.optString("tag", "outbound-$i")
            val tag = when {
                rawTag.equals("direct", ignoreCase = true) -> "direct"
                rawTag.equals("block", ignoreCase = true) -> "block"
                rawTag.equals("dns", ignoreCase = true) -> "dns"
                else -> rawTag
            }
            val sbOb = JSONObject().apply { put("tag", tag) }

            when (protocol) {
                "vless" -> convertVless(xrayOb, sbOb)
                "vmess" -> convertVmess(xrayOb, sbOb)
                "trojan" -> convertTrojan(xrayOb, sbOb)
                "shadowsocks" -> convertShadowsocks(xrayOb, sbOb, extraOutbounds)
                "hysteria", "hy" -> {
                    val settings = xrayOb.optJSONObject("settings")
                    val streamSettings = xrayOb.optJSONObject("streamSettings")
                    val hysteriaSettings = streamSettings?.optJSONObject("hysteriaSettings")
                    val isVersion2 = (settings?.optInt("version", 1) == 2) || (hysteriaSettings?.optInt("version", 1) == 2)
                    if (isVersion2) {
                        convertHysteria2(xrayOb, sbOb)
                    } else {
                        convertHysteria(xrayOb, sbOb)
                    }
                }
                "hysteria2", "hy2" -> convertHysteria2(xrayOb, sbOb)
                "freedom" -> sbOb.put("type", "direct")
                "blackhole" -> sbOb.put("type", "block")
                "socks" -> convertSocks(xrayOb, sbOb)
                "http" -> convertHttp(xrayOb, sbOb)
                else -> continue
            }

            xrayOb.optJSONObject("mux")?.let { mux ->
                val flow = sbOb.optString("flow", "")
                val hasReality = sbOb.optJSONObject("tls")?.has("reality") ?: false
                val type = sbOb.optString("type")

                if (mux.optBoolean("enabled", false) && !flow.contains("vision") && !hasReality && type != "hysteria" && type != "hysteria2") {
                    sbOb.put(
                            "multiplex",
                            JSONObject().apply {
                                put("enabled", true)
                                put("protocol", "smux")
                                val conc = mux.optInt("concurrency", 8)
                                put("max_connections", if (conc <= 0) 8 else conc)
                                put("min_streams", 4)
                                put("max_streams", 64)
                            }
                    )
                }
            }

            val sockopt = xrayOb.optJSONObject("streamSettings")?.optJSONObject("sockopt")
            if (sockopt != null && sockopt.has("dialerProxy")) {
                val proxyTag = sockopt.optString("dialerProxy")
                if (proxyTag.isNotEmpty()) {
                    val normProxyTag = when {
                        proxyTag.equals("direct", ignoreCase = true) -> "direct"
                        proxyTag.equals("block", ignoreCase = true) -> "block"
                        proxyTag.equals("dns", ignoreCase = true) -> "dns"
                        else -> proxyTag
                    }
                    sbOb.put("detour", normProxyTag)
                }
            }

            sbOutbounds.put(sbOb)
        }
        for (extra in extraOutbounds) {
            sbOutbounds.put(extra)
        }
        return sbOutbounds
    }

    private fun convertVless(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "vless")
        val vnext =
                xrayOb.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0) ?: return
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return
        sbOb.put("server", vnext.optString("address"))
        sbOb.put("server_port", vnext.optInt("port"))
        sbOb.put("uuid", user.optString("id"))
        var flow = user.optString("flow", "")
        var pe = if (xrayOb.has("packet_encoding")) {
            xrayOb.optString("packet_encoding", "")
        } else {
            "xudp"
        }
        if (flow == "xtls-rprx-vision-udp443") {
            flow = "xtls-rprx-vision"
            pe = "xudp"
        }
        if (pe.isNotEmpty() && pe != "xudp" && pe != "packetaddr") {
            pe = "xudp"
        }
        sbOb.put("flow", flow)
        sbOb.put("packet_encoding", pe)
        xrayOb.optJSONObject("streamSettings")?.let { convertStreamSettings(it, sbOb) }
    }

    private fun convertVmess(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "vmess")
        val vnext =
                xrayOb.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0) ?: return
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return
        sbOb.put("server", vnext.optString("address"))
        sbOb.put("server_port", vnext.optInt("port"))
        sbOb.put("uuid", user.optString("id"))
        sbOb.put("security", user.optString("security", "auto"))
        sbOb.put("packet_encoding", "xudp")
        xrayOb.optJSONObject("streamSettings")?.let { convertStreamSettings(it, sbOb) }
    }

    private fun convertTrojan(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "trojan")
        val server =
                xrayOb.optJSONObject("settings")?.optJSONArray("servers")?.optJSONObject(0)
                        ?: return
        sbOb.put("server", server.optString("address"))
        sbOb.put("server_port", server.optInt("port"))
        sbOb.put("password", server.optString("password"))
        xrayOb.optJSONObject("streamSettings")?.let { convertStreamSettings(it, sbOb) }
    }

    private fun convertShadowsocks(xrayOb: JSONObject, sbOb: JSONObject, extraOutbounds: MutableList<JSONObject>) {
        sbOb.put("type", "shadowsocks")
        val server =
                xrayOb.optJSONObject("settings")?.optJSONArray("servers")?.optJSONObject(0)
                        ?: return
        sbOb.put("server", server.optString("address"))
        sbOb.put("server_port", server.optInt("port"))
        sbOb.put("method", server.optString("method"))
        sbOb.put("password", server.optString("password"))

        if (xrayOb.has("plugin")) {
            val plugin = xrayOb.optString("plugin")
            if (plugin == "shadowtls") {
                val rawOpts = xrayOb.optString("plugin_opts")
                val optsMap = rawOpts.split(";").associate { opt ->
                    val parts = opt.split("=", limit = 2)
                    if (parts.size == 2) {
                        parts[0].trim().lowercase() to parts[1].trim()
                    } else {
                        opt.trim().lowercase() to "true"
                    }
                }
                val tag = sbOb.optString("tag", "proxy")
                val tlsTag = "$tag-tls"

                val shadowTlsOb = JSONObject().apply {
                    put("type", "shadowtls")
                    put("tag", tlsTag)
                    put("server", server.optString("address"))
                    put("server_port", server.optInt("port"))

                    val versionStr = optsMap["version"] ?: "3"
                    val version = versionStr.toIntOrNull() ?: 3
                    put("version", version)

                    val password = optsMap["password"] ?: ""
                    if (password.isNotEmpty()) {
                        put("password", password)
                    }

                    val sniVal = optsMap["host"] ?: optsMap["sni"] ?: server.optString("address")
                    put("tls", JSONObject().apply {
                        put("enabled", true)
                        put("server_name", sanitizeSni(sniVal))
                    })
                }
                extraOutbounds.add(shadowTlsOb)
                sbOb.put("detour", tlsTag)
                return
            } else {
                sbOb.put("plugin", plugin)
                val rawOpts = xrayOb.optString("plugin_opts")
                val sanitizedOpts = if (rawOpts.contains("sni=")) {
                    rawOpts.split(";").map { opt ->
                        if (opt.startsWith("sni=")) {
                            val value = opt.substringAfter("sni=")
                            "sni=${sanitizeSni(value)}"
                        } else {
                            opt
                        }
                    }.joinToString(";")
                } else {
                    rawOpts
                }
                sbOb.put("plugin_opts", sanitizedOpts)
                return
            }
        }

        val stream = xrayOb.optJSONObject("streamSettings")
        if (stream != null) {
            val network = stream.optString("network", "tcp")
            val security = stream.optString("security", "none")
            if (network == "ws" || security == "tls") {
                sbOb.put("plugin", "v2ray-plugin")
                val opts = mutableListOf<String>()
                opts.add("mode=websocket")

                val wsSettings = stream.optJSONObject("wsSettings")
                val path = wsSettings?.optString("path", "/") ?: "/"
                opts.add("path=$path")

                val tlsSettings = if (security == "tls") stream.optJSONObject("tlsSettings") else null
                if (tlsSettings != null) {
                    opts.add("tls")
                    val serverName = tlsSettings.optString("serverName", "")
                    if (serverName.isNotEmpty()) {
                        opts.add("sni=${sanitizeSni(serverName)}")
                    }
                    val host = wsSettings?.optJSONObject("headers")?.optString("Host", "") ?: ""
                    if (host.isNotEmpty()) {
                        opts.add("host=$host")
                    }
                    val insecure = when {
                        tlsSettings.has("allowInsecure") -> tlsSettings.optBoolean("allowInsecure", false)
                        tlsSettings.has("insecure") -> tlsSettings.optBoolean("insecure", false)
                        tlsSettings.has("skipCertVerify") -> tlsSettings.optBoolean("skipCertVerify", false)
                        else -> false
                    }
                    if (insecure) {
                        opts.add("skipCertVerify")
                        opts.add("skip-cert-verify")
                    }
                } else {
                    val host = wsSettings?.optJSONObject("headers")?.optString("Host", "") ?: ""
                    if (host.isNotEmpty()) {
                        opts.add("host=$host")
                    }
                }
                sbOb.put("plugin_opts", opts.joinToString(";"))
            }
        }
    }

    private fun convertSocks(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "socks")
        val settings = xrayOb.optJSONObject("settings")
        val server = settings?.optJSONArray("servers")?.optJSONObject(0) ?: return
        sbOb.put("server", server.optString("address"))
        sbOb.put("server_port", server.optInt("port"))
        val userObj = server.optJSONArray("users")?.optJSONObject(0)
        if (userObj != null) {
            val user = userObj.optString("user", "")
            val pass = userObj.optString("pass", "")
            if (user.isNotEmpty()) sbOb.put("username", user)
            if (pass.isNotEmpty()) sbOb.put("password", pass)
        }
    }

    private fun convertHttp(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "http")
        val settings = xrayOb.optJSONObject("settings")
        val server = settings?.optJSONArray("servers")?.optJSONObject(0) ?: return
        sbOb.put("server", server.optString("address"))
        sbOb.put("server_port", server.optInt("port"))
        val userObj = server.optJSONArray("user")?.optJSONObject(0)
        if (userObj != null) {
            val user = userObj.optString("user", "")
            val pass = userObj.optString("pass", "")
            if (user.isNotEmpty()) sbOb.put("username", user)
            if (pass.isNotEmpty()) sbOb.put("password", pass)
        }
    }

    private fun convertHysteria(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "hysteria")
        val settings = xrayOb.optJSONObject("settings")
        var host = ""
        var port = 0
        var password = ""

        if (settings != null) {
            val servers = settings.optJSONArray("servers")
            if (servers != null && servers.length() > 0) {
                val server = servers.optJSONObject(0)
                if (server != null) {
                    host = server.optString("address", "")
                    port = server.optInt("port", 0)
                    password = server.optString("password", "")
                }
            }
            if (host.isEmpty()) {
                host = settings.optString("address", "")
            }
            if (port == 0) {
                port = settings.optInt("port", 0)
            }
            if (password.isEmpty()) {
                password = settings.optString("password", "")
            }
        }

        val streamSettings = xrayOb.optJSONObject("streamSettings")
        val hysteriaSettings = streamSettings?.optJSONObject("hysteriaSettings")
        if (password.isEmpty() && hysteriaSettings != null) {
            password = hysteriaSettings.optString("auth", "")
            if (password.isEmpty()) {
                password = hysteriaSettings.optString("auth_str", "")
            }
        }

        if (host.isNotEmpty()) {
            sbOb.put("server", host)
        }
        if (port > 0) {
            sbOb.put("server_port", port)
        }
        if (password.isNotEmpty()) {
            sbOb.put("auth_str", password)
        }

        var upMbps = 0
        var downMbps = 0
        var obfs = ""

        if (settings != null) {
            upMbps = settings.optInt("up_mbps", 0)
            if (upMbps == 0) {
                upMbps = settings.optInt("up", 0)
            }
            downMbps = settings.optInt("down_mbps", 0)
            if (downMbps == 0) {
                downMbps = settings.optInt("down", 0)
            }
            obfs = settings.optString("obfs", "")
        }

        if (hysteriaSettings != null) {
            if (upMbps == 0) {
                upMbps = hysteriaSettings.optInt("up_mbps", 0)
            }
            if (upMbps == 0) {
                upMbps = hysteriaSettings.optInt("up", 0)
            }
            if (downMbps == 0) {
                downMbps = hysteriaSettings.optInt("down_mbps", 0)
            }
            if (downMbps == 0) {
                downMbps = hysteriaSettings.optInt("down", 0)
            }
            if (obfs.isEmpty()) {
                obfs = hysteriaSettings.optString("obfs", "")
            }
        }

        if (upMbps <= 0) {
            upMbps = 100
        }
        if (downMbps <= 0) {
            downMbps = 100
        }
        sbOb.put("up_mbps", upMbps)
        sbOb.put("down_mbps", downMbps)
        if (obfs.isNotEmpty()) {
            sbOb.put("obfs", obfs)
        }

        streamSettings?.let { convertStreamSettings(it, sbOb) }

        val tls = sbOb.optJSONObject("tls")
        if (tls == null) {
            sbOb.put("tls", JSONObject().apply {
                put("enabled", true)
                if (host.isNotEmpty()) {
                    put("server_name", host)
                }
            })
        } else {
            if (!tls.has("enabled")) {
                tls.put("enabled", true)
            }
            if (!tls.has("server_name") && host.isNotEmpty()) {
                tls.put("server_name", host)
            }
        }
    }

    private fun convertHysteria2(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "hysteria2")
        val settings = xrayOb.optJSONObject("settings")
        var host = ""
        var port = 0
        var password = ""

        if (settings != null) {
            val servers = settings.optJSONArray("servers")
            if (servers != null && servers.length() > 0) {
                val server = servers.optJSONObject(0)
                if (server != null) {
                    host = server.optString("address", "")
                    port = server.optInt("port", 0)
                    password = server.optString("password", "")
                }
            }
            if (host.isEmpty()) {
                host = settings.optString("address", "")
            }
            if (port == 0) {
                port = settings.optInt("port", 0)
            }
            if (password.isEmpty()) {
                password = settings.optString("password", "")
            }
        }

        val streamSettings = xrayOb.optJSONObject("streamSettings")
        val hysteriaSettings = streamSettings?.optJSONObject("hysteriaSettings")
        if (password.isEmpty() && hysteriaSettings != null) {
            password = hysteriaSettings.optString("auth", "")
            if (password.isEmpty()) {
                password = hysteriaSettings.optString("password", "")
            }
        }

        if (host.isNotEmpty()) {
            sbOb.put("server", host)
        }
        if (port > 0) {
            sbOb.put("server_port", port)
        }
        if (password.isNotEmpty()) {
            sbOb.put("password", password)
        }

        val mport = settings?.optString("mport", "")
        if (!mport.isNullOrBlank()) {
            val portsArray = org.json.JSONArray()
            mport.split(",").map { it.trim().replace(Regex("[\\s-]+"), ":") }.filter { it.isNotEmpty() }.forEach {
                portsArray.put(it)
            }
            if (portsArray.length() > 0) {
                sbOb.put("server_ports", portsArray)
            }
        }

        val hopIntervalRaw = settings?.optString("hop_interval", "")?.trim() ?: ""
        if (hopIntervalRaw.isNotEmpty()) {
            val hopInterval = if (hopIntervalRaw.all { it.isDigit() }) "${hopIntervalRaw}s" else hopIntervalRaw
            sbOb.put("hop_interval", hopInterval)
        }

        var upMbps = 0
        var downMbps = 0

        if (settings != null) {
            upMbps = settings.optInt("up_mbps", 0)
            if (upMbps == 0) {
                upMbps = settings.optInt("up", 0)
            }
            downMbps = settings.optInt("down_mbps", 0)
            if (downMbps == 0) {
                downMbps = settings.optInt("down", 0)
            }
        }

        if (hysteriaSettings != null) {
            if (upMbps == 0) {
                upMbps = hysteriaSettings.optInt("up_mbps", 0)
            }
            if (upMbps == 0) {
                upMbps = hysteriaSettings.optInt("up", 0)
            }
            if (downMbps == 0) {
                downMbps = hysteriaSettings.optInt("down_mbps", 0)
            }
            if (downMbps == 0) {
                downMbps = hysteriaSettings.optInt("down", 0)
            }
        }

        if (upMbps > 0) {
            sbOb.put("up_mbps", upMbps)
        }
        if (downMbps > 0) {
            sbOb.put("down_mbps", downMbps)
        }

        val obfs = settings?.optJSONObject("obfs") ?: hysteriaSettings?.optJSONObject("obfs")
        obfs?.let { o ->
            val obfsType = o.optString("type", "")
            if (obfsType.isNotEmpty()) {
                sbOb.put("obfs", JSONObject().apply {
                    put("type", obfsType)
                    val password = o.optString("password", "")
                    if (password.isNotEmpty()) put("password", password)
                })
            }
        }

        streamSettings?.let { convertStreamSettings(it, sbOb) }

        val tls = sbOb.optJSONObject("tls")
        if (tls == null) {
            sbOb.put("tls", JSONObject().apply {
                put("enabled", true)
                if (host.isNotEmpty()) {
                    put("server_name", host)
                }
            })
        } else {
            if (!tls.has("enabled")) {
                tls.put("enabled", true)
            }
            if (!tls.has("server_name") && host.isNotEmpty()) {
                tls.put("server_name", host)
            }
        }
    }

    private fun convertStreamSettings(stream: JSONObject, sbOb: JSONObject) {
        val security = stream.optString("security", "none")
        val network = stream.optString("network", "tcp")

        if (security == "tls" || security == "reality") {
            val tls = JSONObject().apply { put("enabled", true) }
            val settings =
                    if (security == "tls") stream.optJSONObject("tlsSettings")
                    else stream.optJSONObject("realitySettings")

            settings?.let { s ->
                val sni = s.optString("serverName", "")
                if (sni.isNotEmpty()) tls.put("server_name", sanitizeSni(sni))
                val insecure = when {
                    s.has("allowInsecure") -> s.optBoolean("allowInsecure", false)
                    s.has("insecure") -> s.optBoolean("insecure", false)
                    s.has("skipCertVerify") -> s.optBoolean("skipCertVerify", false)
                    else -> false
                }
                if (insecure) tls.put("insecure", true)

                val pin = s.optString("pin", "")
                if (pin.isNotEmpty()) {
                    val cleanPin = pin.filterNot { it.isWhitespace() }
                        .substringAfter("sha256/")
                        .substringAfter("SHA256:")
                    
                    val base64Pin = if (cleanPin.length == 64 && cleanPin.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                        try {
                            val bytes = ByteArray(32)
                            for (j in 0 until 32) {
                                bytes[j] = cleanPin.substring(j * 2, j * 2 + 2).toInt(16).toByte()
                            }
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } catch (_: Exception) {
                            cleanPin
                        }
                    } else {
                        cleanPin
                    }
                    tls.put("certificate_public_key_sha256", JSONArray().put(base64Pin))
                }

                val alpnRaw = s.opt("alpn")
                val alpn = JSONArray()
                when (alpnRaw) {
                    is JSONArray -> {
                        for (i in 0 until alpnRaw.length()) {
                            val value = alpnRaw.optString(i, "")
                            if (value.isNotEmpty()) alpn.put(value)
                        }
                    }
                    is String -> {
                        alpnRaw.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { alpn.put(it) }
                    }
                }
                if (alpn.length() > 0) tls.put("alpn", alpn)

                val type = sbOb.optString("type")
                if (type != "hysteria" && type != "hysteria2") {
                val fp = s.optString("fingerprint", "chrome")
                val utlsObj =
                        JSONObject().apply {
                            put("enabled", true)
                            put("fingerprint", if (fp == "random") "chrome" else fp)
                        }
                tls.put("utls", utlsObj)
                }

                if (security == "reality") {
                    val realityObj =
                            JSONObject().apply {
                                put("enabled", true)
                                put("public_key", s.optString("publicKey"))
                                val shortId = s.optString("shortId", "")
                                put("short_id", shortId)
                            }
                    tls.put("reality", realityObj)
                }
            }
            sbOb.put("tls", tls)
        }

        when (network) {
            "ws" -> {
                val ws = stream.optJSONObject("wsSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "ws")
                            put("path", ws?.optString("path", "/"))
                            ws?.optJSONObject("headers")?.let { put("headers", it) }
                        }
                )
            }
            "kcp" -> {
                val kcp = stream.optJSONObject("kcpSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "kcp")
                            val seed = kcp?.optString("seed", "")
                            if (!seed.isNullOrEmpty()) put("seed", seed)
                            val mtu = kcp?.optInt("mtu", 0) ?: 0
                            if (mtu > 0) put("mtu", mtu)
                            val tti = kcp?.optInt("tti", 0) ?: 0
                            if (tti > 0) put("tti", tti)
                        }
                )
            }
            "quic" -> {
                val quic = stream.optJSONObject("quicSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "quic")
                            val key = quic?.optString("key", "")
                            if (!key.isNullOrEmpty()) put("key", key)
                            val security = quic?.optString("security", "none") ?: "none"
                            put("security", security)
                        }
                )
            }
            "grpc" -> {
                val grpc = stream.optJSONObject("grpcSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "grpc")
                            put("service_name", grpc?.optString("serviceName", ""))
                        }
                )
            }
            "xhttp" -> {
                val settings = stream.optJSONObject("xhttpSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "xhttp")
                            put("mode", settings?.optString("mode", "auto") ?: "auto")
                            put("path", settings?.optString("path", "/"))
                            
                            val headers = settings?.optJSONObject("headers")
                            if (headers != null) {
                                put("headers", headers)
                            }

                            val extra = settings?.optJSONObject("extra")
                            if (extra != null) {
                                val keys = extra.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    put(key, extra.get(key))
                                }
                            }

                            val hostOpt = settings?.opt("host")
                            if (hostOpt is JSONArray) {
                                if (hostOpt.length() > 0) {
                                    put("host", hostOpt.optString(0, ""))
                                } else {
                                    put("host", "")
                                }
                            } else if (hostOpt is String) {
                                put("host", hostOpt)
                            } else {
                                put("host", "")
                            }
                        }
                )
            }
            "httpUpgrade", "httpupgrade" -> {
                val settings = stream.optJSONObject("httpUpgradeSettings") ?: stream.optJSONObject("httpupgradeSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "httpupgrade")
                            put("path", settings?.optString("path", "/"))
                            val host = settings?.optString("host", "") ?: ""
                            if (host.isNotEmpty()) {
                                put("host", host)
                            } else {
                                put("host", "")
                            }
                        }
                )
            }
            "h2", "http" -> {
                val settings = stream.optJSONObject("httpSettings")
                sbOb.put(
                        "transport",
                        JSONObject().apply {
                            put("type", "http")
                            put("path", settings?.optString("path", "/"))
                            val host = settings?.optString("host", "") ?: ""
                            if (host.isNotEmpty()) {
                                put("host", JSONArray().put(host))
                            } else {
                                put("host", JSONArray().put(""))
                            }
                        }
                )
            }
        }
    }

    private fun createTunInbound(xray: JSONObject): JSONObject {
        var mtu = 1500
        var stack = "mixed"
        var ipv4Addr = "172.19.0.1/30"
        var ipv6Addr = "fdfe:dcba:9876::1/126"
        var sniffingEnabled = true

        xray.optJSONArray("inbounds")?.let { inbounds ->
            for (i in 0 until inbounds.length()) {
                val inb = inbounds.optJSONObject(i) ?: continue
                val inbType = inb.optString("type", inb.optString("protocol", ""))

                if (inbType == "tun") {
                    val srcMtu = inb.optInt("mtu", 0)
                    if (srcMtu > 0) mtu = srcMtu

                    val srcStack = inb.optString("stack", "")
                    if (srcStack.isNotEmpty()) stack = srcStack

                    val addrField = inb.opt("address")
                    when {
                        addrField is JSONArray && addrField.length() >= 2 -> {
                            val a0 = addrField.optString(0, "")
                            val a1 = addrField.optString(1, "")
                            if (a0.isNotEmpty()) ipv4Addr = a0
                            if (a1.isNotEmpty()) ipv6Addr = a1
                        }
                        addrField is JSONArray && addrField.length() == 1 -> {
                            val a0 = addrField.optString(0, "")
                            if (a0.isNotEmpty()) ipv4Addr = a0
                        }
                        addrField is String && addrField.isNotEmpty() -> ipv4Addr = addrField
                    }
                }

                if (inb.optJSONObject("sniffing")?.optBoolean("enabled", false) == true) {
                    sniffingEnabled = true
                }
            }
        }

        return JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("address", JSONArray().apply {
                put(ipv4Addr)
                put(ipv6Addr)
            })
            put("mtu", mtu)
            put("auto_route", true)
            put("strict_route", true)
            put("stack", stack)
        }
    }

    private fun hasOutbound(obs: JSONArray, type: String): Boolean {
        for (i in 0 until obs.length()) if (obs.optJSONObject(i)?.optString("type") == type)
                return true
        return false
    }

    private fun ensureOutbound(obs: JSONArray, tag: String) {
        for (i in 0 until obs.length()) if (obs.optJSONObject(i)?.optString("tag") == tag) return
        val type = if (tag == "block") "block" else "direct"
        obs.put(
                JSONObject().apply {
                    put("type", type)
                    put("tag", tag)
                }
        )
    }

    
    private fun sanitizeProtocolFields(rules: JSONArray) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val proto = rule.optJSONArray("protocol") ?: continue
            val fixed = JSONArray()
            var changed = false
            for (j in 0 until proto.length()) {
                val s = proto.optString(j, "")
                if (s.startsWith("[") && s.endsWith("]")) {
                    
                    try {
                        val inner = JSONArray(s)
                        for (k in 0 until inner.length()) {
                            val v = inner.optString(k, "")
                            if (v.isNotEmpty()) fixed.put(v)
                        }
                        changed = true
                    } catch (_: Exception) {
                        fixed.put(s)
                    }
                } else if (s.isNotEmpty()) {
                    fixed.put(s)
                }
            }
            if (changed) rule.put("protocol", fixed)
        }
    }

    
    private fun fixDnsRemoteDetour(obj: JSONObject) {
        val dns = obj.optJSONObject("dns") ?: return
        val servers = dns.optJSONArray("servers") ?: return
        val outbounds = obj.optJSONArray("outbounds") ?: return

        
        val hasProxyOutbound = (0 until outbounds.length()).any {
            outbounds.optJSONObject(it)?.optString("tag") == "proxy"
        }
        if (hasProxyOutbound) return

        val realProxyTag = findPrimaryProxyTag(outbounds)
        if (realProxyTag == "proxy") return

        Log.d("V2RayConfigConverter", "fixDnsRemoteDetour: replacing detour 'proxy' with '$realProxyTag'")
        for (i in 0 until servers.length()) {
            val server = servers.optJSONObject(i) ?: continue
            if (server.optString("detour") == "proxy") {
                server.put("detour", realProxyTag)
            }
        }
    }

    private fun findPrimaryProxyTag(outbounds: JSONArray): String {
        val generalTags = listOf("proxy", "auto", "default", "main", "select", "selector", "urltest")
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            if (type == "urltest" || type == "selector") {
                val tag = ob.optString("tag", "")
                if (tag.isNotEmpty() && generalTags.any { tag.equals(it, ignoreCase = true) }) {
                    return tag
                }
            }
        }
        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val tag = ob.optString("tag", "")
            if (tag.equals("proxy", ignoreCase = true)) {
                return tag
            }
        }
        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            if (type == "urltest" || type == "selector") {
                val tag = ob.optString("tag", "")
                if (tag.isNotEmpty()) return tag
            }
        }
        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            val tag = ob.optString("tag", "")
            if (tag.isNotEmpty() && type != "direct" && type != "block" && type != "dns" && type != "dns-out") {
                return tag
            }
        }
        return "proxy"
    }

    private fun fixSingBox(obj: JSONObject): String {
        val route = obj.optJSONObject("route") ?: JSONObject().also { obj.put("route", it) }
        
        if (obj.has("rule_set")) {
            val ruleSets = obj.optJSONArray("rule_set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("V2RayConfigConverter", "Moved rule_set from root to route")
            }
            obj.remove("rule_set")
        }

        if (obj.has("rule-set")) {
            val ruleSets = obj.optJSONArray("rule-set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("V2RayConfigConverter", "Moved rule-set from root to route as rule_set")
            }
            obj.remove("rule-set")
        }

        if (route.has("rule-set")) {
            val ruleSets = route.optJSONArray("rule-set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("V2RayConfigConverter", "Renamed rule-set to rule_set inside route")
            }
            route.remove("rule-set")
        }

        obj.optJSONArray("inbounds")?.let { inbs ->
            for (i in 0 until inbs.length()) {
                inbs.optJSONObject(i)?.takeIf { it.optString("type") == "tun" }?.apply {
                    put("auto_route", true)
                    put("strict_route", true)
                    remove("dns_hijack")
                    
                    remove("sniff")
                    remove("sniff_override_destination")
                }
            }
        }

        val dns = obj.optJSONObject("dns") ?: JSONObject().also { obj.put("dns", it) }
        val dnsServers = dns.optJSONArray("servers")
        val rcodeServersMap = mutableMapOf<String, String>()
        if (dnsServers != null) {
            val migratedServers = JSONArray()
            for (i in 0 until dnsServers.length()) {
                val server = dnsServers.get(i)
                val processedServer = when (server) {
                    is JSONObject -> {
                        val address = server.optString("address", "")
                        if (address.contains("+local://")) {
                            server.put("address", address.replace("+local://", "://"))
                            if (!server.has("detour")) {
                                server.put("detour", "direct")
                            }
                        }
                        server
                    }
                    is String -> {
                        if (server.contains("+local://")) {
                            server.replace("+local://", "://")
                        } else {
                            server
                        }
                    }
                    else -> server
                }
                
                if (processedServer is JSONObject) {
                    val address = processedServer.optString("address", "")
                    if (address.startsWith("rcode://", ignoreCase = true)) {
                        val tag = processedServer.optString("tag", "")
                        if (tag.isNotEmpty()) {
                            val rcode = address.substring(8).uppercase(Locale.ROOT)
                            val finalRcode = when (rcode) {
                                "SUCCESS" -> "NOERROR"
                                "NOERROR" -> "NOERROR"
                                "NXDOMAIN" -> "NXDOMAIN"
                                "REFUSED" -> "REFUSED"
                                "SERVFAIL" -> "SERVFAIL"
                                "FORMERR" -> "FORMERR"
                                "NOTIMP" -> "NOTIMP"
                                else -> "NOERROR"
                            }
                            rcodeServersMap[tag] = finalRcode
                        }
                        continue
                    }
                } else if (processedServer is String) {
                    if (processedServer.startsWith("rcode://", ignoreCase = true)) {
                        continue
                    }
                }
                
                val migrated = migrateDnsServer(processedServer)
                if (migrated != null) {
                    migratedServers.put(migrated)
                } else {
                    migratedServers.put(processedServer)
                }
            }
            dns.put("servers", migratedServers)
        }
        if (!dns.has("strategy")) {
            dns.put("strategy", "prefer_ipv4")
        }

        val dnsRules = dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
        if (rcodeServersMap.isNotEmpty()) {
            for (i in 0 until dnsRules.length()) {
                val rule = dnsRules.optJSONObject(i) ?: continue
                val targetServer = rule.optString("server", "")
                if (targetServer.isNotEmpty() && rcodeServersMap.containsKey(targetServer)) {
                    val rcode = rcodeServersMap[targetServer] ?: "NOERROR"
                    rule.remove("server")
                    rule.put("action", "predefined")
                    rule.put("rcode", rcode)
                }
            }
        }
        val dnsRulesStr = dnsRules.toString()

        val outbounds = obj.optJSONArray("outbounds")
        if (outbounds != null) {
            val proxyDomainsSet = linkedSetOf<String>()
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val type = ob.optString("type", "")
                if (type == "vless") {
                    val flow = ob.optString("flow", "")
                    if (flow == "xtls-rprx-vision-udp443") {
                        ob.put("flow", "xtls-rprx-vision")
                        ob.put("packet_encoding", "xudp")
                    }
                    val pe = ob.optString("packet_encoding", "")
                    if (pe.isNotEmpty() && pe != "xudp" && pe != "packetaddr") {
                        ob.put("packet_encoding", "xudp")
                    }
                }
                val server = ob.optString("server", "")
                if (server.isNotEmpty() && !server[0].isDigit() && !dnsRulesStr.contains(server)) {
                    proxyDomainsSet.add(server)
                }
            }
            val proxyDomains = JSONArray()
            proxyDomainsSet.forEach { proxyDomains.put(it) }
            if (proxyDomains.length() > 0) {
                val newDnsRules = JSONArray()
                newDnsRules.put(
                        JSONObject().apply {
                            put("domain", proxyDomains)
                            put("server", "dns-direct")
                        }
                )
                for (i in 0 until dnsRules.length()) newDnsRules.put(dnsRules.get(i))
                dns.put("rules", newDnsRules)
            }
        }

        
        fixDnsRemoteDetour(obj)

        if (route != null) {
            if (!route.has("auto_detect_interface")) {
                route.put("auto_detect_interface", false)
            }
            val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

            
            
            val actionRulesFromOriginal = JSONArray()
            val regularRules = JSONArray()
            for (i in 0 until rules.length()) {
                val rule = rules.optJSONObject(i) ?: continue
                val action = rule.optString("action", "")
                if (action == "hijack-dns") continue  
                if (action.isNotEmpty()) {
                    actionRulesFromOriginal.put(rule)  
                } else {
                    regularRules.put(rule)
                }
            }

            
            sanitizeProtocolFields(regularRules)

            val newRules = JSONArray()
            
            newRules.put(JSONObject().apply { put("protocol", "dns"); put("action", "hijack-dns") })
            newRules.put(JSONObject().apply { put("port", 53); put("action", "hijack-dns") })
            
            var hasSniff = false
            for (i in 0 until actionRulesFromOriginal.length()) {
                val r = actionRulesFromOriginal.optJSONObject(i) ?: continue
                if (r.optString("action") == "sniff") { hasSniff = true; break }
            }
            if (!hasSniff) newRules.put(JSONObject().apply { put("action", "sniff") })
            
            for (i in 0 until actionRulesFromOriginal.length()) {
                val r = actionRulesFromOriginal.optJSONObject(i) ?: continue
                if (r.optString("action") != "sniff") newRules.put(r)
            }
            
            for (i in 0 until regularRules.length()) newRules.put(regularRules.opt(i))

            route.put("rules", newRules)

            for (i in 0 until newRules.length()) {
                val rule = newRules.optJSONObject(i) ?: continue
                if (rule.has("geosite")) {
                    val gs = rule.optJSONArray("geosite")
                    if (gs != null) {
                        for (j in 0 until gs.length()) {
                            if (gs.optString(j) == "category-ru" || gs.optString(j) == "ru") {
                                rule.remove("geosite")
                                rule.put("rule_set", JSONArray().put("geosite-ru"))
                            }
                        }
                    }
                }
                if (rule.has("geoip")) {
                    val gi = rule.optJSONArray("geoip")
                    if (gi != null) {
                        for (j in 0 until gi.length()) {
                            if (gi.optString(j) == "ru") {
                                rule.remove("geoip")
                                rule.put("rule_set", JSONArray().put("geoip-ru"))
                            }
                        }
                    }
                }
            }

            val routeStr = route.toString()
            if (routeStr.contains("geosite-ru") || routeStr.contains("geoip-ru")) {
                val ruleSets =
                        route.optJSONArray("rule_set") ?: JSONArray().also { route.put("rule_set", it) }
                val tags = mutableSetOf<String>()
                for (i in 0 until ruleSets.length()) {
                    ruleSets.optJSONObject(i)?.optString("tag")?.let { tags.add(it) }
                }

                if (!tags.contains("geosite-ru")) {
                    Log.d("V2RayConfigConverter", "Adding missing geosite-ru rule_set definition")
                    ruleSets.put(
                            JSONObject().apply {
                                put("tag", "geosite-ru")
                                put("type", "local")
                                put("format", "binary")
                                put("path", "geosite-ru.srs")
                            }
                    )
                }
                if (!tags.contains("geoip-ru")) {
                    Log.d("V2RayConfigConverter", "Adding missing geoip-ru rule_set definition")
                    ruleSets.put(
                            JSONObject().apply {
                                put("tag", "geoip-ru")
                                put("type", "local")
                                put("format", "binary")
                                put("path", "geoip-ru.srs")
                            }
                    )
                }
            }
        }


        return obj.toString(2).replace("\\/", "/")
    }

    private fun sanitizeSni(sni: String): String {
        if (sni.isBlank()) return ""
        var clean = sni.trim()
        if (clean.contains("://")) {
            clean = clean.substringAfter("://")
        }
        if (clean.contains("/")) {
            clean = clean.substringBefore("/")
        }
        if (clean.contains(":")) {
            clean = clean.substringBefore(":")
        }
        return clean
    }

    fun parseDnsAddress(address: String): JSONObject {
        val result = JSONObject()
        val cleanAddr = address.trim()
        when {
            cleanAddr.startsWith("https://", ignoreCase = true) -> {
                result.put("type", "https")
                val url = cleanAddr.substring(8)
                val slashIdx = url.indexOf('/')
                val hostPort = if (slashIdx != -1) url.substring(0, slashIdx) else url
                val path = if (slashIdx != -1) url.substring(slashIdx) else "/dns-query"
                
                val colonIdx = hostPort.lastIndexOf(':')
                val ipv6Bracket = hostPort.startsWith("[") && hostPort.contains("]")
                val host = if (ipv6Bracket) {
                    val endBracket = hostPort.indexOf(']')
                    hostPort.substring(1, endBracket)
                } else if (colonIdx != -1 && hostPort.indexOf(':', colonIdx + 1) == -1) {
                    hostPort.substring(0, colonIdx)
                } else {
                    hostPort
                }
                val port = if (ipv6Bracket) {
                    val endBracket = hostPort.indexOf(']')
                    val after = hostPort.substring(endBracket + 1)
                    if (after.startsWith(":")) after.substring(1).toIntOrNull() else null
                } else if (colonIdx != -1 && hostPort.indexOf(':', colonIdx + 1) == -1) {
                    hostPort.substring(colonIdx + 1).toIntOrNull()
                } else {
                    null
                }
                result.put("server", host)
                if (port != null) result.put("server_port", port)
                result.put("path", path)
            }
            cleanAddr.startsWith("tls://", ignoreCase = true) -> {
                result.put("type", "tls")
                val url = cleanAddr.substring(6)
                parseHostPort(url, result)
            }
            cleanAddr.startsWith("tcp://", ignoreCase = true) -> {
                result.put("type", "tcp")
                val url = cleanAddr.substring(6)
                parseHostPort(url, result)
            }
            cleanAddr.startsWith("quic://", ignoreCase = true) -> {
                result.put("type", "quic")
                val url = cleanAddr.substring(7)
                parseHostPort(url, result)
            }
            cleanAddr.startsWith("h3://", ignoreCase = true) -> {
                result.put("type", "h3")
                val url = cleanAddr.substring(5)
                val slashIdx = url.indexOf('/')
                val hostPort = if (slashIdx != -1) url.substring(0, slashIdx) else url
                val path = if (slashIdx != -1) url.substring(slashIdx) else "/dns-query"
                parseHostPort(hostPort, result)
                result.put("path", path)
            }
            cleanAddr.startsWith("rcode://", ignoreCase = true) -> {
                result.put("address", cleanAddr)
            }
            cleanAddr.equals("local", ignoreCase = true) -> {
                result.put("type", "local")
            }
            else -> {
                result.put("type", "udp")
                parseHostPort(cleanAddr, result)
            }
        }
        return result
    }

    private fun parseHostPort(hostPort: String, result: JSONObject) {
        val colonIdx = hostPort.lastIndexOf(':')
        val ipv6Bracket = hostPort.startsWith("[") && hostPort.contains("]")
        val host = if (ipv6Bracket) {
            val endBracket = hostPort.indexOf(']')
            hostPort.substring(1, endBracket)
        } else if (colonIdx != -1 && hostPort.indexOf(':', colonIdx + 1) == -1) {
            hostPort.substring(0, colonIdx)
        } else {
            hostPort
        }
        val port = if (ipv6Bracket) {
            val endBracket = hostPort.indexOf(']')
            val after = hostPort.substring(endBracket + 1)
            if (after.startsWith(":")) after.substring(1).toIntOrNull() else null
        } else if (colonIdx != -1 && colonIdx > hostPort.indexOf('[')) {
            hostPort.substring(colonIdx + 1).toIntOrNull()
        } else {
            null
        }
        result.put("server", host)
        if (port != null) result.put("server_port", port)
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.isEmpty()) return false
        if (host.contains(":")) return true
        val parts = host.split(".")
        return parts.size == 4 && parts.all { it.toIntOrNull() != null }
    }

    fun migrateDnsServerObject(serverObj: JSONObject): JSONObject {
        if (serverObj.has("port")) {
            val port = serverObj.opt("port")
            if (port != null) {
                serverObj.put("server_port", port)
            }
            serverObj.remove("port")
        }
        if (serverObj.has("address_resolver")) {
            val ar = serverObj.opt("address_resolver")
            if (ar != null) {
                serverObj.put("domain_resolver", ar)
            }
            serverObj.remove("address_resolver")
        }
        if (serverObj.optString("detour", "") == "direct") {
            serverObj.remove("detour")
        }
        val address = serverObj.optString("address", "")
        if (address.startsWith("rcode://", ignoreCase = true)) {
            return serverObj
        }
        if (serverObj.has("type") && serverObj.has("server")) {
            serverObj.remove("address")
            return serverObj
        }
        if (address.isEmpty()) {
            if (serverObj.has("type")) {
                return serverObj
            }
            return serverObj
        }
        val parsed = parseDnsAddress(address)
        val keys = parsed.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!serverObj.has(key)) {
                serverObj.put(key, parsed.get(key))
            }
        }
        serverObj.remove("address")
        
        val parsedServer = serverObj.optString("server", "")
        if (parsedServer.equals("localhost", ignoreCase = true)) {
            serverObj.put("server", "127.0.0.1")
        } else if (parsedServer.isNotEmpty() && !isIpAddress(parsedServer) && !serverObj.has("domain_resolver")) {
            val tag = serverObj.optString("tag", "")
            if (tag != "dns-direct") {
                serverObj.put("domain_resolver", "dns-direct")
            }
        }
        
        return serverObj
    }

    fun migrateDnsServer(server: Any?): JSONObject? {
        if (server == null) return null
        return when (server) {
            is JSONObject -> migrateDnsServerObject(server)
            is String -> {
                if (server.startsWith("rcode://", ignoreCase = true)) {
                    JSONObject().put("address", server)
                } else {
                    parseDnsAddress(server)
                }
            }
            else -> null
        }
    }
}
