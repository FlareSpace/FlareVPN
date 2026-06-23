package flare.client.app.data.parser

import org.json.JSONArray
import org.json.JSONObject

object InboundConverter {
    fun createTunInbound(xray: JSONObject): JSONObject {
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
}
