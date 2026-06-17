package flare.client.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import flare.client.app.data.model.DisplayItem
import flare.client.app.data.model.PingState
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import androidx.compose.ui.platform.LocalContext
import flare.client.app.ui.i18n.I18n

@Composable
fun ProfileList(
    items: List<DisplayItem>,
    accentColor: Color,
    pingStyle: String = "time",
    listState: LazyListState = rememberLazyListState(),
    chainedProfileIds: List<Long> = emptyList(),
    onProfileClick: (ProfileSummary) -> Unit,
    onProfileDelete: (ProfileSummary) -> Unit,
    onProfileChainToggle: (ProfileSummary) -> Unit = {},
    onShareProfile: (ProfileSummary) -> Unit,
    onQrProfile: (ProfileSummary) -> Unit,
    onEditProfileJson: (ProfileSummary) -> Unit,
    onEditProfileSimple: (ProfileSummary) -> Unit,
    onSubscriptionToggle: (SubscriptionEntity) -> Unit,
    onSubscriptionDelete: (Long) -> Unit,
    onSubscriptionSpeedTest: (Long) -> Unit,
    onSubscriptionUpdate: (SubscriptionEntity) -> Unit,
    onEditSubscriptionJson: (SubscriptionEntity) -> Unit,
    onSubscriptionPinToggle: (SubscriptionEntity) -> Unit,
    onSubscriptionQr: (SubscriptionEntity) -> Unit,
    onSubscriptionShare: (SubscriptionEntity) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val showTop by remember { derivedStateOf { listState.canScrollBackward } }
    val showBottom by remember { derivedStateOf { listState.canScrollForward } }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .fadingEdge(showTop = showTop, showBottom = showBottom),
        state = listState,
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {

        itemsIndexed(
            items = items,
            key = { _, item ->
                when (item) {
                    is DisplayItem.SubscriptionItem -> "sub_${item.entity.id}"
                    is DisplayItem.ProfileItem -> "prof_${item.entity.id}"
                }
            }
        ) { index, item ->
            if (index > 0 && item is DisplayItem.SubscriptionItem) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            Box(modifier = Modifier.animateItem()) {
                when (item) {
                    is DisplayItem.ProfileItem -> {
                        val isChained = chainedProfileIds.contains(item.entity.id)
                        val chainIdx = chainedProfileIds.indexOf(item.entity.id)
                        val chainNumber = if (chainIdx >= 0) chainIdx + 1 else null

                        SwipeToReveal(
                            cornerType = item.cornerType,
                            isChained = isChained,
                            onDeleteClick = { onProfileDelete(item.entity) },
                            onChainClick = { onProfileChainToggle(item.entity) }
                        ) {
                            val protocolDisplay = remember(item.entity.id, item.entity.uri, item.entity.protocol, item.entity.serverDescription) {
                                getProtocolDisplay(item.entity)
                            }
                            ProfileCard(
                                name = item.entity.name,
                                description = protocolDisplay,
                                isSelected = item.isSelected,
                                pingState = item.pingState,
                                pingStyle = pingStyle,
                                cornerType = item.cornerType,
                                chainNumber = chainNumber,
                                onClick = { onProfileClick(item.entity) },
                                onShareClick = { onShareProfile(item.entity) },
                                onQrCodeClick = { onQrProfile(item.entity) },
                                onEditJsonClick = { 
                                    if (item.entity.uri.startsWith("internal://json")) {
                                        onEditProfileJson(item.entity)
                                    } else {
                                        onEditProfileSimple(item.entity)
                                    }
                                },
                                onEditSimpleClick = { onEditProfileSimple(item.entity) },
                                accentColor = accentColor,
                                hazeState = hazeState
                            )
                        }
                    }
                    is DisplayItem.SubscriptionItem -> {
                        val used = item.entity.upload + item.entity.download
                        val progress = if (item.entity.total > 0) (used.toDouble() / item.entity.total).toFloat() else 0f
                        val displayName = if (item.entity.name == "Мои сервера" || item.entity.name == "My servers") {
                            I18n.strings.sub_my_servers
                        } else {
                            item.entity.name
                        }
                        SubscriptionCard(
                            name = displayName,
                            description = item.entity.description,
                            trafficInfo = formatTraffic(used, item.entity.total),
                            trafficProgress = progress,
                            expire = item.entity.expire,
                            updateInterval = item.entity.updateInterval,
                            isExpanded = item.isExpanded,
                            isRefreshing = item.isRefreshing,
                            cornerType = item.cornerType,
                            onUpdateClick = { onSubscriptionUpdate(item.entity) },
                            onSpeedTestClick = { onSubscriptionSpeedTest(item.entity.id) },
                            onEditJsonClick = { onEditSubscriptionJson(item.entity) },
                            onDeleteClick = { onSubscriptionDelete(item.entity.id) },
                            onClick = { onSubscriptionToggle(item.entity) },
                            isPinned = item.entity.pinned > 0L,
                            showQrAndLink = item.entity.id != -1L,
                            onPinClick = { onSubscriptionPinToggle(item.entity) },
                            onQrClick = { onSubscriptionQr(item.entity) },
                            onShareLinkClick = { onSubscriptionShare(item.entity) },
                            accentColor = accentColor,
                            hazeState = hazeState
                        )
                    }
                }
            }
        }
    }
}

private fun parseQuery(query: String?): Map<String, String> = query?.split("&")?.associate {
    val parts = it.split("=", limit = 2)
    val key = try { java.net.URLDecoder.decode(parts[0], "UTF-8") } catch (_: Exception) { parts[0] }
    val value = try { java.net.URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8") } catch (_: Exception) { parts.getOrElse(1) { "" } }
    key to value
} ?: emptyMap()

private fun parseVmessUri(uri: String): Map<String, String> {
    val b64 = uri.removePrefix("vmess://").trim()
    return try {
        val decoded = String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
        val json = org.json.JSONObject(decoded)
        val map = mutableMapOf<String, String>()
        val net = json.optString("net")
        if (net.isNotEmpty()) map["type"] = net
        val tls = json.optString("tls")
        if (tls.isNotEmpty()) map["security"] = tls
        map
    } catch (_: Exception) {
        try {
            val parsed = java.net.URI(uri)
            parseQuery(parsed.rawQuery)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

private fun parseShadowsocksParams(uri: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    try {
        val parsed = java.net.URI(uri)
        val query = parsed.rawQuery ?: ""
        val params = parseQuery(query)
        val plugin = params["plugin"] ?: ""
        val pluginOpts = params["plugin-opts"] ?: params["plugin_opts"] ?: ""
        val combinedOpts = if (plugin.contains(";")) plugin.substringAfter(";") else pluginOpts
        
        if (combinedOpts.contains("websocket") || combinedOpts.contains("mode=websocket") || combinedOpts.contains("ws") || params["type"] == "ws") {
            map["type"] = "ws"
        } else if (params.containsKey("type")) {
            map["type"] = params["type"] ?: ""
        }
        
        if (combinedOpts.contains("tls") || combinedOpts.contains("security=tls") || combinedOpts.contains("shadowtls") || params["security"] == "tls" || plugin.startsWith("shadowtls")) {
            map["security"] = "tls"
        } else if (params.containsKey("security")) {
            map["security"] = params["security"] ?: ""
        }
    } catch (_: Exception) {}
    return map
}

private fun getProtocolDisplay(entity: ProfileSummary): String {
    val rawProtocol = if (!entity.protocol.isNullOrBlank()) {
        entity.protocol.uppercase()
    } else if (entity.uri.startsWith("internal://json") || entity.uri.isBlank()) {
        "JSON"
    } else {
        entity.uri.substringBefore("://").uppercase()
    }

    val protocol = if (rawProtocol == "SS") "SHADOWSOCKS" else rawProtocol
    val isJson = entity.uri.startsWith("internal://json") || entity.uri.isBlank()

    var transport: String? = null
    var security: String? = null

    if (isJson) {
        val desc = entity.serverDescription
        return if (!desc.isNullOrBlank()) "$protocol / $desc | JSON" else "$protocol | JSON"
    } else {
        val uri = entity.uri
        val scheme = uri.substringBefore("://").lowercase()
        val params = when (scheme) {
            "vmess" -> parseVmessUri(uri)
            "ss", "shadowsocks" -> parseShadowsocksParams(uri)
            else -> {
                try {
                    val parsed = java.net.URI(uri)
                    parseQuery(parsed.rawQuery)
                } catch (_: Exception) {
                    emptyMap()
                }
            }
        }

        val typeVal = params["type"]?.lowercase()
        if (!typeVal.isNullOrBlank()) {
            transport = when (typeVal) {
                "tcp" -> "TCP"
                "ws" -> "WS"
                "grpc" -> "gRPC"
                "httpupgrade" -> "HTTPUpgrade"
                "h2" -> "H2"
                "http" -> "HTTP"
                "xhttp" -> "XHTTP"
                "quic" -> "QUIC"
                "kcp" -> "KCP"
                else -> typeVal.uppercase()
            }
        }

        val secVal = params["security"]?.lowercase()
        if (secVal == "tls") {
            security = "TLS"
        } else if (secVal == "reality") {
            security = "REALITY"
        }

        if (scheme == "hysteria" || scheme == "hysteria2" || scheme == "hy" || scheme == "hy2") {
            security = "TLS"
        }
    }

    val base = if (isJson) "$protocol | JSON" else protocol
    
    val parts = mutableListOf<String>()
    parts.add(base)
    if (!transport.isNullOrBlank()) {
        parts.add(transport)
    }
    if (!security.isNullOrBlank()) {
        parts.add(security)
    }

    val displayString = parts.joinToString(" / ")

    val description = entity.serverDescription
    return if (description.isNullOrBlank()) displayString else "$displayString | $description"
}


private fun formatTraffic(used: Long, total: Long): String {
    if (total == Long.MAX_VALUE || total <= 0) return "${formatBytes(used)} / ∞"
    return "${formatBytes(used)} / ${formatBytes(total)}"
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024 * 1024)
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return when {
        gb >= 1.0 -> java.lang.String.format(java.util.Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> java.lang.String.format(java.util.Locale.US, "%.2f MB", mb)
        else -> "$bytes B"
    }
}
