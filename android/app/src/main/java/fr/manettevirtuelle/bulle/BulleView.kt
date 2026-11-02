package fr.manettevirtuelle.bulle

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView

/**
 * La bulle : un rond avec le logo.
 *  - anneau VERT  : la bulle recoit la manette (et la relaie au PC)
 *  - anneau GRIS  : tu as touche une autre application, touche la bulle pour reprendre la manette
 *  - anneau ROUGE : pas connecte au PC
 * Le doigt et la manette sont distingues par la source de l'evenement.
 */
class BulleView(contexte: Context) : ImageView(contexte) {

    var params: WindowManager.LayoutParams? = null
    var gestionnaire: WindowManager? = null

    private var focus = false
    private var departX = 0
    private var departY = 0
    private var toucheX = 0f
    private var toucheY = 0f

    private val ecouteur: () -> Unit = { majAffichage() }

    init {
        val d = resources.displayMetrics.density
        setImageResource(R.mipmap.ic_launcher)
        setScaleType(ImageView.ScaleType.FIT_CENTER)
        val marge = (4 * d).toInt()
        setPadding(marge, marge, marge, marge)
        setFocusable(true)
        setFocusableInTouchMode(true)
        setOutlineProvider(ViewOutlineProvider.BACKGROUND)
        setClipToOutline(true)
        majAffichage()
    }

    private fun majAffichage() {
        val d = resources.displayMetrics.density
        val couleur = if (Relais.etat != Relais.CONNECTE) {
            "#e5484d"
        } else if (focus) {
            "#30c48d"
        } else {
            "#8b94a8"
        }
        val fond = GradientDrawable()
        fond.shape = GradientDrawable.OVAL
        fond.setColor(Color.parseColor("#181c25"))
        fond.setStroke((4 * d).toInt(), Color.parseColor(couleur))
        setBackground(fond)
        setAlpha(if (focus) 1f else 0.85f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Relais.ajouterEcouteur(ecouteur)
        requestFocus()
    }

    override fun onDetachedFromWindow() {
        Relais.retirerEcouteur(ecouteur)
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        focus = hasWindowFocus
        if (hasWindowFocus) {
            requestFocus()
        } else {
            EntreeManette.toutRelacher()
        }
        majAffichage()
    }

    // Boutons de la manette
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (EntreeManette.touche(event)) return true
        return super.dispatchKeyEvent(event)
    }

    // Sticks et gachettes de la manette
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (EntreeManette.mouvement(event)) return true
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
