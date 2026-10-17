"""Entrena la red en Python y imprime los pesos listos para pegar en Kotlin (IA.kt)."""
import numpy as np
from sklearn.neural_network import MLPClassifier

rng = np.random.default_rng(42)
n = 1500
delta = rng.uniform(-5, 20, n)     # FC reposo actual - FC base
sueno = rng.uniform(4, 10, n)
carga = rng.uniform(0, 100, n)
X = np.c_[delta / 20, sueno / 10, carga / 100]   # misma normalización que en Kotlin

# Etiquetas SINTÉTICAS (no hay datos reales etiquetados): 0 recuperado, 1 carga óptima, 2 riesgo
estres = (carga / 100) * 0.5 + np.clip(delta, 0, 20) / 20 * 0.3 \
         + np.clip(8 - sueno, 0, 4) / 4 * 0.2 + rng.normal(0, 0.03, n)
y = np.where(estres < 0.25, 0, np.where(estres < 0.5, 1, 2))

red = MLPClassifier(hidden_layer_sizes=(6,), activation="relu", max_iter=2000, random_state=42)
red.fit(X, y)
print("Exactitud:", red.score(X, y))

def arr1(v): return "doubleArrayOf(" + ", ".join(f"{x:.6f}" for x in v) + ")"
def arr2(m): return "arrayOf(\n    " + ",\n    ".join(arr1(f) for f in m) + "\n)"

print("val w1 = " + arr2(red.coefs_[0].T))   # (ocultas x entradas)
print("val b1 = " + arr1(red.intercepts_[0]))
print("val w2 = " + arr2(red.coefs_[1].T))   # (salidas x ocultas)
print("val b2 = " + arr1(red.intercepts_[1]))