package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import flare.client.app.R
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.parser.ClipboardParser
import flare.client.app.util.GlassUtils
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import flare.client.app.ui.theme.FlareTheme


private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

private fun parseQuery(query: String?): Map<String, String> = query?.split("&")?.associate {
    val parts = it.split("=", limit = 2)
    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
} ?: emptyMap()

@Composable
fun ProfileSimpleEditor(
    profile: ProfileEntity,
    onSave: (ProfileEntity) -> Unit,
    onBack: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var scheme by remember { mutableStateOf("vless") }
    var tag by remember { mutableStateOf(profile.name) }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var uuid by remember { mutableStateOf("") }
    var flow by remember { mutableStateOf("") }
    var packetEncoding by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var isTls by remember { mutableStateOf(false) }
    var sni by remember { mutableStateOf("") }
    var alpn by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("chrome") }
    var mport by remember { mutableStateOf("") }
    var pbk by remember { mutableStateOf("") }
    var sid by remember { mutableStateOf("") }
    var upMbps by remember { mutableStateOf("") }
    var downMbps by remember { mutableStateOf("") }
    var insecure by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var obfsType by remember { mutableStateOf("") }
    var obfsPassword by remember { mutableStateOf("") }
    var hopInterval by remember { mutableStateOf("") }

    var transport by remember { mutableStateOf("tcp") }
    var tcpHost by remember { mutableStateOf("") }
    var tcpPath by remember { mutableStateOf("") }
    var kcpHost by remember { mutableStateOf("") }
    var kcpPath by remember { mutableStateOf("") }
    var kcpSeed by remember { mutableStateOf("") }
    var kcpMtu by remember { mutableStateOf("1350") }
    var kcpTti by remember { mutableStateOf("50") }
    var wsHost by remember { mutableStateOf("") }
    var wsPath by remember { mutableStateOf("/") }
    var httpUpgradeHost by remember { mutableStateOf("") }
    var httpUpgradePath by remember { mutableStateOf("/") }
    var h2Host by remember { mutableStateOf("") }
    var h2Path by remember { mutableStateOf("/") }
    var quicSecurity by remember { mutableStateOf("none") }
    var quicKey by remember { mutableStateOf("") }
    var grpcAuthority by remember { mutableStateOf("") }
    var grpcServiceName by remember { mutableStateOf("") }

    var xhttpHost by remember { mutableStateOf("") }
    var xhttpPath by remember { mutableStateOf("/") }
    var xhttpMode by remember { mutableStateOf("auto") }
    var isXhttpModeMenuExpanded by remember { mutableStateOf(false) }

    var tlsType by remember { mutableStateOf("TLS") }

    var isTransportMenuExpanded by remember { mutableStateOf(false) }
    var isTlsTypeMenuExpanded by remember { mutableStateOf(false) }
    var isAllowInsecureMenuExpanded by remember { mutableStateOf(false) }

    var isFlowMenuExpanded by remember { mutableStateOf(false) }
    var isPacketEncodingMenuExpanded by remember { mutableStateOf(false) }
    var isMethodMenuExpanded by remember { mutableStateOf(false) }
    var isFpMenuExpanded by remember { mutableStateOf(false) }
    var isObfsMenuExpanded by remember { mutableStateOf(false) }

    var ssNetwork by remember { mutableStateOf("tcp") }
    var ssWsPath by remember { mutableStateOf("/") }
    var ssWsHost by remember { mutableStateOf("") }
    var shadowTlsPassword by remember { mutableStateOf("") }
    var shadowTlsVersion by remember { mutableStateOf("3") }

    var isSsNetworkMenuExpanded by remember { mutableStateOf(false) }
    var isShadowTlsVersionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(profile.uri) {
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

    val isRealitySupported = scheme == "vless" || scheme == "trojan"
    val showReality = isTls && isRealitySupported && tlsType == "Reality"
    val isHysteria = scheme == "hysteria" || scheme == "hy" || scheme == "hysteria2" || scheme == "hy2"
    val isHysteria2 = scheme == "hysteria2" || scheme == "hy2"
    val isShadowsocks = scheme == "ss" || scheme == "shadowsocks"

    val handleSave = {
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
                            "tcp" -> {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(top = 84.dp, bottom = 32.dp)
                    .padding(horizontal = 20.dp)
            ) {
                val uuidLabel = when (scheme) {
                    "vless", "vmess" -> I18n.strings.label_uuid
                    "trojan", "ss", "shadowsocks", "hysteria", "hy", "hysteria2", "hy2" -> I18n.strings.label_password
                    else -> I18n.strings.label_credentials
                }

                
                EditorSectionTitle(
                    text = I18n.strings.simple_editor_basic,
                    iconRes = R.drawable.ic_vpn_key,
                    accentColor = accentColor
                )
                EditorFieldGroup {
                    EditorTextField(
                        label = I18n.strings.simple_editor_tag,
                        value = tag,
                        onValueChange = { tag = it },
                        keyboardType = KeyboardType.Text,
                        accentColor = accentColor
                    )
                    EditorFieldDivider()
                    EditorTextField(
                        label = I18n.strings.simple_editor_server,
                        value = server,
                        onValueChange = { server = it },
                        keyboardType = KeyboardType.Uri,
                        accentColor = accentColor
                    )
                    EditorFieldDivider()
                    EditorTextField(
                        label = I18n.strings.simple_editor_port,
                        value = port,
                        onValueChange = { port = it },
                        keyboardType = KeyboardType.Number,
                        accentColor = accentColor
                    )
                    EditorFieldDivider()
                    EditorTextField(
                        label = uuidLabel,
                        value = uuid,
                        onValueChange = { uuid = it },
                        keyboardType = KeyboardType.Ascii,
                        accentColor = accentColor
                    )
                    
                    AnimatedVisibility(
                        visible = scheme == "vless",
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                    ) {
                        Column {
                            EditorFieldDivider()
                            EditorSelectField(
                                label = I18n.strings.simple_editor_flow,
                                value = if (flow.isEmpty()) "None" else flow,
                                expanded = isFlowMenuExpanded,
                                onExpandedChange = { isFlowMenuExpanded = it },
                                options = listOf("xtls-rprx-vision", ""),
                                optionTitles = listOf("xtls-rprx-vision", "None"),
                                onOptionSelected = { flow = it },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                            EditorFieldDivider()
                            EditorSelectField(
                                label = I18n.strings.simple_editor_packet_encoding,
                                value = if (packetEncoding.isEmpty()) "None" else packetEncoding,
                                expanded = isPacketEncodingMenuExpanded,
                                onExpandedChange = { isPacketEncodingMenuExpanded = it },
                                options = listOf("", "packet", "xudp"),
                                optionTitles = listOf("None", "packet", "xudp"),
                                onOptionSelected = { packetEncoding = it },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isShadowsocks,
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                    ) {
                        Column {
                            EditorFieldDivider()
                            EditorSelectField(
                                label = I18n.strings.simple_editor_method,
                                value = method,
                                expanded = isMethodMenuExpanded,
                                onExpandedChange = { isMethodMenuExpanded = it },
                                options = listOf(
                                    "aes-128-gcm", "aes-256-gcm", "chacha20-poly1305",
                                    "2022-blake3-aes-128-gcm", "2022-blake3-aes-256-gcm",
                                    "2022-blake3-chacha20-poly1305"
                                ),
                                optionTitles = listOf(
                                    "aes-128-gcm", "aes-256-gcm", "chacha20-poly1305",
                                    "2022-blake3-aes-128-gcm", "2022-blake3-aes-256-gcm",
                                    "2022-blake3-chacha20-poly1305"
                                ),
                                onOptionSelected = { method = it },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                        }
                    }
                }

                
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorSectionTitle(
                            text = I18n.strings.simple_editor_tls,
                            iconRes = R.drawable.ic_security,
                            accentColor = accentColor
                        )
                        EditorFieldGroup {
                            EditorSwitchField(
                                label = I18n.strings.simple_editor_enable_tls,
                                checked = isTls,
                                onCheckedChange = { isTls = it },
                                accentColor = accentColor
                            )
                            AnimatedVisibility(
                                visible = isTls,
                                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                                exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                            ) {
                                Column {
                                    AnimatedVisibility(
                                        visible = isRealitySupported,
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorSelectField(
                                                label = I18n.strings.simple_editor_tls_type,
                                                value = tlsType,
                                                expanded = isTlsTypeMenuExpanded,
                                                onExpandedChange = { isTlsTypeMenuExpanded = it },
                                                options = listOf("TLS", "Reality"),
                                                optionTitles = listOf("TLS", "Reality"),
                                                onOptionSelected = { tlsType = it },
                                                accentColor = accentColor,
                                                hazeState = hazeState
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = tlsType == "TLS",
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorSelectField(
                                                label = I18n.strings.simple_editor_allow_insecure,
                                                value = if (insecure) I18n.strings.option_yes else I18n.strings.option_no,
                                                expanded = isAllowInsecureMenuExpanded,
                                                onExpandedChange = { isAllowInsecureMenuExpanded = it },
                                                options = listOf("yes", "no"),
                                                optionTitles = listOf(I18n.strings.option_yes, I18n.strings.option_no),
                                                onOptionSelected = { insecure = it == "yes" },
                                                accentColor = accentColor,
                                                hazeState = hazeState
                                            )
                                        }
                                    }
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_sni,
                                        value = sni,
                                        onValueChange = { sni = it },
                                        keyboardType = KeyboardType.Uri,
                                        accentColor = accentColor
                                    )
                                    
                                    AnimatedVisibility(
                                        visible = !isShadowsocks,
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorTextField(
                                                label = I18n.strings.simple_editor_alpn,
                                                value = alpn,
                                                onValueChange = { alpn = it },
                                                keyboardType = KeyboardType.Text,
                                                placeholder = "h2,http/1.1",
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isHysteria,
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorSwitchField(
                                                label = I18n.strings.simple_editor_allow_insecure,
                                                checked = insecure,
                                                onCheckedChange = { insecure = it },
                                                accentColor = accentColor
                                            )
                                            EditorFieldDivider()
                                            EditorTextField(
                                                label = I18n.strings.simple_editor_cert_pin,
                                                value = pin,
                                                onValueChange = { pin = it },
                                                keyboardType = KeyboardType.Ascii,
                                                placeholder = "SHA-256 fingerprint",
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = !isHysteria && !isShadowsocks,
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorSelectField(
                                                label = I18n.strings.simple_editor_fingerprint,
                                                value = fingerprint,
                                                expanded = isFpMenuExpanded,
                                                onExpandedChange = { isFpMenuExpanded = it },
                                                options = listOf(
                                                    "chrome", "firefox", "safari", "edge",
                                                    "ios", "android", "random", "randomized"
                                                ),
                                                optionTitles = listOf(
                                                    "chrome", "firefox", "safari", "edge",
                                                    "ios", "android", "random", "randomized"
                                                ),
                                                onOptionSelected = { fingerprint = it },
                                                accentColor = accentColor,
                                                hazeState = hazeState
                                            )
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isShadowsocks && ssNetwork == "tcp",
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorTextField(
                                                label = I18n.strings.simple_editor_shadowtls_password,
                                                value = shadowTlsPassword,
                                                onValueChange = { shadowTlsPassword = it },
                                                keyboardType = KeyboardType.Ascii,
                                                accentColor = accentColor
                                            )
                                            EditorFieldDivider()
                                            EditorSelectField(
                                                label = I18n.strings.simple_editor_shadowtls_version,
                                                value = shadowTlsVersion,
                                                expanded = isShadowTlsVersionExpanded,
                                                onExpandedChange = { isShadowTlsVersionExpanded = it },
                                                options = listOf("3", "2", "1"),
                                                optionTitles = listOf("Version 3", "Version 2", "Version 1"),
                                                onOptionSelected = { shadowTlsVersion = it },
                                                accentColor = accentColor,
                                                hazeState = hazeState
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                
                AnimatedVisibility(
                    visible = isShadowsocks,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorSectionTitle(
                            text = I18n.strings.simple_editor_ss_network,
                            iconRes = R.drawable.ic_routing_filled,
                            accentColor = accentColor
                        )
                        EditorFieldGroup {
                            EditorSelectField(
                                label = I18n.strings.simple_editor_ss_network,
                                value = ssNetwork.uppercase(),
                                expanded = isSsNetworkMenuExpanded,
                                onExpandedChange = { isSsNetworkMenuExpanded = it },
                                options = listOf("tcp", "ws"),
                                optionTitles = listOf("TCP", "WebSocket (WS)"),
                                onOptionSelected = { ssNetwork = it },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                            AnimatedVisibility(
                                visible = ssNetwork == "ws",
                                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                                exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_ss_ws_path,
                                        value = ssWsPath,
                                        onValueChange = { ssWsPath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_ss_ws_host,
                                        value = ssWsHost,
                                        onValueChange = { ssWsHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                
                AnimatedVisibility(
                    visible = scheme == "vless",
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorSectionTitle(
                            text = I18n.strings.simple_editor_ss_network,
                            iconRes = R.drawable.ic_routing_filled,
                            accentColor = accentColor
                        )
                        EditorFieldGroup {
                            EditorSelectField(
                                label = I18n.strings.simple_editor_ss_network,
                                value = transport,
                                expanded = isTransportMenuExpanded,
                                onExpandedChange = { isTransportMenuExpanded = it },
                                options = listOf("tcp", "ws", "httpupgrade", "h2", "http", "quic", "grpc", "xhttp"),
                                optionTitles = listOf("tcp", "ws", "httpupgrade", "h2", "http", "quic", "grpc", "xhttp"),
                                onOptionSelected = { transport = it },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                            AnimatedVisibility(
                                visible = transport == "tcp",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_http_host,
                                        value = tcpHost,
                                        onValueChange = { tcpHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_path,
                                        value = tcpPath,
                                        onValueChange = { tcpPath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "kcp",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_host,
                                        value = kcpHost,
                                        onValueChange = { kcpHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_path,
                                        value = kcpPath,
                                        onValueChange = { kcpPath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_kcp_seed,
                                        value = kcpSeed,
                                        onValueChange = { kcpSeed = it },
                                        keyboardType = KeyboardType.Text,
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_mtu,
                                        value = kcpMtu,
                                        onValueChange = { kcpMtu = it },
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "1350",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_tti,
                                        value = kcpTti,
                                        onValueChange = { kcpTti = it },
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "50",
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "ws",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_ss_ws_host,
                                        value = wsHost,
                                        onValueChange = { wsHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_ss_ws_path,
                                        value = wsPath,
                                        onValueChange = { wsPath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "httpupgrade",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_httpupgrade_host,
                                        value = httpUpgradeHost,
                                        onValueChange = { httpUpgradeHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_httpupgrade_path,
                                        value = httpUpgradePath,
                                        onValueChange = { httpUpgradePath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                }
                            }
                             AnimatedVisibility(
                                 visible = transport == "h2" || transport == "http",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = if (transport == "h2") I18n.strings.simple_editor_h2_host else I18n.strings.simple_editor_http_host,
                                        value = h2Host,
                                        onValueChange = { h2Host = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = if (transport == "h2") I18n.strings.simple_editor_h2_path else I18n.strings.simple_editor_path,
                                        value = h2Path,
                                        onValueChange = { h2Path = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "quic",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_quic_security,
                                        value = quicSecurity,
                                        onValueChange = { quicSecurity = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "none",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_quic_key,
                                        value = quicKey,
                                        onValueChange = { quicKey = it },
                                        keyboardType = KeyboardType.Text,
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "grpc",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_grpc_authority,
                                        value = grpcAuthority,
                                        onValueChange = { grpcAuthority = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_grpc_service_name,
                                        value = grpcServiceName,
                                        onValueChange = { grpcServiceName = it },
                                        keyboardType = KeyboardType.Text,
                                        accentColor = accentColor
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = transport == "xhttp",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorSelectField(
                                        label = I18n.strings.simple_editor_mode,
                                        value = xhttpMode,
                                        expanded = isXhttpModeMenuExpanded,
                                        onExpandedChange = { isXhttpModeMenuExpanded = it },
                                        options = listOf("auto", "download", "streaming"),
                                        optionTitles = listOf("auto", "download", "streaming"),
                                        onOptionSelected = { xhttpMode = it },
                                        accentColor = accentColor,
                                        hazeState = hazeState
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_host,
                                        value = xhttpHost,
                                        onValueChange = { xhttpHost = it },
                                        keyboardType = KeyboardType.Uri,
                                        placeholder = "domain.com",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_path,
                                        value = xhttpPath,
                                        onValueChange = { xhttpPath = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "/",
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                
                AnimatedVisibility(
                    visible = showReality,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorSectionTitle(
                            text = I18n.strings.simple_editor_reality,
                            iconRes = R.drawable.ic_settings_advanced,
                            accentColor = accentColor
                        )
                        EditorFieldGroup {
                            EditorTextField(
                                label = I18n.strings.simple_editor_pbk,
                                value = pbk,
                                onValueChange = { pbk = it },
                                keyboardType = KeyboardType.Text,
                                accentColor = accentColor
                            )
                            EditorFieldDivider()
                            EditorTextField(
                                label = I18n.strings.simple_editor_sid,
                                value = sid,
                                onValueChange = { sid = it },
                                keyboardType = KeyboardType.Text,
                                accentColor = accentColor
                            )
                        }
                    }
                }

                
                AnimatedVisibility(
                    visible = isHysteria,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorSectionTitle(
                            text = I18n.strings.simple_editor_hysteria_settings,
                            iconRes = R.drawable.ic_speedometer,
                            accentColor = accentColor
                        )
                        EditorFieldGroup {
                            EditorTextField(
                                label = I18n.strings.simple_editor_up_mbps,
                                value = upMbps,
                                onValueChange = { upMbps = it },
                                keyboardType = KeyboardType.Number,
                                placeholder = "e.g. 100",
                                accentColor = accentColor
                            )
                            EditorFieldDivider()
                            EditorTextField(
                                label = I18n.strings.simple_editor_down_mbps,
                                value = downMbps,
                                onValueChange = { downMbps = it },
                                keyboardType = KeyboardType.Number,
                                placeholder = "e.g. 100",
                                accentColor = accentColor
                            )
                            
                            AnimatedVisibility(
                                visible = isHysteria2,
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorSelectField(
                                        label = I18n.strings.simple_editor_obfs,
                                        value = if (obfsType.isEmpty()) "None" else obfsType,
                                        expanded = isObfsMenuExpanded,
                                        onExpandedChange = { isObfsMenuExpanded = it },
                                        options = listOf("", "salamander"),
                                        optionTitles = listOf("None", "salamander"),
                                        onOptionSelected = { obfsType = it },
                                        accentColor = accentColor,
                                        hazeState = hazeState
                                    )
                                    AnimatedVisibility(
                                        visible = obfsType == "salamander",
                                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                    ) {
                                        Column {
                                            EditorFieldDivider()
                                            EditorTextField(
                                                label = I18n.strings.simple_editor_obfs_pass,
                                                value = obfsPassword,
                                                onValueChange = { obfsPassword = it },
                                                keyboardType = KeyboardType.Text,
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                    
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = "Port Hopping (mport)",
                                        value = mport,
                                        onValueChange = { mport = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "e.g. 20000-50000",
                                        accentColor = accentColor
                                    )
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_hop_interval,
                                        value = hopInterval,
                                        onValueChange = { hopInterval = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "e.g. 10s or 5s",
                                        accentColor = accentColor
                                    )
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = scheme == "hysteria" || scheme == "hy",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column {
                                    EditorFieldDivider()
                                    EditorTextField(
                                        label = I18n.strings.simple_editor_obfs,
                                        value = obfsType,
                                        onValueChange = { obfsType = it },
                                        keyboardType = KeyboardType.Text,
                                        placeholder = "XOR Key",
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        
        FlareTopBar(
            title = I18n.strings.simple_editor_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack,
            subtitle = if (scheme.isNotBlank()) {
                {
                    ProtocolChip(scheme = scheme, accentColor = accentColor)
                }
            } else null,
            actions = {
                IconButton(
                    onClick = { handleSave() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}



@Composable
private fun ProtocolChip(scheme: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = scheme.uppercase(),
            fontFamily = GeologicaMedium,
            fontSize = 11.sp,
            color = accentColor,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun EditorSectionTitle(text: String, iconRes: Int, accentColor: Color) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontFamily = GeologicaMedium,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun EditorFieldGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlareTheme.colors.bgItem)
    ) {
        content()
    }
}

@Composable
private fun EditorFieldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FlareTheme.colors.bgItem)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp)
                .background(FlareTheme.colors.dividerColor)
        )
    }
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    accentColor: Color,
    isPassword: Boolean = false,
    placeholder: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(220),
        label = "editorFieldBg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else FlareTheme.colors.textSecondary,
        animationSpec = tween(220),
        label = "editorLabelColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            text = label,
            fontFamily = GeologicaRegular,
            fontSize = 12.sp,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                fontFamily = GeologicaMedium,
                fontSize = 16.sp,
                color = FlareTheme.colors.textPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = GeologicaRegular,
                            fontSize = 16.sp,
                            color = FlareTheme.colors.textSecondary.copy(alpha = 0.38f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun EditorSelectField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    optionTitles: List<String>,
    onOptionSelected: (String) -> Unit,
    accentColor: Color,
    hazeState: HazeState
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(true) }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Text(
                text = label,
                fontFamily = GeologicaRegular,
                fontSize = 12.sp,
                color = FlareTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    fontFamily = GeologicaMedium,
                    fontSize = 16.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = FlareTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        FlareGlassMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            items = options.mapIndexed { index, option ->
                GlassUtils.MenuItem(index, optionTitles[index]) {
                    onOptionSelected(option)
                }
            },
            hazeState = hazeState,
            alignment = Alignment.CenterEnd
        )
    }
}

@Composable
private fun EditorSwitchField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = GeologicaRegular,
            fontSize = 16.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        FlareGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accentColor = accentColor
        )
    }
}
