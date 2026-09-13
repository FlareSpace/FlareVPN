package flare.client.app.util

import flare.client.app.data.model.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileExportHelperTest {

    @Test
    fun testExportHysteria2FromInternalJson() {
        val configJson = """
            {
              "outbounds": [
                {
                  "type": "hysteria2",
                  "tag": "hy2-node",
                  "server": "hy2.example.com",
                  "server_port": 8443,
                  "password": "secret@password!123",
                  "server_ports": ["20000:30000"],
                  "hop_interval": "30s",
                  "up_mbps": 100,
                  "down_mbps": 200,
                  "obfs": {
                    "type": "salamander",
                    "password": "obfspassword"
                  },
                  "tls": {
                    "enabled": true,
                    "server_name": "sni.example.com",
                    "insecure": true,
                    "alpn": ["h3"],
                    "certificate_public_key_sha256": ["sha256pin=="]
                  }
                },
                {
                  "type": "direct",
                  "tag": "direct"
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 1L,
            name = "Мой Сервер",
            uri = "internal://json",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull("Link should not be null", link)
        assertTrue("Scheme should be hysteria2://", link!!.startsWith("hysteria2://"))
        assertTrue("Server and port should be present", link.contains("hy2.example.com:8443"))
        assertTrue("Password should be encoded", link.contains("secret%40password%21123@"))
        assertTrue("SNI should be present", link.contains("sni=sni.example.com"))
        assertTrue("Insecure should be 1", link.contains("insecure=1"))
        assertTrue("Pin should be present", link.contains("pin=sha256pin%3D%3D"))
        assertTrue("Alpn should be h3", link.contains("alpn=h3"))
        assertTrue("Obfs should be salamander", link.contains("obfs=salamander"))
        assertTrue("Obfs password should be present", link.contains("obfs-password=obfspassword"))
        assertTrue("Mport should be present", link.contains("mport=20000%3A30000"))
        assertTrue("Hop interval should be present", link.contains("hop_interval=30s"))
        assertTrue("Up should be 100", link.contains("up=100"))
        assertTrue("Down should be 200", link.contains("down=200"))
        assertTrue("Name should be URL-encoded in fragment", link.contains("#%D0%9C%D0%BE%D0%B9+%D0%A1%D0%B5%D1%80%D0%B2%D0%B5%D1%80"))
    }

    @Test
    fun testExportHysteria2WithEmptyUriFallsBackToConfigJson() {
        val configJson = """
            {
              "outbounds": [
                {
                  "type": "hysteria2",
                  "server": "1.2.3.4",
                  "server_port": 443,
                  "password": "pass"
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 2L,
            name = "TestNode",
            uri = "",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull("Link should not be null for empty uri", link)
        assertEquals("hysteria2://pass@1.2.3.4:443#TestNode", link)
    }

    @Test
    fun testExportHysteria2WithIpv6Host() {
        val configJson = """
            {
              "outbounds": [
                {
                  "type": "hysteria2",
                  "server": "2001:db8::1",
                  "server_port": 443,
                  "password": "pass"
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 3L,
            name = "IPv6Node",
            uri = "internal://json",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull(link)
        assertTrue(link!!.startsWith("hysteria2://pass@[2001:db8::1]:443#IPv6Node"))
    }

    @Test
    fun testExportHysteria1Legacy() {
        val configJson = """
            {
              "outbounds": [
                {
                  "type": "hysteria",
                  "server": "hy1.example.com",
                  "server_port": 443,
                  "auth_str": "hy1pass",
                  "up_mbps": 50,
                  "down_mbps": 100,
                  "obfs": "myobfs"
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 4L,
            name = "Hy1Node",
            uri = "internal://json",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull(link)
        assertTrue(link!!.startsWith("hysteria://hy1pass@hy1.example.com:443"))
        assertTrue(link.contains("up=50"))
        assertTrue(link.contains("down=100"))
        assertTrue(link.contains("obfs=myobfs"))
        assertTrue(link.endsWith("#Hy1Node"))
    }

    @Test
    fun testExportExistingUriEncodesUpdatedName() {
        val profile = ProfileEntity(
            id = 5L,
            name = "Германия #1",
            uri = "hysteria2://pass@de.example.com:443?sni=de.example.com#OldName",
            configJson = "{}"
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull(link)
        assertTrue(link!!.startsWith("hysteria2://pass@de.example.com:443?sni=de.example.com#"))
        assertTrue(link.contains("%D0%93%D0%B5%D1%80%D0%BC%D0%B0%D0%BD%D0%B8%D1%8F+%231"))
    }

    @Test
    fun testExportShadowsocksFromInternalJson() {
        val configJson = """
            {
              "outbounds": [
                {
                  "type": "shadowsocks",
                  "server": "ss.example.com",
                  "server_port": 8388,
                  "method": "aes-256-gcm",
                  "password": "sspassword"
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 6L,
            name = "SSNode",
            uri = "internal://json",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull(link)
        assertTrue(link!!.startsWith("ss://"))
        assertTrue(link.endsWith("#SSNode"))
    }

    @Test
    fun testExportWireGuardFromInternalJson() {
        val configJson = """
            {
              "endpoints": [
                {
                  "type": "wireguard",
                  "private_key": "privkey==",
                  "address": ["10.0.0.2/32"],
                  "peers": [
                    {
                      "address": "wg.example.com",
                      "port": 51820,
                      "public_key": "pubkey=="
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val profile = ProfileEntity(
            id = 7L,
            name = "WGNode",
            uri = "internal://json",
            configJson = configJson
        )

        val link = ProfileExportHelper.exportLink(profile)
        assertNotNull(link)
        assertTrue(link!!.startsWith("wireguard://privkey%3D%3D@wg.example.com:51820"))
        assertTrue(link.contains("publickey=pubkey%3D%3D"))
        assertTrue(link.contains("address=10.0.0.2%2F32"))
        assertTrue(link.endsWith("#WGNode"))
    }
}
