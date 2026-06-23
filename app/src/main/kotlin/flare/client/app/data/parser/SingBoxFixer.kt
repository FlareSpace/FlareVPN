package flare.client.app.data.parser

import android.util.Log
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object SingBoxFixer {

    fun fixSingBox(obj: JSONObject): String {
        val route = obj.optJSONObject("route") ?: JSONObject().also { obj.put("route", it) }
        
        if (obj.has("rule_set")) {
            val ruleSets = obj.optJSONArray("rule_set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("SingBoxFixer", "Moved rule_set from root to route")
            }
            obj.remove("rule_set")
        }

        if (obj.has("rule-set")) {
            val ruleSets = obj.optJSONArray("rule-set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("SingBoxFixer", "Moved rule-set from root to route as rule_set")
            }
            obj.remove("rule-set")
        }

        if (route.has("rule-set")) {
            val ruleSets = route.optJSONArray("rule-set")
            if (!route.has("rule_set")) {
                route.put("rule_set", ruleSets)
                Log.d("SingBoxFixer", "Renamed rule-set to rule_set inside route")
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
                
                val migrated = DnsConverter.migrateDnsServer(processedServer)
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

        
        DnsConverter.fixDnsRemoteDetour(obj)

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
                Log.d("SingBoxFixer", "Adding missing geosite-ru rule_set definition")
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
                Log.d("SingBoxFixer", "Adding missing geoip-ru rule_set definition")
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


        return obj.toString(2).replace("\\/", "/")
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
}
