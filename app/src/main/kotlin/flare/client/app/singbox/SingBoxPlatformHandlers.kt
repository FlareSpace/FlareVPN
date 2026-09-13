package flare.client.app.singbox

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import flare.client.app.data.SettingsManager
import io.nekohasekai.libbox.*

internal class FlareCommandServerHandler(
    private val getVpnContext: () -> Context?
) : CommandServerHandler {
    private val TAG = "SingBoxManager"

    override fun serviceStop() {
        Log.i(TAG, "serviceStop called from sing-box core")
        val ctx = getVpnContext() ?: return
        val intent = android.content.Intent(ctx, flare.client.app.service.FlareVpnService::class.java).apply {
            action = flare.client.app.service.FlareVpnService.ACTION_STOP
        }
        try {
            ctx.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FlareVpnService with ACTION_STOP", e)
        }
    }
    override fun serviceReload() {}
    override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()
    override fun setSystemProxyEnabled(enabled: Boolean) {}
    override fun writeDebugMessage(message: String?) {
        if (!message.isNullOrBlank()) Log.i(TAG, "[sb] $message")
    }
    override fun connectSSHAgent(): Int = -1
    override fun triggerNativeCrash() {}
}

internal fun listStringIterator(list: List<String>): StringIterator = object : StringIterator {
    private var idx = 0
    override fun hasNext() = idx < list.size
    override fun next(): String = list[idx++]
    override fun len(): Int = list.size
}

internal object NetworkInterfaceHelper {
    fun getInterfaces(context: Context?): NetworkInterfaceIterator? {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val nifs = try {
            java.net.NetworkInterface.getNetworkInterfaces()?.toList()
        } catch (e: Exception) {
            null
        } ?: return null

        val result = ArrayList<io.nekohasekai.libbox.NetworkInterface>()
        val capsMap = HashMap<String, NetworkCapabilities>()
        val linkPropsMap = HashMap<String, LinkProperties>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cm != null) {
            try {
                for (network in cm.allNetworks) {
                    val caps = cm.getNetworkCapabilities(network) ?: continue
                    val lp = cm.getLinkProperties(network) ?: continue
                    val ifName = lp.interfaceName ?: continue
                    capsMap[ifName] = caps
                    linkPropsMap[ifName] = lp
                }
            } catch (_: Exception) {}
        }

        for (nif in nifs) {
            try {
                if (nif.isLoopback) continue
                val caps = capsMap[nif.name]
                if (caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                    continue
                }
                val tunName = SingBoxManager.tunInterfaceName
                if (!tunName.isNullOrBlank() && nif.name == tunName) {
                    continue
                }

                val libboxIf = io.nekohasekai.libbox.NetworkInterface()
                libboxIf.name = nif.name
                libboxIf.index = nif.index
                libboxIf.mtu = try { nif.mtu.takeIf { it > 0 } ?: 1500 } catch (_: Exception) { 1500 }

                var flags = 0
                if (nif.isUp) flags = flags or 1
                if (nif.isLoopback) flags = flags or 8
                if (nif.isPointToPoint) flags = flags or 16
                if (try { nif.supportsMulticast() } catch (_: Exception) { false }) flags = flags or 32
                libboxIf.flags = flags

                val addrs = ArrayList<String>()
                val lp = linkPropsMap[nif.name]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && lp != null) {
                    for (la in lp.linkAddresses) {
                        val str = la.toString()
                        if (str.contains('/')) {
                            addrs.add(str)
                        }
                    }
                }
                if (addrs.isEmpty()) {
                    for (ia in nif.interfaceAddresses) {
                        val addr = ia.address ?: continue
                        val host = addr.hostAddress?.substringBefore('%') ?: continue
                        val prefix = ia.networkPrefixLength
                        val effectivePrefix = if (prefix > 0) prefix.toInt() else (if (addr is java.net.Inet6Address) 64 else 24)
                        addrs.add("$host/$effectivePrefix")
                    }
                }
                libboxIf.addresses = listStringIterator(addrs)

                var ifType = Libbox.InterfaceTypeOther
                var metered = false
                if (caps != null) {
                    metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    ifType = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                        else -> Libbox.InterfaceTypeOther
                    }
                } else {
                    val name = nif.name.lowercase()
                    if (name.startsWith("wlan") || name.startsWith("wifi")) {
                        ifType = Libbox.InterfaceTypeWIFI
                    } else if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") || name.startsWith("radio")) {
                        ifType = Libbox.InterfaceTypeCellular
                        metered = true
                    } else if (name.startsWith("eth")) {
                        ifType = Libbox.InterfaceTypeEthernet
                    }
                }
                libboxIf.type = ifType
                libboxIf.metered = metered

                val dnsList = ArrayList<String>()
                val gwList = ArrayList<String>()
                if (lp != null) {
                    for (dns in lp.dnsServers) {
                        dns.hostAddress?.substringBefore('%')?.let { dnsList.add(it) }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        for (r in lp.routes) {
                            if (r.isDefaultRoute) {
                                r.gateway?.hostAddress?.substringBefore('%')?.let { gwList.add(it) }
                            }
                        }
                    }
                }
                libboxIf.dnsServer = listStringIterator(dnsList)
                libboxIf.gateway = listStringIterator(gwList)

                result.add(libboxIf)
            } catch (_: Exception) {}
        }

        return object : NetworkInterfaceIterator {
            private var idx = 0
            override fun hasNext() = idx < result.size
            override fun next(): io.nekohasekai.libbox.NetworkInterface = result[idx++]
        }
    }
}

internal class FlarePlatformInterface : PlatformInterface {
    private val TAG = "SingBoxManager"

    override fun autoDetectInterfaceControl(fd: Int) {
        SingBoxManager.currentVpnService?.protect(fd)
    }

    override fun clearDNSCache() {}

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String?,
        sourcePort: Int,
        destinationAddress: String?,
        destinationPort: Int
    ): ConnectionOwner? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vpn = SingBoxManager.currentVpnService
            val cm = (vpn ?: SingBoxManager.appContext)?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val src = sourceAddress
            val dst = destinationAddress
            if (cm != null && src != null && dst != null) {
                try {
                    val srcAddr = java.net.InetSocketAddress(src, sourcePort)
                    val dstAddr = java.net.InetSocketAddress(dst, destinationPort)
                    val uid = cm.getConnectionOwnerUid(ipProtocol, srcAddr, dstAddr)
                    if (uid > 0) {
                        val packages = vpn?.packageManager?.getPackagesForUid(uid)
                        return ConnectionOwner().apply {
                            this.userId = uid
                            if (!packages.isNullOrEmpty()) {
                                this.userName = packages[0]
                                this.setAndroidPackageNames(listStringIterator(packages.toList()))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        throw java.util.NoSuchElementException("owner not found")
    }

    override fun getInterfaces(): NetworkInterfaceIterator? =
        NetworkInterfaceHelper.getInterfaces(SingBoxManager.currentVpnService ?: SingBoxManager.appContext)
    override fun includeAllNetworks(): Boolean = false
    override fun localDNSTransport(): LocalDNSTransport? = LocalResolver

    override fun openTun(options: TunOptions?): Int {
        Log.i(
            TAG,
            "openTun called, mtu=${options?.mtu}, autoRoute=${options?.autoRoute}"
        )

        val vpn = SingBoxManager.currentVpnService
        if (vpn == null) {
            Log.e(TAG, "openTun: currentVpnService is null")
            return -1
        }

        try {
            val builder = vpn.Builder().setSession("Flare")

            try {
                val settings = SettingsManager(vpn)
                val modeApps = settings.splitTunnelingModeApps
                if (settings.isSplitTunnelingEnabled && settings.splitTunnelingApps.isNotEmpty()) {
                    for (pkg in settings.splitTunnelingApps) {
                        try {
                            if (modeApps == "whitelist") {
                                builder.addAllowedApplication(pkg)
                            } else {
                                builder.addDisallowedApplication(pkg)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to add application: $pkg", e)
                        }
                    }
                    if (modeApps == "blacklist") {
                        builder.addDisallowedApplication(vpn.packageName)
                    }
                } else {
                    builder.addDisallowedApplication(vpn.packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure VPN apps", e)
            }

            options?.let { opts ->
                builder.setMtu(opts.mtu)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                val inet4 = opts.inet4Address
                while (inet4.hasNext()) {
                    val addr = inet4.next()
                    builder.addAddress(addr.address(), addr.prefix())
                    Log.d(TAG, "openTun: added address ${addr.address()}/${addr.prefix()}")
                }

                val inet6 = opts.inet6Address
                while (inet6.hasNext()) {
                    val addr = inet6.next()
                    builder.addAddress(addr.address(), addr.prefix())
                    Log.d(TAG, "openTun: added IPv6 address ${addr.address()}/${addr.prefix()}")
                }

                if (opts.autoRoute) {
                    var dnsAdded = false
                    try {
                        val dnsIterator = opts.dnsServerAddress
                        if (dnsIterator != null) {
                            while (dnsIterator.hasNext()) {
                                val dnsAddr = dnsIterator.next()?.trim()
                                if (!dnsAddr.isNullOrEmpty()) {
                                    builder.addDnsServer(dnsAddr)
                                    Log.d(TAG, "openTun: added DNS server $dnsAddr")
                                    dnsAdded = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get dnsServerAddress: ${e.message}")
                    }
                    if (!dnsAdded) {
                        Log.w(
                            TAG,
                            "openTun: dnsServerAddress is empty, using 1.1.1.1 fallback"
                        )
                        builder.addDnsServer("1.1.1.1")
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val v4routes = opts.inet4RouteAddress
                        if (v4routes.hasNext()) {
                            while (v4routes.hasNext()) {
                                val r = v4routes.next()
                                builder.addRoute(r.address(), r.prefix())
                            }
                        } else {
                            builder.addRoute("0.0.0.0", 0)
                        }

                        val v6routes = opts.inet6RouteAddress
                        if (v6routes.hasNext()) {
                            while (v6routes.hasNext()) {
                                val r = v6routes.next()
                                builder.addRoute(r.address(), r.prefix())
                            }
                        } else {
                            builder.addRoute("::", 0)
                        }
                    } else {
                        val v4range = opts.inet4RouteRange
                        if (v4range.hasNext()) {
                            while (v4range.hasNext()) {
                                val r = v4range.next()
                                builder.addRoute(r.address(), r.prefix())
                            }
                        } else {
                            builder.addRoute("0.0.0.0", 0)
                        }

                        val v6range = opts.inet6RouteRange
                        if (v6range.hasNext()) {
                            while (v6range.hasNext()) {
                                val r = v6range.next()
                                builder.addRoute(r.address(), r.prefix())
                            }
                        } else {
                            builder.addRoute("::", 0)
                        }
                    }
                } else {
                    builder.addRoute("0.0.0.0", 0)
                    builder.addRoute("::", 0)
                    builder.addDnsServer("1.1.1.1")
                    builder.addDnsServer("8.8.8.8")
                }
            } ?: run {
                builder.addAddress("172.19.0.1", 30)
                builder.addAddress("fdfe:dcba:9876::1", 126)
                builder.addRoute("0.0.0.0", 0)
                builder.addRoute("::", 0)
                builder.addDnsServer("1.1.1.1")
                builder.addDnsServer("8.8.8.8")
            }

            val pfd = builder.establish()
            if (pfd == null) {
                SingBoxManager.lastPermissionError = true
                Log.e(TAG, "openTun: VPN permission missing (establish returned null)")
                return -1
            }

            SingBoxManager.tunPfd?.close()
            SingBoxManager.tunPfd = pfd

            Log.i(TAG, "openTun: established fd=${pfd.fd}")
            return pfd.fd
        } catch (e: Exception) {
            Log.e(TAG, "openTun failed: ${e.message}", e)
            return -1
        }
    }

    override fun readWIFIState(): WIFIState? = null
    override fun sendNotification(notification: Notification?) {}

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = (SingBoxManager.currentVpnService ?: SingBoxManager.appContext)
                ?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            SingBoxManager.networkCallback = object : ConnectivityManager.NetworkCallback() {
                private fun notifyNetworkChange(network: Network) {
                    try {
                        val caps = cm.getNetworkCapabilities(network)
                        
                        if (caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                            return
                        }
                        val props = cm.getLinkProperties(network)
                        val isExpensive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
                        val interfaceName = props?.interfaceName
                        val tunName = SingBoxManager.tunInterfaceName
                        if (interfaceName == "tun0" || (!tunName.isNullOrEmpty() && interfaceName == tunName)) {
                            return
                        }
                        var idx = -1
                        if (interfaceName != null) {
                            try {
                                val ni = java.net.NetworkInterface.getByName(interfaceName)
                                if (ni != null) idx = ni.index
                            } catch (e: Exception) {}
                        }
                        try {
                            SingBoxManager.currentVpnService?.setUnderlyingNetworks(arrayOf(network))
                        } catch (_: Exception) {}
                        listener?.updateDefaultInterface(interfaceName ?: "", idx, isExpensive, false)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in notifyNetworkChange", e)
                    }
                }
                override fun onAvailable(network: Network) { notifyNetworkChange(network) }
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { notifyNetworkChange(network) }
                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) { notifyNetworkChange(network) }
            }
            try {
                cm.registerDefaultNetworkCallback(SingBoxManager.networkCallback!!)
            } catch (e: Exception) {
                Log.e(TAG, "registerDefaultNetworkCallback failed", e)
            }
        }
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = (SingBoxManager.currentVpnService ?: SingBoxManager.appContext)
                ?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            SingBoxManager.networkCallback?.let {
                try {
                    cm.unregisterNetworkCallback(it)
                } catch (e: Exception) {
                    Log.e(TAG, "unregisterNetworkCallback failed", e)
                }
            }
            SingBoxManager.networkCallback = null
        }
    }

    override fun cancelNotification(identifier: String?, typeID: Int) {}
    override fun checkPlatformShell() {}
    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun createBridge(options: BridgeOptions?): BridgeSession? = null
    override fun lookupSFTPServer(): String = ""
    override fun lookupUser(username: String?): PlatformUser? = null
    override fun openShellSession(
        user: PlatformUser?,
        command: String?,
        environ: StringIterator?,
        term: String?,
        rows: Int,
        cols: Int
    ): ShellSession? = null
    override fun readSystemSSHHostKey(): String = ""
    override fun registerMyInterface(name: String?) {
        Log.i(TAG, "registerMyInterface: $name")
        SingBoxManager.tunInterfaceName = name
    }
    override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun tailscaleHostname(): String = ""
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun usePlatformBridge(): Boolean = false
    override fun usePlatformShell(): Boolean = false
    override fun useProcFS(): Boolean = false
}
