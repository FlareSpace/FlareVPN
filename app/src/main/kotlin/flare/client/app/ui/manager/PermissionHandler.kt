package flare.client.app.ui.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class PermissionHandler(
    private val activity: ComponentActivity,
    private val onVpnResult: (Boolean) -> Unit,
    private val onNotificationResult: (Boolean) -> Unit,
    private val onOnboardingNotificationResult: (Boolean) -> Unit,
    private val onBatteryResult: () -> Unit,
    private val onUsageResult: () -> Unit,
    private val onImportFileResult: (Uri?) -> Unit,
    private val onQrScanResult: (String?) -> Unit
) {

    private lateinit var vpnPermLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermLauncher: ActivityResultLauncher<String>
    private lateinit var onboardingNotificationPermLauncher: ActivityResultLauncher<String>
    private lateinit var batteryPermLauncher: ActivityResultLauncher<Intent>
    private lateinit var usagePermLauncher: ActivityResultLauncher<Intent>
    private lateinit var importFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var qrScannerLauncher: ActivityResultLauncher<Intent>

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                vpnPermLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    onVpnResult(result.resultCode == Activity.RESULT_OK)
                }

                notificationPermLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    onNotificationResult(isGranted)
                }

                onboardingNotificationPermLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    onOnboardingNotificationResult(isGranted)
                }

                batteryPermLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    onBatteryResult()
                }

                usagePermLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    onUsageResult()
                }

                importFileLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    onImportFileResult(uri)
                }

                qrScannerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val qrContent = if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.getStringExtra("extra_qr_content")
                    } else null
                    onQrScanResult(qrContent)
                }
            }
        })
    }

    fun launchVpnPermission(intent: Intent) = vpnPermLauncher.launch(intent)
    fun launchNotificationPermission() = notificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    fun launchOnboardingNotificationPermission() = onboardingNotificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    fun launchBatteryPermission(intent: Intent) = batteryPermLauncher.launch(intent)
    fun launchUsagePermission(intent: Intent) = usagePermLauncher.launch(intent)
    fun launchImportFile(mimeTypes: Array<String>) = importFileLauncher.launch(mimeTypes)
    fun launchQrScanner(intent: Intent) = qrScannerLauncher.launch(intent)
}