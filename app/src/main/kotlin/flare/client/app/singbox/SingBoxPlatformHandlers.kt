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
    override fun triggerNativeCrash() {}
    override fun connectSSHAgent(): Int = 0
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
    ): ConnectionOwner? = null

    override fun getInterfaces(): NetworkInterfaceIterator? = null
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
                    try {
                        val dnsServers = opts.dnsServerAddress
                        if (dnsServers != null && dnsServers.hasNext()) {
                            while (dnsServers.hasNext()) {
                                val dnsAddr = dnsServers.next()
                                if (!dnsAddr.isNullOrBlank()) {
                                    builder.addDnsServer(dnsAddr as String)
                                    Log.d(TAG, "openTun: added DNS server $dnsAddr")
                                }
                            }
                        } else {
                            Log.w(
                                TAG,
                                "openTun: dnsServerAddress is empty, using 1.1.1.1 fallback"
                            )
                            builder.addDnsServer("1.1.1.1")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get dnsServerAddress, using 1.1.1.1 fallback: ${e.message}")
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

    override fun readWIFIState(): WIFIState? = null
    override fun sendNotification(notification: Notification?) {}

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = SingBoxManager.currentVpnService?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            SingBoxManager.networkCallback = object : ConnectivityManager.NetworkCallback() {
                private fun notifyNetworkChange(network: Network) {
                    try {
                        val caps = cm.getNetworkCapabilities(network)
                        val props = cm.getLinkProperties(network)
                        val isExpensive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
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
            val cm = SingBoxManager.currentVpnService?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
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

    override fun systemCertificates(): StringIterator? = null
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun useProcFS(): Boolean = true
}
