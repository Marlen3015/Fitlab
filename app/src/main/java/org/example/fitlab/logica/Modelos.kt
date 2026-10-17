package org.example.fitlab.logica

/** MÓDULO 1 - Captura e integración de variables. Un registro = una semana del atleta. */
data class RegistroAtleta(
    val semana: Int,
    val fcReposo: Double,
    val fcMaxima: Double,
    val pesoKg: Double,
    val grasaPct: Double,
    val suenoHoras: Double,
    val carga: Double,          // carga de entrenamiento, escala 0-100
    val calorias: Double,
    val proteinasG: Double,
    val vo2Max: Double
) {
    // Validar aquí garantiza que los módulos siguientes siempre reciben datos sanos.
    init {
        require(semana >= 1) { "La semana debe ser >= 1" }
        require(fcReposo in 30.0..120.0) { "FC en reposo fuera de rango (30-120)" }
        require(fcMaxima in 120.0..230.0) { "FC máxima fuera de rango (120-230)" }
        require(fcMaxima > fcReposo) { "La FC máxima debe ser mayor que la de reposo" }
        require(pesoKg in 30.0..250.0) { "Peso fuera de rango" }
        require(grasaPct in 2.0..60.0) { "% de grasa fuera de rango" }
        require(suenoHoras in 0.0..14.0) { "Horas de sueño fuera de rango" }
        require(carga in 0.0..100.0) { "La carga va de 0 a 100" }
        require(calorias >= 0 && proteinasG >= 0) { "Calorías/proteínas no pueden ser negativas" }
        require(vo2Max in 10.0..90.0) { "VO2 máx fuera de rango" }
    }
}

/** Permite pedir una serie del historial por variable (para la UI y los módulos 2 y 3). */
enum class Variable(val etiqueta: String, val valor: (RegistroAtleta) -> Double) {
    FC_REPOSO("FC en reposo", { it.fcReposo }),
    PESO("Peso (kg)", { it.pesoKg }),
    GRASA("% grasa", { it.grasaPct }),
    SUENO("Sueño (h)", { it.suenoHoras }),
    CARGA("Carga", { it.carga }),
    CALORIAS("Calorías", { it.calorias }),
    PROTEINAS("Proteínas (g)", { it.proteinasG }),
    VO2_MAX("VO2 máx", { it.vo2Max })
}

class HistorialAtleta {
    private val registros = mutableListOf<RegistroAtleta>()

    fun agregar(r: RegistroAtleta) {
        require(registros.none { it.semana == r.semana }) { "Ya existe un registro de la semana ${r.semana}" }
        registros.add(r)
        registros.sortBy { it.semana }
    }

    fun todos(): List<RegistroAtleta> = registros.toList()
    fun ultimo(): RegistroAtleta? = registros.lastOrNull()

    /** Devuelve (semanas, valores) de una variable: es lo que consumen los módulos 2 y 3. */
    fun serie(v: Variable): Pair<List<Double>, List<Double>> =
        registros.map { it.semana.toDouble() } to registros.map { v.valor(it) }

    /** Promedio de FC en reposo de las primeras 3 semanas = "línea base" del atleta. */
    fun fcReposoBase(): Double? =
        registros.take(3).map { it.fcReposo }.takeIf { it.isNotEmpty() }?.average()
}