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
import java.util.UUID

class HysteriaServerCreator(private val context: Context) : VpnServerCreator {
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

    private fun SSHClient.execWithErr(cmd: String): Pair<String, String> {
        var out = ""
        var err = ""
        startSession().use { session ->
            val c = session.exec(cmd)
            val errThread = Thread {
                try {
                    err = c.errorStream.bufferedReader().readText()
                } catch (_: Exception) {}
            }
            errThread.start()
            out = c.inputStream.bufferedReader().readText()
            errThread.join()
            c.join()
        }
        return out.trim() to err.trim()
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

            var hyPath = ssh.exec("command -v hysteria || ( [ -x /usr/local/bin/hysteria ] && echo /usr/local/bin/hysteria ) || ( [ -x /usr/bin/hysteria ] && echo /usr/bin/hysteria)").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (hyPath.isEmpty()) {
                _status.value = I18n.strings.ssh_status_installing_hysteria2
                ssh.exec("sudo -n apt-get update -qq && sudo -n apt-get install -y curl 2>&1")
                ssh.exec("curl -fsSL https://get.hy2.sh/ | sudo -n bash 2>&1")
                hyPath = "/usr/local/bin/hysteria"
            }
            _progress.value = 30

            val isIp = android.util.Patterns.IP_ADDRESS.matcher(config.host).matches()
            
            val port80Check = ssh.exec("sudo ss -tlnp 2>/dev/null | grep ':80 ' | grep -v 'hysteria' || echo 'free'")
            val port443Check = ssh.exec("sudo ss -tlnp 2>/dev/null | grep ':443 ' | grep -v 'hysteria' || echo 'free'")
            val portsFree = port80Check.contains("free") && port443Check.contains("free")
            
            val useAcme = !isIp && portsFree
            
            var fingerprint = ""
            if (!useAcme) {
                _status.value = I18n.strings.ssh_status_generating_cert
                ssh.exec("sudo -n mkdir -p /etc/hysteria")
                
                val sanType = if (android.util.Patterns.IP_ADDRESS.matcher(config.sni).matches()) "IP" else "DNS"
                val opensslCmd = "sudo -n openssl req -x509 -nodes -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -keyout /etc/hysteria/server.key -out /etc/hysteria/server.crt -subj \"/CN=${config.sni}\" -addext \"subjectAltName=$sanType:${config.sni}\" -days 36500 2>&1"
                val (certOut, certErr) = ssh.execWithErr(opensslCmd)
                
                val keyCheck = ssh.exec("sudo -n [ -s /etc/hysteria/server.key ] && echo 'ok' || echo 'error'")
                val crtCheck = ssh.exec("sudo -n [ -s /etc/hysteria/server.crt ] && echo 'ok' || echo 'error'")
                
                if (keyCheck != "ok" || crtCheck != "ok") {
                    throw Exception(I18n.strings.ssh_error_cert + "\n" + I18n.strings.label_output + " [$certOut]\n" + I18n.strings.label_errors + " [$certErr]")
                }
                
                ssh.exec("sudo -n chmod 644 /etc/hysteria/server.crt /etc/hysteria/server.key")

                val fingerprintCmd = "sudo -n openssl x509 -in /etc/hysteria/server.crt -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64"
                fingerprint = ssh.exec(fingerprintCmd).replace("\\s".toRegex(), "").trim()
                
                if (fingerprint.isEmpty() || fingerprint.length < 40 || fingerprint.length > 50) {
                    throw Exception("Failed to get certificate fingerprint. Output: [$fingerprint]")
                }
            }
            _progress.value = 55

            _status.value = I18n.strings.ssh_status_configuring_hysteria2
            val password = UUID.randomUUID().toString()
            
            var obfsBlock = ""
            var clientObfsParams = ""
            if (config.obfsPassword.isNotBlank()) {
                obfsBlock = """
obfs:
  type: salamander
  salamander:
    password: ${config.obfsPassword}
"""
                clientObfsParams = "&obfs=salamander&obfs-password=${config.obfsPassword}"
            }

            val tlsBlock = if (useAcme) {
                """
acme:
  domains:
    - ${config.host}
  email: admin@${config.host}
                """.trimIndent()
            } else {
                """
tls:
  cert: /etc/hysteria/server.crt
  key: /etc/hysteria/server.key
                """.trimIndent()
            }

            val hyConfig = """
listen: :${config.vpnPort}

$tlsBlock

auth:
  type: password
  password: $password
$obfsBlock
masquerade:
  type: proxy
  proxy:
    url: https://${config.sni}
    rewriteHost: true
""".trimIndent()

            val remoteConfigPath = "/etc/hysteria/config.yaml"
            val configB64 = android.util.Base64.encodeToString(
                hyConfig.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            ssh.exec("echo '$configB64' | base64 -d | sudo tee $remoteConfigPath > /dev/null")

            val fileSize = ssh.exec("sudo wc -c < $remoteConfigPath 2>&1")
            if (fileSize.trim() == "0" || fileSize.trim().isEmpty()) {
                throw Exception(I18n.strings.ssh_error_config_write)
            }
            _progress.value = 70

            _progress.value = 75
            _status.value = I18n.strings.ssh_status_configuring ?: "Applying server optimizations..."

            
            val sysctlConfig = """
                net.core.default_qdisc=fq
                net.ipv4.tcp_congestion_control=bbr
                net.core.rmem_max=16777216
                net.core.wmem_max=16777216
                net.core.rmem_default=2097152
                net.core.wmem_default=2097152
                net.ipv4.udp_rmem_min=8192
                net.ipv4.udp_wmem_min=8192
                fs.file-max=1048576
            """.trimIndent()
            val sysctlB64 = android.util.Base64.encodeToString(
                sysctlConfig.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            ssh.exec("echo '$sysctlB64' | base64 -d | sudo tee -a /etc/sysctl.conf > /dev/null")
            ssh.exec("sudo sysctl -p")

            
            ssh.exec("sudo apt-get install -y ufw")
            ssh.exec("sudo ufw allow OpenSSH || sudo ufw allow 22/tcp")
            ssh.exec("sudo ufw allow ${config.vpnPort}/udp")
            if (useAcme) {
                ssh.exec("sudo ufw allow 80/tcp")
                ssh.exec("sudo ufw allow 443/tcp")
            }
            if (!config.mport.isNullOrBlank()) {
                val iptPorts = config.mport.replace(Regex("[\\\\s-]+"), ":")
                ssh.exec("sudo ufw allow $iptPorts/udp")
            }
            ssh.exec("sudo ufw --force enable")

            _status.value = I18n.strings.ssh_status_restarting_hysteria2
            
            var systemdOverride = "[Service]\nRestart=always\nRestartSec=3\nNice=-5\nLimitNOFILE=1048576\nAmbientCapabilities=CAP_NET_BIND_SERVICE\n"
            if (!config.mport.isNullOrBlank()) {
                val iptPorts = config.mport.replace(Regex("[\\\\s-]+"), ":")
                systemdOverride += "ExecStartPost=+-/sbin/iptables -t nat -A PREROUTING -p udp -m multiport --dports $iptPorts -j REDIRECT --to-ports ${config.vpnPort}\n"
                systemdOverride += "ExecStartPost=+-/sbin/ip6tables -t nat -A PREROUTING -p udp -m multiport --dports $iptPorts -j REDIRECT --to-ports ${config.vpnPort}\n"
                systemdOverride += "ExecStopPost=+-/sbin/iptables -t nat -D PREROUTING -p udp -m multiport --dports $iptPorts -j REDIRECT --to-ports ${config.vpnPort}\n"
                systemdOverride += "ExecStopPost=+-/sbin/ip6tables -t nat -D PREROUTING -p udp -m multiport --dports $iptPorts -j REDIRECT --to-ports ${config.vpnPort}\n"
            }
            ssh.exec("sudo -n mkdir -p /etc/systemd/system/hysteria-server.service.d")
            ssh.exec("echo -e '$systemdOverride' | sudo tee /etc/systemd/system/hysteria-server.service.d/override.conf > /dev/null")
            ssh.exec("sudo systemctl daemon-reload 2>&1")
            
            ssh.exec("sudo systemctl enable hysteria-server 2>&1")
            ssh.exec("sudo systemctl restart hysteria-server 2>&1")
            
            val cronCmd = "echo '0 3 * * * root curl -fsSL https://get.hy2.sh/ | bash && systemctl restart hysteria-server' | sudo tee /etc/cron.d/hysteria-update > /dev/null"
            ssh.exec(cronCmd)
            ssh.exec("sudo chmod 644 /etc/cron.d/hysteria-update")
            
            _progress.value = 80

            _status.value = I18n.strings.ssh_status_waiting
            Thread.sleep(3000)

            val serviceStatus = ssh.exec("sudo systemctl is-active hysteria-server 2>&1")
            if (!serviceStatus.trim().equals("active", ignoreCase = true)) {
                val serviceLogs = ssh.exec("sudo journalctl -u hysteria-server -n 40 --no-pager 2>&1")
                throw Exception(I18n.strings.ssh_error_service_start_hysteria2.format(serviceStatus) + ".\n" + I18n.strings.label_logs + "\n${serviceLogs.take(600)}")
            }

            val portCheck = ssh.exec("sudo ss -ulnp 2>/dev/null | grep ':${config.vpnPort}' || echo 'port-check-unavailable'")
            if (!portCheck.contains("${config.vpnPort}") && !portCheck.contains("port-check-unavailable")) {
                throw Exception(I18n.strings.ssh_error_port_not_listening_udp.format(config.vpnPort))
            }

            _progress.value = 90
            _status.value = I18n.strings.ssh_status_generating_client

            val clientPortHoppingParams = if (!config.mport.isNullOrBlank()) "&mport=${config.mport}" else ""
            val pinParam = if (fingerprint.isNotBlank()) "&pin=$fingerprint" else ""
            val finalSni = if (useAcme) config.host else config.sni

            val hysteria2Uri = "hysteria2://$password@${config.host}:${config.vpnPort}" +
                "?sni=$finalSni" +
                pinParam +
                clientPortHoppingParams +
                clientObfsParams +
                "#Flare-${config.host}"

            _progress.value = 100
            hysteria2Uri

        } catch (e: Exception) {
            android.util.Log.e("HysteriaServerCreator", "SSH Setup Error: ${e.message}", e)
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
