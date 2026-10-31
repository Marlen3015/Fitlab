package org.example.fitlab.logica

data class Rango(val min: Double, val max: Double, val unidad: String) {
    fun contiene(v: Double) = v in min..max
}

/**
 * Valores de REFERENCIA generales para atletas. Son orientativos (no sustituyen a un médico
 * o entrenador) y sirven para decirle al usuario cómo "debería" estar cada variable.
 */
object Referencias {
    val FC_REPOSO = Rango(40.0, 60.0, "lpm")        // atletas entrenados suelen estar en 40-60 lpm
    val DELTA_FC = Rango(-5.0, 5.0, "lpm")          // cerca de tu base; con +7 o más ya hay fatiga
    val SUENO = Rango(8.0, 10.0, "h")               // lo recomendable al entrenar con regularidad
    val CARGA = Rango(Utilidad.CARGA_OBJETIVO - 10, Utilidad.CARGA_OBJETIVO + 10, "")   // zona productiva
    val PROTEINA = Rango(1.4, 2.0, "g/kg")          // consumo diario recomendado
    val TRIMP_MODERADA = Rango(50.0, 100.0, "u")    // sesión moderada
    const val VO2_MAX_MIN = 45.0                    // referencia mínima para deportistas amateurs

    /** Rango ideal para las variables que tienen uno claro. */
    fun paraVariable(v: Variable): Rango? = when (v) {
        Variable.FC_REPOSO -> FC_REPOSO
        Variable.SUENO -> SUENO
        Variable.CARGA -> CARGA
        Variable.VO2_MAX -> Rango(VO2_MAX_MIN, 90.0, "ml/kg/min")
        else -> null
    }

    /** Dice si la tendencia es buena o mala según la variable. */
    fun valoracionTendencia(v: Variable, clase: String): String? = when (v) {
        Variable.FC_REPOSO -> when (clase) {
            "al alza" -> "Desfavorable: una FC en reposo que sube suele indicar fatiga."
            "a la baja" -> "Favorable: la FC en reposo baja cuando mejora la condición aeróbica."
            else -> null
        }
        Variable.VO2_MAX -> when (clase) {
            "a la baja" -> "Desfavorable: el VO2 máx debería mantenerse o subir."
            "al alza" -> "Favorable: tu capacidad aeróbica está mejorando."
            else -> null
        }
        Variable.SUENO -> when (clase) {
            "a la baja" -> "Desfavorable: estás durmiendo cada vez menos."
            "al alza" -> "Favorable: estás durmiendo más."
            else -> null
        }
        else -> null
    }

    fun categoriaTrimp(t: Double) = when {
        t < 50 -> "Ligera"
        t < 100 -> "Moderada"
        t < 150 -> "Intensa"
        else -> "Muy intensa"
    }
}
