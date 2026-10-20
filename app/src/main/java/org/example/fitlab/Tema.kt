package org.example.fitlab

import android.graphics.Color

/** Paleta única de la app: oscura, con verde menta como acento (look deportivo). */
object Tema {
    val FONDO = Color.parseColor("#0E1116")
    val TARJETA = Color.parseColor("#171B22")
    val BORDE = Color.parseColor("#262C36")
    val TEXTO = Color.parseColor("#F1F4F9")
    val TEXTO2 = Color.parseColor("#97A1B1")
    val ACENTO = Color.parseColor("#2EE6A6")     // verde: bien / acciones principales
    val ACENTO2 = Color.parseColor("#5B9DFF")    // azul: predicciones y soluciones
    val AMBAR = Color.parseColor("#FFB347")      // precaución
    val ROJO = Color.parseColor("#FF6B6B")       // peligro

    fun colorRiesgo(r: Double) = when {
        r < 35 -> ACENTO
        r < 65 -> AMBAR
        else -> ROJO
    }
}