package fr.manettevirtuelle.bulle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Manette de secours sur l'ecran du telephone : sticks, croix, A/B/X/Y, bumpers, gachettes, Retour/Start. */
class ManetteTactileView(contexte: Context) : View(contexte) {

    private class Touche(val nom: String, val indice: Int, val rond: Boolean, val couleur: Int) {
        var cx = 0f
        var cy = 0f
        var r = 0f
        val rect = RectF()
        var bas = false

        fun contient(x: Float, y: Float): Boolean =
            if (rond) hypot(x - cx, y - cy) <= r * 1.25f else rect.contains(x, y)
    }

    private val gris = Color.parseColor("#2b3140")
    private val touches = listOf(
        Touche("A", 0, true, Color.parseColor("#35c24a")),
        Touche("B", 1, true, Color.parseColor("#e5484d")),
        Touche("X", 2, true, Color.parseColor("#35a7ff")),
        Touche("Y", 3, true, Color.parseColor("#f5c542")),
        Touche("LB", 4, false, gris),
        Touche("RB", 5, false, gris),
        Touche("LT", 100, false, gris),
        Touche("RT", 101, false, gris),
        Touche("RETOUR", 8, true, gris),
        Touche("START", 9, true, gris)
    )

    private val tagGauche = "stick-gauche"
    private val tagDroit = "stick-droit"
    private val tagCroix = "croix"
    private val actifs = HashMap<Int, Any>()

    private var gcx = 0f
    private var gcy = 0f
    private var gr = 0f
    private var rcx = 0f
    private var rcy = 0f
    private var rr = 0f
    private var ccx = 0f
    private var ccy = 0f
    private var cr = 0f

    private var gsx = 0f
    private var gsy = 0f
    private var dsx = 0f
    private var dsy = 0f
    private var dHaut = false
    private var dBas = false
    private var dGauche = false
    private var dDroite = false

    private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
    private val texte = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ecouteur: () -> Unit = { invalidate() }

    init {
        texte.textAlign = Paint.Align.CENTER
        texte.color = Color.WHITE
        texte.isFakeBoldText = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Relais.ajouterEcouteur(ecouteur)
    }

    override fun onDetachedFromWindow() {
        Relais.retirerEcouteur(ecouteur)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val largeur = w.toFloat()
        val hauteur = h.toFloat()
        texte.textSize = 0.07f * hauteur

        gcx = 0.17f * largeur
        gcy = 0.50f * hauteur
        gr = 0.18f * hauteur
        ccx = 0.33f * largeur
        ccy = 0.82f * hauteur
        cr = 0.13f * hauteur
        rcx = 0.67f * largeur
        rcy = 0.80f * hauteur
        rr = 0.17f * hauteur

        val acx = 0.83f * largeur
        val acy = 0.52f * hauteur
        val ecart = 0.16f * hauteur
        val rb = 0.10f * hauteur
        placerRond("A", acx, acy + ecart, rb)
        placerRond("B", acx + ecart, acy, rb)
        placerRond("X", acx - ecart, acy, rb)
        placerRond("Y", acx, acy - ecart, rb)
        placerRond("RETOUR", 0.43f * largeur, 0.10f * hauteur, 0.06f * hauteur)
        placerRond("START", 0.57f * largeur, 0.10f * hauteur, 0.06f * hauteur)
        placerRect("LT", 0.03f * largeur, 0.03f * hauteur, 0.19f * largeur, 0.17f * hauteur)
        placerRect("LB", 0.03f * largeur, 0.19f * hauteur, 0.19f * largeur, 0.33f * hauteur)
        placerRect("RT", 0.81f * largeur, 0.03f * hauteur, 0.97f * largeur, 0.17f * hauteur)
        placerRect("RB", 0.81f * largeur, 0.19f * hauteur, 0.97f * largeur, 0.33f * hauteur)
    }

    private fun placerRond(nom: String, x: Float, y: Float, r: Float) {
        for (t in touches) {
            if (t.nom == nom) {
                t.cx = x
                t.cy = y
                t.r = r
            }
        }
    }

    private fun placerRect(nom: String, g: Float, h: Float, d: Float, b: Float) {
        for (t in touches) {
            if (t.nom == nom) t.rect.set(g, h, d, b)
        }
    }

    // ---------- dessin ----------
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0f1218"))

        // Etat de la connexion
        val connecte = Relais.etat == Relais.CONNECTE
        texte.color = Color.parseColor(if (connecte) "#30c48d" else "#e5484d")
        texte.textSize = 0.05f * height
        canvas.drawText(
            if (connecte) "Connecté au PC" else "Pas connecté : retourne au menu et tape le code à 4 chiffres",
            width / 2f, 0.30f * height, texte
        )
        texte.textSize = 0.07f * height
        texte.color = Color.WHITE

        dessinerStick(canvas, gcx, gcy, gr, gsx, gsy)
        dessinerStick(canvas, rcx, rcy, rr, dsx, dsy)
        dessinerCroix(canvas)

        for (t in touches) {
            pinceau.style = Paint.Style.FILL
            pinceau.color = t.couleur
            pinceau.alpha = if (t.bas) 255 else 170
            if (t.rond) {
                canvas.drawCircle(t.cx, t.cy, t.r, pinceau)
                texte.textSize = (if (t.nom.length > 2) 0.035f else 0.07f) * height
                canvas.drawText(t.nom, t.cx, t.cy - (texte.ascent() + texte.descent()) / 2, texte)
            } else {
                canvas.drawRoundRect(t.rect, 24f, 24f, pinceau)
                texte.textSize = 0.06f * height
                val cy = t.rect.centerY()
                canvas.drawText(t.nom, t.rect.centerX(), cy - (texte.ascent() + texte.descent()) / 2, texte)
            }
        }
        pinceau.alpha = 255
    }

    private fun dessinerStick(canvas: Canvas, cx: Float, cy: Float, r: Float, vx: Float, vy: Float) {
        pinceau.style = Paint.Style.FILL
        pinceau.color = Color.parseColor("#181c25")
        canvas.drawCircle(cx, cy, r, pinceau)
        pinceau.style = Paint.Style.STROKE
        pinceau.strokeWidth = 4f
        pinceau.color = Color.parseColor("#2d3342")
        canvas.drawCircle(cx, cy, r, pinceau)
        pinceau.style = Paint.Style.FILL
        pinceau.color = if (vx != 0f || vy != 0f) Color.parseColor("#35c2ff") else Color.parseColor("#3a4254")
        canvas.drawCircle(cx + vx * r * 0.75f, cy + vy * r * 0.75f, r * 0.45f, pinceau)
    }

    private fun dessinerCroix(canvas: Canvas) {
        val t = cr * 0.33f
        pinceau.style = Paint.Style.FILL
        pinceau.color = if (dHaut) Color.parseColor("#35c2ff") else gris
        canvas.drawRoundRect(RectF(ccx - t, ccy - cr, ccx + t, ccy), 12f, 12f, pinceau)
        pinceau.color = if (dBas) Color.parseColor("#35c2ff") else gris
        canvas.drawRoundRect(RectF(ccx - t, ccy, ccx + t, ccy + cr), 12f, 12f, pinceau)
        pinceau.color = if (dGauche) Color.parseColor("#35c2ff") else gris
        canvas.drawRoundRect(RectF(ccx - cr, ccy - t, ccx, ccy + t), 12f, 12f, pinceau)
        pinceau.color = if (dDroite) Color.parseColor("#35c2ff") else gris
        canvas.drawRoundRect(RectF(ccx, ccy - t, ccx + cr, ccy + t), 12f, 12f, pinceau)
    }

    // ---------- doigts ----------
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                debut(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    deplace(e.getPointerId(i), e.getX(i), e.getY(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                fin(e.getPointerId(e.actionIndex))
            }
            MotionEvent.ACTION_CANCEL -> {
                relacher()
            }
        }
        envoyer()
        invalidate()
        return true
    }

    private fun debut(id: Int, x: Float, y: Float) {
        if (hypot(x - gcx, y - gcy) <= gr * 1.3f) {
            actifs[id] = tagGauche
            deplace(id, x, y)
            return
        }
        if (hypot(x - rcx, y - rcy) <= rr * 1.3f) {
            actifs[id] = tagDroit
            deplace(id, x, y)
            return
        }
        if (hypot(x - ccx, y - ccy) <= cr * 1.25f) {
            actifs[id] = tagCroix
            deplace(id, x, y)
            return
        }
        for (t in touches) {
            if (t.contient(x, y)) {
                t.bas = true
                actifs[id] = t
                return
            }
        }
    }

    private fun position(x: Float, y: Float, cx: Float, cy: Float, r: Float): FloatArray {
        val m = r * 0.75f
        var vx = (x - cx) / m
        var vy = (y - cy) / m
        val n = hypot(vx, vy)
        if (n > 1f) {
            vx /= n
            vy /= n
        }
        return floatArrayOf(vx, vy)
    }

    private fun deplace(id: Int, x: Float, y: Float) {
        val cible = actifs[id] ?: return
        if (cible == tagGauche) {
            val v = position(x, y, gcx, gcy, gr)
            gsx = v[0]
            gsy = v[1]
        } else if (cible == tagDroit) {
            val v = position(x, y, rcx, rcy, rr)
            dsx = v[0]
            dsy = v[1]
        } else if (cible == tagCroix) {
            dHaut = y - ccy < -cr * 0.35f
            dBas = y - ccy > cr * 0.35f
            dGauche = x - ccx < -cr * 0.35f
            dDroite = x - ccx > cr * 0.35f
        }
    }

    private fun fin(id: Int) {
        val cible = actifs.remove(id) ?: return
        if (cible == tagGauche) {
            gsx = 0f
            gsy = 0f
        } else if (cible == tagDroit) {
            dsx = 0f
            dsy = 0f
        } else if (cible == tagCroix) {
            dHaut = false
            dBas = false
            dGauche = false
            dDroite = false
        } else if (cible is Touche) {
            cible.bas = false
        }
    }

    /** Lache tout (changement d'ecran, annulation) et previent le PC. */
    fun relacher() {
        actifs.clear()
        for (t in touches) t.bas = false
        gsx = 0f
        gsy = 0f
        dsx = 0f
        dsy = 0f
        dHaut = false
        dBas = false
        dGauche = false
        dDroite = false
        envoyer()
        invalidate()
    }

    private fun envoyer() {
        val b = BooleanArray(17)
        var lt = 0f
        var rt = 0f
        for (t in touches) {
            if (!t.bas) continue
            if (t.indice == 100) {
                lt = 1f
            } else if (t.indice == 101) {
                rt = 1f
            } else if (t.indice < 17) {
                b[t.indice] = true
            }
        }
        b[12] = dHaut
        b[13] = dBas
        b[14] = dGauche
        b[15] = dDroite
        EntreeManette.depuisTactile(b, gsx, gsy, dsx, dsy, lt, rt)
    }
}
