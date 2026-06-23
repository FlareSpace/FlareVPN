package flare.client.app.data.parser

import android.util.Log
import org.json.JSONObject

object DnsConverter {

    fun fixDnsRemoteDetour(obj: JSONObject) {
        val dns = obj.optJSONObject("dns") ?: return
        val servers = dns.optJSONArray("servers") ?: return
        val outbounds = obj.optJSONArray("outbounds") ?: return

        
        val hasProxyOutbound = (0 until outbounds.length()).any {
            outbounds.optJSONObject(it)?.optString("tag") == "proxy"
        }
        if (hasProxyOutbound) return

        val realProxyTag = OutboundConverter.findPrimaryProxyTag(outbounds)
        if (realProxyTag == "proxy") return

        Log.d("DnsConverter", "fixDnsRemoteDetour: replacing detour 'proxy' with '$realProxyTag'")
        for (i in 0 until servers.length()) {
            val server = servers.optJSONObject(i) ?: continue
            if (server.optString("detour") == "proxy") {
                server.put("detour", realProxyTag)
            }
        }
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
