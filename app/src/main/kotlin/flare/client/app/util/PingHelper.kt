package flare.client.app.util

import android.content.Context
import android.util.Log
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.parser.V2RayConfigConverter
import io.nekohasekai.libbox.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.net.ServerSocket
import flare.client.app.data.model.PingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PingHelper {
    private const val TAG = "PingHelper"

    private val _pingStates = MutableStateFlow<Map<Long, PingState>>(emptyMap())
    val pingStates: StateFlow<Map<Long, PingState>> = _pingStates.asStateFlow()

    fun updatePingState(profileId: Long, state: PingState) {
        _pingStates.update { it.toMutableMap().apply { put(profileId, state) } }
    }

    fun updatePingStates(states: Map<Long, PingState>) {
        _pingStates.update { it + states }
    }

    fun clearPings() {
        _pingStates.value = emptyMap()
    }
    private const val PROXY_PING_BATCH_SIZE = 150
    private const val PROXY_PING_PARALLELISM = 24

    private val directSemaphore = Semaphore(10)

    private val batchMutex = Mutex()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(PROXY_PING_PARALLELISM, 5, TimeUnit.SECONDS))
            .build()
    }

    @Volatile private var libboxSetupDone = false
    private val setupLock = Any()

    private fun ensureLibboxSetup(context: Context) {
        if (libboxSetupDone) return
        synchronized(setupLock) {
            if (libboxSetupDone) return
            try {
                val opts = SetupOptions().apply {
                    basePath        = context.filesDir.absolutePath
                    workingPath     = context.filesDir.absolutePath
                    tempPath        = context.cacheDir.absolutePath
                    fixAndroidStack = true
                    logMaxLines     = 100
                }
                Libbox.setup(opts)
                if (flare.client.app.BuildConfig.DEBUG) Log.i(TAG, "Libbox.setup() success")
                
                
                flare.client.app.singbox.LocalResolver.init(context.applicationContext)
                
                Unit
            } catch (e: Exception) {
                Log.w(TAG, "Libbox.setup() failed: ${e.message}")
            } finally {
                libboxSetupDone = true
            }
        }
    }

    suspend fun pingDirect(profile: ProfileEntity, method: String, timeoutSec: Int = 10): Pair<Long, String?> =
        withContext(Dispatchers.IO) {
            val hostPort = extractHostPort(profile) ?: return@withContext (-1L to "Config Err")
            val host = hostPort.first
            val port = hostPort.second

            val ipAddress = try {
                InetAddress.getByName(host).hostAddress
            } catch (e: Exception) {
                return@withContext (-1L to "DNS Fail")
            }

            directSemaphore.withPermit {
                try {
                    if (method == "ICMP") {
                        val startTime = System.nanoTime()
                        var process: Process? = null
                        try {
                            process = Runtime.getRuntime()
                                .exec(arrayOf("ping", "-c", "1", "-W", timeoutSec.toString(), ipAddress))
                            val output = process.inputStream.bufferedReader().use { it.readText() }
                            val exitCode = process.waitFor()
                            if (exitCode == 0) {
                                val rtt = parseIcmpRtt(output)
                                val finalRtt = if (rtt != -1L) rtt else (System.nanoTime() - startTime) / 1_000_000
                                finalRtt to null
                            } else {
                                -1L to "Unreachable"
                            }
                        } finally {
                            process?.destroy()
                        }
                    } else {
                        val startTime = System.nanoTime()
                        val socket = Socket()
                        flare.client.app.singbox.SingBoxManager.currentVpnService?.protect(socket)
                        socket.use {
                            it.connect(InetSocketAddress(ipAddress, port), timeoutSec * 1000)
                        }
                        ((System.nanoTime() - startTime) / 1_000_000) to null
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    -1L to "Timeout"
                } catch (e: java.net.ConnectException) {
                    -1L to "Refused"
                } catch (e: java.net.NoRouteToHostException) {
                    -1L to "Unreachable"
                } catch (e: Exception) {
                    -1L to (e.message ?: "Error")
                }
            }
        }

    private fun parseIcmpRtt(output: String): Long {
        return try {
            val pattern = Pattern.compile("time=([\\d.]+)")
            val matcher = pattern.matcher(output)
            if (matcher.find()) {
                val timeStr = matcher.group(1) ?: return -1L
                timeStr.toDouble().toLong()
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun pingProxyBatch(
        context: Context,
        profiles: List<ProfileEntity>,
        testUrl: String,
        timeoutSec: Int = 10,
        onResult: suspend (Long, Long, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        ensureLibboxSetup(context)

        batchMutex.withLock {
            profiles.chunked(PROXY_PING_BATCH_SIZE).forEachIndexed { batchIndex, chunk ->
                if (batchIndex > 0) {
                    delay(20) 
                }
                runProxyPingChunk(
                    context = context,
                    profiles = chunk,
                    testUrl = testUrl,
                    timeoutSec = timeoutSec,
                    batchIndex = batchIndex,
                    onResult = onResult
                )
            }
        }
    }

    private suspend fun runProxyPingChunk(
        context: Context,
        profiles: List<ProfileEntity>,
        testUrl: String,
        timeoutSec: Int,
        batchIndex: Int,
        onResult: suspend (Long, Long, String?) -> Unit
    ) {
        if (profiles.isEmpty()) return

        val handler = object : CommandServerHandler {
            override fun serviceStop() {}
            override fun serviceReload() {}
            override fun getSystemProxyStatus() = SystemProxyStatus()
            override fun setSystemProxyEnabled(enabled: Boolean) {}
            override fun writeDebugMessage(message: String?) {}
            override fun triggerNativeCrash() {}
            override fun connectSSHAgent(): Int = 0
        }

        val platform = object : PlatformInterface {
            private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

            override fun autoDetectInterfaceControl(fd: Int) {
                flare.client.app.singbox.SingBoxManager.currentVpnService?.protect(fd)
            }
            override fun clearDNSCache() {}
            override fun closeDefaultInterfaceMonitor(l: InterfaceUpdateListener?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
                    networkCallback?.let {
                        try {
                            cm.unregisterNetworkCallback(it)
                        } catch (e: Exception) {
                            Log.e(TAG, "unregisterNetworkCallback failed", e)
                        }
                    }
                    networkCallback = null
                }
            }
            override fun findConnectionOwner(
                p0: Int, p1: String?, p2: Int, p3: String?, p4: Int
            ): ConnectionOwner? = null
            override fun getInterfaces(): NetworkInterfaceIterator? = null
            override fun includeAllNetworks(): Boolean = false
            override fun localDNSTransport(): LocalDNSTransport? = flare.client.app.singbox.LocalResolver
            override fun openTun(o: TunOptions?): Int = -1
            override fun readWIFIState(): WIFIState? = null
            override fun sendNotification(n: Notification?) {}
            override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
                    networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                        private fun notifyNetworkChange(network: android.net.Network) {
                            try {
                                val caps = cm.getNetworkCapabilities(network)
                                val props = cm.getLinkProperties(network)
                                val isExpensive = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
                                val interfaceName = props?.interfaceName
                                var idx = -1
                                if (interfaceName != null) {
                                    try {
                                        val ni = java.net.NetworkInterface.getByName(interfaceName)
                                        if (ni != null) idx = ni.index
                                    } catch (e: Exception) {}
                                }
                                listener?.updateDefaultInterface(interfaceName ?: "", idx, isExpensive, false)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in notifyNetworkChange", e)
                            }
                        }
                        override fun onAvailable(network: android.net.Network) { notifyNetworkChange(network) }
                        override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: android.net.NetworkCapabilities) { notifyNetworkChange(network) }
                        override fun onLinkPropertiesChanged(network: android.net.Network, linkProperties: android.net.LinkProperties) { notifyNetworkChange(network) }
                    }
                    try {
                        cm.registerDefaultNetworkCallback(networkCallback!!)
                    } catch (e: Exception) {
                        Log.e(TAG, "registerDefaultNetworkCallback failed", e)
                    }
                }
            }
            override fun systemCertificates(): StringIterator? = null
            override fun underNetworkExtension(): Boolean = false
            override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
            override fun useProcFS(): Boolean = true

            override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}
            override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}
            override fun registerMyInterface(name: String?) {}
            override fun usePlatformShell(): Boolean = false
            override fun checkPlatformShell() {}
            override fun openShellSession(
                user: PlatformUser?,
                command: String?,
                environ: StringIterator?,
                term: String?,
                rows: Int,
                cols: Int
            ): ShellSession? = null
            override fun lookupUser(username: String?): PlatformUser? = null
            override fun lookupSFTPServer(): String? = null
            override fun readSystemSSHHostKey(): String? = null
            override fun tailscaleHostname(): String = ""
        }

        val boxService = Libbox.newCommandServer(handler, platform)
        val clashPort = findAvailablePort()
        val clashSecret = java.util.UUID.randomUUID().toString()
        val excludedIndices = HashSet<Int>()
        try {
            var serviceStarted = false
            var retryCount = 0
            val maxRetries = 15
            var batchResult = buildBatchConfig(profiles, testUrl, clashPort, clashSecret, excludedIndices, onResult)

            while (!serviceStarted && retryCount < maxRetries) {
                if (batchResult == null || excludedIndices.size >= profiles.size) {
                    break
                }
                val batchConfig = batchResult.first
                val outboundIndexToProfileIndex = batchResult.second

                try {
                    boxService.startOrReloadService(
                        batchConfig.toString().replace("\\/", "/"),
                        OverrideOptions()
                    )
                    serviceStarted = true
                } catch (e: Exception) {
                    val errMsg = e.message ?: ""
                    Log.e(TAG, "Batch ping core start failed (retry $retryCount): $errMsg")

                    var foundCulprit = false

                    
                    val outboundPattern = Pattern.compile("outbound(?:s)?\\[(\\d+)\\]")
                    val outboundMatcher = outboundPattern.matcher(errMsg)
                    while (outboundMatcher.find()) {
                        val outboundIdx = outboundMatcher.group(1)?.toIntOrNull()
                        if (outboundIdx != null) {
                            val profileIdx = outboundIndexToProfileIndex[outboundIdx]
                            if (profileIdx != null && profileIdx in profiles.indices) {
                                if (!excludedIndices.contains(profileIdx)) {
                                    excludedIndices.add(profileIdx)
                                    onResult(profiles[profileIdx].id, -1L, "Core err")
                                    foundCulprit = true
                                }
                            }
                        }
                    }

                    
                    if (!foundCulprit) {
                        val indexPattern = Pattern.compile("(?:proxy-|\\b[a-zA-Z0-9_-]+-)(\\d+)\\b")
                        val tagMatcher = indexPattern.matcher(errMsg)
                        while (tagMatcher.find()) {
                            val profileIndex = tagMatcher.group(1)?.toIntOrNull()
                            if (profileIndex != null && profileIndex in profiles.indices) {
                                if (!excludedIndices.contains(profileIndex)) {
                                    excludedIndices.add(profileIndex)
                                    onResult(profiles[profileIndex].id, -1L, "Core err")
                                    foundCulprit = true
                                }
                            }
                        }
                    }

                    if (!foundCulprit) {
                        
                        profiles.forEachIndexed { idx, prof ->
                            if (!excludedIndices.contains(idx)) {
                                excludedIndices.add(idx)
                                onResult(prof.id, -1L, "Core err")
                            }
                        }
                        break
                    }

                    retryCount++
                    batchResult = buildBatchConfig(profiles, testUrl, clashPort, clashSecret, excludedIndices, onResult)
                }
            }

            if (!serviceStarted) {
                return
            }

            var ready = false
            val healthStart = System.currentTimeMillis()
            while (!ready && System.currentTimeMillis() - healthStart < 5000) {
                try {
                    val checkReq = Request.Builder()
                        .url("http://127.0.0.1:$clashPort/")
                        .header("Connection", "close")
                        .header("Authorization", "Bearer $clashSecret")
                        .get()
                        .build()
                    okHttpClient.newCall(checkReq).execute().use {
                        if (it.code != 0) ready = true
                    }
                } catch (e: Exception) {
                    delay(50)
                }
            }
            if (!ready) {
                Log.w(TAG, "Clash API failed to start on port $clashPort in time (batch=$batchIndex)")
            }

            val semaphore = Semaphore(PROXY_PING_PARALLELISM)
            coroutineScope {
                profiles.forEachIndexed { index, profile ->
                    if (excludedIndices.contains(index)) return@forEachIndexed
                    launch(Dispatchers.IO) {
                        semaphore.withPermit {
                            var rtt = -1L
                            var errMsg: String? = null
                            try {
                                val tag = "proxy-$index"
                                val url = "http://127.0.0.1:$clashPort/proxies/${java.net.URLEncoder.encode(tag, "UTF-8")}/delay?url=${java.net.URLEncoder.encode(testUrl, "UTF-8")}&timeout=${timeoutSec * 1000}"
                                val request = Request.Builder()
                                    .url(url)
                                    .header("Authorization", "Bearer $clashSecret")
                                    .get()
                                    .build()
                                okHttpClient.newCall(request).execute().use { response ->
                                    val body = response.body.string()
                                    if (response.isSuccessful && body.isNotEmpty()) {
                                        val json = JSONObject(body)
                                        rtt = json.optLong("delay", -1L)
                                        if (rtt == -1L) errMsg = "Timeout"
                                    } else {
                                        errMsg = if (body.isNotEmpty()) {
                                            try {
                                                val msg = JSONObject(body).optString("message", "")
                                                when {
                                                    msg.contains("timeout", ignoreCase = true) -> "Timeout"
                                                    msg.contains("TLS", ignoreCase = true) -> "TLS Failed"
                                                    msg.contains("unreachable", ignoreCase = true) -> "Unreachable"
                                                    msg.contains("connection refused", ignoreCase = true) -> "Refused"
                                                    msg.contains("error occurred", ignoreCase = true) -> "Failed"
                                                    msg.length > 20 -> msg.substring(0, 17) + ".."
                                                    msg.isNotBlank() -> msg
                                                    else -> "${response.code}"
                                                }
                                            } catch (_: Exception) {
                                                "${response.code}"
                                            }
                                        } else {
                                            "${response.code}"
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Ping failed for profile ${profile.id}: ${e.message}")
                                errMsg = when {
                                    e is java.net.SocketTimeoutException -> "Timeout"
                                    e.message?.contains("timeout", ignoreCase = true) == true -> "Timeout"
                                    else -> "Error"
                                }
                            }
                            onResult(profile.id, rtt, errMsg)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch ping failed (core start error, batch=$batchIndex): ${e.message}", e)
            profiles.forEachIndexed { idx, prof ->
                if (!excludedIndices.contains(idx)) {
                    onResult(prof.id, -1L, "Core err")
                }
            }
        } finally {
            try {
                boxService.closeService()
                boxService.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun buildBatchConfig(
        profiles: List<ProfileEntity>,
        testUrl: String,
        clashPort: Int,
        clashSecret: String,
        excludedIndices: MutableSet<Int>,
        onResult: suspend (Long, Long, String?) -> Unit
    ): Pair<JSONObject, Map<Int, Int>>? {
        return try {
            val outbounds = JSONArray()
            val proxyTags = ArrayList<String>()
            val outboundIndexToProfileIndex = HashMap<Int, Int>()
            var profileDnsDirectServer = ""

            profiles.forEachIndexed { index, profile ->
                if (excludedIndices.contains(index)) return@forEachIndexed
                try {
                    val converted = V2RayConfigConverter.convertIfNeeded(profile.configJson)
                    val profileJson = JSONObject(converted)

                    
                    if (profileDnsDirectServer.isBlank()) {
                        val pDns = profileJson.optJSONObject("dns")
                        val pServers = pDns?.optJSONArray("servers")
                        if (pServers != null) {
                            for (si in 0 until pServers.length()) {
                                val srv = pServers.optJSONObject(si) ?: continue
                                if (srv.optString("tag") == "dns-direct" && srv.optString("detour", "").isBlank()) {
                                    val addr = srv.optString("server", "")
                                    if (addr.isNotBlank()) {
                                        profileDnsDirectServer = addr
                                        break
                                    }
                                }
                            }
                        }
                    }

                    
                    
                    val endpoints = profileJson.optJSONArray("endpoints")
                    if (endpoints != null) {
                        for (ei in 0 until endpoints.length()) {
                            val ep = endpoints.optJSONObject(ei) ?: continue
                            if (ep.optString("type").equals("wireguard", ignoreCase = true)) {
                                excludedIndices.add(index)
                                onResult(profile.id, -1L, "UDP")
                                return@forEachIndexed
                            }
                        }
                    }

                    val profileOutbounds = profileJson.optJSONArray("outbounds") ?: JSONArray()
                    var mainProxyTag = ""
                    for (i in 0 until profileOutbounds.length()) {
                        val ob = profileOutbounds.optJSONObject(i) ?: continue
                        val t = ob.optString("type").lowercase(java.util.Locale.ROOT)
                        if (t != "direct" && t != "block" && t != "dns" && t != "urltest" && t != "selector" && t.isNotBlank()) {
                            mainProxyTag = ob.optString("tag")
                            break
                        }
                    }
                    if (mainProxyTag.isBlank()) {
                        excludedIndices.add(index)
                        onResult(profile.id, -1L, "Config Err")
                        return@forEachIndexed
                    }

                    val mainTagMapped = "proxy-$index"
                    proxyTags.add(mainTagMapped)

                    for (i in 0 until profileOutbounds.length()) {
                        val ob = profileOutbounds.optJSONObject(i) ?: continue
                        val t = ob.optString("type").lowercase(java.util.Locale.ROOT)
                        if (t == "direct" || t == "block" || t == "dns" || t == "urltest" || t == "selector") continue
                        val oldTag = ob.optString("tag")
                        if (oldTag.isNotBlank()) {
                            ob.put("tag", if (oldTag == mainProxyTag) mainTagMapped else "$oldTag-$index")
                        }
                        if (ob.has("outbounds")) {
                            val obList = ob.optJSONArray("outbounds")
                            if (obList != null) {
                                val newList = JSONArray()
                                for (j in 0 until obList.length()) {
                                    val entry = obList.optString(j)
                                    newList.put(if (entry == mainProxyTag) mainTagMapped else "$entry-$index")
                                }
                                ob.put("outbounds", newList)
                            }
                        }
                        if (ob.has("detour")) {
                            val detourName = ob.optString("detour")
                            if (detourName.isNotBlank()) {
                                if (detourName.equals("direct", ignoreCase = true)) {
                                    ob.put("detour", "direct")
                                } else if (detourName.equals("block", ignoreCase = true)) {
                                    ob.put("detour", "block")
                                } else {
                                    ob.put("detour", if (detourName == mainProxyTag) mainTagMapped else "$detourName-$index")
                                }
                            }
                        }

                        val type = ob.optString("type")
                        if (type != "hysteria" && type != "hysteria2") {
                            ob.optJSONObject("tls")?.let { tls ->
                                tls.put("utls", JSONObject().apply {
                                    put("enabled", true)
                                    put("fingerprint", "chrome")
                                })
                            }
                        }

                        val outboundIdx = outbounds.length()
                        outboundIndexToProfileIndex[outboundIdx] = index

                        outbounds.put(ob)
                    }
                } catch (e: Exception) {
                    excludedIndices.add(index)
                    onResult(profile.id, -1L, "Config Err")
                }
            }

            if (outbounds.length() == 0) return null

            outbounds.put(JSONObject().apply {
                put("type", "urltest")
                put("tag", "urltest-ping")
                put("outbounds", JSONArray(proxyTags))
                put("url", testUrl)
                put("interval", "10m")
            })

            
            val proxyServerHosts = mutableSetOf<String>()
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val server = ob.optString("server", "")
                if (server.isNotBlank() && !server.matches(Regex("^[\\d.]+$")) && !server.contains(":")) {
                    proxyServerHosts.add(server)
                }
            }
            val dnsDirectServer = profileDnsDirectServer.ifBlank { "8.8.8.8" }

            val testHost = extractUrlHost(testUrl)
            val config = JSONObject().apply {
                put("experimental", JSONObject().apply {
                    put("clash_api", JSONObject().apply {
                        put("external_controller", "127.0.0.1:$clashPort")
                        put("secret", clashSecret)
                    })
                })
                put("log", JSONObject().apply { put("level", "error") })
                put("dns", JSONObject().apply {
                    put("servers", JSONArray().apply {
                        put(JSONObject().apply {
                            put("tag", "dns-direct")
                            put("type", "udp")
                            put("server", dnsDirectServer)
                        })
                        put(JSONObject().apply {
                            put("tag", "dns-cf")
                            put("type", "udp")
                            put("server", "1.1.1.1")
                        })
                        put(JSONObject().apply {
                            put("tag", "dns-local")
                            put("type", "local")
                        })
                        put(JSONObject().apply {
                            put("tag", "dns-fakeip")
                            put("type", "fakeip")
                            put("inet4_range", "198.18.0.0/15")
                        })
                    })
                    put("rules", JSONArray().apply {
                        put(JSONObject().apply {
                            put("outbound", JSONArray().put("direct"))
                            put("server", "dns-local")
                        })
                        if (proxyServerHosts.isNotEmpty()) {
                            put(JSONObject().apply {
                                put("domain", JSONArray().also { arr -> proxyServerHosts.forEach { arr.put(it) } })
                                put("server", "dns-local")
                            })
                        }
                        if (!testHost.isNullOrBlank()) {
                            put(JSONObject().apply {
                                put("domain", JSONArray().apply {
                                    put(testHost)
                                    if (testHost.startsWith("www.")) {
                                        put(testHost.substring(4))
                                    } else {
                                        put("www.$testHost")
                                    }
                                })
                                put("server", "dns-fakeip")
                            })
                        }
                    })
                    put("final", "dns-local")
                })
                put("inbounds", JSONArray())
                put("outbounds", outbounds.apply {
                    put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
                    put(JSONObject().apply { put("type", "block"); put("tag", "block") })
                })
                put("route", JSONObject().apply {
                    put("auto_detect_interface", false)
                    put("final", "direct")
                })
            }
            config to outboundIndexToProfileIndex
        } catch (e: Exception) {
            null
        }
    }

    private fun extractHostPort(profile: ProfileEntity): Pair<String, Int>? {
        return try {
            val converted = V2RayConfigConverter.convertIfNeeded(profile.configJson)
            val json = JSONObject(converted)

            
            val endpoints = json.optJSONArray("endpoints")
            if (endpoints != null) {
                for (ei in 0 until endpoints.length()) {
                    val ep = endpoints.optJSONObject(ei) ?: continue
                    if (ep.optString("type").equals("wireguard", ignoreCase = true)) {
                        val peers = ep.optJSONArray("peers")
                        val peer = peers?.optJSONObject(0)
                        val host = peer?.optString("address") ?: continue
                        val port = peer.optInt("port", 51820)
                        if (host.isNotEmpty()) return host to port
                    }
                }
            }

            val outbounds = json.optJSONArray("outbounds") ?: return null
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val type = ob.optString("type")
                if (type != "direct" && type != "block" && type.isNotBlank()) {
                    val host = ob.optString("server")
                    val port = ob.optInt("server_port", 443)
                    if (host.isNotEmpty()) return host to port
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun <T> Semaphore.withPermit(block: suspend () -> T): T {
        acquire()
        return try { block() } finally { release() }
    }

    private fun findAvailablePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (e: Exception) {
            9094
        }
    }

    private fun extractUrlHost(urlStr: String): String? {
        return try {
            val uri = java.net.URI(urlStr)
            val host = uri.host
            if (host.isNullOrBlank()) {
                val matcher = Pattern.compile("https?://([^:/]+)").matcher(urlStr)
                if (matcher.find()) matcher.group(1) else null
            } else {
                host
            }
        } catch (e: Exception) {
            null
        }
    }
}
