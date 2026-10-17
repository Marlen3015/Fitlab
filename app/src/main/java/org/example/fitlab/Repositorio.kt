package org.example.fitlab

import org.example.fitlab.logica.*

/**
 * Datos compartidos entre todas las pantallas (viven mientras la app esté abierta).
 * Es un `object` = una sola instancia, así el Módulo 1 guarda y los demás leen lo mismo.
 */
object Repositorio {
    var historial = HistorialAtleta()
    var pesos = Pesos(0.4, 0.2, 0.4)          // pesos de la utilidad; Hill Climbing los ajusta
    val muestras = mutableListOf<Muestra>()   // bienestar reportado, para Hill Climbing

    /** Cuánto subió la FC en reposo actual respecto a la base del atleta (señal de cansancio). */
    fun deltaFc(): Double {
        val ultimo = historial.ultimo() ?: return 0.0
        val base = historial.fcReposoBase() ?: return 0.0
        return ultimo.fcReposo - base
    }

    fun limpiar() {
        historial = HistorialAtleta()
        muestras.clear()
        pesos = Pesos(0.4, 0.2, 0.4)
    }

    /** 6 semanas con una historia: la carga sube, el sueño baja y la FC en reposo se eleva. */
    fun cargarEjemplo() {
        val h = HistorialAtleta()
        //          sem  fcRep fcMax peso  grasa sueño carga kcal   prot  vo2
        val datos = listOf(
            doubleArrayOf(1.0, 58.0, 190.0, 72.0, 15.0, 7.5, 50.0, 2600.0, 130.0, 48.0),
            doubleArrayOf(2.0, 57.0, 190.0, 71.8, 14.8, 7.2, 55.0, 2600.0, 130.0, 48.4),
            doubleArrayOf(3.0, 57.0, 190.0, 71.6, 14.7, 7.0, 60.0, 2650.0, 135.0, 48.9),
            doubleArrayOf(4.0, 58.0, 190.0, 71.5, 14.6, 6.5, 68.0, 2650.0, 135.0, 49.2),
            doubleArrayOf(5.0, 60.0, 190.0, 71.3, 14.5, 6.0, 75.0, 2700.0, 140.0, 49.3),
            doubleArrayOf(6.0, 62.0, 190.0, 71.0, 14.4, 5.8, 80.0, 2700.0, 140.0, 49.2)
        )
        datos.forEach { d ->
            h.agregar(RegistroAtleta(d[0].toInt(), d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]))
        }
        historial = h
    }

    fun cargarMuestrasEjemplo() {
        muestras.clear()
        muestras.addAll(listOf(
            Muestra(Estado(40.0, 8.0), 0.0, 0.8),
            Muestra(Estado(80.0, 5.5), 6.0, 0.2),
            Muestra(Estado(60.0, 7.0), 1.0, 0.6),
            Muestra(Estado(90.0, 6.0), 8.0, 0.1)
        ))
    }
}