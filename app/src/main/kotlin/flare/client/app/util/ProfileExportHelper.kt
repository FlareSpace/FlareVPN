package flare.client.app.util

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import flare.client.app.data.model.ProfileEntity
import java.net.URLEncoder
import java.util.Locale

object ProfileExportHelper {

    fun exportLink(profile: ProfileEntity): String? {
        val trimmedUri = profile.uri.trim()
        if (trimmedUri.isNotEmpty() && !trimmedUri.startsWith("internal://")) {
            return if (profile.name.isNotBlank() && trimmedUri.contains("://")) {
                val baseUri = trimmedUri.substringBefore("#")
                "$baseUri#${URLEncoder.encode(profile.name, "UTF-8")}"
            } else {
                trimmedUri
            }
        }

        try {
            val config = JSONObject(profile.configJson)
            val outbounds = config.optJSONArray("outbounds")
            if (outbounds != null) {
                for (i in 0 until outbounds.length()) {
                    val outbound = outbounds.optJSONObject(i) ?: continue
                    val type = outbound.optString("type").lowercase(Locale.ROOT)
                    when (type) {
                        "hysteria2", "hy2" -> return generateHysteria2Link(outbound, profile.name)
                        "hysteria", "hy" -> return generateHysteriaLink(outbound, profile.name)
                        "vless" -> return generateVlessLink(outbound, profile.name)
                        "vmess" -> return generateVmessLink(outbound, profile.name)
                        "trojan" -> return generateTrojanLink(outbound, profile.name)
                        "shadowsocks", "ss" -> return generateShadowsocksLink(outbound, profile.name)
                        "wireguard", "wg" -> return generateWireGuardLink(outbound, profile.name)
                    }
                }
            }
            val endpoints = config.optJSONArray("endpoints")
            if (endpoints != null) {
                for (i in 0 until endpoints.length()) {
                    val endpoint = endpoints.optJSONObject(i) ?: continue
                    val type = endpoint.optString("type").lowercase(Locale.ROOT)
                    if (type == "wireguard" || type == "wg") {
                        return generateWireGuardLink(endpoint, profile.name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (trimmedUri.isNotEmpty()) {
            return trimmedUri
        }
        return null
    }

    private fun formatHost(host: String): String {
        return if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
    }

    private fun generateHysteria2Link(ob: JSONObject, name: String): String {
        val server = ob.optString("server").ifEmpty { ob.optString("address") }
        val port = ob.optInt("server_port", 0).let { if (it > 0) it else ob.optInt("port", 443) }
        val formattedServer = formatHost(server)
        val password = ob.optString("password").ifEmpty { ob.optString("auth").ifEmpty { ob.optString("auth_str") } }

        val authority = if (password.isNotEmpty()) {
            val encodedPw = URLEncoder.encode(password, "UTF-8").replace("+", "%20")
            "$encodedPw@$formattedServer:$port"
        } else {
            "$formattedServer:$port"
        }

        val sb = StringBuilder("hysteria2://$authority")
        val params = mutableMapOf<String, String>()

        val tls = ob.optJSONObject("tls")
        val sni = tls?.optString("server_name", "") ?: ""
        if (sni.isNotEmpty()) {
            params["sni"] = sni
        }

        val insecure = tls?.optBoolean("insecure", false) ?: false
        if (insecure) {
            params["insecure"] = "1"
        }

        var pin = tls?.optString("pin", "") ?: ""
        if (pin.isEmpty()) {
            val pinArr = tls?.optJSONArray("certificate_public_key_sha256")
            if (pinArr != null && pinArr.length() > 0) {
                pin = pinArr.optString(0)
            }
        }
        if (pin.isNotEmpty()) {
            params["pin"] = pin
        }

        val alpnArr = tls?.optJSONArray("alpn")
        if (alpnArr != null && alpnArr.length() > 0) {
            val alpnList = (0 until alpnArr.length()).map { alpnArr.optString(it) }.filter { it.isNotEmpty() }
            if (alpnList.isNotEmpty()) {
                params["alpn"] = alpnList.joinToString(",")
            }
        } else {
            val alpnStr = tls?.optString("alpn", "") ?: ""
            if (alpnStr.isNotEmpty()) {
                params["alpn"] = alpnStr
            }
        }

        val obfs = ob.optJSONObject("obfs")
        if (obfs != null) {
            val obfsType = obfs.optString("type", "")
            if (obfsType.isNotEmpty()) {
                params["obfs"] = obfsType
                val obfsPassword = obfs.optString("password", "")
                if (obfsPassword.isNotEmpty()) {
                    params["obfs-password"] = obfsPassword
                }
            }
        } else {
            val obfsStr = ob.optString("obfs", "")
            if (obfsStr.isNotEmpty()) {
                params["obfs"] = obfsStr
            }
        }

        val serverPorts = ob.opt("server_ports") ?: ob.opt("mport")
        when (serverPorts) {
            is JSONArray -> {
                val list = (0 until serverPorts.length()).map { serverPorts.optString(it) }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) {
                    params["mport"] = list.joinToString(",")
                }
            }
            is String -> {
                if (serverPorts.isNotEmpty()) {
                    params["mport"] = serverPorts
                }
            }
        }

        val hopInterval = ob.optString("hop_interval", "").ifEmpty { ob.optString("hop-interval", "") }
        if (hopInterval.isNotEmpty()) {
            params["hop_interval"] = hopInterval
        }

        val up = ob.optInt("up_mbps", 0).let { if (it > 0) it else ob.optInt("up", 0) }
        val down = ob.optInt("down_mbps", 0).let { if (it > 0) it else ob.optInt("down", 0) }
        if (up > 0) params["up"] = up.toString()
        if (down > 0) params["down"] = down.toString()

        if (params.isNotEmpty()) {
            val paramString = params.entries.joinToString("&") {
                "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
            }
            sb.append("?$paramString")
        }

        val displayName = name.ifEmpty { ob.optString("tag", "") }
        if (displayName.isNotEmpty() && displayName != "proxy") {
            sb.append("#${URLEncoder.encode(displayName, "UTF-8")}")
        }

        return sb.toString()
    }

    private fun generateHysteriaLink(ob: JSONObject, name: String): String {
        val server = ob.optString("server").ifEmpty { ob.optString("address") }
        val port = ob.optInt("server_port", 0).let { if (it > 0) it else ob.optInt("port", 443) }
        val formattedServer = formatHost(server)
        val auth = ob.optString("auth_str").ifEmpty { ob.optString("password").ifEmpty { ob.optString("auth") } }

        val authority = if (auth.isNotEmpty()) {
            val encodedAuth = URLEncoder.encode(auth, "UTF-8").replace("+", "%20")
            "$encodedAuth@$formattedServer:$port"
        } else {
            "$formattedServer:$port"
        }

        val sb = StringBuilder("hysteria://$authority")
        val params = mutableMapOf<String, String>()

        val tls = ob.optJSONObject("tls")
        val sni = tls?.optString("server_name", "") ?: ""
        if (sni.isNotEmpty()) {
            params["sni"] = sni
        }

        val insecure = tls?.optBoolean("insecure", false) ?: false
        if (insecure) {
            params["insecure"] = "1"
        }

        val alpnArr = tls?.optJSONArray("alpn")
        if (alpnArr != null && alpnArr.length() > 0) {
            val alpnList = (0 until alpnArr.length()).map { alpnArr.optString(it) }.filter { it.isNotEmpty() }
            if (alpnList.isNotEmpty()) {
                params["alpn"] = alpnList.joinToString(",")
            }
        } else {
            val alpnStr = tls?.optString("alpn", "") ?: ""
            if (alpnStr.isNotEmpty()) {
                params["alpn"] = alpnStr
            }
        }

        val obfs = ob.optString("obfs", "")
        if (obfs.isNotEmpty()) {
            params["obfs"] = obfs
        }

        val up = ob.optInt("up_mbps", 0).let { if (it > 0) it else ob.optInt("up", 0) }
        val down = ob.optInt("down_mbps", 0).let { if (it > 0) it else ob.optInt("down", 0) }
        if (up > 0) params["up"] = up.toString()
        if (down > 0) params["down"] = down.toString()

        if (params.isNotEmpty()) {
            val paramString = params.entries.joinToString("&") {
                "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
            }
            sb.append("?$paramString")
        }

        val displayName = name.ifEmpty { ob.optString("tag", "") }
        if (displayName.isNotEmpty() && displayName != "proxy") {
            sb.append("#${URLEncoder.encode(displayName, "UTF-8")}")
        }

        return sb.toString()
    }

    private fun generateShadowsocksLink(ob: JSONObject, name: String): String {
        val server = ob.optString("server").ifEmpty { ob.optString("address") }
        val port = ob.optInt("server_port", 0).let { if (it > 0) it else ob.optInt("port", 8388) }
        val formattedServer = formatHost(server)
        val method = ob.optString("method")
        val password = ob.optString("password")
        val userInfo = "$method:$password"
        val encodedUserInfo = Base64.encodeToString(userInfo.toByteArray(), Base64.NO_WRAP)
        val sb = StringBuilder("ss://$encodedUserInfo@$formattedServer:$port")

        val plugin = ob.optString("plugin", "")
        val pluginOpts = ob.optString("plugin_opts", "")
        if (plugin.isNotEmpty()) {
            val combined = if (pluginOpts.isNotEmpty()) "$plugin;$pluginOpts" else plugin
            sb.append("?plugin=${URLEncoder.encode(combined, "UTF-8")}")
        }

        val displayName = name.ifEmpty { ob.optString("tag", "") }
        if (displayName.isNotEmpty() && displayName != "proxy") {
            sb.append("#${URLEncoder.encode(displayName, "UTF-8")}")
        }
        return sb.toString()
    }

    private fun generateWireGuardLink(ob: JSONObject, name: String): String {
        val privateKey = ob.optString("private_key")
        val peers = ob.optJSONArray("peers")
        val peer = peers?.optJSONObject(0)
        val server = (peer?.optString("address") ?: "").ifEmpty { ob.optString("server", "") }
        val port = peer?.optInt("port", 0)?.let { if (it > 0) it else null } ?: ob.optInt("server_port", 51820)
        val formattedServer = formatHost(server)
        val publicKey = peer?.optString("public_key", "") ?: ""
        val addressArr = ob.optJSONArray("address")
        val address = if (addressArr != null && addressArr.length() > 0) addressArr.optString(0) else ""

        val authority = if (privateKey.isNotEmpty()) {
            "${URLEncoder.encode(privateKey, "UTF-8")}@$formattedServer:$port"
        } else {
            "$formattedServer:$port"
        }
        val sb = StringBuilder("wireguard://$authority")
        val params = mutableMapOf<String, String>()
        if (publicKey.isNotEmpty()) params["publickey"] = publicKey
        if (address.isNotEmpty()) params["address"] = address
        val presharedKey = peer?.optString("pre_shared_key", "") ?: ""
        if (presharedKey.isNotEmpty()) params["presharedkey"] = presharedKey
        val mtu = ob.optInt("mtu", 0)
        if (mtu > 0) params["mtu"] = mtu.toString()

        if (params.isNotEmpty()) {
            val paramString = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
            sb.append("?$paramString")
        }
        val displayName = name.ifEmpty { ob.optString("tag", "") }
        if (displayName.isNotEmpty() && displayName != "proxy") {
            sb.append("#${URLEncoder.encode(displayName, "UTF-8")}")
        }
        return sb.toString()
    }

    private fun generateVlessLink(ob: JSONObject, name: String): String {
        val server = ob.optString("server")
        val port = ob.optInt("server_port")
        val formattedServer = formatHost(server)
        val uuid = ob.optString("uuid")
        val sb = StringBuilder("vless://$uuid@$formattedServer:$port?")

        val params = mutableMapOf<String, String>()
        val flow = ob.optString("flow")
        if (flow.isNotEmpty()) params["flow"] = flow

        val packetEncoding = ob.optString("packet_encoding")
        if (packetEncoding.isNotEmpty()) params["packetEncoding"] = packetEncoding

        val tls = ob.optJSONObject("tls")
        if (tls != null && tls.optBoolean("enabled", false)) {
            val reality = tls.optJSONObject("reality")
            if (reality != null && reality.optBoolean("enabled", true)) {
                params["security"] = "reality"
                val sni = reality.optString("server_name")
                params["sni"] = sni.ifEmpty { tls.optString("server_name") }
                params["pbk"] = reality.optString("public_key")
                val sid = reality.optString("short_id")
                if (sid.isNotEmpty()) params["sid"] = sid
            } else {
                params["security"] = "tls"
                params["sni"] = tls.optString("server_name")
            }
            val utls = tls.optJSONObject("utls")
            if (utls != null) {
                params["fp"] = utls.optString("fingerprint", "chrome")
            } else {
                params["fp"] = "chrome"
            }
        } else {
            params["security"] = "none"
        }

        val transport = ob.optJSONObject("transport")
        if (transport != null) {
            val type = transport.optString("type")
            params["type"] = type
            if (type == "ws") {
                params["path"] = transport.optString("path", "/")
                transport.optJSONObject("headers")?.optString("Host")?.takeIf { it.isNotEmpty() }?.let {
                    params["host"] = it
                }
            } else if (type == "grpc") {
                params["serviceName"] = transport.optString("service_name")
            } else if (type == "http" || type == "h2") {
                params["path"] = transport.optString("path", "/")
                val hostArr = transport.optJSONArray("host")
                if (hostArr != null && hostArr.length() > 0) {
                    params["host"] = hostArr.optString(0)
                }
            }
        } else {
            params["type"] = "tcp"
        }

        val paramString = params.entries.joinToString("&") { 
            "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" 
        }
        sb.append(paramString)
        if (name.isNotEmpty()) {
            sb.append("#${URLEncoder.encode(name, "UTF-8")}")
        }
        return sb.toString()
    }
    
    private fun generateVmessLink(ob: JSONObject, name: String): String {
        val json = JSONObject()
        json.put("v", "2")
        json.put("ps", name)
        json.put("add", ob.optString("server"))
        json.put("port", ob.optInt("server_port"))
        json.put("id", ob.optString("uuid"))
        json.put("aid", 0)
        
        val tls = ob.optJSONObject("tls")
        if (tls != null && tls.optBoolean("enabled", false)) {
            json.put("tls", "tls")
            json.put("sni", tls.optString("server_name"))
        } else {
            json.put("tls", "")
        }
        
        val transport = ob.optJSONObject("transport")
        if (transport != null) {
            json.put("net", transport.optString("type"))
            if (transport.optString("type") == "ws") {
                json.put("path", transport.optString("path", "/"))
                json.put("host", transport.optJSONObject("headers")?.optString("Host", "") ?: "")
            } else if (transport.optString("type") == "http" || transport.optString("type") == "h2") {
                json.put("path", transport.optString("path", "/"))
                val hostArr = transport.optJSONArray("host")
                json.put("host", if (hostArr != null && hostArr.length() > 0) hostArr.optString(0) else "")
            }
        } else {
            json.put("net", "tcp")
        }
        
        json.put("type", "none")
        val base64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
        return "vmess://$base64"
    }

    private fun generateTrojanLink(ob: JSONObject, name: String): String {
        val server = ob.optString("server")
        val port = ob.optInt("server_port")
        val formattedServer = formatHost(server)
        val pw = ob.optString("password")
        val sb = StringBuilder("trojan://${URLEncoder.encode(pw, "UTF-8")}@$formattedServer:$port?")

        val params = mutableMapOf<String, String>()

        val tls = ob.optJSONObject("tls")
        if (tls != null && tls.optBoolean("enabled", false)) {
            params["security"] = "tls"
            params["sni"] = tls.optString("server_name")
        } else {
            params["security"] = "none"
        }

        val transport = ob.optJSONObject("transport")
        if (transport != null) {
            val type = transport.optString("type")
            params["type"] = type
            if (type == "ws") {
                params["path"] = transport.optString("path", "/")
                transport.optJSONObject("headers")?.optString("Host")?.takeIf { it.isNotEmpty() }?.let {
                    params["host"] = it
                }
            } else if (type == "grpc") {
                params["serviceName"] = transport.optString("service_name")
            } else if (type == "http" || type == "h2") {
                params["path"] = transport.optString("path", "/")
                val hostArr = transport.optJSONArray("host")
                if (hostArr != null && hostArr.length() > 0) {
                    params["host"] = hostArr.optString(0)
                }
            }
        } else {
            params["type"] = "tcp"
        }

        val paramString = params.entries.joinToString("&") { 
            "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" 
        }
        sb.append(paramString)
        if (name.isNotEmpty()) {
            sb.append("#${URLEncoder.encode(name, "UTF-8")}")
        }
        return sb.toString()
    }
}
