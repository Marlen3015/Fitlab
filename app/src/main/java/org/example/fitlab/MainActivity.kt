package org.example.fitlab

import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import org.example.fitlab.logica.RiesgoDifuso

/** Menú principal: resumen de la última semana + un acceso por módulo. */
class MainActivity : PantallaBase() {
    override val nombre get() = "Fitlab"
    override val subtitulo get() = "Análisis deportivo para atletas"
    override val mostrarAtras get() = false

    private lateinit var mSemana: MetricaView
    private lateinit var mVo2: MetricaView
    private lateinit var mFc: MetricaView
    private lateinit var riesgoTxt: TextView
    private lateinit var barraRiesgo: Medidor

    override fun construir() {
        tarjeta("Resumen de la última semana") {
            val m = filaMetricas("Semana", "VO2 máx", "FC reposo")
            mSemana = m[0]; mVo2 = m[1]; mFc = m[2]
            riesgoTxt = nota("")
            barraRiesgo = medidor()
        }

        TextView(this).apply {
            text = "MÓDULOS"; textSize = 12f; setTextColor(Tema.TEXTO2)
            setPadding(dp(4), dp(20), 0, dp(2))
            contenido.addView(this)
        }
        item("1", "Captura de variables", "Registra FC, peso, sueño, carga y más", Modulo1Activity::class.java)
        item("2", "Tendencia", "Mínimos cuadrados y R²", Modulo2Activity::class.java)
        item("3", "Predicción", "Extrapolación de Newton a 1-2 semanas", Modulo3Activity::class.java)
        item("4", "Optimización del sueño", "Newton-Raphson: horas necesarias", Modulo4Activity::class.java)
        item("5", "Carga de la sesión", "TRIMP con Regla de Simpson", Modulo5Activity::class.java)
        item("6", "Riesgo de sobreentrenamiento", "Lógica difusa", DifusaActivity::class.java)
        item("7", "Agente inteligente", "Red neuronal, búsqueda y personalización", IAActivity::class.java)
    }

    override fun onResume() {
        super.onResume()
        val u = Repositorio.historial.ultimo()
        if (u == null) {
            mSemana.set("—"); mVo2.set("—"); mFc.set("—")
            riesgoTxt.text = "Aún no hay datos. Entra a 1 · Captura de variables."
            riesgoTxt.setTextColor(Tema.TEXTO2)
            barraRiesgo.set(0.0)
        } else {
            val r = RiesgoDifuso.evaluar(u.carga, u.suenoHoras, Repositorio.deltaFc())
            mSemana.set(u.semana.toString())
            mVo2.set(f(u.vo2Max, 1))
            mFc.set(u.fcReposo.toInt().toString())
            riesgoTxt.text = "Riesgo de sobreentrenamiento: ${f(r, 0)}/100 · ${RiesgoDifuso.etiqueta(r)}"
            riesgoTxt.setTextColor(Tema.colorRiesgo(r))
            barraRiesgo.set(r)
        }
    }

    private fun item(num: String, titulo: String, desc: String, clase: Class<*>) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = redondo(Tema.TARJETA, 16, Tema.BORDE)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) }
            isClickable = true
            setOnClickListener { startActivity(Intent(this@MainActivity, clase)) }
        }
        fila.addView(TextView(this).apply {
            text = num; textSize = 16f; gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD); setTextColor(Tema.FONDO)
            background = circulo(Tema.ACENTO)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(14) }
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = titulo; textSize = 16f
            setTypeface(typeface, Typeface.BOLD); setTextColor(Tema.TEXTO)
        })
        col.addView(TextView(this).apply { text = desc; textSize = 13f; setTextColor(Tema.TEXTO2) })
        fila.addView(col)
        fila.addView(TextView(this).apply { text = "›"; textSize = 24f; setTextColor(Tema.TEXTO2) })
        contenido.addView(fila)
    }
}