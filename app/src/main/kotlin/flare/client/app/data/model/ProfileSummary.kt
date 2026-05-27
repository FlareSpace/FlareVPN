package flare.client.app.data.model

data class ProfileSummary(
    val id: Long,
    val name: String,
    val uri: String,
    val serverDescription: String?,
    val subscriptionId: Long?,
    val isSelected: Boolean,
    val protocol: String?
)

