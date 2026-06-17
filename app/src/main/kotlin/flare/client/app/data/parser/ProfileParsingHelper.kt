package flare.client.app.data.parser

import org.json.JSONArray
import org.json.JSONObject

object ProfileParsingHelper {
    fun parseTransportAndSecurityFromJson(configJson: String): String? {
        try {
            val json = JSONObject(configJson)
            val outbounds = json.optJSONArray("outbounds")
            val outbound = outbounds?.optJSONObject(0) ?: return null
            var transport: String? = null
            var security: String? = null

            val transportObj = outbound.optJSONObject("transport")
            val transportType = transportObj?.optString("type")
            if (!transportType.isNullOrBlank()) {
                transport = when (transportType.lowercase()) {
                    "tcp" -> "TCP"
                    "ws" -> "WS"
                    "grpc" -> "gRPC"
                    "httpupgrade" -> "HTTPUpgrade"
                    "h2" -> "H2"
                    "http" -> "HTTP"
                    "xhttp" -> "XHTTP"
                    "quic" -> "QUIC"
                    "kcp" -> "KCP"
                    else -> transportType.uppercase()
                }
            } else {
                val plugin = outbound.optString("plugin")
                if (plugin == "v2ray-plugin") {
                    val opts = outbound.optString("plugin_opts") ?: ""
                    if (opts.contains("mode=websocket") || opts.contains("websocket")) {
                        transport = "WS"
                    }
                } else if (plugin == "shadowtls") {
                    transport = "TCP"
                } else {
                    val type = outbound.optString("type").lowercase()
                    if (type == "vless" || type == "vmess" || type == "trojan" || type == "shadowsocks" || type == "shadowtls" || type == "socks" || type == "http") {
                        transport = "TCP"
                    }
                }
            }

            val tlsObj = outbound.optJSONObject("tls")
            if (tlsObj != null && tlsObj.optBoolean("enabled", false)) {
                val realityObj = tlsObj.optJSONObject("reality")
                security = if (realityObj != null && realityObj.optBoolean("enabled", false)) {
                    "REALITY"
                } else {
                    "TLS"
                }
            } else {
                val secVal = outbound.optString("security").lowercase()
                if (secVal == "tls") {
                    security = "TLS"
                } else if (secVal == "reality") {
                    security = "REALITY"
                }
            }

            val parts = mutableListOf<String>()
            if (!transport.isNullOrBlank()) parts.add(transport)
            if (!security.isNullOrBlank()) parts.add(security)
            return if (parts.isNotEmpty()) parts.joinToString(" / ") else null
        } catch (_: Exception) {
            return null
        }
    }
}
