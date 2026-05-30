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
import java.security.SecureRandom

class ShadowsocksServerCreator(
    private val context: Context
) : VpnServerCreator {
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

            var singboxPath = ssh.exec("command -v sing-box || ( [ -x /usr/local/bin/sing-box ] && echo /usr/local/bin/sing-box ) || ( [ -x /usr/bin/sing-box ] && echo /usr/bin/sing-box)").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (singboxPath.isEmpty()) {
                _status.value = I18n.strings.ssh_status_installing_shadowsocks
                
                ssh.exec("sudo -n apt-get update -qq && sudo -n apt-get install -y curl gnupg2 2>&1")
                
                ssh.exec("sudo -n mkdir -p /etc/apt/keyrings")
                ssh.exec("sudo -n curl -fsSL https://sing-box.sagernet.org/sing-box.gpg -o /etc/apt/keyrings/sing-box.gpg")
                ssh.exec("sudo -n chmod a+r /etc/apt/keyrings/sing-box.gpg")
                ssh.exec("echo \"deb [signed-by=/etc/apt/keyrings/sing-box.gpg] https://deb.sagernet.org/ sing-box main\" | sudo tee /etc/apt/sources.list.d/sing-box.list > /dev/null")
                ssh.exec("sudo -n apt-get update -qq")
                ssh.exec("sudo -n apt-get install -y sing-box 2>&1")
                singboxPath = ssh.exec("command -v sing-box || ( [ -x /usr/local/bin/sing-box ] && echo /usr/local/bin/sing-box ) || ( [ -x /usr/bin/sing-box ] && echo /usr/bin/sing-box)").lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            }

            if (singboxPath.isEmpty()) {
                
                try {
                    val arch = ssh.exec("uname -m")
                    val sbArch = when {
                        arch.contains("x86_64") -> "amd64"
                        arch.contains("aarch64") -> "arm64"
                        arch.contains("armv7") -> "armv7"
                        else -> "amd64"
                    }
                    val versionLine = ssh.exec("curl -sI https://github.com/SagerNet/sing-box/releases/latest | grep -i location")
                    val version = versionLine.substringAfter("/tag/v").trim().removeSuffix("\r")
                    if (version.isNotEmpty() && version.length < 15) {
                        val downloadUrl = "https://github.com/SagerNet/sing-box/releases/download/v$version/sing-box-$version-linux-$sbArch.tar.gz"
                        ssh.exec("curl -Lo /tmp/sing-box.tar.gz $downloadUrl")
                        ssh.exec("tar -xzf /tmp/sing-box.tar.gz -C /tmp")
                        ssh.exec("sudo -n mv /tmp/sing-box-$version-linux-$sbArch/sing-box /usr/local/bin/sing-box")
                        ssh.exec("sudo -n chmod +x /usr/local/bin/sing-box")
                        singboxPath = "/usr/local/bin/sing-box"
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("ShadowsocksServerCreator", "Fallback download failed", ex)
                }
            }

            if (singboxPath.isEmpty()) {
                throw Exception("Failed to install sing-box on the server.")
            }
            _progress.value = 50

            _status.value = I18n.strings.ssh_status_configuring_shadowsocks
            val keyBytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            val password = android.util.Base64.encodeToString(keyBytes, android.util.Base64.NO_WRAP)

            val stlsKeyBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val shadowTlsPassword = android.util.Base64.encodeToString(stlsKeyBytes, android.util.Base64.NO_WRAP)

            val singBoxConfig = org.json.JSONObject().apply {
                put("log", org.json.JSONObject().apply {
                    put("level", "warn")
                })
                put("inbounds", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("type", "shadowsocks")
                        put("tag", "shadowsocks-in")
                        put("listen", "127.0.0.1")
                        put("listen_port", 18388)
                        put("method", "2022-blake3-aes-256-gcm")
                        put("password", password)
                    })
                    put(org.json.JSONObject().apply {
                        put("type", "shadowtls")
                        put("tag", "shadowtls-in")
                        put("listen", "0.0.0.0")
                        put("listen_port", config.vpnPort)
                        put("version", 3)
                        put("users", org.json.JSONArray().put(org.json.JSONObject().apply {
                            put("name", "flare")
                            put("password", shadowTlsPassword)
                        }))
                        put("handshake", org.json.JSONObject().apply {
                            put("server", config.sni)
                            put("server_port", 443)
                        })
                        put("detour", "shadowsocks-in")
                    })
                })
                put("outbounds", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("type", "direct")
                        put("tag", "direct")
                    })
                })
            }

            val remoteConfigDir = "/etc/sing-box"
            ssh.exec("sudo -n mkdir -p $remoteConfigDir")
            val remoteConfigPath = "$remoteConfigDir/config.json"
            val configB64 = android.util.Base64.encodeToString(
                singBoxConfig.toString(2).toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            ssh.exec("echo '$configB64' | base64 -d | sudo tee $remoteConfigPath > /dev/null")
            ssh.exec("sudo chmod 644 $remoteConfigPath")

            val fileSize = ssh.exec("sudo wc -c < $remoteConfigPath 2>&1")
            if (fileSize.trim() == "0" || fileSize.trim().isEmpty()) {
                throw Exception(I18n.strings.ssh_error_config_write)
            }

            val serviceExists = ssh.exec("[ -f /lib/systemd/system/sing-box.service ] || [ -f /etc/systemd/system/sing-box.service ] && echo 'yes' || echo 'no'").trim()
            if (serviceExists != "yes") {
                val serviceContent = """
                    [Unit]
                    Description=sing-box service
                    After=network.target nss-lookup.target

                    [Service]
                    CapabilityBoundingSet=CAP_NET_ADMIN CAP_NET_BIND_SERVICE
                    AmbientCapabilities=CAP_NET_ADMIN CAP_NET_BIND_SERVICE
                    ExecStart=$singboxPath run -c /etc/sing-box/config.json
                    Restart=on-failure
                    RestartSec=10s
                    LimitNPROC=500
                    LimitNOFILE=1000000

                    [Install]
                    WantedBy=multi-user.target
                """.trimIndent()
                val serviceB64 = android.util.Base64.encodeToString(serviceContent.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                ssh.exec("echo '$serviceB64' | base64 -d | sudo tee /etc/systemd/system/sing-box.service > /dev/null")
                ssh.exec("sudo systemctl daemon-reload")
            }
            _progress.value = 75

            _status.value = I18n.strings.ssh_status_restarting_shadowsocks
            ssh.exec("sudo systemctl enable sing-box 2>&1")
            ssh.exec("sudo systemctl restart sing-box 2>&1")
            _progress.value = 85

            _status.value = I18n.strings.ssh_status_waiting
            Thread.sleep(3000)

            val serviceStatus = ssh.exec("sudo systemctl is-active sing-box 2>&1")
            if (!serviceStatus.trim().equals("active", ignoreCase = true)) {
                val serviceLogs = ssh.exec("sudo journalctl -u sing-box -n 40 --no-pager 2>&1")
                throw Exception(I18n.strings.ssh_error_service_start_shadowsocks.format(serviceStatus) + ".\n" + I18n.strings.label_logs + "\n${serviceLogs.take(600)}")
            }

            val portCheck = ssh.exec("sudo ss -tlnp 2>/dev/null | grep ':${config.vpnPort}' || echo 'port-check-unavailable'")
            if (!portCheck.contains("${config.vpnPort}") && !portCheck.contains("port-check-unavailable")) {
                throw Exception(I18n.strings.ssh_error_port_not_listening.format(config.vpnPort))
            }

            _progress.value = 95
            _status.value = I18n.strings.ssh_status_generating_client

            val rawUserInfo = "2022-blake3-aes-256-gcm:$password"
            val base64UserInfo = android.util.Base64.encodeToString(rawUserInfo.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

            val pluginOpts = "password=$shadowTlsPassword;version=3;host=${config.sni}"
            val encodedPlugin = java.net.URLEncoder.encode("shadowtls;$pluginOpts", "UTF-8")

            val shadowsocksUri = "ss://$base64UserInfo@${config.host}:${config.vpnPort}" +
                "?plugin=$encodedPlugin" +
                "&security=tls" +
                "&sni=${config.sni}" +
                "&shadowtls-password=${java.net.URLEncoder.encode(shadowTlsPassword, "UTF-8")}" +
                "&shadowtls-version=3" +
                "&type=tcp" +
                "#Flare-${config.host}"

            _progress.value = 100
            shadowsocksUri

        } catch (e: Exception) {
            android.util.Log.e("ShadowsocksServerCreator", "SSH Setup Error: ${e.message}", e)
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
