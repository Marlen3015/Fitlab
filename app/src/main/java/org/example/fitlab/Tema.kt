package org.example.fitlab

import android.graphics.Color

/** Paleta única de la app: fondo claro y verde suave como acento (sin colores neón). */
object Tema {
    val FONDO = Color.parseColor("#F4F7F4")
    val TARJETA = Color.parseColor("#FFFFFF")
    val BORDE = Color.parseColor("#DDE5DF")
    val TEXTO = Color.parseColor("#1C2B22")
    val TEXTO2 = Color.parseColor("#66766C")
    val ACENTO = Color.parseColor("#2E8B57")         // verde bosque suave: acciones y "todo bien"
    val ACENTO_SUAVE = Color.parseColor("#E3F1E8")   // verde muy claro: botones secundarios y zonas ideales
    val ACENTO2 = Color.parseColor("#3B78B5")        // azul apagado: predicciones y soluciones
    val AMBAR = Color.parseColor("#C98A12")          // precaución
    val ROJO = Color.parseColor("#C4453B")           // peligro
    val SOBRE_ACENTO = Color.WHITE                   // texto sobre fondos verdes

    val GRAD_BAJO = Color.parseColor("#6DBE8C")
    val GRAD_MEDIO = Color.parseColor("#E8C05A")
    val GRAD_ALTO = Color.parseColor("#DC6B61")

    fun colorRiesgo(r: Double) = when {
        r < 35 -> ACENTO
        r < 65 -> AMBAR
        else -> ROJO
    }
}