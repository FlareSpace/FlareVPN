package flare.client.app.data.parser

import org.json.JSONArray
import org.json.JSONObject

object ProtocolConverters {

    fun convertVless(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "vless")
        val vnext =
            xrayOb.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0) ?: return
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return
        sbOb.put("server", vnext.optString("address"))
        sbOb.put("server_port", vnext.optInt("port"))
        sbOb.put("uuid", user.optString("id"))
        var flow = user.optString("flow", "").takeIf { it != "null" } ?: ""
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

    fun convertVmess(xrayOb: JSONObject, sbOb: JSONObject) {
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

    fun convertTrojan(xrayOb: JSONObject, sbOb: JSONObject) {
        sbOb.put("type", "trojan")
        val server =
            xrayOb.optJSONObject("settings")?.optJSONArray("servers")?.optJSONObject(0)
                ?: return
        sbOb.put("server", server.optString("address"))
        sbOb.put("server_port", server.optInt("port"))
        sbOb.put("password", server.optString("password"))
        xrayOb.optJSONObject("streamSettings")?.let { convertStreamSettings(it, sbOb) }
    }

    fun convertShadowsocks(xrayOb: JSONObject, sbOb: JSONObject, extraOutbounds: MutableList<JSONObject>) {
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

    fun convertSocks(xrayOb: JSONObject, sbOb: JSONObject) {
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

    fun convertHttp(xrayOb: JSONObject, sbOb: JSONObject) {
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

    fun convertHysteria(xrayOb: JSONObject, sbOb: JSONObject) {
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

    fun convertHysteria2(xrayOb: JSONObject, sbOb: JSONObject) {
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
}
