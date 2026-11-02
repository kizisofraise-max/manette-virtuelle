package fr.manettevirtuelle.bulle

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Traduit les signaux Android de la manette vers l'etat attendu par le PC
 * (indices des boutons = ceux de la page web : 0=A 1=B 2=X 3=Y 4=LB 5=RB
 * 8=Retour 9=Start 10=L3 11=R3 12..15=croix 16=Guide).
 * Partage entre la bulle et l'ecran de l'application.
 */
object EntreeManette {
    private val boutons = BooleanArray(17)
    private var l2 = false
    private var r2 = false
    private var gx = 0f
    private var gy = 0f
    private var dx = 0f
    private var dy = 0f
    private var lt = 0f
    private var rt = 0f
    private var hatX = 0f
    private var hatY = 0f

    @Volatile
    var dernier = "aucun signal"

    @Volatile
    var nbSignaux = 0

    private fun source(src: Int, type: Int): Boolean = (src and type) == type

    fun estManette(src: Int): Boolean =
        source(src, InputDevice.SOURCE_GAMEPAD) ||
            source(src, InputDevice.SOURCE_JOYSTICK) ||
            source(src, InputDevice.SOURCE_DPAD)

    private fun indice(code: Int): Int = when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> 0
        KeyEvent.KEYCODE_BUTTON_B -> 1
        KeyEvent.KEYCODE_BUTTON_X -> 2
        KeyEvent.KEYCODE_BUTTON_Y -> 3
        KeyEvent.KEYCODE_BUTTON_L1 -> 4
        KeyEvent.KEYCODE_BUTTON_R1 -> 5
        KeyEvent.KEYCODE_BUTTON_SELECT -> 8
        KeyEvent.KEYCODE_BUTTON_START -> 9
        KeyEvent.KEYCODE_BUTTON_THUMBL -> 10
        KeyEvent.KEYCODE_BUTTON_THUMBR -> 11
        KeyEvent.KEYCODE_DPAD_UP -> 12
        KeyEvent.KEYCODE_DPAD_DOWN -> 13
        KeyEvent.KEYCODE_DPAD_LEFT -> 14
        KeyEvent.KEYCODE_DPAD_RIGHT -> 15
        KeyEvent.KEYCODE_BUTTON_MODE -> 16
        else -> -1
    }

    private fun zone(v: Float): Float = if (abs(v) < 0.06f) 0f else v

    private fun f(v: Float): String = String.format(Locale.ROOT, "%.3f", v)

    /** Boutons. Renvoie true si l'evenement vient d'une manette (on le garde pour nous). */
    fun touche(e: KeyEvent): Boolean {
        if (!estManette(e.source)) return false
        if (e.repeatCount > 0) return true
        val bas = e.action == KeyEvent.ACTION_DOWN
        val i = indice(e.keyCode)
        if (i >= 0) {
            boutons[i] = bas
        } else if (e.keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            l2 = bas
        } else if (e.keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            r2 = bas
        }
        nbSignaux++
        dernier = KeyEvent.keyCodeToString(e.keyCode).removePrefix("KEYCODE_") +
            (if (bas) " (appuyé)" else " (relâché)")
        envoyerEtat()
        return true
    }

    /** Sticks, gachettes, croix directionnelle. */
    fun mouvement(e: MotionEvent): Boolean {
        if (!source(e.source, InputDevice.SOURCE_JOYSTICK) || e.actionMasked != MotionEvent.ACTION_MOVE) {
            return false
        }
        gx = zone(e.getAxisValue(MotionEvent.AXIS_X))
        gy = zone(e.getAxisValue(MotionEvent.AXIS_Y))
        dx = zone(e.getAxisValue(MotionEvent.AXIS_Z))
        dy = zone(e.getAxisValue(MotionEvent.AXIS_RZ))
        lt = max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE))
        rt = max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER), e.getAxisValue(MotionEvent.AXIS_GAS))
        val hx = e.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
        if (hx != hatX || hy != hatY) {
            hatX = hx
            hatY = hy
            boutons[14] = hx < -0.5f
            boutons[15] = hx > 0.5f
            boutons[12] = hy < -0.5f
            boutons[13] = hy > 0.5f
        }
        nbSignaux++
        dernier = String.format(
            Locale.ROOT, "sticks G %.1f/%.1f · D %.1f/%.1f · L2 %.1f · R2 %.1f",
            gx, gy, dx, dy, lt, rt
        )
        envoyerEtat()
        return true
    }

    /** A appeler quand la manette n'est plus a nous (evite les boutons bloques sur le PC). */
    fun toutRelacher() {
        for (i in boutons.indices) boutons[i] = false
        l2 = false
        r2 = false
        gx = 0f
        gy = 0f
        dx = 0f
        dy = 0f
        lt = 0f
        rt = 0f
        hatX = 0f
        hatY = 0f
        envoyerEtat()
    }

    /** Manette tactile de secours : l'etat de l'ecran remplace l'etat courant. */
    fun depuisTactile(b: BooleanArray, sgx: Float, sgy: Float, sdx: Float, sdy: Float, sl: Float, sr: Float) {
        for (i in boutons.indices) boutons[i] = i < b.size && b[i]
        gx = sgx
        gy = sgy
        dx = sdx
        dy = sdy
        lt = sl
        rt = sr
        l2 = false
        r2 = false
        nbSignaux++
        dernier = "manette tactile"
        envoyerEtat()
    }

    private fun envoyerEtat() {
        val sb = StringBuilder("{\"b\":[")
        for (i in 0..16) {
            if (i > 0) sb.append(',')
            sb.append(if (boutons[i]) '1' else '0')
        }
        sb.append("],\"a\":[")
        sb.append(f(gx)).append(',').append(f(gy)).append(',').append(f(dx)).append(',').append(f(dy))
        sb.append("],\"lt\":").append(f(max(lt, if (l2) 1f else 0f)))
        sb.append(",\"rt\":").append(f(max(rt, if (r2) 1f else 0f)))
        sb.append('}')
        Relais.envoyer(sb.toString())
    }
}
