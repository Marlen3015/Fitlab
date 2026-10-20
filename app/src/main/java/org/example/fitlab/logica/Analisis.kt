package org.example.fitlab.logica

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/** MÓDULO 2 - Mínimos cuadrados con R². */
data class Tendencia(val pendiente: Double, val ordenada: Double, val r2: Double, val clase: String) {
    fun predecir(semana: Double) = pendiente * semana + ordenada
}

object MinimosCuadrados {
    fun analizar(historial: HistorialAtleta, variable: Variable): Tendencia {
        val (x, y) = historial.serie(variable)
        require(x.size >= 3) { "Se necesitan al menos 3 registros" }
        val n = x.size
        val sx = x.sum(); val sy = y.sum()
        val sxy = x.indices.sumOf { x[it] * y[it] }
        val sxx = x.sumOf { it * it }
        val m = (n * sxy - sx * sy) / (n * sxx - sx * sx)
        val b = (sy - m * sx) / n
        // R² = qué tanto de la variación de los datos explica la recta (1 = perfecto)
        val media = y.average()
        val ssRes = x.indices.sumOf { (y[it] - (m * x[it] + b)).let { e -> e * e } }
        val ssTot = y.sumOf { (it - media) * (it - media) }
        val r2 = if (ssTot != 0.0) 1 - ssRes / ssTot else 1.0
        val clase = when {
            abs(m) < 0.05 -> "estable"
            m > 0 -> "al alza"
            else -> "a la baja"
        }
        return Tendencia(m, b, r2, clase)
    }
}

/** MÓDULO 3 - Extrapolación de Newton (diferencias hacia atrás). */
object NewtonExtrapolacion {
    const val MAX_PASOS = 2      // más allá de 2 semanas la predicción ya no es confiable

    fun tablaDiferencias(valores: List<Double>): List<List<Double>> {
        val tabla = mutableListOf(valores)
        while (tabla.last().size > 1) {
            val ant = tabla.last()
            tabla.add(List(ant.size - 1) { ant[it + 1] - ant[it] })
        }
        return tabla
    }

    /** Estima el valor `pasos` semanas después del último registro. Asume semanas consecutivas. */
    fun extrapolar(valores: List<Double>, pasos: Int): Double {
        require(valores.size >= 3) { "Se necesitan al menos 3 registros" }
        require(pasos in 1..MAX_PASOS) { "Solo se permite predecir 1 o 2 semanas" }
        val ultimos = valores.takeLast(4)          // 4 puntos: más daría una curva "nerviosa" (sobreajuste)
        var resultado = 0.0
        var termino = 1.0
        tablaDiferencias(ultimos).forEachIndexed { k, nivel ->
            if (k > 0) termino *= (pasos + k - 1).toDouble() / k
            resultado += termino * nivel.last()
        }
        return resultado
    }

    fun predecir(historial: HistorialAtleta, variable: Variable, pasos: Int): Double =
        extrapolar(historial.serie(variable).second, pasos)
}

/**
 * MÓDULO 4 - Newton-Raphson: ¿cuántas horas de sueño necesito para recuperar una carga dada?
 * Modelo simple (supuesto del proyecto): capacidad de recuperación = 100·(1 − e^(−K·horas)).
 * Rendimientos decrecientes: las primeras horas recuperan más que las últimas.
 * Buscamos la raíz de f(s) = capacidad(s) − carga.
 */
object OptimizacionSueno {
    private const val K = 0.35
    fun capacidad(s: Double) = 100.0 * (1 - exp(-K * s))
    private fun derivada(s: Double) = 100.0 * K * exp(-K * s)

    fun newtonRaphson(f: (Double) -> Double, df: (Double) -> Double,
                      x0: Double, tol: Double = 1e-6, maxIter: Int = 50): Double {
        var x = x0
        repeat(maxIter) {
            val d = df(x)
            check(abs(d) > 1e-12) { "Derivada ~0, no se puede continuar" }
            val xNuevo = x - f(x) / d
            if (abs(xNuevo - x) < tol) return xNuevo
            x = xNuevo
        }
        error("Newton-Raphson no convergió")
    }

    fun suenoNecesario(carga: Double): Double {
        require(carga >= 0 && carga < 99) { "Carga fuera de rango para este modelo" }
        return newtonRaphson({ capacidad(it) - carga }, { derivada(it) }, x0 = 7.0)
    }

    /** Comprobación analítica: s = −ln(1 − carga/100)/K. Útil para validar ante la maestra. */
    fun suenoAnalitico(carga: Double) = -ln(1 - carga / 100) / K
}

/**
 * MÓDULO 5 - TRIMP de Banister con Regla de Simpson 1/3.
 * TRIMP = ∫ FCR(t)·a·e^(b·FCR(t)) dt, con FCR = (FC − FCrep)/(FCmax − FCrep).
 */
object Trimp {
    fun calcular(fcMuestras: List<Double>, dtMin: Double, fcRep: Double, fcMax: Double,
                 hombre: Boolean = true): Double {
        require(fcMuestras.size >= 3) { "Se necesitan al menos 3 muestras" }
        val (a, b) = if (hombre) 0.64 to 1.92 else 0.86 to 1.67
        val g = fcMuestras.map { fc ->
            val fcr = ((fc - fcRep) / (fcMax - fcRep)).coerceIn(0.0, 1.0)
            fcr * a * exp(b * fcr)
        }
        val n = g.size - 1                         // número de intervalos
        val nSimp = if (n % 2 == 0) n else n - 1   // Simpson exige intervalos pares
        var suma = g[0] + g[nSimp]
        for (i in 1 until nSimp) suma += if (i % 2 == 1) 4 * g[i] else 2 * g[i]
        var integral = suma * dtMin / 3
        if (nSimp < n) integral += (g[n - 1] + g[n]) / 2 * dtMin   // último tramo con trapecio
        return integral
    }
}