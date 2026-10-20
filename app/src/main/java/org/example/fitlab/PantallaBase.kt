package org.example.fitlab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.EdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

enum class Tipo { DECIMAL, ENTERO, TEXTO }

/** Número grande con su etiqueta debajo (ej. "0.47" / "Pendiente"). */
class MetricaView(context: Context, etiqueta: String) : LinearLayout(context) {
    private val valor = TextView(context)

    init {
        orientation = LinearLayout.VERTICAL
        valor.apply {
            text = "—"; textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Tema.TEXTO)
        }
        val et = TextView(context).apply { text = etiqueta; textSize = 12f; setTextColor(Tema.TEXTO2) }
        addView(valor); addView(et)
    }

    fun set(v: String, color: Int = Tema.TEXTO) { valor.text = v; valor.setTextColor(color) }
}

/**
 * Base de todas las pantallas: fondo oscuro, encabezado con botón de regreso y ayudas
 * para armar tarjetas, campos, botones, métricas y gráficas sin escribir XML.
 */
abstract class PantallaBase : AppCompatActivity() {
    protected lateinit var contenido: LinearLayout
    protected lateinit var destino: LinearLayout       // dónde se agregan los elementos (la página o una tarjeta)
    abstract val nombre: String
    open val subtitulo: String get() = ""
    open val mostrarAtras: Boolean get() = true
    protected abstract fun construir()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val scroll = ScrollView(this).apply { setBackgroundColor(Tema.FONDO) }
        contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(32))
        }
        destino = contenido
        scroll.addView(contenido)
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
        encabezado()
        construir()
    }

    // ───────── utilidades ─────────
    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Formatea con punto decimal sin importar el idioma del teléfono. */
    protected fun f(x: Double, dec: Int = 2): String = String.format(Locale.US, "%.${dec}f", x)

    protected fun redondo(color: Int, radioDp: Int, borde: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radioDp).toFloat()
        if (borde != null) setStroke(dp(1), borde)
    }

    protected fun circulo(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }

    /** Lee un número del campo (acepta coma o punto). null si está vacío o mal escrito. */
    protected fun numero(et: EditText): Double? =
        et.text.toString().trim().replace(',', '.').toDoubleOrNull()

    // ───────── estructura ─────────
    private fun encabezado() {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        if (mostrarAtras) {
            TextView(this).apply {
                text = "←"; textSize = 26f; setTextColor(Tema.ACENTO)
                setPadding(0, 0, dp(14), 0)
                setOnClickListener { finish() }
                fila.addView(this)
            }
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        TextView(this).apply {
            text = nombre; textSize = 24f
            setTypeface(typeface, Typeface.BOLD); setTextColor(Tema.TEXTO)
            col.addView(this)
        }
        if (subtitulo.isNotEmpty()) {
            TextView(this).apply { text = subtitulo; textSize = 13f; setTextColor(Tema.TEXTO2); col.addView(this) }
        }
        fila.addView(col)
        contenido.addView(fila)
    }

    /** Tarjeta redondeada. Todo lo que se cree dentro del bloque cae en ella; devuelve lo que regrese el bloque. */
    protected fun <T> tarjeta(titulo: String? = null, bloque: () -> T): T {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = redondo(Tema.TARJETA, 16, Tema.BORDE)
            setPadding(dp(16), dp(14), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) }
        }
        contenido.addView(card)
        if (titulo != null) {
            TextView(this).apply {
                text = titulo; textSize = 15f
                setTypeface(typeface, Typeface.BOLD); setTextColor(Tema.TEXTO)
                card.addView(this)
            }
        }
        val anterior = destino
        destino = card
        try { return bloque() } finally { destino = anterior }
    }

    protected fun nota(t: String): TextView = TextView(this).apply {
        text = t; textSize = 13f; setTextColor(Tema.TEXTO2)
        setPadding(0, dp(4), 0, dp(4))
        destino.addView(this)
    }

    protected fun destacado(): TextView = TextView(this).apply {
        textSize = 26f; setTypeface(typeface, Typeface.BOLD); setTextColor(Tema.TEXTO)
        setPadding(0, dp(6), 0, dp(6))
        destino.addView(this)
    }

    protected fun resultado(): TextView = TextView(this).apply {
        textSize = 14f; setTextColor(Tema.TEXTO2)
        setPadding(0, dp(10), 0, 0)
        setTextIsSelectable(true)
        destino.addView(this)
    }

    // ───────── entradas ─────────
    private fun crearCampo(padre: LinearLayout, etiqueta: String, valor: String, tipo: Tipo): EditText {
        TextView(this).apply {
            text = etiqueta; textSize = 12f; setTextColor(Tema.TEXTO2)
            setPadding(0, dp(10), 0, dp(4))
            padre.addView(this)
        }
        return EditText(this).apply {
            setText(valor); setSingleLine(); textSize = 16f
            setTextColor(Tema.TEXTO); setHintTextColor(Tema.TEXTO2)
            background = redondo(Tema.FONDO, 10, Tema.BORDE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            inputType = when (tipo) {
                Tipo.DECIMAL -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                Tipo.ENTERO -> InputType.TYPE_CLASS_NUMBER
                Tipo.TEXTO -> InputType.TYPE_CLASS_TEXT
            }
            padre.addView(this)
        }
    }

    protected fun campo(etiqueta: String, valor: String = "", tipo: Tipo = Tipo.DECIMAL): EditText =
        crearCampo(destino, etiqueta, valor, tipo)

    /** Dos campos lado a lado (ahorra espacio en formularios largos). */
    protected fun par(
        etiqueta1: String, valor1: String, etiqueta2: String, valor2: String,
        tipo1: Tipo = Tipo.DECIMAL, tipo2: Tipo = Tipo.DECIMAL
    ): Pair<EditText, EditText> {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        destino.addView(fila)
        fun columna(primera: Boolean): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                if (primera) rightMargin = dp(8) else leftMargin = dp(8)
            }
            fila.addView(this)
        }
        val a = crearCampo(columna(true), etiqueta1, valor1, tipo1)
        val b = crearCampo(columna(false), etiqueta2, valor2, tipo2)
        return a to b
    }

    protected fun selector(opciones: List<String>): Spinner {
        val adaptador = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opciones) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(Tema.TEXTO); textSize = 16f
                    setPadding(dp(12), dp(12), dp(36), dp(12))
                }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(Tema.TEXTO); setBackgroundColor(Tema.TARJETA)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
        }
        val caja = FrameLayout(this).apply {
            background = redondo(Tema.FONDO, 10, Tema.BORDE)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) }
        }
        val spinner = Spinner(this).apply {
            adapter = adaptador
            setBackgroundColor(Color.TRANSPARENT)
            setPopupBackgroundDrawable(redondo(Tema.TARJETA, 10, Tema.BORDE))
        }
        caja.addView(spinner, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        caja.addView(TextView(this).apply {
            text = "▾"; textSize = 16f; setTextColor(Tema.TEXTO2)
        }, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = dp(14) })
        destino.addView(caja)
        return spinner
    }

    /** Ejecuta la acción cada vez que se elige una opción (también una vez al abrir la pantalla). */
    protected fun alElegir(s: Spinner, accion: () -> Unit) {
        s.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { accion() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    protected fun casilla(texto: String, marcada: Boolean): CheckBox = CheckBox(this).apply {
        text = texto; isChecked = marcada; setTextColor(Tema.TEXTO)
        destino.addView(this)
    }

    protected fun boton(texto: String, primario: Boolean = true, accion: () -> Unit): Button = Button(this).apply {
        text = texto; textSize = 15f
        setAllCaps(false)
        setTextColor(if (primario) Tema.FONDO else Tema.TEXTO)
        background = redondo(if (primario) Tema.ACENTO else Tema.BORDE, 12)
        stateListAnimator = null
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(48)).apply { topMargin = dp(12) }
        setOnClickListener { accion() }
        destino.addView(this)
    }

    // ───────── visualización ─────────
    protected fun filaMetricas(vararg etiquetas: String): List<MetricaView> {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(4))
        }
        destino.addView(fila)
        return etiquetas.map { e ->
            MetricaView(this, e).also { m ->
                m.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                fila.addView(m)
            }
        }
    }

    protected fun grafica(alturaDp: Int = 220): GraficaLinea = GraficaLinea(this).also {
        it.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(alturaDp)).apply { topMargin = dp(8) }
        destino.addView(it)
    }

    protected fun medidor(): Medidor = Medidor(this).also {
        it.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(64)).apply { topMargin = dp(8) }
        destino.addView(it)
    }

    /** Etiqueta + barra de progreso (para probabilidades). */
    protected fun barraEtiquetada(): Pair<TextView, BarraView> {
        val txt = TextView(this).apply {
            textSize = 13f; setTextColor(Tema.TEXTO2)
            setPadding(0, dp(8), 0, dp(4))
            destino.addView(this)
        }
        val barra = BarraView(this).also {
            it.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(10))
            destino.addView(it)
        }
        return txt to barra
    }
}