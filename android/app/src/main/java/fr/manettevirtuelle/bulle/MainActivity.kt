package fr.manettevirtuelle.bulle

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var etat: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val d = resources.displayMetrics.density
        val marge = (20 * d).toInt()

        val colonne = LinearLayout(this)
        colonne.orientation = LinearLayout.VERTICAL
        colonne.setPadding(marge, marge * 2, marge, marge)

        val titre = TextView(this)
        titre.text = "Manette Bulle (test)"
        titre.textSize = 24f
        colonne.addView(titre)

        val consignes = TextView(this)
        consignes.text = "\nTest : la manette peut-elle rester active quand tu utilises une autre application ?\n\n" +
            "1. Autorise l'affichage par-dessus les autres applications.\n" +
            "2. Lance la bulle.\n" +
            "3. Relie ta manette en Bluetooth, puis ouvre une autre application.\n" +
            "4. La bulle doit devenir VERTE (MANETTE ACTIVE) et afficher les boutons que tu presses.\n" +
            "5. Touche une autre application : la bulle devient grise. Retouche la bulle : redevient-elle verte ?\n\n" +
            "Tu peux deplacer la bulle avec le doigt.\n"
        consignes.textSize = 16f
        colonne.addView(consignes)

        etat = TextView(this)
        etat.textSize = 16f
        colonne.addView(etat)

        val b1 = Button(this)
        b1.text = "1. Autoriser la bulle"
        b1.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        colonne.addView(b1)

        val b2 = Button(this)
        b2.text = "2. Lancer la bulle"
        b2.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, BulleService::class.java))
            } else {
                majEtat()
            }
        }
        colonne.addView(b2)

        val b3 = Button(this)
        b3.text = "Arreter la bulle"
        b3.setOnClickListener { stopService(Intent(this, BulleService::class.java)) }
        colonne.addView(b3)

        val defilement = ScrollView(this)
        defilement.addView(colonne)
        setContentView(defilement)
    }

    override fun onResume() {
        super.onResume()
        majEtat()
    }

    private fun majEtat() {
        etat.text = if (Settings.canDrawOverlays(this)) {
            "Autorisation d'affichage : OUI\n"
        } else {
            "Autorisation d'affichage : NON (fais l'etape 1)\n"
        }
    }
}
