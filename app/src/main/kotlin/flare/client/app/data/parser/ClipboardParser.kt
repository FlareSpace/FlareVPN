package flare.client.app.data.parser

import flare.client.app.ui.i18n.I18n

import android.content.Context
import android.util.Base64
import flare.client.app.R
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.SubscriptionEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

object ClipboardParser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val singleSchemes = setOf("vless", "vmess", "ss", "trojan", "shadowsocks", "hysteria", "hy", "hysteria2", "hy2", "wireguard", "wg")

    private val PROFILE_TITLE_REGEX = Regex("""^(?:#|//|;)?\s*profile-title\s*[:=]\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val UPDATE_INTERVAL_REGEX = Regex("""^(?:#|//|;)?\s*(?:profile-update-interval|subscription-update-interval|update-interval|interval-update)\s*[:=]\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val INTERVAL_SUFFIX_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*([a-zA-Zа-яА-Я.]+)?$""")

    sealed class ParseResult {
        data class SingleProfile(val profile: ProfileEntity) : ParseResult()
        data class MultipleProfiles(val profiles: List<ProfileEntity>) : ParseResult()
        data class Subscription(val subscription: SubscriptionEntity, val profiles: List<ProfileEntity>) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    suspend fun parse(context: Context, text: String, hwid: String? = null, deviceName: String? = null, androidVersion: String? = null, userAgent: String? = null): ParseResult {
        val trimmed = text.trim()

        val multiLinks = extractSingleProxyLinks(trimmed)
        if (multiLinks.size > 1) {
            val profiles = multiLinks.mapNotNull { link ->
                try {
                    buildProfileFromUri(context, link, subscriptionId = null)
                } catch (_: Exception) {
                    null
                }
            }
            return if (profiles.isNotEmpty()) {
                ParseResult.MultipleProfiles(profiles)
            } else {
                ParseResult.Error(I18n.strings.error_invalid_format)
            }
        }

        return when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> parseFullJson(context, trimmed)
            singleSchemes.any { trimmed.startsWith("$it://", ignoreCase = true) } -> parseSingleProxy(context, trimmed)
            trimmed.startsWith("http://") -> ParseResult.Error(I18n.strings.error_subscription_https_required)
            trimmed.startsWith("https://") -> parseSubscriptionUrl(context, trimmed, hwid, deviceName, androidVersion, userAgent)
            else -> ParseResult.Error(I18n.strings.error_invalid_format)
        }
    }

    private fun extractSingleProxyLinks(text: String): List<String> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && singleSchemes.any { scheme -> line.startsWith("$scheme://", ignoreCase = true) } }
            .toList()
    }

    private fun parseSingleProxy(context: Context, uri: String): ParseResult {
        return try {
            val profile = buildProfileFromUri(context, uri, subscriptionId = null)
            ParseResult.SingleProfile(profile)
        } catch (e: Exception) {
            ParseResult.Error(I18n.strings.error_parsing.format(e.message ?: ""))
        }
    }

    private fun parseFullJson(context: Context, text: String): ParseResult {
        return try {
            val json = JSONObject(text)
            val name = extractNameFromJson(context, json)
            val configJson = V2RayConfigConverter.convertIfNeeded(text)
            val protocol = try {
                val outbounds = JSONObject(configJson).optJSONArray("outbounds")
                outbounds?.optJSONObject(0)?.optString("type")
            } catch (_: Exception) { null }
            val desc = ProfileParsingHelper.parseTransportAndSecurityFromJson(configJson)
            ParseResult.SingleProfile(ProfileEntity(name = name, uri = "internal://json", configJson = configJson, serverDescription = desc, subscriptionId = null, protocol = protocol))
        } catch (e: Exception) {
            ParseResult.Error(I18n.strings.error_json.format(e.message ?: ""))
        }
    }

    private suspend fun parseSubscriptionUrl(context: Context, url: String, hwid: String? = null, deviceName: String? = null, androidVersion: String? = null, userAgent: String? = null): ParseResult {
        return try {
            val ua = when (userAgent) {
                null -> "Happ/3.21.1"
                "custom" -> "Flare/1.2.0"
                else -> userAgent
            }
            var finalUrl = url
            
            
            if (hwid != null) {
                val base = if (url.contains("#")) url.substringBefore("#") else url
                val fragment = if (url.contains("#")) "#" + url.substringAfter("#") else ""
                var currentUrl = base
                
                val separator = if (currentUrl.contains("?")) "&" else "?"
                currentUrl = "$currentUrl${separator}hwid=$hwid"
                finalUrl = currentUrl + fragment
            }

            val requestBuilder = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", ua)
            
            
            if (androidVersion != null) {
                requestBuilder.header("x-ver-os", androidVersion)
            }
            if (hwid != null) {
                requestBuilder.header("x-hwid", hwid)
            }
            if (deviceName != null) {
                requestBuilder.header("x-device-model", deviceName)
            }

            val request = requestBuilder.build()
            val response = try {
                httpClient.newCall(request).await()
            } catch (e: Exception) {
                return ParseResult.Error(I18n.strings.error_subscription.format(e.message ?: ""))
            }

            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return ParseResult.Error(I18n.strings.error_subscription.format("HTTP $code"))
            }

            val body = response.body.string()
            val profileTitle = response.header("profile-title")
            val contentDisposition = response.header("content-disposition")

            val proxyLines = decodeSubscriptionBody(body)
            var bodyProfileTitle: String? = null
            var bodyUpdateInterval: String? = null
            
            val filteredProxyLines = proxyLines.filter { line ->
                val trimmedLine = line.trim()
                val match = PROFILE_TITLE_REGEX.find(trimmedLine)
                if (match != null) {
                    if (bodyProfileTitle == null) {
                        bodyProfileTitle = match.groupValues[1].trim()
                    }
                    false
                } else {
                    val matchInterval = UPDATE_INTERVAL_REGEX.find(trimmedLine)
                    if (matchInterval != null) {
                        if (bodyUpdateInterval == null) {
                            bodyUpdateInterval = matchInterval.groupValues[1].trim()
                        }
                        false
                    } else {
                        true
                    }
                }
            }

            val finalProfileTitle = profileTitle ?: bodyProfileTitle
            val name = extractSubscriptionName(url, finalProfileTitle, contentDisposition)
            
            var headerInterval: String? = null
            for (headerName in response.headers.names()) {
                val lowerName = headerName.lowercase()
                if (lowerName.contains("update-interval") || lowerName.contains("interval-update")) {
                    val valStr = response.header(headerName)
                    if (!valStr.isNullOrBlank()) {
                        headerInterval = valStr
                        break
                    }
                }
            }
            val finalIntervalStr = headerInterval ?: bodyUpdateInterval
            val parsedIntervalSeconds = finalIntervalStr?.let { parseIntervalToSeconds(it) } ?: 0L

            val userInfo = response.header("subscription-userinfo")
            val descParts = mutableListOf<String>()

            val announce = response.header("announce")
            if (announce != null) descParts.add(decodeIfNeeded(announce))

            val profileDesc = response.header("profile-description") ?: response.header("profile-message") ?: response.header("description")
            if (profileDesc != null) descParts.add(decodeIfNeeded(profileDesc))

            val supportUrl = response.header("support-url") ?: ""
            val webPageUrl = response.header("profile-web-page-url") ?: ""

            val description = descParts.joinToString("\n")
            var upload = 0L
            var download = 0L
            var total = 0L
            var expire = 0L
            if (userInfo != null) {
                val parts = userInfo.split(";")
                for (part in parts) {
                    val kv = part.split("=", limit = 2)
                    if (kv.size == 2) {
                        val k = kv[0].trim()
                        val v = kv[1].trim().toLongOrNull() ?: 0L
                        when (k) {
                            "upload" -> upload = v
                            "download" -> download = v
                            "total" -> total = v
                            "expire" -> expire = v
                        }
                    }
                }
            }
            val profiles = filteredProxyLines.mapIndexedNotNull { _, line -> try { buildProfileFromUri(context, line.trim(), 0L) } catch (_: Exception) { null } }
            response.close()
            if (profiles.isEmpty()) return ParseResult.Error(I18n.strings.error_subscription_empty)
            ParseResult.Subscription(
                SubscriptionEntity(
                    name = name,
                    url = url,
                    upload = upload,
                    download = download,
                    total = total,
                    expire = expire,
                    description = description,
                    supportUrl = supportUrl,
                    webPageUrl = webPageUrl,
                    updateInterval = parsedIntervalSeconds,
                    lastUpdated = System.currentTimeMillis()
                ),
                profiles
            )
        } catch (e: Exception) {
            ParseResult.Error(I18n.strings.error_subscription.format(e.message ?: ""))
        }
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            })
            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                    
                }
            }
        }
    }

    private fun extractSubscriptionName(url: String, profileTitle: String?, contentDisposition: String?): String {
        if (!profileTitle.isNullOrBlank()) {
            return decodeIfNeeded(profileTitle)
        }
        if (!contentDisposition.isNullOrBlank()) {
            val filename = contentDisposition.split(";")
                .map { it.trim() }
                .find { it.startsWith("filename=") }
                ?.substringAfter("filename=")
                ?.removeSurrounding("\"")
            if (!filename.isNullOrBlank()) return filename
        }
        return try { URI(url).host ?: url } catch (_: Exception) { url }
    }

    private fun decodeIfNeeded(text: String): String {
        return try {
            val trimmed = text.trim()
            val decodedBase64 = if (trimmed.startsWith("base64:")) {
                try {
                    val b64 = trimmed.substringAfter("base64:")
                    String(Base64.decode(b64.trim(), Base64.DEFAULT)).trim()
                } catch (_: Exception) { trimmed }
            } else {
                trimmed
            }
            java.net.URLDecoder.decode(decodedBase64, "UTF-8")
        } catch (_: Exception) {
            text.trim()
        }
    }

    private fun extractNameFromJson(context: Context, json: JSONObject): String {
        return json.optString("remarks").takeIf { it.isNotBlank() }
            ?: json.optString("tag").takeIf { it.isNotBlank() }
            ?: json.optJSONArray("outbounds")?.optJSONObject(0)?.let {
                it.optString("tag").takeIf { tag -> tag.isNotBlank() && tag != "proxy" }
                    ?: it.optString("server").takeIf { srv -> srv.isNotBlank() }
            }
            ?: I18n.strings.label_imported_profile
    }

    private fun decodeSubscriptionBody(body: String): List<String> {
        val trimmed = body.trim()

        if (trimmed.startsWith("[")) {
            return try {
                val arr = org.json.JSONArray(trimmed)
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                    .ifEmpty {
                        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.toString() }
                    }
            } catch (_: Exception) {
                body.lineSequence().filter { it.isNotBlank() }.toList()
            }
        }

        val lines = trimmed.lineSequence().filter { it.isNotBlank() }.toList()
        val looksLikePlainList = lines.isNotEmpty() && lines.all { line ->
            val l = line.trim()
            l.contains("://") || l.startsWith("{") || l.startsWith("[")
        }
        if (looksLikePlainList && lines.size > 1) {
            return splitJsonAware(trimmed)
        }

        val flat = trimmed.replace("\r", "").replace("\n", "")
        return try {
            val clean = flat.replace("-", "+").replace("_", "/")
            val padded = when (clean.length % 4) { 2 -> "$clean=="; 3 -> "$clean="; else -> clean }
            val decoded = String(Base64.decode(padded, Base64.DEFAULT)).trim()
            splitJsonAware(decoded)
        } catch (_: Exception) {
            splitJsonAware(trimmed)
        }
    }

    private fun splitJsonAware(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        val current = StringBuilder()
        var inJson = false

        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty()) {
                if (inJson && depth == 0 && current.isNotEmpty()) {
                    results.add(current.toString().trim())
                    current.clear()
                    inJson = false
                }
                continue
            }

            if (!inJson && (l.startsWith("{") || l.startsWith("["))) {
                inJson = true
            }

            if (inJson) {
                current.append(line).append('\n')
                for (i in 0 until l.length) {
                    val c = l[i]
                    if (c == '{' || c == '[') depth++
                    else if (c == '}' || c == ']') depth--
                }
                if (depth <= 0) {
                    results.add(current.toString().trim())
                    current.clear()
                    inJson = false
                    depth = 0
                }
            } else {
                if (l.isNotEmpty()) results.add(l)
            }
        }
        if (current.isNotEmpty()) results.add(current.toString().trim())
        return results.filter { it.isNotBlank() }
    }

    fun buildProfileFromUri(context: Context, uri: String, subscriptionId: Long?): ProfileEntity {
        val trimmed = uri.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val json = JSONObject(trimmed)
            val name = extractNameFromJson(context, json)
            val configJson = V2RayConfigConverter.convertIfNeeded(trimmed)
            val protocol = try {
                val outbounds = JSONObject(configJson).optJSONArray("outbounds")
                outbounds?.optJSONObject(0)?.optString("type")
            } catch (_: Exception) { null }
            val desc = ProfileParsingHelper.parseTransportAndSecurityFromJson(configJson)
            return ProfileEntity(name = name, uri = "internal://json", configJson = configJson, serverDescription = desc, subscriptionId = subscriptionId, protocol = protocol)
        }

        var processedUri = uri
        

        val parsed = URI(processedUri)
        val scheme = parsed.scheme?.lowercase() ?: ""
        val displayName = extractDisplayName(processedUri)
        val params = parseQuery(parsed.rawQuery)

        var vmessJson: JSONObject? = null
        val proxyServer = when (scheme) {
            "vmess" -> {
                val b64 = processedUri.removePrefix("vmess://").trim()
                try {
                    val decoded = String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
                    val json = JSONObject(decoded)
                    vmessJson = json
                    json.optString("add", "")
                } catch (_: Exception) { "" }
            }
            else -> parsed.host ?: ""
        }

        val xrayOutbound = when (scheme) {
            "vless" -> buildVlessOutbound(parsed, params)
            "vmess" -> buildVmessOutbound(processedUri, vmessJson)
            "ss", "shadowsocks" -> buildShadowsocksOutbound(processedUri)
            "trojan" -> buildTrojanOutbound(parsed, params)
            "hysteria", "hy" -> buildHysteriaOutbound(parsed, params)
            "hysteria2", "hy2" -> buildHysteria2Outbound(parsed, params)
            "wireguard", "wg" -> null
            else -> throw IllegalArgumentException("Protocol $scheme not supported")
        }
        val protocol = if (scheme == "wg" || scheme == "wireguard") "wireguard" else scheme
        val configJson = if (scheme == "wireguard" || scheme == "wg") {
            val wgEndpoint = buildWireGuardEndpoint(processedUri, parsed, params)
            buildMinimalSingBoxConfigWithEndpoint(wgEndpoint, proxyServer)
        } else {
            val sbOutbounds = V2RayConfigConverter.convertOutboundsPublic(JSONArray().put(xrayOutbound))
            val list = mutableListOf<JSONObject>()
            for (i in 0 until sbOutbounds.length()) {
                sbOutbounds.optJSONObject(i)?.let { list.add(it) }
            }
            if (list.isEmpty()) {
                throw IllegalArgumentException("Failed to convert outbound for $scheme")
            }
            buildMinimalSingBoxConfig(list, proxyServer)
        }
        return ProfileEntity(name = displayName, uri = processedUri, configJson = configJson, subscriptionId = subscriptionId, protocol = protocol)
    }

    private fun buildMinimalSingBoxConfig(proxyOutbounds: List<JSONObject>, proxyServer: String): String {
        val sb = JSONObject()

        sb.put("log", JSONObject().apply {
            put("level", "info")
            put("timestamp", true)
        })

        val proxyDomains = JSONArray().apply {
            if (proxyServer.isNotEmpty() && !proxyServer[0].isDigit()) put(proxyServer)
        }
        sb.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "dns-remote")
                    put("type", "https")
                    put("server", "1.1.1.1")
                    put("path", "/dns-query")
                    put("domain_resolver", "dns-direct")
                    put("detour", "proxy")
                })
                put(JSONObject().apply {
                    put("tag", "dns-direct")
                    put("type", "udp")
                    put("server", "8.8.8.8")
                })

            })
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("outbound", JSONArray().put("direct"))
                    put("server", "dns-direct")
                })
                if (proxyDomains.length() > 0) {
                    put(JSONObject().apply {
                        put("domain", proxyDomains)
                        put("server", "dns-direct")
                    })
                }
            })
            put("final", "dns-remote")
            put("strategy", "prefer_ipv4")
            put("independent_cache", true)
        })

        sb.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                put("address", JSONArray().apply {
                    put("172.19.0.1/30")
                    put("fdfe:dcba:9876::1/126")
                })
                put("mtu", 1500)
                put("auto_route", true)
                put("strict_route", true)
                put("stack", "mixed")
            })
        })

        sb.put("outbounds", JSONArray().apply {
            proxyOutbounds.forEach { put(it) }
            put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            put(JSONObject().apply { put("type", "block"); put("tag", "block") })
        })

        sb.put("route", JSONObject().apply {
            put("auto_detect_interface", false)
            put("final", "proxy")
            put("rules", JSONArray().apply {
                put(JSONObject().apply { put("protocol", "dns"); put("action", "hijack-dns") })
                put(JSONObject().apply { put("port", 53); put("action", "hijack-dns") })
                put(JSONObject().apply { put("action", "sniff") })
                put(JSONObject().apply { put("protocol", JSONArray().put("bittorrent")); put("outbound", "direct") })
                put(JSONObject().apply { put("ip_is_private", true); put("outbound", "direct") })
                if (proxyDomains.length() > 0) {
                    put(JSONObject().apply { put("domain", proxyDomains); put("outbound", "direct") })
                }
                put(JSONObject().apply { put("rule_set", JSONArray().put("geosite-ru")); put("outbound", "direct") })
                put(JSONObject().apply { put("rule_set", JSONArray().put("geoip-ru")); put("outbound", "direct") })
            })
            put("rule_set", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "geosite-ru")
                    put("type", "local")
                    put("format", "binary")
                    put("path", "geosite-ru.srs")
                })
                put(JSONObject().apply {
                    put("tag", "geoip-ru")
                    put("type", "local")
                    put("format", "binary")
                    put("path", "geoip-ru.srs")
                })
            })
        })

        return sb.toString(2).replace("\\/", "/")
    }

    
    
    private fun buildMinimalSingBoxConfigWithEndpoint(wgEndpoint: JSONObject, proxyServer: String): String {
        val sb = JSONObject()

        sb.put("log", JSONObject().apply {
            put("level", "info")
            put("timestamp", true)
        })

        val proxyDomains = JSONArray().apply {
            if (proxyServer.isNotEmpty() && !proxyServer[0].isDigit()) put(proxyServer)
        }

        sb.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "dns-remote")
                    put("type", "https")
                    put("server", "1.1.1.1")
                    put("path", "/dns-query")
                    put("domain_resolver", "dns-direct")
                    put("detour", "proxy")
                })
                put(JSONObject().apply {
                    put("tag", "dns-direct")
                    put("type", "udp")
                    put("server", "8.8.8.8")
                })
            })
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("outbound", JSONArray().put("direct"))
                    put("server", "dns-direct")
                })
                if (proxyDomains.length() > 0) {
                    put(JSONObject().apply {
                        put("domain", proxyDomains)
                        put("server", "dns-direct")
                    })
                }
            })
            put("final", "dns-remote")
            put("strategy", "prefer_ipv4")
            put("independent_cache", true)
        })

        sb.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                put("address", JSONArray().apply {
                    put("172.19.0.1/30")
                    put("fdfe:dcba:9876::1/126")
                })
                put("mtu", 1500)
                put("auto_route", true)
                put("strict_route", true)
                put("stack", "mixed")
            })
        })

        
        
        sb.put("endpoints", JSONArray().put(wgEndpoint))

        sb.put("outbounds", JSONArray().apply {
            put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            put(JSONObject().apply { put("type", "block"); put("tag", "block") })
        })

        sb.put("route", JSONObject().apply {
            put("auto_detect_interface", false)
            put("final", "proxy")
            put("rules", JSONArray().apply {
                put(JSONObject().apply { put("protocol", "dns"); put("action", "hijack-dns") })
                put(JSONObject().apply { put("port", 53); put("action", "hijack-dns") })
                put(JSONObject().apply { put("action", "sniff") })
                put(JSONObject().apply { put("protocol", JSONArray().put("bittorrent")); put("outbound", "direct") })
                put(JSONObject().apply { put("ip_is_private", true); put("outbound", "direct") })
                if (proxyDomains.length() > 0) {
                    put(JSONObject().apply { put("domain", proxyDomains); put("outbound", "direct") })
                }
                put(JSONObject().apply { put("rule_set", JSONArray().put("geosite-ru")); put("outbound", "direct") })
                put(JSONObject().apply { put("rule_set", JSONArray().put("geoip-ru")); put("outbound", "direct") })
            })
            put("rule_set", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "geosite-ru")
                    put("type", "local")
                    put("format", "binary")
                    put("path", "geosite-ru.srs")
                })
                put(JSONObject().apply {
                    put("tag", "geoip-ru")
                    put("type", "local")
                    put("format", "binary")
                    put("path", "geoip-ru.srs")
                })
            })
        })

        return sb.toString(2).replace("\\/", "/")
    }

    private fun buildVlessOutbound(parsed: URI, params: Map<String, String>): JSONObject = JSONObject().apply {
        put("protocol", "vless")
        put("tag", "proxy")
        val pe = params["packetEncoding"] ?: params["packet_encoding"]
        if (pe != null) {
            put("packet_encoding", pe)
        }
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(JSONObject().apply {
                put("address", parsed.host)
                put("port", if (parsed.port > 0) parsed.port else 443)
                put("users", JSONArray().put(JSONObject().apply {
                    put("id", parsed.userInfo)
                    put("flow", params["flow"] ?: "")
                    put("encryption", "none")
                }))
            }))
        })
        put("streamSettings", buildStreamSettings(parsed.host, params))
    }

    private fun buildVmessOutbound(uri: String, parsedJson: JSONObject? = null): JSONObject {
        val json = parsedJson ?: run {
            val b64 = uri.removePrefix("vmess://").trim()
            JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
        }
        return JSONObject().apply {
            put("protocol", "vmess")
            put("tag", "proxy")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", json.optString("add"))
                    put("port", json.optInt("port", 443))
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", json.optString("id"))
                        put("alterId", json.optInt("aid", 0))
                        put("security", "auto")
                    }))
                }))
            })
            val params = mutableMapOf<String, String>()
            params["security"] = json.optString("tls").takeIf { it.isNotBlank() } ?: "none"
            params["type"] = json.optString("net").takeIf { it.isNotBlank() } ?: "tcp"
            params["path"] = json.optString("path", "")
            params["host"] = json.optString("host", "")
            params["sni"] = json.optString("sni", "")
            params["pbk"] = json.optString("pbk", "")
            params["sid"] = json.optString("sid", "")
            params["fp"] = json.optString("fp", "")
            params["insecure"] = json.optString("insecure", "")
            params["allowInsecure"] = json.optString("allowInsecure", "")

            put("streamSettings", buildStreamSettings(json.optString("add"), params))
        }
    }

    private fun buildShadowsocksOutbound(uri: String): JSONObject {
        val clean = uri.trim()
        val schemePrefix = if (clean.startsWith("shadowsocks://", ignoreCase = true)) {
            "shadowsocks://"
        } else {
            "ss://"
        }
        
        
        val mainPart = clean.substring(schemePrefix.length).substringBefore("#")
        val urlPart = mainPart.substringBefore("?")
        val queryPart = if (mainPart.contains("?")) mainPart.substringAfter("?") else ""
        
        
        var decodedMain = ""
        try {
            val normalizedB64 = urlPart.replace("-", "+").replace("_", "/")
            val padded = when (normalizedB64.length % 4) {
                2 -> "$normalizedB64=="
                3 -> "$normalizedB64="
                else -> normalizedB64
            }
            val decodedBytes = Base64.decode(padded, Base64.DEFAULT)
            decodedMain = String(decodedBytes, Charsets.UTF_8)
        } catch (_: Exception) {}
        
        var method = ""
        var password = ""
        var host = ""
        var port = 8388
        
        if (decodedMain.contains("@") && decodedMain.contains(":")) {
            val methodPassword = decodedMain.substringBeforeLast("@")
            val hostPort = decodedMain.substringAfterLast("@")
            
            method = methodPassword.substringBefore(":")
            password = methodPassword.substringAfter(":")
            
            if (hostPort.contains(":")) {
                host = hostPort.substringBeforeLast(":")
                port = hostPort.substringAfterLast(":").toIntOrNull() ?: 8388
            } else {
                host = hostPort
            }
        } else {
            
            
            if (urlPart.contains("@")) {
                val authority = urlPart.substringBeforeLast("@")
                val hostPort = urlPart.substringAfterLast("@")
                
                val decodedAuth = if (!authority.contains(":")) {
                    try {
                        val normalizedB64 = authority.replace("-", "+").replace("_", "/")
                        val padded = when (normalizedB64.length % 4) {
                            2 -> "$normalizedB64=="
                            3 -> "$normalizedB64="
                            else -> normalizedB64
                        }
                        String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8)
                    } catch (_: Exception) {
                        authority
                    }
                } else {
                    authority
                }
                
                method = decodedAuth.substringBefore(":")
                password = decodedAuth.substringAfter(":")
                
                if (hostPort.contains(":")) {
                    host = hostPort.substringBeforeLast(":")
                    port = hostPort.substringAfterLast(":").toIntOrNull() ?: 8388
                } else {
                    host = hostPort
                }
            } else {
                
                try {
                    val parsed = URI(uri)
                    host = parsed.host ?: ""
                    port = if (parsed.port > 0) parsed.port else 8388
                    val userInfo = try { String(Base64.decode(parsed.userInfo ?: "", Base64.DEFAULT)) } catch (_: Exception) { parsed.userInfo ?: ":" }
                    method = userInfo.substringBefore(":")
                    password = userInfo.substringAfter(":")
                } catch (_: Exception) {}
            }
        }
        
        if (host.isEmpty()) {
            throw IllegalArgumentException("Invalid Shadowsocks URI: host is empty")
        }
        
        val outbound = JSONObject().apply {
            put("protocol", "shadowsocks")
            put("tag", "proxy")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("method", method)
                    put("password", password)
                }))
            })
        }
        
        val params = parseQuery(queryPart)
        if (params.isNotEmpty()) {
            val processedParams = params.toMutableMap()
            val pluginVal = params["plugin"] ?: ""
            val hasPlugin = pluginVal.isNotEmpty() || params.containsKey("plugin-opts") || params.containsKey("plugin_opts")
            if (hasPlugin) {
                val pluginName = if (pluginVal.isNotEmpty()) pluginVal.substringBefore(";") else "v2ray-plugin"
                val optionsStr = if (pluginVal.contains(";")) {
                    pluginVal.substringAfter(";")
                } else {
                    params["plugin-opts"] ?: params["plugin_opts"] ?: ""
                }
                
                outbound.put("plugin", pluginName)
                outbound.put("plugin_opts", optionsStr)
                
                val optsMap = optionsStr.split(";").associate { opt ->
                    val parts = opt.split("=", limit = 2)
                    if (parts.size == 2) {
                        parts[0].trim() to parts[1].trim()
                    } else {
                        opt.trim() to "true"
                    }
                }
                
                if (optsMap["mode"] == "websocket" || optsMap.containsKey("websocket") || optsMap.containsKey("mode=websocket")) {
                    processedParams["type"] = "ws"
                }
                optsMap["path"]?.let { processedParams["path"] = it }
                if (optsMap.containsKey("tls")) {
                    processedParams["security"] = "tls"
                }
                optsMap["host"]?.let {
                    processedParams["host"] = it
                    if (!optsMap.containsKey("sni")) {
                        processedParams["sni"] = it
                    }
                }
                optsMap["sni"]?.let {
                    processedParams["sni"] = it
                }
                if (optsMap.containsKey("skipCertVerify") || optsMap.containsKey("skip-cert-verify")) {
                    processedParams["allowInsecure"] = "1"
                }
            }
            outbound.put("streamSettings", buildStreamSettings(host, processedParams))
        }
        return outbound
    }

    private fun buildTrojanOutbound(parsed: URI, params: Map<String, String>): JSONObject = JSONObject().apply {
        put("protocol", "trojan")
        put("tag", "proxy")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", parsed.host)
                put("port", if (parsed.port > 0) parsed.port else 443)
                put("password", parsed.userInfo)
            }))
        })
        put("streamSettings", buildStreamSettings(parsed.host, params))
    }

    private fun buildHysteriaOutbound(parsed: URI, params: Map<String, String>): JSONObject = JSONObject().apply {
        val authString = parsed.userInfo?.takeIf { it.isNotBlank() }
            ?: firstNonBlankParam(params, "auth", "auth_str", "auth-str", "password")
            ?: ""
        val upMbps = firstNonBlankParam(params, "upmbps", "up-mbps", "up")?.toIntOrNull()
        val downMbps = firstNonBlankParam(params, "downmbps", "down-mbps", "down")?.toIntOrNull()
        val obfs = firstNonBlankParam(params, "obfs")
        val sni = firstNonBlankParam(params, "sni", "peer") ?: parsed.host ?: ""
        val insecure = parseFlexibleBoolean(
            firstNonBlankParam(params, "insecure", "allowInsecure", "skip-cert-verify")
        ) ?: false
        val alpn = firstNonBlankParam(params, "alpn")

        put("protocol", "hysteria")
        put("tag", "proxy")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", parsed.host)
                put("port", if (parsed.port > 0) parsed.port else 443)
                put("password", authString)
            }))
            if (upMbps != null && upMbps > 0) put("up_mbps", upMbps)
            if (downMbps != null && downMbps > 0) put("down_mbps", downMbps)
            if (!obfs.isNullOrBlank()) {
                put("obfs", obfs)
            }
        })
        put("streamSettings", JSONObject().apply {
            put("security", "tls")
            put("tlsSettings", JSONObject().apply {
                put("serverName", sni)
                put("allowInsecure", insecure)
                if (!alpn.isNullOrBlank()) put("alpn", alpn)
            })
        })
    }

    private fun buildHysteria2Outbound(parsed: URI, params: Map<String, String>): JSONObject = JSONObject().apply {
        val password = parsed.userInfo?.takeIf { it.isNotBlank() }
            ?: firstNonBlankParam(params, "password", "auth")
            ?: ""
        val upMbps = firstNonBlankParam(params, "upmbps", "up-mbps", "up")?.toIntOrNull()
        val downMbps = firstNonBlankParam(params, "downmbps", "down-mbps", "down")?.toIntOrNull()
        val obfsType = firstNonBlankParam(params, "obfs", "obfs-type")
        val obfsPassword = firstNonBlankParam(params, "obfs-password", "obfspassword")
        val sni = firstNonBlankParam(params, "sni", "peer") ?: parsed.host ?: ""
        val insecure = parseFlexibleBoolean(
            firstNonBlankParam(params, "insecure", "allowInsecure", "skip-cert-verify")
        ) ?: false
        val alpn = firstNonBlankParam(params, "alpn")
        val pin = firstNonBlankParam(params, "pin")
        val mport = firstNonBlankParam(params, "mport")
        val hopInterval = firstNonBlankParam(params, "hop_interval", "hop-interval", "hopInterval")

        put("protocol", "hysteria2")
        put("tag", "proxy")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", parsed.host)
                put("port", if (parsed.port > 0) parsed.port else 443)
                put("password", password)
            }))
            if (upMbps != null && upMbps > 0) put("up_mbps", upMbps)
            if (downMbps != null && downMbps > 0) put("down_mbps", downMbps)
            if (!mport.isNullOrBlank()) put("mport", mport)
            if (!hopInterval.isNullOrBlank()) put("hop_interval", hopInterval)
            if (!obfsType.isNullOrBlank()) {
                put("obfs", JSONObject().apply {
                    put("type", obfsType)
                    if (!obfsPassword.isNullOrBlank()) put("password", obfsPassword)
                })
            }
        })
        put("streamSettings", JSONObject().apply {
            put("security", "tls")
            put("tlsSettings", JSONObject().apply {
                put("serverName", sni)
                put("allowInsecure", insecure)
                if (!alpn.isNullOrBlank()) put("alpn", alpn)
                if (!pin.isNullOrBlank()) put("pin", pin)
            })
        })
    }

    
    private fun buildWireGuardEndpoint(rawUri: String, parsed: URI, params: Map<String, String>): JSONObject = JSONObject().apply {
        val privateKey = extractWireGuardPrivateKey(rawUri, parsed, params)
            ?: throw IllegalArgumentException("WireGuard private key is missing")
        val peerPublicKey = firstNonBlankParam(
            params,
            "publickey",
            "public-key",
            "peer_public_key",
            "peer-public-key"
        ) ?: throw IllegalArgumentException("WireGuard peer public key is missing")
        val localAddresses = parseWireGuardLocalAddresses(params)
        val server = parsed.host ?: firstNonBlankParam(params, "server", "host")
        if (server.isNullOrBlank()) {
            throw IllegalArgumentException("WireGuard endpoint host is missing")
        }
        val serverPort = if (parsed.port > 0) parsed.port else (firstNonBlankParam(params, "port")?.toIntOrNull() ?: 51820)

        
        put("type", "wireguard")
        put("tag", "proxy")
        put("address", localAddresses)
        put("private_key", privateKey)

        val peerObj = JSONObject().apply {
            put("address", server)       
            put("port", serverPort)      
            put("public_key", peerPublicKey)
            put("allowed_ips", JSONArray().apply {
                put("0.0.0.0/0")
                put("::/0")
            })
            firstNonBlankParam(params, "presharedkey", "pre_shared_key", "pre-shared-key")?.let {
                put("pre_shared_key", it)
            }
            parseWireGuardReserved(params)?.let { put("reserved", it) }
        }

        put("peers", JSONArray().put(peerObj))

        firstNonBlankParam(params, "mtu")?.toIntOrNull()?.takeIf { it > 0 }?.let { put("mtu", it) }
    }

    private fun buildStreamSettings(host: String, params: Map<String, String>): JSONObject = JSONObject().apply {
        val security = params["security"] ?: "none"
        put("security", security)
        val networkType = params["type"] ?: "tcp"
        put("network", networkType)
        if (security == "tls") {
            put("tlsSettings", JSONObject().apply {
                put("serverName", params["sni"] ?: host)
                val ins = parseFlexibleBoolean(firstNonBlankParam(params, "insecure", "allowInsecure", "skip-cert-verify", "allowinsecure")) ?: false
                put("allowInsecure", ins)
                val pinVal = params["pin"]
                if (!pinVal.isNullOrBlank()) {
                    put("pin", pinVal)
                }
            })
        } else if (security == "reality") {
            put("realitySettings", JSONObject().apply {
                put("serverName", params["sni"] ?: host)
                put("publicKey", params["pbk"] ?: "")
                put("shortId", params["sid"] ?: "")
                put("fingerprint", params["fp"] ?: "chrome")
            })
        }
        when (networkType) {
            "tcp" -> {
                val tcpHostVal = params["host"] ?: ""
                val tcpPathVal = params["path"] ?: ""
                if (tcpHostVal.isNotEmpty() || tcpPathVal.isNotEmpty()) {
                    put("tcpSettings", JSONObject().apply {
                        put("header", JSONObject().apply {
                            put("type", "http")
                            put("request", JSONObject().apply {
                                put("version", "1.1")
                                put("method", "GET")
                                put("path", JSONArray().put(if (tcpPathVal.isNotEmpty()) tcpPathVal else "/"))
                                put("headers", JSONObject().apply {
                                    put("Host", JSONArray().put(tcpHostVal))
                                })
                            })
                        })
                    })
                }
            }
            "kcp" -> {
                put("kcpSettings", JSONObject().apply {
                    put("seed", params["seed"] ?: params["kcpSeed"] ?: "")
                    put("mtu", params["mtu"]?.toIntOrNull() ?: 1350)
                    put("tti", params["tti"]?.toIntOrNull() ?: 50)
                })
            }
            "ws" -> {
                put("wsSettings", JSONObject().apply {
                    put("path", params["path"] ?: "/")
                    put("headers", JSONObject().apply {
                        put("Host", params["host"] ?: "")
                    })
                })
            }
            "httpupgrade", "httpUpgrade" -> {
                put("httpUpgradeSettings", JSONObject().apply {
                    put("path", params["path"] ?: "/")
                    put("host", params["host"] ?: "")
                })
            }
            "h2", "http" -> {
                put("httpSettings", JSONObject().apply {
                    put("path", params["path"] ?: "/")
                    put("host", params["host"] ?: "")
                })
            }
            "xhttp" -> {
                put("xhttpSettings", JSONObject().apply {
                    put("path", params["path"] ?: "/")
                    val hostVal = params["host"] ?: ""
                    if (hostVal.isNotEmpty()) {
                        put("host", hostVal)
                    }
                    val modeVal = params["mode"] ?: "auto"
                    put("mode", modeVal)
                })
            }
            "quic" -> {
                put("quicSettings", JSONObject().apply {
                    put("security", params["quicSecurity"] ?: params["security"] ?: "none")
                    put("key", params["key"] ?: params["quicKey"] ?: "")
                })
            }
            "grpc" -> {
                put("grpcSettings", JSONObject().apply {
                    put("serviceName", params["serviceName"] ?: "")
                    put("authority", params["authority"] ?: params["grpcAuthority"] ?: "")
                })
            }
        }
    }

    private fun extractDisplayName(uri: String): String = try {
        val fragment = URI(uri).fragment
        if (!fragment.isNullOrBlank()) URLDecoder.decode(fragment, "UTF-8") else uri.substringBefore("?").take(40)
    } catch (_: Exception) { uri.substringBefore("?").take(40) }

    private fun firstNonBlankParam(params: Map<String, String>, vararg keys: String): String? {
        for (key in keys) {
            val value = params[key]
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun parseWireGuardLocalAddresses(params: Map<String, String>): JSONArray {
        val addressRaw = firstNonBlankParam(params, "address", "local_address", "local-address")
            ?: "10.7.0.2/32"
        val addresses = addressRaw
            .split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return JSONArray().apply {
            addresses.forEach { put(it) }
        }
    }

    private fun parseWireGuardReserved(params: Map<String, String>): JSONArray? {
        val reservedRaw = firstNonBlankParam(params, "reserved") ?: return null
        val bytes = reservedRaw
            .split(",", ";")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..255 }
        if (bytes.size != 3) return null
        return JSONArray().apply { bytes.forEach { put(it) } }
    }

    private fun extractWireGuardPrivateKey(rawUri: String, parsed: URI, params: Map<String, String>): String? {
        val fromQuery = firstNonBlankParam(params, "privatekey", "private_key", "private-key", "secretkey", "secret_key")
        if (!fromQuery.isNullOrBlank()) return fromQuery
        val parsedInfo = parsed.userInfo
        if (!parsedInfo.isNullOrBlank()) return parsedInfo

        val prefix = "${parsed.scheme}://"
        if (!rawUri.startsWith(prefix)) return null
        val authorityPart = rawUri.removePrefix(prefix).substringBefore("/")
        val rawUserInfo = authorityPart.substringBefore("@", "")
        return if (rawUserInfo.isBlank()) null else URLDecoder.decode(rawUserInfo, "UTF-8")
    }

    private fun parseFlexibleBoolean(value: String?): Boolean? {
        val normalized = value?.trim()?.lowercase() ?: return null
        return when (normalized) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> null
        }
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        val result = HashMap<String, String>()
        var start = 0
        val len = query.length
        while (start < len) {
            var nextAmp = query.indexOf('&', start)
            if (nextAmp == -1) nextAmp = len
            
            val eq = query.indexOf('=', start)
            if (eq != -1 && eq < nextAmp) {
                val key = URLDecoder.decode(query.substring(start, eq), "UTF-8")
                val valueRaw = query.substring(eq + 1, nextAmp)
                val decodedValue = try {
                    URLDecoder.decode(valueRaw.replace("+", "%2B"), "UTF-8")
                } catch (_: Exception) {
                    URLDecoder.decode(valueRaw, "UTF-8")
                }
                result[key] = decodedValue
            } else {
                val key = URLDecoder.decode(query.substring(start, nextAmp), "UTF-8")
                result[key] = ""
            }
            start = nextAmp + 1
        }
        return result
    }

    private fun parseIntervalToSeconds(valueStr: String): Long? {
        val clean = valueStr.trim().lowercase()
        if (clean.isEmpty()) return null

        val suffixMatch = INTERVAL_SUFFIX_REGEX.find(clean)
        if (suffixMatch != null) {
            val numValue = suffixMatch.groupValues[1].toDoubleOrNull() ?: return null
            val unit = suffixMatch.groupValues[2].trim().removeSuffix(".")
            if (unit.isEmpty()) {
                val intValue = numValue.toLong()
                return if (intValue >= 300) {
                    intValue
                } else {
                    intValue * 3600L
                }
            }
            return when (unit) {
                "h", "ч", "hour", "hours" -> (numValue * 3600L).toLong()
                "d", "д", "day", "days" -> (numValue * 86400L).toLong()
                "m", "м", "min", "minute", "minutes" -> (numValue * 60L).toLong()
                "s", "с", "sec", "second", "seconds" -> numValue.toLong()
                else -> {
                    val intValue = numValue.toLong()
                    if (intValue >= 300) intValue else intValue * 3600L
                }
            }
        }
        return null
    }
}
