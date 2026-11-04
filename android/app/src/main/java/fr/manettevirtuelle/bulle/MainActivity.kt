package fr.manettevirtuelle.bulle

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var champAdresse: EditText
    private lateinit var champCode: EditText
    private lateinit var boutonConnexion: Button
    private lateinit var texteEtat: TextView
    private lateinit var texteAppareils: TextView
    private lateinit var texteSignal: TextView
    private lateinit var texteBulle: TextView
    private lateinit var texteManuel: TextView
    private lateinit var boutonTactile: Button
    private lateinit var boutonAutoriser: Button
    private lateinit var boutonAfficher: Button
    private lateinit var boutonCacher: Button

    private var casquesConnus: Set<String>? = null
    private val ecouteurRelais: () -> Unit = { majConnexion() }

    private val ecouteurAppareils = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            majAppareils()
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            majAppareils()
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            majAppareils()
        }
    }

    private val rappelAudio = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            majCasques()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            majCasques()
        }
    }

    // ---------- petits outils d'affichage ----------
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun carte(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        val fond = GradientDrawable()
        fond.cornerRadius = dp(14).toFloat()
        fond.setColor(Color.parseColor("#181c25"))
        fond.setStroke(dp(1), Color.parseColor("#2d3342"))
        c.setBackground(fond)
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, dp(12))
        c.layoutParams = lp
        return c
    }

    private fun texte(t: String, taille: Float, couleur: String = "#e8ecf4", gras: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = taille
        v.setTextColor(Color.parseColor(couleur))
        if (gras) v.setTypeface(v.typeface, Typeface.BOLD)
        v.setPadding(0, dp(2), 0, dp(4))
        return v
    }

    private fun bouton(t: String, action: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.setOnClickListener { action() }
        return b
    }

    // ---------- creation de l'ecran ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val prefs = getSharedPreferences("manette", Context.MODE_PRIVATE)

        val colonne = LinearLayout(this)
        colonne.orientation = LinearLayout.VERTICAL
        colonne.setPadding(dp(16), dp(32), dp(16), dp(16))

        // En-tete
        val entete = LinearLayout(this)
        entete.orientation = LinearLayout.HORIZONTAL
        entete.gravity = android.view.Gravity.CENTER_VERTICAL
        entete.setPadding(0, 0, 0, dp(16))
        val logo = ImageView(this)
        logo.setImageResource(R.mipmap.ic_launcher)
        logo.layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        val titre = texte("Manette Virtuelle Phone", 22f, gras = true)
        titre.setPadding(dp(14), 0, 0, 0)
        entete.addView(logo)
        entete.addView(titre)
        colonne.addView(entete)

        // 1. Connexion au PC
        val c1 = carte()
        c1.addView(texte("1. Connexion au PC", 17f, gras = true))
        c1.addView(
            texte(
                "Lance le programme sur ton PC, puis tape ici le code à 4 chiffres affiché dans sa fenêtre. " +
                    "Le téléphone et le PC doivent être sur le même Wi-Fi.",
                13f, "#8b94a8"
            )
        )
        champCode = EditText(this)
        champCode.hint = "Code à 4 chiffres"
        champCode.setHintTextColor(Color.parseColor("#6b7488"))
        champCode.setTextColor(Color.WHITE)
        champCode.textSize = 22f
        champCode.inputType = InputType.TYPE_CLASS_NUMBER
        champCode.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        c1.addView(champCode)

        boutonConnexion = bouton("Se connecter") { actionConnexion() }
        c1.addView(boutonConnexion)
        texteEtat = texte("", 14f)
        c1.addView(texteEtat)

        texteManuel = texte("Ça ne trouve pas mon PC ? Saisir son adresse à la main", 13f, "#35c2ff")
        texteManuel.setPadding(0, dp(10), 0, dp(4))
        c1.addView(texteManuel)
        champAdresse = EditText(this)
        champAdresse.hint = "Adresse du PC (ex : 192.168.1.113)"
        champAdresse.setHintTextColor(Color.parseColor("#6b7488"))
        champAdresse.setTextColor(Color.WHITE)
        champAdresse.setSingleLine(true)
        champAdresse.setText(prefs.getString("adresse", ""))
        champAdresse.visibility = View.GONE
        c1.addView(champAdresse)
        texteManuel.setOnClickListener {
            champAdresse.visibility = if (champAdresse.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        colonne.addView(c1)

        // 2. Appareils
        val c2 = carte()
        c2.addView(texte("2. Appareils détectés", 17f, gras = true))
        texteAppareils = texte("", 13f)
        c2.addView(texteAppareils)
        c2.addView(texte("Appuie sur un bouton de ta manette pour voir son nom :", 13f, "#8b94a8"))
        texteSignal = texte("aucun signal", 14f, "#35c2ff")
        c2.addView(texteSignal)
        colonne.addView(c2)

        // 3. Manette tactile de secours
        val c3 = carte()
        c3.addView(texte("3. Manette tactile de secours", 17f, gras = true))
        c3.addView(
            texte(
                "Pas de manette sous la main ? Utilise l'écran du téléphone comme manette (sticks, croix, boutons, gâchettes). " +
                    "Disponible une fois connecté au PC.",
                13f, "#8b94a8"
            )
        )
        boutonTactile = bouton("Ouvrir la manette tactile") {
            startActivity(Intent(this, ManetteTactileActivity::class.java))
        }
        c3.addView(boutonTactile)
        colonne.addView(c3)

        // 4. Bulle
        val c4 = carte()
        c4.addView(texte("4. Bulle flottante", 17f, gras = true))
        c4.addView(
            texte(
                "La bulle apparaît quand tu quittes l'application, par-dessus les autres applications, pour que ta manette continue de fonctionner. " +
                    "Quand cette application est ouverte, la bulle se cache.\n" +
                    "Rond vert : manette active. Gris : touche la bulle pour reprendre la manette. Rouge : pas connecté au PC.\n" +
                    "Pour la retirer : fais glisser la bulle sur la croix qui apparaît en bas de l'écran, ou touche « Désactiver la bulle », ou ferme l'application depuis la liste des applications récentes.",
                13f, "#8b94a8"
            )
        )
        texteBulle = texte("", 14f)
        c4.addView(texteBulle)
        boutonAutoriser = bouton("Autoriser la bulle") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        c4.addView(boutonAutoriser)
        boutonAfficher = bouton("Activer la bulle") {
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, BulleService::class.java))
            } else {
                majBulle()
            }
        }
        c4.addView(boutonAfficher)
        boutonCacher = bouton("Désactiver la bulle") {
            stopService(Intent(this, BulleService::class.java))
        }
        c4.addView(boutonCacher)
        colonne.addView(c4)

        colonne.addView(
            texte(
                "Aucun compte nécessaire : tout reste entre ton téléphone et ton PC, sur ton Wi-Fi.",
                12f, "#8b94a8"
            )
        )

        val defilement = ScrollView(this)
        defilement.setBackgroundColor(Color.parseColor("#0f1218"))
        defilement.addView(colonne)
        setContentView(defilement)

        // Surveillance des appareils et du son
        val im = getSystemService(Context.INPUT_SERVICE) as InputManager
        im.registerInputDeviceListener(ecouteurAppareils, null)
        casquesConnus = lireCasques()
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.registerAudioDeviceCallback(rappelAudio, null)

        majConnexion()
        majAppareils()
        majBulle()
    }

    override fun onResume() {
        super.onResume()
        Relais.appAuPremierPlan = true
        Relais.notifier()
        Relais.ajouterEcouteur(ecouteurRelais)
        majConnexion()
        majAppareils()
        majBulle()
    }

    override fun onPause() {
        Relais.retirerEcouteur(ecouteurRelais)
        EntreeManette.toutRelacher()
        Relais.appAuPremierPlan = false
        Relais.notifier()
        super.onPause()
    }

    override fun onDestroy() {
        val im = getSystemService(Context.INPUT_SERVICE) as InputManager
        im.unregisterInputDeviceListener(ecouteurAppareils)
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.unregisterAudioDeviceCallback(rappelAudio)
        super.onDestroy()
    }

    // ---------- la manette pendant que l'application est au premier plan ----------
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (EntreeManette.touche(event)) {
            majSignal()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (EntreeManette.mouvement(ev)) {
            majSignal()
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    private fun majSignal() {
        texteSignal.text = EntreeManette.dernier + "  (" + EntreeManette.nbSignaux + " signaux)"
    }

    // ---------- connexion ----------
    private fun actionConnexion() {
        if (Relais.etat == Relais.CONNECTE || Relais.etat == Relais.CONNEXION) {
            Relais.deconnecter()
            return
        }
        val clavier = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        clavier.hideSoftInputFromWindow(champCode.windowToken, 0)

        val code = champCode.text.toString().trim()
        val adresse = champAdresse.text.toString().trim()
        if (champAdresse.visibility == View.VISIBLE && adresse.isNotEmpty()) {
            getSharedPreferences("manette", Context.MODE_PRIVATE)
                .edit().putString("adresse", Relais.nettoyerAdresse(adresse)).apply()
            Relais.connecter(adresse, code)
        } else {
            Relais.connecterAuto(this, code)
        }
    }

    private fun majConnexion() {
        texteEtat.text = Relais.message
        val couleur = when (Relais.etat) {
            Relais.CONNECTE -> "#30c48d"
            Relais.ECHEC -> "#e5484d"
            else -> "#8b94a8"
        }
        texteEtat.setTextColor(Color.parseColor(couleur))
        boutonConnexion.text =
            if (Relais.etat == Relais.CONNECTE || Relais.etat == Relais.CONNEXION) "Se déconnecter" else "Se connecter"

        // Tant que le code n'est pas bon, la bulle et la manette tactile sont bloquées
        val connecte = Relais.etat == Relais.CONNECTE
        for (b in listOf(boutonTactile, boutonAutoriser, boutonAfficher, boutonCacher)) {
            b.isEnabled = connecte
            b.alpha = if (connecte) 1f else 0.4f
        }
        majBulle()
    }

    private fun majBulle() {
        texteBulle.text = if (Relais.etat != Relais.CONNECTE) {
            "Connecte-toi d'abord au PC (étape 1) pour utiliser la bulle."
        } else if (Settings.canDrawOverlays(this)) {
            "Autorisation d'affichage : OUI"
        } else {
            "Autorisation d'affichage : NON (touche « Autoriser la bulle »)"
        }
    }

    // ---------- appareils ----------
    private fun famille(d: InputDevice): String {
        val nom = (d.name ?: "").lowercase()
        val v = d.vendorId
        return if (v == 0x045e || nom.contains("xbox") || nom.contains("x-box")) {
            "Xbox"
        } else if (v == 0x054c || nom.contains("dualshock") || nom.contains("dualsense") ||
            nom.contains("playstation") || nom.contains("wireless controller")
        ) {
            "PlayStation"
        } else if (v == 0x057e || nom.contains("nintendo") || nom.contains("pro controller") || nom.contains("joy-con")) {
            "Nintendo Switch"
        } else if (v == 0x28de || nom.contains("steam")) {
            "Steam"
        } else if (v == 0x2dc8 || nom.contains("8bitdo")) {
            "8BitDo"
        } else if (v == 0x046d || nom.contains("logitech")) {
            "Logitech"
        } else if (v == 0x1532 || nom.contains("razer")) {
            "Razer"
        } else {
            "générique"
        }
    }

    private fun decrireManette(d: InputDevice): String {
        val standard = intArrayOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE
        )
        val nbStandard = d.hasKeys(*standard).count { it }

        val supplementaires = ArrayList<Int>()
        supplementaires.add(KeyEvent.KEYCODE_BUTTON_C)
        supplementaires.add(KeyEvent.KEYCODE_BUTTON_Z)
        for (k in 188..203) supplementaires.add(k)
        val nbPlus = d.hasKeys(*supplementaires.toIntArray()).count { it }

        val axes = HashSet<Int>()
        for (r in d.motionRanges) axes.add(r.axis)
        val gauche = axes.contains(MotionEvent.AXIS_X) && axes.contains(MotionEvent.AXIS_Y)
        val droit = (axes.contains(MotionEvent.AXIS_Z) && axes.contains(MotionEvent.AXIS_RZ)) ||
            (axes.contains(MotionEvent.AXIS_RX) && axes.contains(MotionEvent.AXIS_RY))
        val sticks = (if (gauche) 1 else 0) + (if (droit) 1 else 0)
        val gachettes = axes.contains(MotionEvent.AXIS_LTRIGGER) || axes.contains(MotionEvent.AXIS_BRAKE)

        val sb = StringBuilder()
        sb.append("🎮 Manette ").append(famille(d)).append(" : ").append(d.name ?: "?").append('\n')
        sb.append("   Le PC la verra comme : manette Xbox 360\n")
        sb.append("   Boutons standard : ").append(nbStandard).append("/13 · boutons en plus : ").append(nbPlus)
        sb.append(" · sticks : ").append(sticks)
        sb.append(" · gâchettes analogiques : ").append(if (gachettes) "oui" else "non")
        if (nbPlus > 0) {
            sb.append("\n   Les boutons en plus (palettes, partage...) ne sont pas encore transmis par l'application.")
        }
        return sb.toString()
    }

    private fun majAppareils() {
        val lignes = ArrayList<String>()
        for (id in InputDevice.getDeviceIds()) {
            val d = InputDevice.getDevice(id) ?: continue
            if (d.isVirtual) continue
            val nom = d.name ?: ""
            val bas = nom.lowercase()
            if (bas.contains("touchpad") || bas.contains("motion sensor")) continue
            val s = d.sources
            val manette = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            val souris = (s and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
            val clavier = (s and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD &&
                d.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            if (manette) {
                lignes.add(decrireManette(d))
            } else if (souris) {
                lignes.add("🖱 Souris : $nom\n   Détectée, mais pas encore transmise au PC (seule la manette l'est pour l'instant).")
            } else if (clavier) {
                lignes.add("⌨ Clavier : $nom\n   Détecté, mais pas encore transmis au PC (seule la manette l'est pour l'instant).")
            }
        }
        for (nom in casquesConnus ?: emptySet<String>()) {
            lignes.add("🎧 Casque ou enceinte : $nom\n   Manette Virtuelle ne peut pas transmettre la voix ni le son au PC. Ils restent sur ton téléphone.")
        }
        texteAppareils.text = if (lignes.isEmpty()) {
            "En attente d'un appareil : manette, souris ou clavier.\nRelie ta manette au téléphone en Bluetooth (Paramètres > Bluetooth)."
        } else {
            lignes.joinToString("\n\n")
        }
    }

    // ---------- casques et enceintes ----------
    private fun lireCasques(): Set<String> {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val res = LinkedHashSet<String>()
        for (d in audio.getDevices(AudioManager.GET_DEVICES_ALL)) {
            val t = d.type
            val casque = t == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                t == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                t == AudioDeviceInfo.TYPE_USB_HEADSET ||
                t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                t == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= 31 && t == AudioDeviceInfo.TYPE_BLE_HEADSET)
            if (casque) {
                val nom = d.productName?.toString() ?: ""
                res.add(if (nom.isBlank()) "Casque" else nom)
            }
        }
        return res
    }

    private fun majCasques() {
        val maintenant = lireCasques()
        val avant = casquesConnus
        casquesConnus = maintenant
        if (avant != null) {
            for (nom in maintenant) {
                if (!avant.contains(nom)) {
                    Toast.makeText(
                        this,
                        "« $nom » : Manette Virtuelle ne peut pas transmettre la voix ni le son au PC. Ils restent sur ton téléphone.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        majAppareils()
    }
}
