package flare.client.app.util

import flare.client.app.ui.i18n.I18n
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.MessageDigest

class WireGuardServerCreator(private val context: Context) : VpnServerCreator {
    private val prefs = context.getSharedPreferences("flare_settings", Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(0)
    override val progress: StateFlow<Int> = _progress

    private val _status = MutableStateFlow("")
    override val status: StateFlow<String> = _status

    private inner class PinnedHostKeyVerifier : HostKeyVerifier {
        var observedFingerprint: String? = null

        override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> =
            mutableListOf()

        override fun verify(hostname: String?, port: Int, key: java.security.PublicKey?): Boolean {
            if (hostname.isNullOrBlank() || key == null) return false

            val fingerprint = key.fingerprintSha256()
            observedFingerprint = fingerprint
            val prefKey = hostKeyPrefKey(hostname, port)
            val expectedFingerprint = prefs.getString(prefKey, null)
            return expectedFingerprint == null || expectedFingerprint == fingerprint
        }
    }

    private fun SSHClient.exec(cmd: String): String {
        var out = ""
        startSession().use { session ->
            val c = session.exec(cmd)
            val errThread = Thread {
                try {
                    c.errorStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) {}
            }
            errThread.start()
            out = c.inputStream.bufferedReader().readText()
            errThread.join()
            c.join()
        }
        return out.trim()
    }

    override suspend fun setup(config: VpnServerConfig): String? = withContext(Dispatchers.IO) {
        SecurityInitializer.init()
        prefs.edit().remove(hostKeyPrefKey(config.host, config.sshPort)).apply()
        val ssh = SSHClient()
        val hostVerifier = PinnedHostKeyVerifier()
        try {
            _status.value = I18n.strings.ssh_status_connecting
            ssh.addHostKeyVerifier(hostVerifier)
            ssh.connectTimeout = 10000
            ssh.timeout = 15000
            ssh.connect(config.host, config.sshPort)
            ssh.authPassword(config.user, config.pass)
            hostVerifier.observedFingerprint?.let { fingerprint ->
                prefs.edit().putString(hostKeyPrefKey(config.host, config.sshPort), fingerprint).apply()
            }
            _progress.value = 10

            var wgPath = ssh.exec("command -v wg || ( [ -x /usr/bin/wg ] && echo /usr/bin/wg )").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (wgPath.isEmpty()) {
                _status.value = I18n.strings.ssh_status_installing_wireguard
                ssh.exec("sudo -n apt-get update -qq && sudo -n apt-get install -y wireguard iptables 2>&1")
                wgPath = "/usr/bin/wg"
            }
            _progress.value = 30

            _status.value = I18n.strings.ssh_status_configuring_wireguard
            ssh.exec("sudo -n mkdir -p /etc/wireguard")
            ssh.exec("sudo -n chmod 700 /etc/wireguard")

            
            ssh.exec("sudo -n sysctl -w net.ipv4.ip_forward=1")
            ssh.exec("sudo -n sysctl -w net.ipv6.conf.all.forwarding=1")
            ssh.exec("echo 'net.ipv4.ip_forward=1' | sudo tee /etc/sysctl.d/99-wireguard-forward.conf > /dev/null")
            ssh.exec("echo 'net.ipv6.conf.all.forwarding=1' | sudo tee -a /etc/sysctl.d/99-wireguard-forward.conf > /dev/null")

            _progress.value = 55

            
            val serverPrivateKey = ssh.exec("sudo -n wg genkey").trim()
            val serverPublicKey = ssh.exec("echo '$serverPrivateKey' | wg pubkey").trim()
            val clientPrivateKey = ssh.exec("sudo -n wg genkey").trim()
            val clientPublicKey = ssh.exec("echo '$clientPrivateKey' | wg pubkey").trim()

            if (serverPrivateKey.isEmpty() || serverPublicKey.isEmpty() || clientPrivateKey.isEmpty() || clientPublicKey.isEmpty()) {
                throw Exception("Failed to generate WireGuard keys on the remote server.")
            }

            var interfaceName = ssh.exec("ip route show default | awk '{print ${'$'}5}' | head -n1").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (interfaceName.isEmpty()) {
                interfaceName = ssh.exec("ip route | grep default | head -n1 | sed 's/.*dev //' | cut -d' ' -f1").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            }
            if (interfaceName.isEmpty()) {
                interfaceName = "eth0"
            }

            val wgConfig = """
[Interface]
PrivateKey = $serverPrivateKey
Address = 10.7.0.1/24, fdde:dcba:9876::1/64
ListenPort = ${config.vpnPort}
PostUp = iptables -A FORWARD -i %i -j ACCEPT; iptables -t nat -A POSTROUTING -o $interfaceName -j MASQUERADE; ip6tables -A FORWARD -i %i -j ACCEPT; ip6tables -t nat -A POSTROUTING -o $interfaceName -j MASQUERADE
PostDown = iptables -D FORWARD -i %i -j ACCEPT; iptables -t nat -D POSTROUTING -o $interfaceName -j MASQUERADE; ip6tables -D FORWARD -i %i -j ACCEPT; ip6tables -t nat -D POSTROUTING -o $interfaceName -j MASQUERADE

[Peer]
PublicKey = $clientPublicKey
AllowedIPs = 10.7.0.2/32, fdde:dcba:9876::2/128
""".trimIndent()

            val remoteConfigPath = "/etc/wireguard/wg0.conf"
            val configB64 = android.util.Base64.encodeToString(
                wgConfig.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            ssh.exec("echo '$configB64' | base64 -d | sudo tee $remoteConfigPath > /dev/null")
            ssh.exec("sudo -n chmod 600 $remoteConfigPath")

            val fileCheck = ssh.exec("sudo -n [ -s $remoteConfigPath ] && echo 'ok' || echo 'error'")
            if (fileCheck != "ok") {
                throw Exception(I18n.strings.ssh_error_config_write)
            }
            _progress.value = 70

            _status.value = I18n.strings.ssh_status_restarting_wireguard
            ssh.exec("sudo systemctl stop wg-quick@wg0 2>&1")
            ssh.exec("sudo systemctl disable wg-quick@wg0 2>&1")
            ssh.exec("sudo systemctl enable wg-quick@wg0 2>&1")
            ssh.exec("sudo systemctl start wg-quick@wg0 2>&1")
            _progress.value = 80

            _status.value = I18n.strings.ssh_status_waiting
            Thread.sleep(3000)

            val serviceStatus = ssh.exec("sudo systemctl is-active wg-quick@wg0 2>&1")
            if (!serviceStatus.trim().equals("active", ignoreCase = true)) {
                val serviceLogs = ssh.exec("sudo journalctl -u wg-quick@wg0 -n 40 --no-pager 2>&1")
                throw Exception(I18n.strings.ssh_error_service_start_wireguard.format(serviceStatus) + ".\n" + I18n.strings.label_logs + "\n${serviceLogs.take(600)}")
            }

            
            val interfaceCheck = ssh.exec("sudo ip link show wg0 2>&1")
            if (!interfaceCheck.contains("wg0")) {
                throw Exception("WireGuard interface wg0 was not created. Output: $interfaceCheck")
            }

            _progress.value = 90
            _status.value = I18n.strings.ssh_status_generating_client

            
            val encodedClientPrivateKey = java.net.URLEncoder.encode(clientPrivateKey, "UTF-8")
            val encodedPublicKey = java.net.URLEncoder.encode(serverPublicKey, "UTF-8")
            val wgUri = "wireguard://$encodedClientPrivateKey@${config.host}:${config.vpnPort}" +
                "?publickey=$encodedPublicKey" +
                "&address=10.7.0.2/32,fdde:dcba:9876::2/128" +
                "#Flare-${config.host}"

            _progress.value = 100
            wgUri

        } catch (e: Exception) {
            android.util.Log.e("WireGuardServerCreator", "SSH Setup Error: ${e.message}", e)
            _status.value = I18n.strings.ssh_error_generic.format(e.message?.take(200) ?: "")
            null
        } finally {
            try { ssh.disconnect() } catch (_: Exception) {}
        }
    }

    private fun hostKeyPrefKey(host: String, port: Int): String = "ssh_host_key_$host:$port"

    private fun java.security.PublicKey.fingerprintSha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
        val encodedDigest = android.util.Base64.encodeToString(
            digest,
            android.util.Base64.NO_WRAP
        ).trimEnd('=')
        return "SHA256:$encodedDigest"
    }
}
