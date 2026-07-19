package flare.client.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconSwitcher {
    private const val TAG = "AppIconSwitcher"

    private val componentMap = mapOf(
        "main" to "flare.client.app.MainActivityAliasDefault",
        "monochrome" to "flare.client.app.MainActivityAliasMonochrome",
        "softplush" to "flare.client.app.MainActivityAliasSoftPlush",
        "blueprint" to "flare.client.app.MainActivityAliasBlueprint"
    )

    fun switchIcon(context: Context, oldIcon: String, newIcon: String) {
        val pm = context.packageManager
        val targetComponent = componentMap[newIcon] ?: return

        Log.d(TAG, "Switching app icon from $oldIcon to $newIcon ($targetComponent)")

        
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, targetComponent),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable component: $targetComponent", e)
        }

        
        for ((key, className) in componentMap) {
            if (key != newIcon) {
                try {
                    Log.d(TAG, "Disabling component: $className")
                    pm.setComponentEnabledSetting(
                        ComponentName(context, className),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disable component: $className", e)
                }
            }
        }
    }
}
