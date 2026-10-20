package org.example.fitlab

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import java.util.Locale

private fun num(x: Double): String =
    if (x == Math.rint(x) && Math.abs(x) < 1e6) String.format(Locale.US, "%.0f", x)
    else String.format(Locale.US, "%.1f", x)

/** Gráfica de línea hecha a mano con Canvas (sin librerías): datos, recta de tendencia y puntos futuros. */
class GraficaLinea(context: Context) : View(context) {
    private var xs: List<Double> = emptyList()
    private var ys: List<Double> = emptyList()
    private var recta: Pair<Double, Double>? = null
    private var futuros: List<Pair<Double, Double>> = emptyList()
    private var colorSerie = Tema.ACENTO

    private val d = resources.displayMetrics.density
    private val pLinea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f * d
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val pPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pRecta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f * d; color = Tema.TEXTO2
        pathEffect = DashPathEffect(floatArrayOf(10f * d, 8f * d), 0f)
    }
    private val pFuturo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.5f * d; color = Tema.ACENTO2
        pathEffect = DashPathEffect(floatArrayOf(8f * d, 6f * d), 0f)
    }
    private val pRejilla = Paint().apply { color = Tema.BORDE; strokeWidth = 1f * d }
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Tema.TEXTO2; textSize = 11f * d }
    private val pTextoAzul = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Tema.ACENTO2; textSize = 12f * d; textAlign = Paint.Align.CENTER
    }

    fun datos(
        x: List<Double>, y: List<Double>,
        recta: Pair<Double, Double>? = null,
        futuros: List<Pair<Double, Double>> = emptyList(),
        color: Int = Tema.ACENTO
    ) {
        xs = x; ys = y; this.recta = recta; this.futuros = futuros; colorSerie = color
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val izq = 44f * d; val der = 14f * d; val arr = 18f * d; val aba = 26f * d
        if (xs.size < 2) {
            pTexto.textAlign = Paint.Align.CENTER
            c.drawText("Sin datos suficientes", w / 2, h / 2, pTexto)
            return
        }
        val todosX = xs + futuros.map { it.first }
        val extraY = recta?.let { r -> listOf(r.first * xs.first() + r.second, r.first * (todosX.maxOrNull() ?: 0.0) + r.second) }
            ?: emptyList()
        val todosY = ys + futuros.map { it.second } + extraY

        val minX = todosX.minOrNull() ?: 0.0
        val maxX = (todosX.maxOrNull() ?: 1.0).let { if (it == minX) minX + 1 else it }
        val rawMin = todosY.minOrNull() ?: 0.0
        val rawMax = todosY.maxOrNull() ?: 1.0
        val margen = (if (rawMax - rawMin == 0.0) 1.0 else rawMax - rawMin) * 0.12
        val y0 = rawMin - margen
        val y1 = rawMax + margen

        fun px(x: Double) = (izq + (x - minX) / (maxX - minX) * (w - izq - der)).toFloat()
        fun py(y: Double) = (arr + (1 - (y - y0) / (y1 - y0)) * (h - arr - aba)).toFloat()

        // rejilla horizontal con valores
        pTexto.textAlign = Paint.Align.RIGHT
        for (i in 0..3) {
            val v = y0 + (y1 - y0) * i / 3
            c.drawLine(izq, py(v), w - der, py(v), pRejilla)
            val txt = if (y1 - y0 >= 10) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)
            c.drawText(txt, izq - 6f * d, py(v) + 4f * d, pTexto)
        }
        // etiquetas del eje x
        pTexto.textAlign = Paint.Align.CENTER
        val etiquetasX = todosX.distinct()
        val paso = if (etiquetasX.size > 8) 2 else 1
        etiquetasX.forEachIndexed { i, x -> if (i % paso == 0) c.drawText(num(x), px(x), h - 6f * d, pTexto) }

        // recta de tendencia
        recta?.let { (m, b) ->
            val p = Path()
            p.moveTo(px(minX), py(m * minX + b))
            p.lineTo(px(maxX), py(m * maxX + b))
            c.drawPath(p, pRecta)
        }
        // serie real
        val serie = Path()
        xs.indices.forEach { i ->
            if (i == 0) serie.moveTo(px(xs[i]), py(ys[i])) else serie.lineTo(px(xs[i]), py(ys[i]))
        }
        pLinea.color = colorSerie
        c.drawPath(serie, pLinea)
        pPunto.color = colorSerie
        xs.indices.forEach { i -> c.drawCircle(px(xs[i]), py(ys[i]), 4f * d, pPunto) }

        // puntos futuros (predicción)
        if (futuros.isNotEmpty()) {
            val fut = Path()
            fut.moveTo(px(xs.last()), py(ys.last()))
            futuros.forEach { (x, y) -> fut.lineTo(px(x), py(y)) }
            c.drawPath(fut, pFuturo)
            pPunto.color = Tema.ACENTO2
            futuros.forEach { (x, y) ->
                c.drawCircle(px(x), py(y), 5.5f * d, pPunto)
                c.drawText(String.format(Locale.US, "%.1f", y), px(x), py(y) - 10f * d, pTextoAzul)
            }
        }
    }
}

/** Barra de riesgo 0-100 con degradado verde→ámbar→rojo y una marca en el valor. */
class Medidor(context: Context) : View(context) {
    private var valor = 0.0
    private val d = resources.displayMetrics.density
    private val pBarra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pMarca = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Tema.TEXTO2; textSize = 11f * d }

    fun set(v: Double) { valor = v.coerceIn(0.0, 100.0); invalidate() }

    override fun onDraw(c: Canvas) {
        val x0 = 12f * d; val x1 = width - 12f * d
        val top = 14f * d; val alto = 12f * d
        pBarra.shader = LinearGradient(
            x0, 0f, x1, 0f,
            intArrayOf(Tema.ACENTO, Tema.AMBAR, Tema.ROJO),
            floatArrayOf(0.2f, 0.55f, 0.9f), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(RectF(x0, top, x1, top + alto), alto / 2, alto / 2, pBarra)
        val cx = x0 + (x1 - x0) * (valor / 100.0).toFloat()
        pMarca.color = Tema.TEXTO
        c.drawCircle(cx, top + alto / 2, 11f * d, pMarca)
        pMarca.color = Tema.colorRiesgo(valor)
        c.drawCircle(cx, top + alto / 2, 6.5f * d, pMarca)
        val yTxt = top + alto + 24f * d
        pTexto.textAlign = Paint.Align.LEFT;   c.drawText("Bajo", x0, yTxt, pTexto)
        pTexto.textAlign = Paint.Align.CENTER; c.drawText("Medio", (x0 + x1) / 2, yTxt, pTexto)
        pTexto.textAlign = Paint.Align.RIGHT;  c.drawText("Alto", x1, yTxt, pTexto)
    }
}

/** Barra horizontal simple para mostrar una probabilidad (0-1). */
class BarraView(context: Context) : View(context) {
    private var fraccion = 0.0
    private var color = Tema.ACENTO
    private val pFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Tema.BORDE }
    private val pRelleno = Paint(Paint.ANTI_ALIAS_FLAG)

    fun set(f: Double, c: Int) { fraccion = f.coerceIn(0.0, 1.0); color = c; invalidate() }

    override fun onDraw(c: Canvas) {
        val h = height.toFloat(); val w = width.toFloat()
        c.drawRoundRect(RectF(0f, 0f, w, h), h / 2, h / 2, pFondo)
        if (fraccion > 0) {
            pRelleno.color = color
            c.drawRoundRect(RectF(0f, 0f, maxOf(h, w * fraccion.toFloat()), h), h / 2, h / 2, pRelleno)
        }
    }
}