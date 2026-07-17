package flare.client.app.ui.components.editor

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.*
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.parser.ClipboardParser
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

private fun parseQuery(query: String?): Map<String, String> = query?.split("&")?.associate {
    val parts = it.split("=", limit = 2)
    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
} ?: emptyMap()

class SimpleEditorState(
    val profile: ProfileEntity,
    private val context: Context
) {
    var scheme by mutableStateOf("vless")
    var tag by mutableStateOf(profile.name)
    var server by mutableStateOf("")
    var port by mutableStateOf("")
    var uuid by mutableStateOf("")
    var flow by mutableStateOf("")
    var packetEncoding by mutableStateOf("")
    var method by mutableStateOf("")
    var isTls by mutableStateOf(false)
    var sni by mutableStateOf("")
    var alpn by mutableStateOf("")
    var fingerprint by mutableStateOf("chrome")
    var mport by mutableStateOf("")
    var pbk by mutableStateOf("")
    var sid by mutableStateOf("")
    var upMbps by mutableStateOf("")
    var downMbps by mutableStateOf("")
    var insecure by mutableStateOf(false)
    var pin by mutableStateOf("")
    var obfsType by mutableStateOf("")
    var obfsPassword by mutableStateOf("")
    var hopInterval by mutableStateOf("")

    var transport by mutableStateOf("tcp")
    var tcpHost by mutableStateOf("")
    var tcpPath by mutableStateOf("")
    var kcpHost by mutableStateOf("")
    var kcpPath by mutableStateOf("")
    var kcpSeed by mutableStateOf("")
    var kcpMtu by mutableStateOf("1350")
    var kcpTti by mutableStateOf("50")
    var wsHost by mutableStateOf("")
    var wsPath by mutableStateOf("/")
    var httpUpgradeHost by mutableStateOf("")
    var httpUpgradePath by mutableStateOf("/")
    var h2Host by mutableStateOf("")
    var h2Path by mutableStateOf("/")
    var quicSecurity by mutableStateOf("none")
    var quicKey by mutableStateOf("")
    var grpcAuthority by mutableStateOf("")
    var grpcServiceName by mutableStateOf("")

    var xhttpHost by mutableStateOf("")
    var xhttpPath by mutableStateOf("/")
    var xhttpMode by mutableStateOf("auto")
    var isXhttpModeMenuExpanded by mutableStateOf(false)

    var tlsType by mutableStateOf("TLS")

    var isTransportMenuExpanded by mutableStateOf(false)
    var isTlsTypeMenuExpanded by mutableStateOf(false)
    var isAllowInsecureMenuExpanded by mutableStateOf(false)

    var isFlowMenuExpanded by mutableStateOf(false)
    var isPacketEncodingMenuExpanded by mutableStateOf(false)
    var isMethodMenuExpanded by mutableStateOf(false)
    var isFpMenuExpanded by mutableStateOf(false)
    var isObfsMenuExpanded by mutableStateOf(false)

    var ssNetwork by mutableStateOf("tcp")
    var ssWsPath by mutableStateOf("/")
    var ssWsHost by mutableStateOf("")
    var shadowTlsPassword by mutableStateOf("")
    var shadowTlsVersion by mutableStateOf("3")

    var isSsNetworkMenuExpanded by mutableStateOf(false)
    var isShadowTlsVersionExpanded by mutableStateOf(false)

    val isRealitySupported: Boolean get() = scheme == "vless" || scheme == "trojan"
    val showReality: Boolean get() = isTls && isRealitySupported && tlsType == "Reality"
    val isHysteria: Boolean get() = scheme == "hysteria" || scheme == "hy" || scheme == "hysteria2" || scheme == "hy2"
    val isHysteria2: Boolean get() = scheme == "hysteria2" || scheme == "hy2"
    val isShadowsocks: Boolean get() = scheme == "ss" || scheme == "shadowsocks"

    fun parseUri() {
        try {
            val uri = URI(profile.uri)
            scheme = uri.scheme ?: "vless"
            val queryParams = parseQuery(uri.rawQuery)

            tag = profile.name
            server = uri.host ?: ""
            port = if (uri.port > 0) uri.port.toString() else ""

            when (scheme) {
                "vless", "trojan" -> {
                    uuid = uri.userInfo ?: ""
                    flow = queryParams["flow"] ?: ""
                    packetEncoding = queryParams["packetEncoding"] ?: queryParams["packet_encoding"] ?: ""
                    if (flow == "xtls-rprx-vision-udp443") {
                        flow = "xtls-rprx-vision"
                        packetEncoding = "xudp"
                    }
                    val sec = queryParams["security"] ?: "none"
                    isTls = sec == "tls" || sec == "reality"
                    tlsType = if (sec == "reality") "Reality" else "TLS"
                    sni = queryParams["sni"] ?: uri.host ?: ""
                    alpn = queryParams["alpn"] ?: ""
                    fingerprint = queryParams["fp"] ?: "chrome"
                    if (sec == "reality" || queryParams.containsKey("pbk")) {
                        pbk = queryParams["pbk"] ?: ""
                        sid = queryParams["sid"] ?: ""
                    } else {
                        pbk = ""
                        sid = ""
                    }
                    insecure = queryParams["allowinsecure"] == "1" || queryParams["allowinsecure"] == "true" ||
                            queryParams["allowInsecure"] == "1" || queryParams["allowInsecure"] == "true" ||
                            queryParams["insecure"] == "1" || queryParams["insecure"] == "true"

                    transport = queryParams["type"] ?: "tcp"
                    
                    tcpHost = queryParams["host"] ?: ""
                    tcpPath = queryParams["path"] ?: ""
                    kcpHost = queryParams["host"] ?: ""
                    kcpPath = queryParams["path"] ?: ""
                    kcpSeed = queryParams["seed"] ?: queryParams["kcpSeed"] ?: ""
                    kcpMtu = queryParams["mtu"] ?: "1350"
                    kcpTti = queryParams["tti"] ?: "50"
                    wsHost = queryParams["host"] ?: ""
                    wsPath = queryParams["path"] ?: "/"
                    httpUpgradeHost = queryParams["host"] ?: ""
                    httpUpgradePath = queryParams["path"] ?: "/"
                    h2Host = queryParams["host"] ?: ""
                    h2Path = queryParams["path"] ?: "/"
                    val qSec = queryParams["quicSecurity"] ?: queryParams["security"] ?: "none"
                    quicSecurity = if (qSec == "tls" || qSec == "reality") "none" else qSec
                    quicKey = queryParams["key"] ?: queryParams["quicKey"] ?: ""
                    grpcAuthority = queryParams["authority"] ?: queryParams["grpcAuthority"] ?: ""
                    grpcServiceName = queryParams["serviceName"] ?: queryParams["grpcServiceName"] ?: ""
                    xhttpHost = queryParams["host"] ?: ""
                    xhttpPath = queryParams["path"] ?: "/"
                    xhttpMode = queryParams["mode"] ?: "auto"
                }
                "vmess" -> {
                    val b64 = profile.uri.removePrefix("vmess://").trim()
                    try {
                        val json = org.json.JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
                        tag = profile.name
                        server = json.optString("add")
                        port = json.optString("port")
                        uuid = json.optString("id")
                        sni = json.optString("sni")
                        alpn = json.optString("alpn")
                        isTls = json.optString("tls") == "tls"
                    } catch (_: Exception) {}
                }
                "ss", "shadowsocks" -> {
                    val userInfo = try {
                        String(Base64.decode(uri.userInfo ?: "", Base64.DEFAULT))
                    } catch (_: Exception) {
                        uri.userInfo ?: ":"
                    }
                    method = userInfo.substringBefore(":")
                    uuid = userInfo.substringAfter(":")

                    val pluginVal = queryParams["plugin"] ?: ""
                    val pluginOpts = queryParams["plugin-opts"] ?: queryParams["plugin_opts"] ?: ""
                    val combinedOpts = if (pluginVal.contains(";")) {
                        pluginVal.substringAfter(";")
                    } else {
                        pluginOpts
                    }
                    val optsMap = combinedOpts.split(";").associate { opt ->
                        val parts = opt.split("=", limit = 2)
                        if (parts.size == 2) {
                            parts[0].trim().lowercase() to parts[1].trim()
                        } else {
                            opt.trim().lowercase() to "true"
                        }
                    }

                    val isWs = combinedOpts.contains("websocket") || combinedOpts.contains("mode=websocket") || optsMap["mode"] == "websocket" || queryParams["type"] == "ws"
                    ssNetwork = if (isWs) "ws" else "tcp"
                    ssWsPath = optsMap["path"] ?: queryParams["path"] ?: "/"
                    ssWsHost = optsMap["host"] ?: queryParams["host"] ?: ""

                    val hasTls = combinedOpts.contains("tls") || optsMap.containsKey("tls") || queryParams["security"] == "tls"
                    val isShadowTls = pluginVal.startsWith("shadowtls") || queryParams["plugin"]?.startsWith("shadowtls") == true
                    isTls = hasTls || isShadowTls
                    sni = optsMap["host"] ?: optsMap["sni"] ?: queryParams["sni"] ?: queryParams["host"] ?: uri.host ?: ""

                    if (isShadowTls) {
                        shadowTlsPassword = optsMap["password"] ?: queryParams["shadowtls-password"] ?: ""
                        shadowTlsVersion = optsMap["version"] ?: queryParams["shadowtls-version"] ?: "3"
                    } else {
                        shadowTlsPassword = ""
                        shadowTlsVersion = "3"
                    }
                }
                "hysteria", "hy", "hysteria2", "hy2" -> {
                    uuid = uri.userInfo ?: ""
                    isTls = true
                    sni = queryParams["sni"] ?: queryParams["peer"] ?: uri.host ?: ""
                    alpn = queryParams["alpn"] ?: ""
                    upMbps = queryParams["upmbps"] ?: queryParams["up-mbps"] ?: queryParams["up"] ?: ""
                    downMbps = queryParams["downmbps"] ?: queryParams["down-mbps"] ?: queryParams["down"] ?: ""
                    insecure = queryParams["insecure"] == "1" || queryParams["insecure"] == "true" ||
                        queryParams["allowInsecure"] == "true" || queryParams["skip-cert-verify"] == "true"
                    pin = queryParams["pin"] ?: ""
                    if (scheme == "hysteria2" || scheme == "hy2") {
                        obfsType = queryParams["obfs"] ?: queryParams["obfs-type"] ?: ""
                        obfsPassword = queryParams["obfs-password"] ?: queryParams["obfspassword"] ?: ""
                        mport = queryParams["mport"] ?: ""
                        hopInterval = queryParams["hop_interval"] ?: queryParams["hop-interval"] ?: queryParams["hopInterval"] ?: ""
                    } else {
                        obfsType = queryParams["obfs"] ?: ""
                        obfsPassword = ""
                        mport = queryParams["mport"] ?: ""
                        hopInterval = ""
                    }
                    fingerprint = queryParams["fp"] ?: queryParams["fingerprint"] ?: "chrome"
                }
                else -> {
                    uuid = uri.userInfo ?: ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleSave(onSave: (ProfileEntity) -> Unit) {
        try {
            val newName = tag.trim()
            val host = server.trim()
            val portText = port.trim()
            val cred = uuid.trim()

            val newUri = when (scheme) {
                "vless", "trojan" -> {
                    val portStr = if (portText.isNotEmpty()) ":$portText" else ""
                    val query = mutableListOf<String>()

                    if (scheme == "vless") {
                        query.add("type=$transport")
                        when (transport) {
                            "tcp", "raw" -> {
                                if (tcpHost.isNotEmpty()) query.add("host=${encode(tcpHost)}")
                                if (tcpPath.isNotEmpty()) query.add("path=${encode(tcpPath)}")
                            }
                            "kcp" -> {
                                if (kcpHost.isNotEmpty()) query.add("host=${encode(kcpHost)}")
                                if (kcpPath.isNotEmpty()) query.add("path=${encode(kcpPath)}")
                                if (kcpSeed.isNotEmpty()) query.add("seed=${encode(kcpSeed)}")
                                if (kcpMtu.isNotEmpty()) query.add("mtu=${encode(kcpMtu)}")
                                if (kcpTti.isNotEmpty()) query.add("tti=${encode(kcpTti)}")
                            }
                            "ws" -> {
                                if (wsHost.isNotEmpty()) query.add("host=${encode(wsHost)}")
                                if (wsPath.isNotEmpty()) query.add("path=${encode(wsPath)}")
                            }
                            "httpupgrade" -> {
                                if (httpUpgradeHost.isNotEmpty()) query.add("host=${encode(httpUpgradeHost)}")
                                if (httpUpgradePath.isNotEmpty()) query.add("path=${encode(httpUpgradePath)}")
                            }
                            "h2", "http" -> {
                                if (h2Host.isNotEmpty()) query.add("host=${encode(h2Host)}")
                                if (h2Path.isNotEmpty()) query.add("path=${encode(h2Path)}")
                            }
                            "quic" -> {
                                query.add("quicSecurity=${encode(quicSecurity)}")
                                if (quicKey.isNotEmpty()) query.add("key=${encode(quicKey)}")
                            }
                            "grpc" -> {
                                if (grpcAuthority.isNotEmpty()) query.add("authority=${encode(grpcAuthority)}")
                                if (grpcServiceName.isNotEmpty()) query.add("serviceName=${encode(grpcServiceName)}")
                            }
                            "xhttp" -> {
                                if (xhttpHost.isNotEmpty()) query.add("host=${encode(xhttpHost)}")
                                if (xhttpPath.isNotEmpty()) query.add("path=${encode(xhttpPath)}")
                                if (xhttpMode.isNotEmpty()) query.add("mode=${encode(xhttpMode)}")
                            }
                        }
                    } else {
                        val parsed = URI(profile.uri)
                        val originalParams = parseQuery(parsed.rawQuery)
                        query.add("type=" + (originalParams["type"] ?: "tcp"))
                    }

                    if (scheme == "vless" && flow.isNotEmpty()) {
                        query.add("flow=${encode(flow)}")
                    }

                    if (scheme == "vless" && packetEncoding.isNotEmpty()) {
                        query.add("packetEncoding=${encode(packetEncoding)}")
                    }

                    if (isTls) {
                        if (tlsType == "Reality") {
                            query.add("security=reality")
                            if (pbk.isNotEmpty()) {
                                query.add("pbk=${encode(pbk.trim())}")
                            }
                            if (sid.trim().isNotEmpty()) {
                                query.add("sid=${encode(sid.trim())}")
                            }
                        } else {
                            query.add("security=tls")
                            if (insecure) {
                                query.add("allowinsecure=1")
                            } else {
                                query.add("allowinsecure=0")
                            }
                        }

                        if (sni.trim().isNotEmpty()) query.add("sni=${encode(sni.trim())}")
                        if (alpn.trim().isNotEmpty()) query.add("alpn=${encode(alpn.trim())}")
                        if (fingerprint.trim().isNotEmpty()) query.add("fp=${encode(fingerprint.trim())}")
                    } else {
                        query.add("security=none")
                    }

                    "$scheme://$cred@$host$portStr?${query.joinToString("&")}#${encode(newName)}"
                }
                "vmess" -> {
                    val b64 = profile.uri.removePrefix("vmess://").trim()
                    val json = try {
                        org.json.JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
                    } catch (_: Exception) {
                        org.json.JSONObject()
                    }
                    json.put("ps", newName)
                    json.put("add", host)
                    json.put("port", portText.toIntOrNull() ?: 443)
                    json.put("id", cred)
                    if (isTls) {
                        json.put("tls", "tls")
                        json.put("sni", sni.trim())
                        json.put("alpn", alpn.trim())
                    } else {
                        json.put("tls", "")
                    }
                    val newB64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                    "vmess://$newB64"
                }
                "ss", "shadowsocks" -> {
                    val portStr = if (portText.isNotEmpty()) ":$portText" else ""
                    val auth = Base64.encodeToString("$method:$cred".toByteArray(), Base64.NO_WRAP)
                    val query = mutableListOf<String>()

                    if (isTls) {
                        if (ssNetwork == "ws") {
                            val opts = mutableListOf("mode=websocket")
                            if (ssWsPath.isNotEmpty()) opts.add("path=${ssWsPath.trim()}")
                            if (ssWsHost.isNotEmpty()) opts.add("host=${ssWsHost.trim()}")
                            opts.add("tls")
                            if (sni.trim().isNotEmpty()) opts.add("sni=${sni.trim()}")
                            query.add("plugin=v2ray-plugin%3B${encode(opts.joinToString(";"))}")
                            query.add("security=tls")
                            if (sni.trim().isNotEmpty()) query.add("sni=${encode(sni.trim())}")
                            if (ssWsPath.isNotEmpty()) query.add("path=${encode(ssWsPath.trim())}")
                            if (ssWsHost.isNotEmpty()) query.add("host=${encode(ssWsHost.trim())}")
                            query.add("type=ws")
                        } else {
                            val opts = mutableListOf<String>()
                            if (shadowTlsPassword.isNotEmpty()) opts.add("password=${shadowTlsPassword.trim()}")
                            if (shadowTlsVersion.isNotEmpty()) opts.add("version=${shadowTlsVersion.trim()}")
                            if (sni.trim().isNotEmpty()) opts.add("host=${sni.trim()}")
                            query.add("plugin=shadowtls%3B${encode(opts.joinToString(";"))}")
                            query.add("security=tls")
                            if (sni.trim().isNotEmpty()) query.add("sni=${encode(sni.trim())}")
                            if (shadowTlsPassword.isNotEmpty()) query.add("shadowtls-password=${encode(shadowTlsPassword.trim())}")
                            if (shadowTlsVersion.isNotEmpty()) query.add("shadowtls-version=${encode(shadowTlsVersion.trim())}")
                            query.add("type=tcp")
                        }
                    } else {
                        if (ssNetwork == "ws") {
                            val opts = mutableListOf("mode=websocket")
                            if (ssWsPath.isNotEmpty()) opts.add("path=${ssWsPath.trim()}")
                            if (ssWsHost.isNotEmpty()) opts.add("host=${ssWsHost.trim()}")
                            query.add("plugin=v2ray-plugin%3B${encode(opts.joinToString(";"))}")
                            if (ssWsPath.isNotEmpty()) query.add("path=${encode(ssWsPath.trim())}")
                            if (ssWsHost.isNotEmpty()) query.add("host=${encode(ssWsHost.trim())}")
                            query.add("type=ws")
                        }
                    }
                    val params = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                    "ss://$auth@$host$portStr$params#${encode(newName)}"
                }
                "hysteria", "hy", "hysteria2", "hy2" -> {
                    val portStr = if (portText.isNotEmpty()) ":$portText" else ""
                    val query = mutableListOf<String>()
                    if (sni.trim().isNotEmpty()) query.add("sni=${encode(sni.trim())}")
                    if (alpn.trim().isNotEmpty()) query.add("alpn=${encode(alpn.trim())}")
                    if (insecure) query.add("insecure=true")
                    if (pin.trim().isNotEmpty()) query.add("pin=${encode(pin.trim())}")
                    if (upMbps.trim().isNotEmpty()) query.add("up=${encode(upMbps.trim())}")
                    if (downMbps.trim().isNotEmpty()) query.add("down=${encode(downMbps.trim())}")

                    if (scheme == "hysteria2" || scheme == "hy2") {
                        if (obfsType.trim().isNotEmpty()) {
                            query.add("obfs=${encode(obfsType.trim())}")
                            if (obfsPassword.trim().isNotEmpty()) {
                                query.add("obfs-password=${encode(obfsPassword.trim())}")
                            }
                        }
                        if (hopInterval.trim().isNotEmpty()) {
                            query.add("hop_interval=${encode(hopInterval.trim())}")
                        }
                    } else {
                        if (obfsType.trim().isNotEmpty()) {
                            query.add("obfs=${encode(obfsType.trim())}")
                        }
                    }
                    if (mport.trim().isNotEmpty()) query.add("mport=${encode(mport.trim())}")
                    
                    val params = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                    "$scheme://$cred@$host$portStr$params#${encode(newName)}"
                }
                else -> profile.uri
            }

            val updatedProfile = ClipboardParser.buildProfileFromUri(context, newUri, profile.subscriptionId)
            onSave(updatedProfile.copy(
                id = profile.id,
                name = newName,
                isSelected = profile.isSelected,
                serverDescription = profile.serverDescription
            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun rememberSimpleEditorState(profile: ProfileEntity, context: Context): SimpleEditorState {
    val state = remember { SimpleEditorState(profile, context) }
    LaunchedEffect(profile.uri) {
        state.parseUri()
    }
    return state
}
