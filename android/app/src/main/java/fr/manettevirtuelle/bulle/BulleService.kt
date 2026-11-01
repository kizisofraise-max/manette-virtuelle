package fr.manettevirtuelle.bulle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

class BulleService : Service() {

    private var gestionnaire: WindowManager? = null
    private var bulle: BulleView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        demarrerPremierPlan()
        afficherBulle()
        return START_NOT_STICKY
    }

    private fun demarrerPremierPlan() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Bulle manette", NotificationManager.IMPORTANCE_LOW)
        )
        val arret = PendingIntent.getService(
            this, 0,
            Intent(this, BulleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        @Suppress("DEPRECATION")
        val notif = Notification.Builder(this, CANAL)
            .setContentTitle("Manette Bulle (test)")
            .setContentText("La bulle est affichee. Touche-la pour reprendre la manette.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arreter", arret)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_NOTIF, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_NOTIF, notif)
        }
    }

    private fun afficherBulle() {
        if (bulle != null) return
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val vue = BulleView(this)
        // Fenetre focusable (pas de FLAG_NOT_FOCUSABLE) : c'est elle qui recoit la manette.
        // FLAG_NOT_TOUCH_MODAL : les touches hors de la bulle vont a l'application derriere.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 300
        vue.params = params
        vue.gestionnaire = wm
        try {
            wm.addView(vue, params)
            gestionnaire = wm
            bulle = vue
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            bulle?.let { gestionnaire?.removeView(it) }
        } catch (e: Exception) {
        }
        bulle = null
        super.onDestroy()
    }

    companion object {
        const val CANAL = "bulle_manette"
        const val ID_NOTIF = 1
        const val ACTION_STOP = "fr.manettevirtuelle.bulle.STOP"
    }
}
