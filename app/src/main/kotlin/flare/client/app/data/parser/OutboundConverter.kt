package flare.client.app.data.parser

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object OutboundConverter {

    fun convertOutboundsPublic(xrayOutbounds: JSONArray): JSONArray = convertOutbounds(xrayOutbounds)

    fun convertOutbounds(xrayOutbounds: JSONArray): JSONArray {
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
                "vless" -> ProtocolConverters.convertVless(xrayOb, sbOb)
                "vmess" -> ProtocolConverters.convertVmess(xrayOb, sbOb)
                "trojan" -> ProtocolConverters.convertTrojan(xrayOb, sbOb)
                "shadowsocks" -> ProtocolConverters.convertShadowsocks(xrayOb, sbOb, extraOutbounds)
                "hysteria", "hy" -> {
                    val settings = xrayOb.optJSONObject("settings")
                    val streamSettings = xrayOb.optJSONObject("streamSettings")
                    val hysteriaSettings = streamSettings?.optJSONObject("hysteriaSettings")
                    val isVersion2 = (settings?.optInt("version", 1) == 2) || (hysteriaSettings?.optInt("version", 1) == 2)
                    if (isVersion2) {
                        ProtocolConverters.convertHysteria2(xrayOb, sbOb)
                    } else {
                        ProtocolConverters.convertHysteria(xrayOb, sbOb)
                    }
                }
                "hysteria2", "hy2" -> ProtocolConverters.convertHysteria2(xrayOb, sbOb)
                "freedom" -> sbOb.put("type", "direct")
                "blackhole" -> sbOb.put("type", "block")
                "socks" -> ProtocolConverters.convertSocks(xrayOb, sbOb)
                "http" -> ProtocolConverters.convertHttp(xrayOb, sbOb)
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

        for (i in 0 until sbOutbounds.length()) {
            val ob = sbOutbounds.optJSONObject(i) ?: continue
            val server = ob.optString("server", "")
            if (server.isNotEmpty() && !isIpAddress(server)) {
                ob.put("domain_resolver", "dns-direct")
            }
        }

        return sbOutbounds
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.isEmpty()) return false
        if (host.contains(":")) return true
        val parts = host.split(".")
        return parts.size == 4 && parts.all { it.toIntOrNull() != null }
    }

    fun hasOutbound(obs: JSONArray, type: String): Boolean {
        for (i in 0 until obs.length()) if (obs.optJSONObject(i)?.optString("type") == type)
            return true
        return false
    }

    fun ensureOutbound(obs: JSONArray, tag: String) {
        for (i in 0 until obs.length()) if (obs.optJSONObject(i)?.optString("tag") == tag) return
        val type = if (tag == "block") "block" else "direct"
        obs.put(
            JSONObject().apply {
                put("type", type)
                put("tag", tag)
            }
        )
    }

    fun findPrimaryProxyTag(outbounds: JSONArray): String {
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
}
