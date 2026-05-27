package flare.client.app.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import flare.client.app.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri


enum class NotificationType {
    SUCCESS, ERROR, WARNING
}

data class NotificationData(
    val type: NotificationType,
    val text: String,
    val durationSec: Int,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null,
    val iconRes: Int? = null
)

object AppNotificationManager {
    private val _notifications = MutableSharedFlow<NotificationData>(extraBufferCapacity = 10)
    val notifications: SharedFlow<NotificationData> = _notifications.asSharedFlow()

    private const val BEST_PROFILE_CHANNEL = "best_profile_updates"
    private const val BEST_PROFILE_NOTIF_ID = 1002

    fun showNotification(
        type: NotificationType,
        text: String,
        durationSec: Int,
        actionText: String? = null,
        iconRes: Int? = null,
        onAction: (() -> Unit)? = null
    ) {
        _notifications.tryEmit(NotificationData(type, text, durationSec, actionText, onAction, iconRes))
    }

    fun showSystemNotification(
        context: Context, 
        title: String, 
        text: String, 
        isHighPriority: Boolean = false,
        actionText: String? = null,
        downloadUrl: String? = null
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = if (isHighPriority) "adaptive_tunnel_updates" else BEST_PROFILE_CHANNEL
        val notifId = if (isHighPriority) 1003 else BEST_PROFILE_NOTIF_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val importance = if (isHighPriority) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(
                    channelId,
                    if (isHighPriority) "Adaptive Tunnel Updates" else "Profile Updates",
                    importance
                )
                if (isHighPriority) {
                    channel.enableVibration(true)
                    channel.vibrationPattern = longArrayOf(0, 250)
                }
                manager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setAutoCancel(true)
            .apply {
                if (isHighPriority) {
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setVibrate(longArrayOf(0, 250))
                }
                if (downloadUrl != null && actionText != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setContentIntent(pendingIntent)
                    addAction(R.drawable.ic_vpn_key, actionText, pendingIntent)
                }
            }
            .build()
            
        manager.notify(notifId, notification)
    }
}

