package org.example.fitlab

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

enum class Tipo { DECIMAL, ENTERO, TEXTO }

/**
 * Base de todas las pantallas. Arma el scroll vertical y trae ayudas para crear
 * campos, botones y resultados desde código. Así cada módulo es UN archivo corto
 * y no hay que mantener 8 layouts XML.
 */
abstract class PantallaBase : AppCompatActivity() {
    protected lateinit var contenido: LinearLayout
    abstract val nombre: String
    protected abstract fun construir()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scroll = ScrollView(this)
        contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        scroll.addView(contenido)
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
        TextView(this).apply {
            text = nombre
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
            contenido.addView(this)
        }
        construir()
    }

    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Formatea un número con punto decimal sin importar el idioma del teléfono. */
    protected fun f(x: Double, dec: Int = 2): String = String.format(Locale.US, "%.${dec}f", x)

    protected fun nota(t: String): TextView = TextView(this).apply {
        text = t
        textSize = 14f
        setPadding(0, 0, 0, dp(8))
        contenido.addView(this)
    }

    protected fun campo(etiqueta: String, valor: String = "", tipo: Tipo = Tipo.DECIMAL): EditText {
        TextView(this).apply {
            text = etiqueta
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
            contenido.addView(this)
        }
        return EditText(this).apply {
            setText(valor)
            setSingleLine()
            inputType = when (tipo) {
                Tipo.DECIMAL -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                Tipo.ENTERO -> InputType.TYPE_CLASS_NUMBER
                Tipo.TEXTO -> InputType.TYPE_CLASS_TEXT
            }
            contenido.addView(this)
        }
    }

    protected fun selector(opciones: List<String>): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@PantallaBase, android.R.layout.simple_spinner_dropdown_item, opciones)
        contenido.addView(this)
    }

    protected fun casilla(texto: String, marcada: Boolean): CheckBox = CheckBox(this).apply {
        text = texto
        isChecked = marcada
        contenido.addView(this)
    }

    protected fun boton(texto: String, accion: () -> Unit): Button = Button(this).apply {
        text = texto
        setOnClickListener { accion() }
        contenido.addView(this)
    }

    protected fun resultado(): TextView = TextView(this).apply {
        textSize = 16f
        setPadding(0, dp(12), 0, dp(12))
        setTextIsSelectable(true)
        contenido.addView(this)
    }

    /** Lee un número del campo (acepta coma o punto). Devuelve null si está vacío o mal escrito. */
    protected fun numero(et: EditText): Double? =
        et.text.toString().trim().replace(',', '.').toDoubleOrNull()
}
