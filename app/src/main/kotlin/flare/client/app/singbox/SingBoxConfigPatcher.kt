package flare.client.app.singbox

import android.content.Context
import android.util.Log
import flare.client.app.data.SettingsManager
import flare.client.app.data.parser.V2RayConfigConverter
import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigPatcher {

    private const val TAG = "SingBoxConfigPatcher"

    fun patchConfig(
        configContent: String,
        context: Context,
        logFilePath: String?,
        clashSecret: String
    ): String {
        val settings = SettingsManager(context)
        var patchedConfig =
            if (logFilePath != null) {
                injectLogOutput(configContent, logFilePath, settings)
            } else configContent

        return injectAdvancedSettings(patchedConfig, context, clashSecret)
    }

    private fun injectLogOutput(configJson: String, logFilePath: String, settings: SettingsManager): String {
        return try {
            val obj = JSONObject(configJson)
            val log = obj.optJSONObject("log") ?: JSONObject()
            
            val level = settings.coreLogLevel
            if (settings.isCoreLogEnabled && level != "none") {
                log.put("disabled", false)
                log.put("level", level)
                log.put("output", logFilePath)
            } else {
                log.put("disabled", true)
                log.remove("level")
                log.remove("output")
            }
            
            obj.put("log", log)
            obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.w(TAG, "injectLogOutput failed (non-fatal): ${e.message}")
            configJson
        }
    }

    private fun injectAdvancedSettings(configJson: String, context: Context, clashSecret: String): String {
        return try {
            val settings = SettingsManager(context)
            val obj = JSONObject(configJson)

            sanitizeOutboundTags(obj)

            run {
                val route = obj.optJSONObject("route")
                val ruleSets = route?.optJSONArray("rule_set")
                if (ruleSets != null) {
                    val filesDir = context.filesDir.absolutePath
                    for (i in 0 until ruleSets.length()) {
                        val rs = ruleSets.optJSONObject(i) ?: continue
                        val path = rs.optString("path", "")
                        if (path.isNotEmpty() && !path.startsWith("/") && !path.startsWith("http")) {
                            rs.put("path", "$filesDir/$path")
                        }
                    }
                }
            }

            val experimental = obj.optJSONObject("experimental") ?: JSONObject().also { obj.put("experimental", it) }
            val clashApi = experimental.optJSONObject("clash_api") ?: JSONObject().also { experimental.put("clash_api", it) }
            clashApi.put("external_controller", "127.0.0.1:9092")
            if (clashSecret.isNotEmpty()) {
                clashApi.put("secret", clashSecret)
            }

            val outboundsArr = obj.optJSONArray("outbounds") ?: JSONArray()
            val primaryProxyTag = SingBoxConfigChainer.findPrimaryProxyTag(outboundsArr)

            run {
                val dns = obj.optJSONObject("dns")
                val servers = dns?.optJSONArray("servers")
                val outbounds = obj.optJSONArray("outbounds")
                if (dns != null && servers != null && outbounds != null) {
                    val hasProxyOutbound = (0 until outbounds.length()).any {
                        outbounds.optJSONObject(it)?.optString("tag") == "proxy"
                    }
                    if (!hasProxyOutbound) {
                        val realProxyTag = SingBoxConfigChainer.findPrimaryProxyTag(outbounds)
                        if (realProxyTag != "proxy") {
                            Log.i(TAG, "injectAdvancedSettings: fixing dns-remote detour 'proxy' → '$realProxyTag'")
                            for (i in 0 until servers.length()) {
                                val server = servers.optJSONObject(i) ?: continue
                                if (server.optString("detour") == "proxy") {
                                    server.put("detour", realProxyTag)
                                }
                            }
                        }
                    }
                }
            }

            if (settings.isSplitTunnelingEnabled && settings.splitTunnelingSites.isNotEmpty()) {
                val modeSites = settings.splitTunnelingModeSites
                val sites = settings.splitTunnelingSites.toList()
                val domainsToAdd = sites.toHashSet()
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

                    val proxyTag = SingBoxConfigChainer.findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = if (modeSites == "whitelist") proxyTag else "direct"

                    val actionRules = JSONArray()
                    val routingRules = mutableListOf<JSONObject>()
                    for (i in 0 until rules.length()) {
                        val rule = rules.optJSONObject(i) ?: continue
                        if (rule.has("action")) actionRules.put(rule)
                        else routingRules.add(rule)
                    }

                    var merged = false
                    for (rule in routingRules) {
                        val ruleOutbound = rule.optString("outbound", "")
                        val hasDomainField = rule.has("domain_suffix") || rule.has("domain")
                        
                        val isPureDomainRule = hasDomainField &&
                            !rule.has("rule_set") && !rule.has("ip_is_private") &&
                            !rule.has("port") && !rule.has("network") && !rule.has("process_name")
                        if (ruleOutbound == targetOutbound && isPureDomainRule) {
                            val existing = rule.optJSONArray("domain_suffix") ?: JSONArray()
                            val existingSet = (0 until existing.length()).map { existing.optString(it) }.toSet()
                            domainsToAdd.forEach { if (it !in existingSet) existing.put(it) }
                            rule.put("domain_suffix", existing)
                            
                            if (rule.has("domain") && !rule.has("domain_suffix")) {
                                rule.put("domain_suffix", existing)
                            }
                            merged = true
                            Log.i(TAG, "injectAdvancedSettings: merged sites into existing '$targetOutbound' rule")
                            break
                        }
                    }

                    if (!merged) {
                        val siteRule = JSONObject().apply {
                            put("domain_suffix", JSONArray().also { sites.forEach(it::put) })
                            put("outbound", targetOutbound)
                        }
                        routingRules.add(0, siteRule)
                        Log.i(TAG, "injectAdvancedSettings: created new '$targetOutbound' domain rule")
                    }

                    route.put("final", if (modeSites == "whitelist") "direct" else proxyTag)

                    val newRules = JSONArray()
                    for (i in 0 until actionRules.length()) newRules.put(actionRules.opt(i))
                    routingRules.forEach { newRules.put(it) }
                    route.put("rules", newRules)

                    val dns = obj.optJSONObject("dns")
                    if (dns != null) {
                        val dnsRules = dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
                        val domainsArray = JSONArray().also { sites.forEach(it::put) }
                        val dnsRule = JSONObject().apply {
                            put("domain_suffix", domainsArray)
                            put("server", if (modeSites == "whitelist") "dns-remote" else "dns-direct")
                        }
                        
                        val newDnsRules = JSONArray()
                        var dnsInserted = false
                        for (i in 0 until dnsRules.length()) {
                            val dr = dnsRules.optJSONObject(i) ?: continue
                            newDnsRules.put(dr)
                            if (!dnsInserted) {
                                newDnsRules.put(dnsRule)
                                dnsInserted = true
                            }
                        }
                        if (!dnsInserted) newDnsRules.put(dnsRule)
                        dns.put("rules", newDnsRules)
                    }

                    Log.i(TAG, "injectAdvancedSettings: sites split tunneling done, mode=$modeSites, proxyTag=$proxyTag, merged=$merged, sites=$sites")
                }
            }
 
            if (settings.isRoutingMainEnabled) {
                val mode = settings.routingMainMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = SingBoxConfigChainer.findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    injectOrUpdateRuleSet(rules, route, "geoip-ru", targetOutbound, "geoip-ru.srs", context)
                    injectOrUpdateRuleSet(rules, route, "geosite-ru", targetOutbound, "geosite-ru.srs", context)
                    Log.i(TAG, "injectAdvancedSettings: Main routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingGlobalEnabled) {
                val mode = settings.routingGlobalMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = SingBoxConfigChainer.findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    injectOrUpdateRuleSet(rules, route, "geosite-global", targetOutbound, "geosite-global.srs", context)
                    Log.i(TAG, "injectAdvancedSettings: Global routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingMediaEnabled) {
                val mode = settings.routingMediaMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = SingBoxConfigChainer.findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    
                    listOf("youtube", "netflix", "twitch", "disney").forEach { tag ->
                        injectOrUpdateRuleSet(rules, route, "geosite-$tag", targetOutbound, "geosite-$tag.srs", context)
                    }
                    Log.i(TAG, "injectAdvancedSettings: Media routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingSocialEnabled) {
                val mode = settings.routingSocialMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = SingBoxConfigChainer.findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    
                    listOf("telegram", "facebook", "instagram", "twitter", "tiktok").forEach { tag ->
                        injectOrUpdateRuleSet(rules, route, "geosite-$tag", targetOutbound, "geosite-$tag.srs", context)
                    }
                    Log.i(TAG, "injectAdvancedSettings: Social routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isTlsSpoofEnabled) {
                val domain = settings.tlsSpoofDomain.trim()
                val method = settings.tlsSpoofMethod.trim()
                if (domain.isNotEmpty()) {
                    val route = obj.optJSONObject("route")
                    if (route != null) {
                        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                        val spoofRule = JSONObject().apply {
                            put("action", "route-options")
                            put("tls_spoof", domain)
                            if (method.isNotEmpty()) {
                                put("tls_spoof_method", method)
                            }
                            put("protocol", JSONArray().put("tls"))
                        }

                        val newRules = JSONArray()
                        newRules.put(spoofRule)
                        for (i in 0 until rules.length()) {
                            newRules.put(rules.opt(i))
                        }
                        route.put("rules", newRules)
                        Log.i(TAG, "injectAdvancedSettings: TLS Spoof rule injected: domain=$domain, method=$method")
                    }
                }
            }

            val fingerprint = settings.fingerprint
            if (fingerprint != "auto") {
                val outbounds = obj.optJSONArray("outbounds")
                if (outbounds != null) {
                    for (i in 0 until outbounds.length()) {
                        val ob = outbounds.optJSONObject(i) ?: continue
                        
                        val type = ob.optString("type")
                        if (type == "hysteria" || type == "hysteria2" || type == "tuic") {
                            continue
                        }
                        
                        val tls = ob.optJSONObject("tls")
                        if (tls != null) {
                            var utls = tls.optJSONObject("utls")
                            if (utls == null) {
                                utls = JSONObject().apply { put("enabled", true) }
                                tls.put("utls", utls)
                            } else {
                                utls.put("enabled", true)
                            }
                            utls.put("fingerprint", fingerprint)
                            Log.i(TAG, "injectAdvancedSettings: set utls fingerprint to $fingerprint for outbound '${ob.optString("tag")}'")
                        }
                    }
                }
            }

            val dnsUrl = when (settings.remoteDnsMode) {
                "cloudflare_doh" -> "https://1.1.1.1/dns-query"
                "adguard_doh" -> "https://94.140.14.14/dns-query"
                "google_dot" -> "tls://dns.google"
                "custom" -> settings.remoteDnsUrl
                else -> ""
            }
            if (dnsUrl.isNotBlank()) {
                val dns = obj.optJSONObject("dns")
                if (dns != null) {
                    val servers = dns.optJSONArray("servers")
                    if (servers != null) {
                        for (i in 0 until servers.length()) {
                            val server = servers.optJSONObject(i)
                            if (server != null && server.optString("tag") == "dns-remote") {
                                server.remove("type")
                                server.remove("server")
                                server.remove("server_port")
                                server.remove("path")
                                server.remove("responses")
                                server.remove("domain_resolver")
                                
                                server.put("address", dnsUrl)
                                V2RayConfigConverter.migrateDnsServerObject(server)
                                Log.i(
                                        TAG,
                                        "injectAdvancedSettings: overridden dns-remote address to $dnsUrl"
                                )
                                break
                            }
                        }
                    }
                }
            } else {
                val isDohEnabled = settings.isRemoteDnsDohEnabled
                val dns = obj.optJSONObject("dns")
                if (dns != null) {
                    val servers = dns.optJSONArray("servers")
                    if (servers != null) {
                        for (i in 0 until servers.length()) {
                            val server = servers.optJSONObject(i) ?: continue
                            if (server.optString("tag") == "dns-remote") {
                                val currentType = server.optString("type", "")
                                val currentServer = server.optString("server", "")
                                
                                if (isDohEnabled) {
                                    if (currentType == "udp" || currentType == "tcp") {
                                        server.remove("type")
                                        server.remove("server")
                                        server.remove("server_port")
                                        server.remove("path")
                                        server.remove("responses")
                                        server.remove("domain_resolver")
                                        server.put("address", "https://$currentServer/dns-query")
                                        V2RayConfigConverter.migrateDnsServerObject(server)
                                        Log.i(TAG, "injectAdvancedSettings: converted dns-remote to DoH")
                                    }
                                } else {
                                    if (currentType != "udp") {
                                        server.put("type", "udp")
                                        server.remove("path")
                                        Log.i(TAG, "injectAdvancedSettings: converted dns-remote to udp")
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }

            val mtuValue = settings.mtu.toIntOrNull() ?: 1500
            val stackValue = settings.tunStack
            val fakeIpEnabled = settings.isFakeIpEnabled
            val inbounds = obj.optJSONArray("inbounds")
            if (inbounds != null) {
                for (i in 0 until inbounds.length()) {
                    val inb = inbounds.optJSONObject(i) ?: continue
                    if (inb.optString("type") == "tun") {
                        inb.put("mtu", mtuValue)
                        inb.put("stack", stackValue)
                        Log.i(
                                TAG,
                                "injectAdvancedSettings: set TUN mtu=$mtuValue, stack=$stackValue"
                        )
                        break
                    }
                }
            }

            if (fakeIpEnabled) {
                val dns = obj.optJSONObject("dns")
                if (dns != null) {
                    dns.put("reverse_mapping", true)
                    val servers =
                            dns.optJSONArray("servers")
                                    ?: JSONArray().also { dns.put("servers", it) }

                    var hasFakeIp = false
                    for (i in 0 until servers.length()) {
                        if (servers.optJSONObject(i)?.optString("tag") == "dns-fakeip") {
                            hasFakeIp = true
                            break
                        }
                    }
                    if (!hasFakeIp) {
                        servers.put(
                                JSONObject().apply {
                                    put("type", "fakeip")
                                    put("tag", "dns-fakeip")
                                    put("inet4_range", "198.18.0.0/15")
                                    put("inet6_range", "fc00::/18")
                                }
                        )
                        Log.i(TAG, "injectAdvancedSettings: added dns-fakeip server")
                    }

                    val dnsRules =
                            dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
                    var hasQueryTypeRule = false
                    for (i in 0 until dnsRules.length()) {
                        val r = dnsRules.optJSONObject(i) ?: continue
                        val qt = r.optJSONArray("query_type")
                        if (qt != null && r.optString("server") == "dns-fakeip") {
                            hasQueryTypeRule = true
                            break
                        }
                    }
                    if (!hasQueryTypeRule) {
                        dnsRules.put(
                                JSONObject().apply {
                                    put("query_type", JSONArray().put("A").put("AAAA"))
                                    put("server", "dns-fakeip")
                                }
                        )
                        Log.i(TAG, "injectAdvancedSettings: added fakeip query_type DNS rule")
                    }
                } else {
                    Log.w(
                            TAG,
                            "injectAdvancedSettings: no dns section found, skipping fakeip injection"
                    )
                }
            }

            val outbounds =
                    obj.optJSONArray("outbounds") ?: return obj.toString().replace("\\/", "/")

            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val type = ob.optString("type")
                if (type != "urltest" && type != "selector" && type != "direct" && type != "block" && type != "dns") {
                    if (!ob.has("connect_timeout")) {
                        ob.put("connect_timeout", "5s")
                        Log.i(TAG, "injectAdvancedSettings: set connect_timeout to 5s for outbound '${ob.optString("tag")}'")
                    }
                }
            }

            if (settings.isFragmentationEnabled) {
                val intervalMs = settings.fragmentInterval.trim().toIntOrNull() ?: 10
                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(i) ?: continue
                    val tls = ob.optJSONObject("tls")
                    if (tls != null) {
                        tls.put("fragment", true)
                        tls.put("record_fragment", true)

                        if (settings.packetType != "disabled") {
                            tls.put("fragment_fallback_delay", "${intervalMs}ms")
                        }
                        
                        Log.i(TAG, "injectAdvancedSettings: fragment injected on '${ob.optString("tag")}'")
                    }
                }
            }

            if (settings.isMuxEnabled) {
                val maxStreams = settings.muxMaxStreams.toIntOrNull()?.coerceIn(1, 128) ?: 8
                val protocol = settings.muxProtocol.ifBlank { "smux" }
                val padding = settings.muxPadding

                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(i) ?: continue
                    val type = ob.optString("type")
                    if (type == "direct" || type == "block" || type == "dns" || type == "urltest" || type == "selector") continue
                    if (type == "hysteria" || type == "hysteria2") continue

                    val flow = ob.optString("flow", "")
                    val hasReality = ob.optJSONObject("tls")?.has("reality") ?: false

                    if (flow.contains("vision") || hasReality) {
                        continue
                    }

                    ob.put(
                            "multiplex",
                            JSONObject().apply {
                                put("enabled", true)
                                put("protocol", protocol)
                                put("max_connections", 4)
                                put("min_streams", 4)
                                put("max_streams", maxStreams)
                                if (protocol == "smux") {
                                    put("padding", padding)
                                }
                            }
                    )
                    Log.i(
                            TAG,
                            "injectAdvancedSettings: mux injected on '${ob.optString("tag")}' " +
                                    "(protocol=$protocol, max_streams=$maxStreams, padding=$padding)"
                    )
                }
            }

            obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.e(TAG, "injectAdvancedSettings failed: ${e.message}", e)
            configJson
        }
    }

    private fun sanitizeOutboundTags(obj: JSONObject) {
        val outbounds = obj.optJSONArray("outbounds") ?: return
        val seenTags = mutableSetOf<String>()

        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val tag = outbound.optString("tag", "")
            if (tag.isEmpty()) continue

            if (seenTags.contains(tag)) {
                var counter = 1
                var newTag = "${tag}_$counter"
                while (seenTags.contains(newTag)) {
                    counter++
                    newTag = "${tag}_$counter"
                }
                outbound.put("tag", newTag)
                Log.w(TAG, "Sanitizer: Duplicate tag found '$tag', renamed to '$newTag'")
                seenTags.add(newTag)
            } else {
                seenTags.add(tag)
            }
        }
    }

    private fun findActionRulesCount(rules: JSONArray): Int {
        var count = 0
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (rule.has("action")) count++ else break
        }
        return count
    }

    private fun injectOrUpdateRuleSet(
        rules: JSONArray,
        route: JSONObject,
        ruleSetTag: String,
        targetOutbound: String,
        srsFileName: String,
        context: Context
    ) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val rs = rule.optJSONArray("rule_set") ?: continue
            for (j in 0 until rs.length()) {
                if (rs.optString(j) == ruleSetTag) {
                    rule.put("outbound", targetOutbound)
                    Log.i(TAG, "injectOrUpdateRuleSet: updated existing '$ruleSetTag' → outbound=$targetOutbound")
                    ensureRuleSetDef(route, ruleSetTag, srsFileName, context)
                    return
                }
            }
        }
        
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("rule_set", JSONArray().put(ruleSetTag))
            put("outbound", targetOutbound)
        }
        
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))

        ensureRuleSetDef(route, ruleSetTag, srsFileName, context)
        Log.i(TAG, "injectOrUpdateRuleSet: inserted '$ruleSetTag' at pos=$insertPos, outbound=$targetOutbound")
    }

    private fun injectOrUpdatePrivateIpRule(rules: JSONArray, targetOutbound: String) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (rule.optBoolean("ip_is_private", false)) {
                rule.put("outbound", targetOutbound)
                Log.i(TAG, "injectOrUpdatePrivateIpRule: updated existing → outbound=$targetOutbound")
                return
            }
        }
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("ip_is_private", true)
            put("outbound", targetOutbound)
        }
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))
        Log.i(TAG, "injectOrUpdatePrivateIpRule: inserted at pos=$insertPos, outbound=$targetOutbound")
    }

    private fun injectOrUpdateProtocolRule(rules: JSONArray, protocol: String, targetOutbound: String) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val proto = rule.optJSONArray("protocol") ?: continue
            for (j in 0 until proto.length()) {
                if (proto.optString(j) == protocol) {
                    rule.put("outbound", targetOutbound)
                    Log.i(TAG, "injectOrUpdateProtocolRule: updated existing '$protocol' → outbound=$targetOutbound")
                    return
                }
            }
        }
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("protocol", JSONArray().put(protocol))
            put("outbound", targetOutbound)
        }
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))
        Log.i(TAG, "injectOrUpdateProtocolRule: inserted '$protocol' at pos=$insertPos, outbound=$targetOutbound")
    }

    private fun ensureRuleSetDef(route: JSONObject, tag: String, srsFileName: String, context: Context) {
        val ruleSets = route.optJSONArray("rule_set") ?: JSONArray().also { route.put("rule_set", it) }
        for (i in 0 until ruleSets.length()) {
            if (ruleSets.optJSONObject(i)?.optString("tag") == tag) return
        }
        ruleSets.put(JSONObject().apply {
            put("tag", tag)
            put("type", "local")
            put("format", "binary")
            val filesDir = context.filesDir.absolutePath
            val absolutePath = if (srsFileName.startsWith("/")) srsFileName else "$filesDir/$srsFileName"
            put("path", absolutePath)
        })
        Log.i(TAG, "ensureRuleSetDef: added definition for '$tag'")
    }
}
