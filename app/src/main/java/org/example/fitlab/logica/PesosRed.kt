package org.example.fitlab.logica

// Pesos entrenados con entrenar_red.py (MLPClassifier, 3 entradas -> 6 ocultas -> 3 clases).
// Si cambias el entrenamiento, vuelve a correr el script y pega los nuevos arreglos.
object PesosRed {
    val w1 = arrayOf(
        doubleArrayOf(-0.150070, -0.764116, 0.483937),
        doubleArrayOf(0.746084, 0.380727, -1.118038),
        doubleArrayOf(-1.599276, 3.051968, -3.045276),
        doubleArrayOf(-0.000000, 0.000000, -0.000000),
        doubleArrayOf(-0.000000, -0.000000, -0.000000),
        doubleArrayOf(1.674099, -0.950306, 3.117667)
    )
    val b1 = doubleArrayOf(-0.165237, -0.543737, 1.270075, -0.588704, -0.339426, 0.751199)
    val w2 = arrayOf(
        doubleArrayOf(-0.068600, -0.798457, 3.160436, 0.000000, -0.000000, -2.976573),
        doubleArrayOf(0.373917, 1.227399, 0.382733, 0.000000, -0.000000, 0.056720),
        doubleArrayOf(-0.406473, -1.465508, -4.070156, 0.000000, 0.000000, 1.968231)
    )
    val b2 = doubleArrayOf(-2.137587, 1.127837, -0.057357)

    val red = RedNeuronal(w1, b1, w2, b2)
}