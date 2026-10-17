package org.example.fitlab.logica

import java.util.PriorityQueue
import kotlin.math.exp

/**
 * RED NEURONAL - clasifica el estado del atleta.
 * Se ENTRENA en Python con MLPClassifier (ver entrenar_red.py) y aquí solo se hace la
 * propagación hacia adelante con los pesos exportados (scikit-learn no corre en Android).
 */
enum class EstadoAtleta { RECUPERADO, CARGA_OPTIMA, RIESGO }

class RedNeuronal(
    private val w1: Array<DoubleArray>, private val b1: DoubleArray,   // capa oculta (ReLU)
    private val w2: Array<DoubleArray>, private val b2: DoubleArray    // salida (softmax)
) {
    /** Misma normalización que en entrenar_red.py. */
    fun caracteristicas(deltaFc: Double, sueno: Double, carga: Double) =
        doubleArrayOf(deltaFc / 20.0, sueno / 10.0, carga / 100.0)

    fun clasificar(x: DoubleArray): Pair<EstadoAtleta, DoubleArray> {
        val oculta = DoubleArray(b1.size) { j ->
            maxOf(0.0, b1[j] + x.indices.sumOf { i -> w1[j][i] * x[i] })
        }
        val z = DoubleArray(b2.size) { k -> b2[k] + oculta.indices.sumOf { j -> w2[k][j] * oculta[j] } }
        val zMax = z.max()
        val e = z.map { exp(it - zMax) }          // restar el máximo evita desbordes
        val prob = DoubleArray(e.size) { e[it] / e.sum() }
        return EstadoAtleta.values()[prob.indices.maxByOrNull { prob[it] }!!] to prob
    }
}

/** HEURÍSTICA / UTILIDAD (agente basado en utilidad). */
data class Estado(val carga: Double, val sueno: Double)
data class Pesos(val estimulo: Double, val recuperacion: Double, val riesgo: Double) {
    fun normalizado(): Pesos {
        val s = estimulo + recuperacion + riesgo
        return if (s == 0.0) Pesos(1 / 3.0, 1 / 3.0, 1 / 3.0) else Pesos(estimulo / s, recuperacion / s, riesgo / s)
    }
}

object Utilidad {
    const val CARGA_OBJETIVO = 70.0   // carga "productiva": menos no mejora, más ya no suma

    // Premia el estímulo (cerca de la carga objetivo) y la recuperación (sueño); castiga el riesgo difuso.
    // Si el estímulo siguiera creciendo con la carga, el agente siempre pediría entrenar más.
    fun valor(e: Estado, deltaFc: Double, p: Pesos): Double {
        val estimulo = 1.0 - kotlin.math.abs(e.carga - CARGA_OBJETIVO) / CARGA_OBJETIVO
        return p.estimulo * estimulo +
                p.recuperacion * (e.sueno / 10.0) -
                p.riesgo * (RiesgoDifuso.evaluar(e.carga, e.sueno, deltaFc) / 100.0)
    }
}

/** BEST-FIRST SEARCH acotado: ajustes vecinos de carga (±5) y sueño (±0.5 h), máximo ±15 y ±1.5 h respecto al estado actual. */
data class Recomendacion(val estado: Estado, val utilidad: Double, val pasos: List<String>)

class RecomendadorBFS(
    private val maxExpansiones: Int = 40,
    private val maxCambioCarga: Double = 15.0,   // un ajuste razonable para UNA semana
    private val maxCambioSueno: Double = 1.5
) {
    private class Nodo(val estado: Estado, val u: Double, val camino: List<String>)

    fun recomendar(inicial: Estado, deltaFc: Double, p: Pesos): Recomendacion {
        val cola = PriorityQueue<Nodo>(compareByDescending { it.u })   // siempre sale el de mayor utilidad
        val visitados = mutableSetOf(inicial)
        val raiz = Nodo(inicial, Utilidad.valor(inicial, deltaFc, p), emptyList())
        cola.add(raiz)
        var mejor = raiz
        var expansiones = 0
        while (cola.isNotEmpty() && expansiones < maxExpansiones) {   // la cota evita explorar sin fin
            val actual = cola.poll()
            expansiones++
            if (actual.u > mejor.u) mejor = actual
            val acciones = listOf(
                "Subir carga 5" to Estado(actual.estado.carga + 5, actual.estado.sueno),
                "Bajar carga 5" to Estado(actual.estado.carga - 5, actual.estado.sueno),
                "Dormir 30 min más" to Estado(actual.estado.carga, actual.estado.sueno + 0.5),
                "Dormir 30 min menos" to Estado(actual.estado.carga, actual.estado.sueno - 0.5)
            )
            for ((nombre, e) in acciones) {
                val cargaOk = e.carga in 0.0..100.0 &&
                        kotlin.math.abs(e.carga - inicial.carga) <= maxCambioCarga
                val suenoOk = e.sueno in 4.0..10.0 &&
                        kotlin.math.abs(e.sueno - inicial.sueno) <= maxCambioSueno
                if (!cargaOk || !suenoOk || e in visitados) continue
                visitados.add(e)
                cola.add(Nodo(e, Utilidad.valor(e, deltaFc, p), actual.camino + nombre))
            }
        }
        return Recomendacion(mejor.estado, mejor.u, mejor.camino)
    }
}

/** HILL CLIMBING: ajusta los pesos de la utilidad a cada atleta usando su bienestar reportado (0-1). */
data class Muestra(val estado: Estado, val deltaFc: Double, val bienestar: Double)

object HillClimbing {
    private fun error(m: List<Muestra>, p: Pesos) =
        m.map { Utilidad.valor(it.estado, it.deltaFc, p) - it.bienestar }.map { it * it }.average()

    fun ajustar(muestras: List<Muestra>, inicial: Pesos = Pesos(1 / 3.0, 1 / 3.0, 1 / 3.0),
                paso: Double = 0.05, maxIter: Int = 200): Pesos {
        require(muestras.isNotEmpty()) { "Se necesitan muestras del atleta" }
        var actual = inicial.normalizado()
        var errActual = error(muestras, actual)
        repeat(maxIter) {
            val vecinos = listOf(
                actual.copy(estimulo = actual.estimulo + paso), actual.copy(estimulo = actual.estimulo - paso),
                actual.copy(recuperacion = actual.recuperacion + paso), actual.copy(recuperacion = actual.recuperacion - paso),
                actual.copy(riesgo = actual.riesgo + paso), actual.copy(riesgo = actual.riesgo - paso)
            ).filter { it.estimulo >= 0 && it.recuperacion >= 0 && it.riesgo >= 0 }.map { it.normalizado() }
            val mejor = vecinos.minByOrNull { error(muestras, it) } ?: return actual
            val errMejor = error(muestras, mejor)
            if (errMejor >= errActual) return actual     // ya no hay vecino mejor = óptimo local
            actual = mejor; errActual = errMejor
        }
        return actual
    }
}