package flare.client.app.util

import kotlinx.coroutines.flow.StateFlow

data class VpnServerConfig(
    val host: String,
    val sshPort: Int,
    val user: String,
    val pass: String,
    val vpnPort: Int,
    val sni: String = "",
    val obfsPassword: String = "",
    val fingerprint: String = "chrome",
    val mport: String? = null,
    val transport: String = "tcp",
    val transportHost: String = "",
    val transportPath: String = "",
    val serviceName: String = "",
    val xhttpMode: String = "auto"
)

interface VpnServerCreator {
    val progress: StateFlow<Int>
    val status: StateFlow<String>
    
    suspend fun setup(config: VpnServerConfig): String?
}
