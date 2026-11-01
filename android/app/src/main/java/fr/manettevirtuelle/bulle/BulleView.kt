package fr.manettevirtuelle.bulle

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale

/**
 * La bulle : une petite fenetre flottante.
 * - Verte quand elle a le "focus" (c'est elle qui recoit la manette).
 * - Grise quand tu as touche une autre application. Retouche-la pour reprendre la manette.
 * - Le doigt (ecran tactile) et la manette sont distingues par la source de l'evenement.
 */
class BulleView(contexte: Context) : TextView(contexte) {

    var params: WindowManager.LayoutParams? = null
    var gestionnaire: WindowManager? = null

    private var actif = false
    private var nbSignaux = 0
    private var appareil = "-"
    private var dernier = "aucun signal"

    private var departX = 0
    private var departY = 0
    private var toucheX = 0f
    private var toucheY = 0f

    init {
        val d = resources.displayMetrics.density
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(Color.WHITE)
        setTypeface(Typeface.MONOSPACE)
        setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
        setWidth((230 * d).toInt())
        setFocusable(true)
        setFocusableInTouchMode(true)
        majAffichage()
    }

    private fun majAffichage() {
        val d = resources.displayMetrics.density
        val fond = GradientDrawable()
        fond.cornerRadius = 18 * d
        fond.setColor(Color.parseColor(if (actif) "#1f9d6b" else "#555b6b"))
        setBackground(fond)
        val statut = if (actif) "MANETTE ACTIVE" else "TOUCHE-MOI (manette en pause)"
        setText(statut + "\n" + dernier + "\n" + nbSignaux + " signaux | " + appareil)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        actif = hasWindowFocus
        if (hasWindowFocus) requestFocus()
        majAffichage()
    }

    // Boutons de la manette
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val src = event.source
        val manette = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (src and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD ||
            (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (!manette) return super.dispatchKeyEvent(event)

        if (event.repeatCount == 0) {
            nbSignaux++
            appareil = event.device?.name ?: "?"
            val nom = KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
            dernier = nom + if (event.action == KeyEvent.ACTION_DOWN) " (appuye)" else " (relache)"
            majAffichage()
        }
        return true
    }

    // Sticks et gachettes de la manette
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val src = event.source
        val joystick = (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (joystick && event.action == MotionEvent.ACTION_MOVE) {
            nbSignaux++
            appareil = event.device?.name ?: "?"
            dernier = String.format(
                Locale.ROOT,
                "G %.1f/%.1f  D %.1f/%.1f\nL2 %.1f  R2 %.1f",
                event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y),
                event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ),
                event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            )
            majAffichage()
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    // Doigt sur la bulle : on la deplace
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val p = params ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                departX = p.x
                departY = p.y
                toucheX = event.rawX
                toucheY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                p.x = departX + (event.rawX - toucheX).toInt()
                p.y = departY + (event.rawY - toucheY).toInt()
                gestionnaire?.updateViewLayout(this, p)
            }
        }
        return true
    }
}
