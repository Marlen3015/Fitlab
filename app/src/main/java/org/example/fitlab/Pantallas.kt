package org.example.fitlab

import org.example.fitlab.logica.*

// ───────────────────────── MÓDULO 1 ─────────────────────────
class Modulo1Activity : PantallaBase() {
    override val nombre get() = "1 · Captura de variables"

    override fun construir() {
        nota("Registra los datos de una semana. Todos los demás módulos leen este historial.")
        val sig = (Repositorio.historial.ultimo()?.semana ?: 0) + 1
        val semana = campo("Semana", sig.toString(), Tipo.ENTERO)
        val fcRep = campo("FC en reposo (lpm)")
        val fcMax = campo("FC máxima (lpm)", "190")
        val peso = campo("Peso (kg)")
        val grasa = campo("% de grasa")
        val sueno = campo("Sueño promedio (h)")
        val carga = campo("Carga de entrenamiento (0-100)")
        val kcal = campo("Calorías")
        val prot = campo("Proteínas (g)")
        val vo2 = campo("VO2 máx")
        val salida = resultado()

        fun refrescar() {
            val todos = Repositorio.historial.todos()
            salida.text = if (todos.isEmpty()) "Aún no hay registros." else
                "Registros guardados: ${todos.size}\n" + todos.joinToString("\n") {
                    "Sem ${it.semana} · FCrep ${it.fcReposo.toInt()} · sueño ${f(it.suenoHoras, 1)} h · " +
                            "carga ${it.carga.toInt()} · VO2 ${f(it.vo2Max, 1)}"
                }
            semana.setText(((todos.lastOrNull()?.semana ?: 0) + 1).toString())
        }

        boton("Guardar registro") {
            val v = listOf(semana, fcRep, fcMax, peso, grasa, sueno, carga, kcal, prot, vo2).map { numero(it) }
            if (v.any { it == null }) {
                salida.text = "Llena todos los campos con números."
                return@boton
            }
            try {
                Repositorio.historial.agregar(
                    RegistroAtleta(
                        semana = v[0]!!.toInt(), fcReposo = v[1]!!, fcMaxima = v[2]!!, pesoKg = v[3]!!,
                        grasaPct = v[4]!!, suenoHoras = v[5]!!, carga = v[6]!!, calorias = v[7]!!,
                        proteinasG = v[8]!!, vo2Max = v[9]!!
                    )
                )
                refrescar()
            } catch (e: IllegalArgumentException) {
                salida.text = "No se guardó: ${e.message}"
            }
        }
        boton("Cargar datos de ejemplo (6 semanas)") { Repositorio.cargarEjemplo(); refrescar() }
        boton("Borrar historial") { Repositorio.limpiar(); refrescar() }
        refrescar()
    }
}

// ───────────────────────── MÓDULO 2 ─────────────────────────
class Modulo2Activity : PantallaBase() {
    override val nombre get() = "2 · Tendencia (mínimos cuadrados)"

    override fun construir() {
        nota("Ajusta una recta a la variable elegida. R² dice qué tan confiable es esa recta.")
        val variable = selector(Variable.values().map { it.etiqueta })
        val sig = (Repositorio.historial.ultimo()?.semana ?: 0) + 1
        val semana = campo("Semana a predecir", sig.toString(), Tipo.ENTERO)
        val salida = resultado()

        boton("Analizar tendencia") {
            val v = Variable.values()[variable.selectedItemPosition]
            try {
                val t = MinimosCuadrados.analizar(Repositorio.historial, v)
                val s = numero(semana) ?: sig.toDouble()
                val confianza = when {
                    t.r2 >= 0.8 -> "alta: la recta describe bien los datos"
                    t.r2 >= 0.5 -> "moderada"
                    else -> "baja: los datos no siguen una línea clara"
                }
                salida.text = "${v.etiqueta}\n" +
                        "Recta: y = ${f(t.pendiente, 3)}·x + ${f(t.ordenada, 2)}\n" +
                        "Tendencia: ${t.clase}\n" +
                        "R² = ${f(t.r2, 3)} (confianza $confianza)\n" +
                        "Predicción semana ${s.toInt()}: ${f(t.predecir(s), 2)}"
            } catch (e: IllegalArgumentException) {
                salida.text = e.message ?: "Datos insuficientes"
            }
        }
    }
}

// ───────────────────────── MÓDULO 3 ─────────────────────────
class Modulo3Activity : PantallaBase() {
    override val nombre get() = "3 · Predicción (extrapolación de Newton)"

    override fun construir() {
        nota("Estima las próximas 1 o 2 semanas con los últimos 4 registros. Más lejos que eso deja de ser confiable.")
        val vars = listOf(Variable.VO2_MAX, Variable.FC_REPOSO)
        val variable = selector(vars.map { it.etiqueta })
        val pasos = selector(listOf("1 semana adelante", "2 semanas adelante"))
        val salida = resultado()

        boton("Predecir") {
            val v = vars[variable.selectedItemPosition]
            val n = pasos.selectedItemPosition + 1
            try {
                val valores = Repositorio.historial.serie(v).second
                val ultimos = valores.takeLast(4)
                val tabla = NewtonExtrapolacion.tablaDiferencias(ultimos)
                val pred = NewtonExtrapolacion.predecir(Repositorio.historial, v, n)
                val semanaDestino = (Repositorio.historial.ultimo()?.semana ?: 0) + n
                salida.text = "${v.etiqueta}: últimos valores ${ultimos.joinToString(", ") { f(it, 1) }}\n\n" +
                        "Tabla de diferencias:\n" +
                        tabla.mapIndexed { k, fila -> "Δ$k: " + fila.joinToString("  ") { f(it, 2) } }.joinToString("\n") +
                        "\n\nPredicción semana $semanaDestino: ${f(pred, 2)}"
            } catch (e: IllegalArgumentException) {
                salida.text = e.message ?: "Datos insuficientes"
            }
        }
    }
}

// ───────────────────────── MÓDULO 4 ─────────────────────────
class Modulo4Activity : PantallaBase() {
    override val nombre get() = "4 · Optimización (Newton-Raphson)"

    override fun construir() {
        nota("Encuentra las horas de sueño necesarias para recuperarte de una carga de entrenamiento dada.")
        val ultimo = Repositorio.historial.ultimo()
        val carga = campo("Carga de entrenamiento (0-98)", ultimo?.carga?.toInt()?.toString() ?: "60")
        val salida = resultado()

        boton("Calcular sueño necesario") {
            val c = numero(carga)
            if (c == null) { salida.text = "Escribe un número."; return@boton }
            try {
                val h = OptimizacionSueno.suenoNecesario(c)
                val exacto = OptimizacionSueno.suenoAnalitico(c)
                var txt = "Carga $c → necesitas dormir ${f(h, 2)} h\n" +
                        "(comprobación analítica: ${f(exacto, 2)} h)"
                if (ultimo != null) {
                    val dif = h - ultimo.suenoHoras
                    txt += if (dif > 0) "\n\nTu última semana dormiste ${f(ultimo.suenoHoras, 1)} h: te faltan ${f(dif, 1)} h."
                    else "\n\nTu última semana dormiste ${f(ultimo.suenoHoras, 1)} h: es suficiente."
                }
                salida.text = txt
            } catch (e: Exception) {
                salida.text = e.message ?: "No se pudo calcular"
            }
        }
    }
}

// ───────────────────────── MÓDULO 5 ─────────────────────────
class Modulo5Activity : PantallaBase() {
    override val nombre get() = "5 · TRIMP (Regla de Simpson)"

    override fun construir() {
        nota("Mide la carga interna de una sesión integrando la FC en el tiempo. Pega las FC separadas por comas.")
        val ultimo = Repositorio.historial.ultimo()
        val fcs = campo("FC medida durante la sesión (lpm, separadas por coma)", "120,140,155,160,150", Tipo.TEXTO)
        val dt = campo("Minutos entre cada medición", "5")
        val fcRep = campo("FC en reposo", ultimo?.fcReposo?.toInt()?.toString() ?: "60")
        val fcMax = campo("FC máxima", ultimo?.fcMaxima?.toInt()?.toString() ?: "190")
        val hombre = casilla("Fórmula masculina (si no, femenina)", true)
        val salida = resultado()

        boton("Calcular TRIMP") {
            val lista = fcs.text.toString().split(",").map { it.trim().replace(',', '.').toDoubleOrNull() }
            val d = numero(dt); val r = numero(fcRep); val m = numero(fcMax)
            if (lista.any { it == null } || d == null || r == null || m == null) {
                salida.text = "Revisa los datos: todo debe ser números."
                return@boton
            }
            try {
                val muestras = lista.map { it!! }
                val trimp = Trimp.calcular(muestras, d, r, m, hombre.isChecked)
                salida.text = "TRIMP = ${f(trimp, 1)} unidades\n" +
                        "Duración: ${f((muestras.size - 1) * d, 0)} min con ${muestras.size} mediciones.\n" +
                        "Úsalo para comparar sesiones entre sí: mientras más alto, más exigente fue."
            } catch (e: IllegalArgumentException) {
                salida.text = e.message ?: "Datos insuficientes"
            }
        }
    }
}

// ───────────────────────── LÓGICA DIFUSA ─────────────────────────
class DifusaActivity : PantallaBase() {
    override val nombre get() = "6 · Riesgo de sobreentrenamiento (lógica difusa)"

    override fun construir() {
        nota("Combina carga, sueño y cuánto subió tu FC en reposo para estimar el riesgo (0-100).")
        val ultimo = Repositorio.historial.ultimo()
        val carga = campo("Carga (0-100)", ultimo?.carga?.toInt()?.toString() ?: "60")
        val sueno = campo("Sueño (h)", ultimo?.suenoHoras?.toString() ?: "7")
        val delta = campo("FC en reposo sobre tu base (lpm)", f(Repositorio.deltaFc(), 1))
        val salida = resultado()

        boton("Evaluar riesgo") {
            val c = numero(carga); val s = numero(sueno); val d = numero(delta)
            if (c == null || s == null || d == null) { salida.text = "Escribe números en los tres campos."; return@boton }
            val riesgo = RiesgoDifuso.evaluar(c, s, d)
            salida.text = "Riesgo: ${f(riesgo, 0)} / 100 → ${RiesgoDifuso.etiqueta(riesgo)}\n" +
                    "(0-35 bajo · 35-65 medio · 65-100 alto)"
        }
    }
}

// ───────────────────────── IA: RED + BFS + HILL CLIMBING ─────────────────────────
class IAActivity : PantallaBase() {
    override val nombre get() = "7 · Agente inteligente"

    override fun construir() {
        nota("La red neuronal clasifica tu estado y el Best-First Search recomienda un ajuste de carga y sueño.")
        val salida1 = resultado()

        boton("Clasificar y recomendar") {
            val u = Repositorio.historial.ultimo()
            if (u == null) { salida1.text = "Primero captura datos en el Módulo 1."; return@boton }
            val delta = Repositorio.deltaFc()
            val red = PesosRed.red
            val (estado, prob) = red.clasificar(red.caracteristicas(delta, u.suenoHoras, u.carga))
            val riesgo = RiesgoDifuso.evaluar(u.carga, u.suenoHoras, delta)
            val rec = RecomendadorBFS().recomendar(Estado(u.carga, u.suenoHoras), delta, Repositorio.pesos)
            val resumen = rec.pasos.groupingBy { it }.eachCount()
                .entries.joinToString("\n") { "• ${it.key}" + if (it.value > 1) " ×${it.value}" else "" }
            salida1.text = "Estado (red neuronal): ${estado.name}\n" +
                    "Probabilidades: recuperado ${f(prob[0] * 100, 0)}% · óptima ${f(prob[1] * 100, 0)}% · riesgo ${f(prob[2] * 100, 0)}%\n" +
                    "Riesgo difuso: ${f(riesgo, 0)}/100\n\n" +
                    "Recomendación (Best-First):\n" +
                    "Carga ${u.carga.toInt()} → ${rec.estado.carga.toInt()} · Sueño ${f(u.suenoHoras, 1)} → ${f(rec.estado.sueno, 1)} h\n" +
                    (if (resumen.isEmpty()) "Mantén tu plan actual." else resumen)
        }

        nota("— Personalizar (Hill Climbing) —\nCalifica cómo te sentiste la última semana; con 3 o más muestras se ajustan los pesos de la utilidad a ti.")
        val bienestar = campo("¿Cómo te sentiste? (0 muy mal - 10 excelente)", "7")
        val salida2 = resultado()

        fun estadoMuestras() {
            val p = Repositorio.pesos
            salida2.text = "Muestras guardadas: ${Repositorio.muestras.size}\n" +
                    "Pesos actuales → estímulo ${f(p.estimulo, 2)} · recuperación ${f(p.recuperacion, 2)} · riesgo ${f(p.riesgo, 2)}"
        }

        boton("Guardar muestra de la última semana") {
            val u = Repositorio.historial.ultimo()
            val b = numero(bienestar)
            if (u == null || b == null || b < 0 || b > 10) { salida2.text = "Captura datos y escribe un bienestar de 0 a 10."; return@boton }
            Repositorio.muestras.add(Muestra(Estado(u.carga, u.suenoHoras), Repositorio.deltaFc(), b / 10.0))
            estadoMuestras()
        }
        boton("Cargar muestras de ejemplo") { Repositorio.cargarMuestrasEjemplo(); estadoMuestras() }
        boton("Ajustar pesos (Hill Climbing)") {
            if (Repositorio.muestras.size < 3) { salida2.text = "Necesitas al menos 3 muestras."; return@boton }
            Repositorio.pesos = HillClimbing.ajustar(Repositorio.muestras, Repositorio.pesos)
            estadoMuestras()
        }
        estadoMuestras()
    }
}