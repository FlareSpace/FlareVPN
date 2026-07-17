package flare.client.app.data.api

data class SubscriptionInfoResponse(
    val has_sub: Boolean,
    val keys: List<KeyInfo>? = null,
    val balance: Int = 0,
    val has_telegram: Boolean = false
)

data class KeyInfo(
    val id: Int,
    val plan: String,
    val status: String,
    val expires_at: String,
    val devices: List<Device>?,
    val marzban_username: String?,
    val sub_link: String?,
    val ip_limit: Int? = null,
    val name: String? = null,
    val used_traffic: Long? = null,
    val data_limit: Long? = null
)

data class Device(
    val id: Int,
    val hwid: String,
    val name: String,
    val os_version: String? = null,
    val user_agent: String? = null,
    val created_at: String
)

data class AddDeviceRequest(
    val hwid: String,
    val name: String,
    val sub_id: Int? = null
)

data class AddDeviceResponse(
    val sub_link: String
)

data class BaseResponse(
    val status: String
)

data class RenameKeyRequest(
    val sub_id: Int,
    val name: String
)

data class CreateKeyResponse(
    val status: String,
    val sub_link: String,
    val balance: Int = 0
)
