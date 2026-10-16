package dev.controlparental.child.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Unidades dp y contraste explícito: independiente del tema heredado del teléfono. */
internal object EstiloLocal {
    val fondo = Color.rgb(244, 247, 252)
    val tinta = Color.rgb(23, 36, 58)
    val primario = Color.rgb(31, 75, 153)
    fun dp(c: Context, n: Int) = (c.resources.displayMetrics.density * n).toInt()
    fun texto(v: TextView, titulo: Boolean = false) {
        v.setTextColor(tinta); v.textSize = if (titulo) 28f else 17f
        v.setLineSpacing(dp(v.context, 3).toFloat(), 1f)
        if (titulo) v.setTypeface(null, Typeface.BOLD)
        v.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(v.context, if (titulo) 20 else 12)
        }
    }
    fun boton(v: Button) {
        v.isAllCaps = false; v.textSize = 17f; v.minHeight = dp(v.context, 56)
        v.setPadding(dp(v.context, 16), dp(v.context, 12), dp(v.context, 16), dp(v.context, 12))
        v.setTextColor(if (v.isEnabled) Color.WHITE else Color.rgb(76, 86, 105))
        v.background = GradientDrawable().apply {
            setColor(if (v.isEnabled) primario else Color.rgb(220, 226, 237))
            cornerRadius = dp(v.context, 16).toFloat()
        }
        v.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(v.context, 12) }
    }
}
