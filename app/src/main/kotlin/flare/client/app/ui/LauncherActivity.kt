package flare.client.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            data = intent.data
            if (intent.extras != null) putExtras(intent.extras!!)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        startActivity(mainIntent)
        finish()
    }
}
