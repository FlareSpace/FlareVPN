package flare.client.app.data.parser

import org.json.JSONArray
import org.json.JSONObject

object V2RayConfigConverter {

    fun convertIfNeeded(json: String, isDoHEnabled: Boolean = true): String {
        val trimmed = json.trim()
        return try {
            val obj = JSONObject(trimmed)
            when {
                isSingBoxFormat(obj) -> SingBoxFixer.fixSingBox(obj)
                isV2RayFormat(obj) -> convertV2RayToSingBox(obj, isDoHEnabled)
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

    fun convertV2RayToSingBox(xray: JSONObject, isDoHEnabled: Boolean = true): String {
        val sb = JSONObject()

        sb.put(
            "log",
            JSONObject().apply {
                put("level", "info")
                put("timestamp", true)
            }
        )

        val xrayOutbounds = xray.optJSONArray("outbounds") ?: JSONArray()
        val sbOutbounds = OutboundConverter.convertOutbounds(xrayOutbounds)

        OutboundConverter.ensureOutbound(sbOutbounds, "direct")
        OutboundConverter.ensureOutbound(sbOutbounds, "block")

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

        var primaryDns = if (isDoHEnabled) "https://1.1.1.1/dns-query" else "1.1.1.1"
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
                if (first.isNotEmpty()) {
                    primaryDns = first.replace("+local://", "://")
                    if (isDoHEnabled && !primaryDns.startsWith("https://") && !primaryDns.startsWith("tls://") && !primaryDns.startsWith("quic://") && !primaryDns.startsWith("h3://") && !primaryDns.startsWith("tcp://")) {
                        primaryDns = "https://$primaryDns/dns-query"
                    }
                }
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
        
        sbDnsServers.put(DnsConverter.migrateDnsServerObject(JSONObject().apply {
            put("tag", "dns-remote")
            put("address", primaryDns)
            put("domain_resolver", "dns-direct")
            put("detour", OutboundConverter.findPrimaryProxyTag(sbOutbounds))
        }))
        
        sbDnsServers.put(DnsConverter.migrateDnsServerObject(JSONObject().apply {
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
                        sbDnsServers.put(DnsConverter.migrateDnsServerObject(JSONObject().apply {
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
        sbInbounds.put(InboundConverter.createTunInbound(xray))
        sb.put("inbounds", sbInbounds)

        sb.put("outbounds", sbOutbounds)

        val sbRoute =
            JSONObject().apply {
                put("auto_detect_interface", false)
                val primaryProxyTag = OutboundConverter.findPrimaryProxyTag(sbOutbounds)
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
                            put("path", "${rs}.srs")
                        }
                    )
                }

                put("rules", sbRules)
                if (sbRuleSets.length() > 0) {
                    put("rule_set", sbRuleSets)
                }
            }
        
        val finalPrimaryTag = OutboundConverter.findPrimaryProxyTag(sbOutbounds)
        val dnsObj = sb.optJSONObject("dns")
        if (dnsObj != null) {
            val serversObj = dnsObj.optJSONArray("servers")
            if (serversObj != null) {
                for (i in 0 until serversObj.length()) {
                    val server = serversObj.optJSONObject(i) ?: continue
                    if (server.optString("tag") == "dns-remote") {
                        server.put("detour", finalPrimaryTag)
                    }
                }
            }
        }

        sb.put("route", sbRoute)

        return sb.toString(2).replace("\\/", "/")
    }

    fun convertOutboundsPublic(xrayOutbounds: JSONArray): JSONArray = OutboundConverter.convertOutboundsPublic(xrayOutbounds)

    fun parseDnsAddress(address: String): JSONObject = DnsConverter.parseDnsAddress(address)

    fun migrateDnsServerObject(serverObj: JSONObject): JSONObject = DnsConverter.migrateDnsServerObject(serverObj)

    fun migrateDnsServer(server: Any?): JSONObject? = DnsConverter.migrateDnsServer(server)
}
