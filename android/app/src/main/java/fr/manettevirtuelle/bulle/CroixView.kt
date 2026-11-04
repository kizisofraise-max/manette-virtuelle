package fr.manettevirtuelle.bulle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/** La croix qui apparait en bas de l'ecran quand on deplace la bulle : on y lache la bulle pour la fermer. */
class CroixView(contexte: Context) : View(contexte) {

    var actif = false
        set(valeur) {
            if (field != valeur) {
                field = valeur
                setScaleX(if (valeur) 1.3f else 1f)
                setScaleY(if (valeur) 1.3f else 1f)
                invalidate()
            }
        }

    private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 3f

        pinceau.style = Paint.Style.FILL
        pinceau.color = if (actif) Color.parseColor("#e5484d") else Color.parseColor("#d9181c25")
        canvas.drawCircle(cx, cy, r, pinceau)

        pinceau.style = Paint.Style.STROKE
        pinceau.strokeWidth = 3f
        pinceau.color = Color.parseColor("#cceeeeee")
        canvas.drawCircle(cx, cy, r, pinceau)

        val d = r * 0.35f
        pinceau.strokeWidth = r * 0.14f
        pinceau.strokeCap = Paint.Cap.ROUND
        pinceau.color = Color.WHITE
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, pinceau)
        canvas.drawLine(cx - d, cy + d, cx + d, cy - d, pinceau)
    }
}
