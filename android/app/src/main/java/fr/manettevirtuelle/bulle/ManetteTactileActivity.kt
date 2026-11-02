package fr.manettevirtuelle.bulle

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class ManetteTactileActivity : Activity() {

    private lateinit var vue: ManetteTactileView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vue = ManetteTactileView(this)
        setContentView(vue)
    }

    override fun onResume() {
        super.onResume()
        Relais.appAuPremierPlan = true
        Relais.notifier()
    }

    override fun onPause() {
        vue.relacher()
        Relais.appAuPremierPlan = false
        Relais.notifier()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }
}
