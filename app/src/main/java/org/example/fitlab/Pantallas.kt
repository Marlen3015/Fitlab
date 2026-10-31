package org.example.fitlab

import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import org.example.fitlab.logica.*
import java.util.Locale

private const val SIN_DATOS = "Captura al menos 3 semanas en el Módulo 1 (o carga los datos de ejemplo)."

private fun corto(x: Double) =
    if (x == Math.rint(x)) String.format(Locale.US, "%.0f", x) else String.format(Locale.US, "%.1f", x)

// ───────────────────────── MÓDULO 1 · CAPTURA ─────────────────────────
class Modulo1Activity : PantallaBase() {
    override val nombre get() = "1 · Captura"
    override val subtitulo get() = "Registro semanal del atleta"

    override fun construir() {
        lateinit var lista: TextView
        lateinit var mensaje: TextView
        lateinit var campos: List<EditText>
        lateinit var filas: List<FilaRango>
        lateinit var selVar: Spinner
        lateinit var grafica: GraficaLinea

        fun dibujarGrafica() {
            val idx = selVar.selectedItemPosition
            if (idx >= 0) {
                val v = Variable.values()[idx]
                val (xs, ys) = Repositorio.historial.serie(v)
                grafica.datos(xs, ys, banda = Referencias.paraVariable(v)?.let { it.min to it.max })
            }
        }

        fun refrescar() {
            val todos = Repositorio.historial.todos()
            lista.text = if (todos.isEmpty()) "Aún no hay registros." else
                todos.joinToString("\n") {
                    "Sem ${it.semana} · FC ${it.fcReposo.toInt()} · sueño ${f(it.suenoHoras, 1)} h · " +
                            "carga ${it.carga.toInt()} · VO2 ${f(it.vo2Max, 1)}"
                }
            campos[0].setText(((todos.lastOrNull()?.semana ?: 0) + 1).toString())

            val u = Repositorio.historial.ultimo()
            if (u == null) {
                listOf("FC en reposo", "FC sobre tu base", "Sueño", "Carga", "Proteínas")
                    .forEachIndexed { i, t -> filas[i].vaciar(t) }
            } else {
                val delta = Repositorio.deltaFc()
                val prot = u.proteinasG / u.pesoKg
                filas[0].set("FC en reposo", u.fcReposo, "${u.fcReposo.toInt()} lpm", Referencias.FC_REPOSO, 30.0, 90.0)
                filas[1].set("FC sobre tu base", delta, "${if (delta >= 0) "+" else ""}${f(delta, 1)} lpm", Referencias.DELTA_FC, -10.0, 15.0)
                filas[2].set("Sueño", u.suenoHoras, "${f(u.suenoHoras, 1)} h", Referencias.SUENO, 0.0, 12.0)
                filas[3].set("Carga", u.carga, "${u.carga.toInt()}", Referencias.CARGA, 0.0, 100.0)
                filas[4].set("Proteínas", prot, "${f(prot, 1)} g/kg", Referencias.PROTEINA, 0.5, 3.0)
            }
            dibujarGrafica()
        }

        val sig = (Repositorio.historial.ultimo()?.semana ?: 0) + 1
        tarjeta("Datos de la semana") {
            val (semana, fcRep) = par("Semana", sig.toString(), "FC en reposo (lpm)", "", tipo1 = Tipo.ENTERO)
            val (fcMax, peso) = par("FC máxima (lpm)", "190", "Peso (kg)", "")
            val (grasa, sueno) = par("% de grasa", "", "Sueño promedio (h)", "")
            val (carga, vo2) = par("Carga (0-100)", "", "VO2 máx", "")
            val (kcal, prot) = par("Calorías", "", "Proteínas (g)", "")
            campos = listOf(semana, fcRep, fcMax, peso, grasa, sueno, carga, kcal, prot, vo2)
            mensaje = resultado()

            boton("Guardar registro") {
                val v = campos.map { numero(it) }
                if (v.any { it == null }) {
                    mensaje.setTextColor(Tema.AMBAR); mensaje.text = "Llena todos los campos con números."
                } else {
                    try {
                        Repositorio.historial.agregar(RegistroAtleta(
                            semana = v[0]!!.toInt(), fcReposo = v[1]!!, fcMaxima = v[2]!!, pesoKg = v[3]!!,
                            grasaPct = v[4]!!, suenoHoras = v[5]!!, carga = v[6]!!, calorias = v[7]!!,
                            proteinasG = v[8]!!, vo2Max = v[9]!!
                        ))
                        mensaje.setTextColor(Tema.ACENTO); mensaje.text = "Registro guardado."
                        refrescar()
                    } catch (e: IllegalArgumentException) {
                        mensaje.setTextColor(Tema.ROJO); mensaje.text = "No se guardó: ${e.message}"
                    }
                }
            }
            boton("Cargar datos de ejemplo", primario = false) {
                Repositorio.cargarEjemplo(); mensaje.text = ""; refrescar()
            }
            boton("Borrar historial", primario = false) {
                Repositorio.limpiar(); mensaje.text = ""; refrescar()
            }
        }
        tarjeta("¿Cómo va el atleta?") {
            nota("Última semana frente a los valores de referencia para atletas.")
            filas = List(5) { filaRango() }
            nota("Referencias generales y orientativas; no sustituyen a un entrenador o médico.")
        }
        tarjeta("Evolución") {
            selVar = selector(Variable.values().map { it.etiqueta })
            grafica = grafica(200)
        }
        tarjeta("Historial") { lista = resultado() }

        alElegir(selVar) { dibujarGrafica() }
        refrescar()
    }
}

// ───────────────────────── MÓDULO 2 · TENDENCIA ─────────────────────────
class Modulo2Activity : PantallaBase() {
    override val nombre get() = "2 · Tendencia"
    override val subtitulo get() = "Mínimos cuadrados y R²"

    override fun construir() {
        var calcular: () -> Unit = {}
        val sig = (Repositorio.historial.ultimo()?.semana ?: 0) + 1

        val (selector, semana) = tarjeta("Variable") {
            val s = selector(Variable.values().map { it.etiqueta })
            val sem = campo("Semana a predecir", sig.toString(), Tipo.ENTERO)
            boton("Analizar") { calcular() }
            s to sem
        }
        val (grafica, m, texto) = tarjeta("Resultado") {
            val g = grafica()
            val mm = filaMetricas("Pendiente", "R²", "Tendencia")
            Triple(g, mm, resultado())
        }

        calcular = {
            val idx = selector.selectedItemPosition
            if (idx >= 0) {
                val v = Variable.values()[idx]
                try {
                    val t = MinimosCuadrados.analizar(Repositorio.historial, v)
                    val (xs, ys) = Repositorio.historial.serie(v)
                    val s = numero(semana) ?: (xs.last() + 1.0)
                    val futuros = if (s > xs.last()) listOf(s to t.predecir(s)) else emptyList()
                    val rango = Referencias.paraVariable(v)
                    grafica.datos(xs, ys, recta = t.pendiente to t.ordenada, futuros = futuros,
                        banda = rango?.let { it.min to it.max })
                    val colorR2 = if (t.r2 >= 0.8) Tema.ACENTO else if (t.r2 >= 0.5) Tema.AMBAR else Tema.ROJO
                    m[0].set(f(t.pendiente, 3)); m[1].set(f(t.r2, 2), colorR2); m[2].set(t.clase)
                    val confianza = when {
                        t.r2 >= 0.8 -> "alta: la recta describe bien los datos"
                        t.r2 >= 0.5 -> "moderada"
                        else -> "baja: los datos no siguen una línea clara"
                    }
                    var txt = "Recta: y = ${f(t.pendiente, 3)}·x + ${f(t.ordenada, 2)}\n" +
                            "Confianza de la tendencia: $confianza\n" +
                            "Predicción semana ${s.toInt()}: ${f(t.predecir(s), 2)}"
                    Referencias.valoracionTendencia(v, t.clase)?.let { txt += "\n\n$it" }
                    rango?.let { txt += "\nRango ideal: ${corto(it.min)}–${corto(it.max)} ${it.unidad}".trimEnd() }
                    texto.text = txt
                } catch (e: Exception) {
                    grafica.datos(emptyList(), emptyList())
                    m.forEach { it.set("—") }
                    texto.text = SIN_DATOS
                }
            }
        }
        alElegir(selector) { calcular() }
        calcular()
    }
}

// ───────────────────────── MÓDULO 3 · PREDICCIÓN ─────────────────────────
class Modulo3Activity : PantallaBase() {
    override val nombre get() = "3 · Predicción"
    override val subtitulo get() = "Extrapolación de Newton (1-2 semanas)"

    override fun construir() {
        var calcular: () -> Unit = {}
        val vars = listOf(Variable.VO2_MAX, Variable.FC_REPOSO)

        val (variable, pasos) = tarjeta("Qué predecir") {
            val v = selector(vars.map { it.etiqueta })
            val p = selector(listOf("1 semana adelante", "2 semanas adelante"))
            boton("Predecir") { calcular() }
            v to p
        }
        val (grafica, m, texto) = tarjeta("Resultado") {
            val g = grafica()
            val mm = filaMetricas("Último valor", "Predicción", "Cambio")
            Triple(g, mm, resultado())
        }

        calcular = {
            val idxV = variable.selectedItemPosition
            val idxP = pasos.selectedItemPosition
            if (idxV >= 0 && idxP >= 0) {
                val v = vars[idxV]
                val n = idxP + 1
                try {
                    val (xs, ys) = Repositorio.historial.serie(v)
                    if (xs.size < 3) throw IllegalArgumentException("Datos insuficientes")
                    
                    val futuros = (1..n).map { k ->
                        (xs.last() + k) to NewtonExtrapolacion.predecir(Repositorio.historial, v, k)
                    }
                    val rango = Referencias.paraVariable(v)
                    grafica.datos(xs, ys, futuros = futuros, banda = rango?.let { it.min to it.max })

                    val pred = futuros.last().second
                    val cambio = pred - ys.last()
                    m[0].set(f(ys.last(), 1)); m[1].set(f(pred, 1), Tema.ACENTO2)
                    m[2].set((if (cambio >= 0) "+" else "") + f(cambio, 1))

                    val interpretacion = when (v) {
                        Variable.FC_REPOSO -> {
                            val r = rango ?: Rango(40.0, 60.0, "lpm")
                            val dentro = r.contiene(pred)
                            "Se espera una FC en reposo de ${f(pred, 0)} lpm: " +
                                    (if (dentro) "dentro" else "fuera") + " del rango ideal (${corto(r.min)}–${corto(r.max)}).\n" +
                                    (if (cambio > 1) "Si sube, puede indicar fatiga acumulada." else "")
                        }
                        else -> if (cambio >= 0) "Se espera que tu VO2 máx se mantenga o mejore."
                        else "Se espera una baja del VO2 máx; revisa tu carga y tu descanso."
                    }
                    val tabla = NewtonExtrapolacion.tablaDiferencias(ys.takeLast(4))
                    texto.text = interpretacion.trim() + "\n\nTabla de diferencias (últimos 4 valores):\n" +
                            tabla.mapIndexed { k, fila -> "Δ$k: " + fila.joinToString("   ") { f(it, 2) } }.joinToString("\n")
                } catch (e: Exception) {
                    grafica.datos(emptyList(), emptyList())
                    m.forEach { it.set("—") }
                    texto.text = SIN_DATOS
                }
            }
        }
        alElegir(variable) { calcular() }
        alElegir(pasos) { calcular() }
        calcular()
    }
}

// ───────────────────────── MÓDULO 4 · OPTIMIZACIÓN ─────────────────────────
class Modulo4Activity : PantallaBase() {
    override val nombre get() = "4 · Sueño óptimo"
    override val subtitulo get() = "Newton-Raphson"

    override fun construir() {
        var calcular: () -> Unit = {}
        val ultimo = Repositorio.historial.ultimo()
        lateinit var grafica: GraficaLinea
        lateinit var m: List<MetricaView>
        lateinit var fila: FilaRango
        lateinit var texto: TextView

        val carga = tarjeta("Carga a recuperar") {
            nota("Horas de sueño necesarias para recuperarte de una carga dada.")
            val c = campo("Carga de entrenamiento (0-94)", ultimo?.carga?.toInt()?.toString() ?: "60")
            boton("Calcular") { calcular() }
            c
        }
        tarjeta("Resultado") {
            nota("Curva: capacidad de recuperación según las horas de sueño. El punto azul es la solución.")
            grafica = grafica()
            m = filaMetricas("Sueño necesario", "Comprobación", "Tu sueño")
            fila = filaRango()
            texto = resultado()
        }

        calcular = {
            val c = numero(carga)
            if (c == null) {
                texto.text = "Escribe un número."
            } else try {
                val h = OptimizacionSueno.suenoNecesario(c)
                val exacto = OptimizacionSueno.suenoAnalitico(c)
                val horas = (4..10).map { it.toDouble() }
                grafica.datos(horas, horas.map { OptimizacionSueno.capacidad(it) }, futuros = listOf(h to c))
                m[0].set("${f(h, 2)} h", Tema.ACENTO2)
                m[1].set("${f(exacto, 2)} h")
                m[2].set(if (ultimo != null) "${f(ultimo.suenoHoras, 1)} h" else "—")
                fila.set("Sueño necesario", h, "${f(h, 1)} h", Referencias.SUENO, 0.0, 12.0,
                    estadoTexto = "Mínimo para esta carga", color = Tema.ACENTO2,
                    leyenda = "Recomendado al entrenar: 8–10 h")
                texto.text = "Con ${f(h, 1)} h te recuperas de esta carga, pero lo recomendable para un atleta es dormir 8–10 h." +
                        (if (ultimo != null && ultimo.suenoHoras < h) "\n\nTu última semana dormiste ${f(ultimo.suenoHoras, 1)} h: te faltan ${f(h - ultimo.suenoHoras, 1)} h." else "")
            } catch (e: Exception) {
                texto.text = e.message ?: "No se pudo calcular."
            }
        }
        calcular()
    }
}

// ───────────────────────── MÓDULO 5 · TRIMP ─────────────────────────
class Modulo5Activity : PantallaBase() {
    override val nombre get() = "5 · TRIMP"
    override val subtitulo get() = "Carga de la sesión con Regla de Simpson"

    override fun construir() {
        var calcular: () -> Unit = {}
        val ultimo = Repositorio.historial.ultimo()
        lateinit var grafica: GraficaLinea
        lateinit var m: List<MetricaView>
        lateinit var fila: FilaRango
        lateinit var texto: TextView

        val campos = tarjeta("Datos de la sesión") {
            val fcs = campo("FC medida durante la sesión (lpm, separadas por coma)", "120,140,155,160,150", Tipo.TEXTO)
            val (dt, fcRep) = par("Minutos entre mediciones", "5", "FC en reposo", ultimo?.fcReposo?.toInt()?.toString() ?: "60")
            val fcMax = campo("FC máxima", ultimo?.fcMaxima?.toInt()?.toString() ?: "190")
            val hombre = casilla("Fórmula masculina (desmarcada: femenina)", true)
            boton("Calcular TRIMP") { calcular() }
            listOf(fcs, dt, fcRep, fcMax) to hombre
        }
        tarjeta("Resultado") {
            grafica = grafica(200)
            m = filaMetricas("TRIMP", "Duración", "FC media")
            fila = filaRango()
            texto = resultado()
        }

        calcular = {
            val (entradas, hombre) = campos
            val lista = entradas[0].text.toString().split(",").map { it.trim().toDoubleOrNull() }
            val dt = numero(entradas[1]); val r = numero(entradas[2]); val mx = numero(entradas[3])
            if (lista.any { it == null } || dt == null || r == null || mx == null) {
                texto.text = "Revisa los datos: todo debe ser números."
            } else try {
                val fcs = lista.map { it!! }
                val trimp = Trimp.calcular(fcs, dt, r, mx, hombre.isChecked)
                grafica.datos(fcs.indices.map { it * dt }, fcs, color = Tema.ROJO)
                m[0].set(f(trimp, 1), Tema.ACENTO)
                m[1].set("${f((fcs.size - 1) * dt, 0)} min")
                m[2].set(f(fcs.average(), 0))
                val color = when {
                    trimp < 50 -> Tema.ACENTO2
                    trimp < 100 -> Tema.ACENTO
                    trimp < 150 -> Tema.AMBAR
                    else -> Tema.ROJO
                }
                fila.set("Carga de la sesión", trimp, "${f(trimp, 0)} u", Referencias.TRIMP_MODERADA, 0.0, 200.0,
                    estadoTexto = Referencias.categoriaTrimp(trimp), color = color,
                    leyenda = "Zona moderada: 50–100 u (referencia orientativa)")
                texto.text = "Úsalo para comparar sesiones: mientras más alto, más exigente fue."
            } catch (e: IllegalArgumentException) {
                texto.text = e.message ?: "Datos insuficientes."
            }
        }
        calcular()
    }
}

// ───────────────────────── LÓGICA DIFUSA ─────────────────────────
class DifusaActivity : PantallaBase() {
    override val nombre get() = "6 · Riesgo"
    override val subtitulo get() = "Sobreentrenamiento con lógica difusa"

    override fun construir() {
        var calcular: () -> Unit = {}
        val ultimo = Repositorio.historial.ultimo()
        lateinit var filas: List<FilaRango>

        val campos = tarjeta("Entradas") {
            val (carga, sueno) = par(
                "Carga (0-100)", ultimo?.carga?.toInt()?.toString() ?: "60",
                "Sueño (h)", ultimo?.suenoHoras?.toString() ?: "7"
            )
            val delta = campo("FC en reposo sobre tu base (lpm)", f(Repositorio.deltaFc(), 1))
            boton("Evaluar riesgo") { calcular() }
            listOf(carga, sueno, delta)
        }
        val (nivel, barra, texto) = tarjeta("Resultado") {
            val n = destacado()
            val b = medidor()
            Triple(n, b, resultado())
        }
        tarjeta("Tus entradas frente a lo ideal") { filas = List(3) { filaRango() } }

        calcular = {
            val c = numero(campos[0]); val s = numero(campos[1]); val d = numero(campos[2])
            if (c == null || s == null || d == null) {
                texto.text = "Escribe números en los tres campos."
            } else {
                val r = RiesgoDifuso.evaluar(c, s, d)
                nivel.text = "${f(r, 0)} / 100 · ${RiesgoDifuso.etiqueta(r)}"
                nivel.setTextColor(Tema.colorRiesgo(r))
                barra.set(r)
                texto.text = when {
                    r < 35 -> "Puedes mantener o subir un poco la carga."
                    r < 65 -> "Vigila tu recuperación y evita subir la carga esta semana."
                    else -> "Reduce la carga y prioriza dormir."
                }
                filas[0].set("Carga", c, corto(c), Referencias.CARGA, 0.0, 100.0)
                filas[1].set("Sueño", s, "${f(s, 1)} h", Referencias.SUENO, 0.0, 12.0)
                filas[2].set("FC sobre tu base", d, "${if (d >= 0) "+" else ""}${f(d, 1)} lpm", Referencias.DELTA_FC, -10.0, 15.0)
            }
        }
        calcular()
    }
}

// ───────────────────────── IA: RED + BFS + HILL CLIMBING ─────────────────────────
class IAActivity : PantallaBase() {
    override val nombre get() = "7 · Agente"
    override val subtitulo get() = "Red neuronal, Best-First y Hill Climbing"

    override fun construir() {
        var analizar: () -> Unit = {}

        val (estadoTxt, barras, riesgoTxt) = tarjeta("Estado actual (red neuronal)") {
            val e = destacado()
            val b = List(3) { barraEtiquetada() }
            val r = resultado()
            boton("Analizar") { analizar() }
            Triple(e, b, r)
        }
        val (m, pasosTxt) = tarjeta("Recomendación (Best-First Search)") {
            val mm = filaMetricas("Carga", "Sueño")
            mm to resultado()
        }

        lateinit var estadoMuestras: TextView
        fun mostrarPesos() {
            val p = Repositorio.pesos
            estadoMuestras.text = "Muestras: ${Repositorio.muestras.size}\n" +
                    "Pesos → estímulo ${f(p.estimulo, 2)} · recuperación ${f(p.recuperacion, 2)} · riesgo ${f(p.riesgo, 2)}"
        }
        tarjeta("Personalizar (Hill Climbing)") {
            nota("Califica cómo te sentiste la última semana. Con 3 o más muestras se ajustan los pesos de la utilidad a ti.")
            val bienestar = campo("Bienestar (0 muy mal - 10 excelente)", "7")
            estadoMuestras = resultado()
            boton("Guardar muestra de la última semana", primario = false) {
                val u = Repositorio.historial.ultimo()
                val b = numero(bienestar)
                if (u == null || b == null || b < 0 || b > 10) {
                    estadoMuestras.text = "Captura datos en el Módulo 1 y escribe un bienestar de 0 a 10."
                } else {
                    Repositorio.muestras.add(Muestra(Estado(u.carga, u.suenoHoras), Repositorio.deltaFc(), b / 10.0))
                    mostrarPesos()
                }
            }
            boton("Cargar muestras de ejemplo", primario = false) { Repositorio.cargarMuestrasEjemplo(); mostrarPesos() }
            boton("Ajustar pesos (Hill Climbing)") {
                if (Repositorio.muestras.size < 3) {
                    estadoMuestras.text = "Necesitas al menos 3 muestras."
                } else {
                    Repositorio.pesos = HillClimbing.ajustar(Repositorio.muestras, Repositorio.pesos)
                    mostrarPesos()
                    analizar()
                }
            }
        }

        analizar = {
            val u = Repositorio.historial.ultimo()
            if (u == null) {
                estadoTxt.text = "Sin datos"; estadoTxt.setTextColor(Tema.TEXTO2)
                riesgoTxt.text = "Captura datos en el Módulo 1 (o carga el ejemplo)."
                barras.forEach { (t, b) -> t.text = ""; b.set(0.0, Tema.BORDE) }
                m.forEach { it.set("—") }
                pasosTxt.text = ""
            } else {
                val delta = Repositorio.deltaFc()
                val red = PesosRed.red
                val (est, prob) = red.clasificar(red.caracteristicas(delta, u.suenoHoras, u.carga))
                val nombres = listOf("Recuperado", "Carga óptima", "Riesgo de sobreentrenamiento")
                val colores = listOf(Tema.ACENTO, Tema.ACENTO2, Tema.ROJO)
                estadoTxt.text = nombres[est.ordinal]; estadoTxt.setTextColor(colores[est.ordinal])
                barras.forEachIndexed { i, (t, b) ->
                    t.text = "${nombres[i]} · ${f(prob[i] * 100, 0)}%"
                    b.set(prob[i], colores[i])
                }
                val riesgo = RiesgoDifuso.evaluar(u.carga, u.suenoHoras, delta)
                riesgoTxt.text = "Riesgo difuso: ${f(riesgo, 0)}/100 · FC en reposo ${if (delta >= 0) "+" else ""}${f(delta, 1)} lpm vs tu base"

                val rec = RecomendadorBFS().recomendar(Estado(u.carga, u.suenoHoras), delta, Repositorio.pesos)
                m[0].set("${u.carga.toInt()} → ${rec.estado.carga.toInt()}", Tema.ACENTO2)
                m[1].set("${f(u.suenoHoras, 1)} → ${f(rec.estado.sueno, 1)} h", Tema.ACENTO2)
                val resumen = rec.pasos.groupingBy { it }.eachCount()
                    .entries.joinToString("\n") { "• ${it.key}" + if (it.value > 1) " ×${it.value}" else "" }
                pasosTxt.text = if (resumen.isEmpty()) "Mantén tu plan actual." else resumen
            }
        }
        mostrarPesos()
        analizar()
    }
}
