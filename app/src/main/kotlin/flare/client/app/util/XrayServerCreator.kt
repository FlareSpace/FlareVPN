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
import java.security.SecureRandom
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

object SecurityInitializer {
    private var initialized = false
    fun init() {
        if (!initialized) {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            initialized = true
        }
    }
}

class XrayServerCreator(private val context: Context) : VpnServerCreator {
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

            var xrayPath = ssh.exec("command -v xray || ( [ -x /usr/local/bin/xray ] && echo /usr/local/bin/xray ) || ( [ -x /usr/bin/xray ] && echo /usr/bin/xray)").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (xrayPath.isEmpty()) {
                _status.value = I18n.strings.ssh_status_installing_xray
                ssh.exec("sudo -n apt-get update -qq && sudo -n apt-get install -y curl 2>&1")
                ssh.exec("curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh | sudo -n bash -s -- install 2>&1")
                xrayPath = "/usr/local/bin/xray"
            }
            _progress.value = 30

            _status.value = I18n.strings.ssh_status_generating_keys
            val uuid = UUID.randomUUID().toString()
            val shortId = ByteArray(8).apply { SecureRandom().nextBytes(this) }
                .joinToString("") { "%02x".format(it) }

            val (keyOut, keyErr) = ssh.execWithErr("sudo -n $xrayPath x25519 2>&1")
            val keyLines = keyOut.lines()
            val privateKey = keyLines
                .find { it.contains("Private", ignoreCase = true) && it.contains("key", ignoreCase = true) }
                ?.substringAfter(":")?.trim()
            val publicKey = keyLines
                .find { it.contains("Public", ignoreCase = true) && it.contains("key", ignoreCase = true) }
                ?.substringAfter(":")?.trim()

            if (privateKey.isNullOrEmpty() || publicKey.isNullOrEmpty()) {
                throw Exception(I18n.strings.ssh_error_keys + "\n" + I18n.strings.label_output + " [$keyOut]\n" + I18n.strings.label_errors + " [$keyErr]")
            }
            _progress.value = 55

            _status.value = I18n.strings.ssh_status_configuring
            val xrayConfig = """
{
  "log": { "loglevel": "warning" },
  "inbounds": [
    {
      "listen": "0.0.0.0",
      "port": ${config.vpnPort},
      "protocol": "vless",
      "settings": {
        "clients": [
          {
            "id": "$uuid",
            "flow": "xtls-rprx-vision"
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "dest": "${config.sni}:443",
          "xver": 0,
          "serverNames": ["${config.sni}"],
          "privateKey": "$privateKey",
          "shortIds": ["", "$shortId"]
        }
      },
      "sniffing": {
        "enabled": true,
        "destOverride": ["http", "tls", "quic"]
      }
    }
  ],
  "outbounds": [
    { "protocol": "freedom", "tag": "direct" },
    { "protocol": "blackhole", "tag": "block" }
  ]
}""".trimIndent()

            val remoteConfigPath = "/usr/local/etc/xray/config.json"
            val configB64 = android.util.Base64.encodeToString(
                xrayConfig.toByteArray(Charsets.UTF_8),
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

            
            ssh.exec("sudo mkdir -p /etc/systemd/system/xray.service.d")
            ssh.exec("echo -e '[Service]\\nRestart=always\\nRestartSec=3s' | sudo tee /etc/systemd/system/xray.service.d/override.conf > /dev/null")
            ssh.exec("sudo systemctl daemon-reload")

            
            val sysctlConfig = """
                net.core.default_qdisc=fq
                net.ipv4.tcp_congestion_control=bbr
                net.core.rmem_max=2500000
                net.core.wmem_max=2500000
                fs.file-max=51200
            """.trimIndent()
            val sysctlB64 = android.util.Base64.encodeToString(
                sysctlConfig.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            ssh.exec("echo '$sysctlB64' | base64 -d | sudo tee -a /etc/sysctl.conf > /dev/null")
            ssh.exec("sudo sysctl -p")

            
            ssh.exec("sudo apt-get install -y ufw")
            ssh.exec("sudo ufw allow OpenSSH || sudo ufw allow 22/tcp")
            ssh.exec("sudo ufw allow ${config.vpnPort}/tcp")
            ssh.exec("sudo ufw allow ${config.vpnPort}/udp")
            ssh.exec("sudo ufw --force enable")

            _status.value = I18n.strings.ssh_status_restarting
            ssh.exec("sudo systemctl enable xray 2>&1")
            ssh.exec("sudo systemctl restart xray 2>&1")
            _progress.value = 85

            _status.value = I18n.strings.ssh_status_waiting
            Thread.sleep(3000)

            val serviceStatus = ssh.exec("sudo systemctl is-active xray 2>&1")
            if (!serviceStatus.trim().equals("active", ignoreCase = true)) {
                val serviceLogs = ssh.exec("sudo journalctl -u xray -n 40 --no-pager 2>&1")
                throw Exception(I18n.strings.ssh_error_service_start.format(serviceStatus) + ".\n" + I18n.strings.label_logs + "\n${serviceLogs.take(600)}")
            }

            val portCheck = ssh.exec("sudo ss -tlnp 2>/dev/null | grep ':${config.vpnPort}' || echo 'port-check-unavailable'")
            if (!portCheck.contains("${config.vpnPort}") && !portCheck.contains("port-check-unavailable")) {
                throw Exception(I18n.strings.ssh_error_port_not_listening.format(config.vpnPort))
            }

            _progress.value = 90
            _status.value = I18n.strings.ssh_status_generating_client
            val vlessUri = "vless://$uuid@${config.host}:${config.vpnPort}" +
                "?security=reality" +
                "&flow=xtls-rprx-vision" +
                "&sni=${config.sni}" +
                "&pbk=${java.net.URLEncoder.encode(publicKey, "UTF-8")}" +
                "&sid=$shortId" +
                "&fp=firefox" +
                "&packetEncoding=xudp" +
                "&type=tcp" +
                "#Flare-${config.host}"

            _progress.value = 100
            vlessUri

        } catch (e: Exception) {
            android.util.Log.e("XrayServerCreator", "SSH Setup Error: ${e.message}", e)
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
