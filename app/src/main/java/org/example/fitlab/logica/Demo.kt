package org.example.fitlab.logica

/** Prueba rápida de todos los módulos. Córrela desde un Kotlin scratch o un unit test. */
fun main() {
    val h = HistorialAtleta()
    val datos = listOf(  // semana, fcRep, fcMax, peso, grasa, sueño, carga, kcal, prot, vo2
        listOf(1.0, 58.0, 190.0, 72.0, 15.0, 7.5, 50.0, 2600.0, 130.0, 48.0),
        listOf(2.0, 57.0, 190.0, 71.8, 14.8, 7.2, 55.0, 2600.0, 130.0, 48.5),
        listOf(3.0, 57.0, 190.0, 71.5, 14.6, 7.0, 60.0, 2650.0, 135.0, 49.0),
        listOf(4.0, 56.0, 190.0, 71.4, 14.5, 7.4, 62.0, 2650.0, 135.0, 49.4),
        listOf(5.0, 56.0, 190.0, 71.2, 14.3, 7.1, 65.0, 2700.0, 140.0, 49.9)
    )
    datos.forEach { d ->
        h.agregar(RegistroAtleta(d[0].toInt(), d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]))
    }

    println("M2: " + MinimosCuadrados.analizar(h, Variable.VO2_MAX))
    println("M3: VO2 en 2 semanas = " + NewtonExtrapolacion.predecir(h, Variable.VO2_MAX, 2))
    println("M4: sueño para carga 65 = " + OptimizacionSueno.suenoNecesario(65.0) +
            " (analítico " + OptimizacionSueno.suenoAnalitico(65.0) + ")")
    println("M5: TRIMP = " + Trimp.calcular(listOf(120.0, 140.0, 155.0, 160.0, 150.0), 5.0, 56.0, 190.0))

    val deltaFc = h.ultimo()!!.fcReposo - h.fcReposoBase()!!
    println("Riesgo difuso = " + RiesgoDifuso.evaluar(65.0, 7.1, deltaFc))

    val rec = RecomendadorBFS().recomendar(Estado(65.0, 7.1), deltaFc, Pesos(0.4, 0.3, 0.3))
    println("BFS: $rec")

    val muestras = listOf(
        Muestra(Estado(40.0, 8.0), 0.0, 0.8), Muestra(Estado(80.0, 5.5), 6.0, 0.2),
        Muestra(Estado(60.0, 7.0), 1.0, 0.6), Muestra(Estado(90.0, 6.0), 8.0, 0.1)
    )
    println("Hill Climbing: " + HillClimbing.ajustar(muestras))
}