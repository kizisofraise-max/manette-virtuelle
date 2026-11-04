package fr.manettevirtuelle.bulle

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * La bulle : un rond avec le logo.
 *  - rond VERT  : la bulle recoit la manette (et la relaie au PC)
 *  - rond GRIS  : tu as touche une autre application, touche la bulle pour reprendre la manette
 *  - rond ROUGE : pas connecte au PC
 * Quand l'application Manette Virtuelle est ouverte, la bulle se cache et ne prend plus le
 * focus (sinon le clavier ne pourrait pas s'ouvrir dans l'application).
 */
class BulleView(contexte: Context) : ImageView(contexte) {

    var params: WindowManager.LayoutParams? = null
    var gestionnaire: WindowManager? = null

    private var focus = false
    private var departX = 0
    private var departY = 0
    private var toucheX = 0f
    private var toucheY = 0f
    private var deplace = false
    private var surCroix = false
    private var croix: CroixView? = null

    private val appliquer = Runnable { majFenetre() }
    private val ecouteur: () -> Unit = {
        majAffichage()
        removeCallbacks(appliquer)
        if (Relais.appAuPremierPlan) {
            majFenetre()
        } else {
            postDelayed(appliquer, 300)
        }
    }

    init {
        val d = resources.displayMetrics.density
        setImageResource(R.drawable.ic_manette)
        setScaleType(ImageView.ScaleType.FIT_CENTER)
        val marge = (7 * d).toInt()
        setPadding(marge, marge, marge, marge)
        setFocusable(true)
        setFocusableInTouchMode(true)
        setOutlineProvider(ViewOutlineProvider.BACKGROUND)
        setClipToOutline(true)
        majAffichage()
    }

    private fun majAffichage() {
        val d = resources.displayMetrics.density
        // La couleur du rond change avec l'etat, la manette blanche reste devant
        val couleur = if (Relais.etat != Relais.CONNECTE) {
            "#e5484d"
        } else if (focus) {
            "#1fa971"
        } else {
            "#6b7488"
        }
        val fond = GradientDrawable()
        fond.shape = GradientDrawable.OVAL
        fond.setColor(Color.parseColor(couleur))
        fond.setStroke((2 * d).toInt(), Color.parseColor("#59000000"))
        setBackground(fond)
        setAlpha(if (focus) 1f else 0.9f)
    }

    /** Application ouverte : bulle invisible, non touchable, sans focus. Sinon : bulle normale. */
    private fun majFenetre() {
        val p = params ?: return
        val wm = gestionnaire ?: return
        val ouvert = Relais.appAuPremierPlan
        val masque = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val nouveau = if (ouvert) (p.flags or masque) else (p.flags and masque.inv())
        if (nouveau != p.flags) {
            p.flags = nouveau
            try {
                wm.updateViewLayout(this, p)
            } catch (e: Exception) {
            }
        }
        setVisibility(if (ouvert) View.GONE else View.VISIBLE)
        if (!ouvert) requestFocus()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Relais.ajouterEcouteur(ecouteur)
        requestFocus()
        post { majFenetre() }
    }

    override fun onDetachedFromWindow() {
        Relais.retirerEcouteur(ecouteur)
        removeCallbacks(appliquer)
        cacherCroix()
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

    // Doigt sur la bulle : on la deplace. Lachee sur la croix, elle se ferme.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val p = params ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                departX = p.x
                departY = p.y
                toucheX = event.rawX
                toucheY = event.rawY
                deplace = false
                surCroix = false
            }
            MotionEvent.ACTION_MOVE -> {
                val seuil = 8 * resources.displayMetrics.density
                if (!deplace && (abs(event.rawX - toucheX) > seuil || abs(event.rawY - toucheY) > seuil)) {
                    deplace = true
                    montrerCroix()
                }
                if (deplace) {
                    p.x = departX + (event.rawX - toucheX).toInt()
                    p.y = departY + (event.rawY - toucheY).toInt()
                    gestionnaire?.updateViewLayout(this, p)
                    surCroix = pointeSurCroix(event.rawX, event.rawY)
                    croix?.actif = surCroix
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val fermer = deplace && surCroix && event.actionMasked == MotionEvent.ACTION_UP
                cacherCroix()
                deplace = false
                surCroix = false
                if (fermer) {
                    context.stopService(Intent(context, BulleService::class.java))
                }
            }
        }
        return true
    }

    private fun montrerCroix() {
        val wm = gestionnaire ?: return
        if (croix != null) return
        val d = resources.displayMetrics.density
        val taille = (64 * d).toInt()
        val c = CroixView(context)
        val p = WindowManager.LayoutParams(
            taille,
            taille,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        p.y = (56 * d).toInt()
        try {
            wm.addView(c, p)
            croix = c
        } catch (e: Exception) {
        }
    }

    private fun cacherCroix() {
        val c = croix ?: return
        croix = null
        try {
            gestionnaire?.removeView(c)
        } catch (e: Exception) {
        }
    }

    private fun pointeSurCroix(x: Float, y: Float): Boolean {
        val c = croix ?: return false
        if (c.width == 0 || !c.isAttachedToWindow) return false
        val pos = IntArray(2)
        c.getLocationOnScreen(pos)
        val cx = pos[0] + c.width / 2f
        val cy = pos[1] + c.height / 2f
        val rayon = 70 * resources.displayMetrics.density
        return hypot(x - cx, y - cy) <= rayon
    }
}
