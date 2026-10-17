package org.example.fitlab.logica

/**
 * LÓGICA DIFUSA - riesgo de sobreentrenamiento (0-100).
 * Tipo Sugeno simplificado: cada regla da un valor fijo y la salida es el promedio ponderado.
 * Es más simple que Mamdani (no hay que "desdifusificar" áreas) y suficiente para el proyecto.
 */
object RiesgoDifuso {
    /** Trapecio: sube de a a b, vale 1 de b a c, baja de c a d. Con b=c es triángulo. */
    private fun trap(x: Double, a: Double, b: Double, c: Double, d: Double) = when {
        x < a || x > d -> 0.0
        x in b..c -> 1.0
        x < b -> (x - a) / (b - a)
        else -> (d - x) / (d - c)
    }

    // Conjuntos difusos de cada entrada
    private fun cargaBaja(x: Double) = trap(x, 0.0, 0.0, 25.0, 45.0)
    private fun cargaMedia(x: Double) = trap(x, 30.0, 45.0, 55.0, 70.0)
    private fun cargaAlta(x: Double) = trap(x, 55.0, 75.0, 100.0, 100.0)
    private fun suenoBajo(h: Double) = trap(h, 0.0, 0.0, 5.0, 6.5)
    private fun suenoMedio(h: Double) = trap(h, 5.5, 6.5, 7.5, 8.5)
    private fun suenoAlto(h: Double) = trap(h, 7.5, 8.5, 14.0, 14.0)
    // deltaFc = FC en reposo actual − FC base del atleta (latidos de más = cansancio)
    private fun fcNormal(d: Double) = trap(d, -40.0, -40.0, 2.0, 5.0)
    private fun fcElevada(d: Double) = trap(d, 3.0, 7.0, 40.0, 40.0)

    private const val BAJO = 10.0
    private const val MEDIO = 50.0
    private const val MEDIO_ALTO = 70.0
    private const val ALTO = 90.0

    fun evaluar(carga: Double, sueno: Double, deltaFc: Double): Double {
        // AND = mínimo de los grados de pertenencia
        val reglas = listOf(
            minOf(cargaAlta(carga), suenoBajo(sueno)) to ALTO,
            minOf(cargaAlta(carga), fcElevada(deltaFc)) to ALTO,
            minOf(suenoBajo(sueno), fcElevada(deltaFc)) to ALTO,
            minOf(cargaMedia(carga), suenoBajo(sueno)) to MEDIO_ALTO,
            minOf(cargaMedia(carga), suenoMedio(sueno)) to MEDIO,
            minOf(cargaAlta(carga), suenoAlto(sueno)) to MEDIO,
            minOf(suenoAlto(sueno), fcNormal(deltaFc)) to BAJO,
            cargaBaja(carga) to BAJO
        )
        val peso = reglas.sumOf { it.first }
        return if (peso == 0.0) MEDIO else reglas.sumOf { it.first * it.second } / peso
    }

    fun etiqueta(riesgo: Double) = when {
        riesgo < 35 -> "Bajo"
        riesgo < 65 -> "Medio"
        else -> "Alto"
    }
}