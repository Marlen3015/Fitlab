package org.example.fitlab;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Menú principal: un botón por módulo. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        col.setPadding(p, p, p, p);
        scroll.addView(col);
        setContentView(scroll);

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        TextView titulo = new TextView(this);
        titulo.setText("Fitlab");
        titulo.setTextSize(28f);
        titulo.setTypeface(titulo.getTypeface(), Typeface.BOLD);
        col.addView(titulo);

        TextView sub = new TextView(this);
        sub.setText("Análisis deportivo para atletas");
        sub.setPadding(0, 0, 0, p);
        col.addView(sub);

        agregar(col, "1 · Captura de variables", Modulo1Activity.class);
        agregar(col, "2 · Tendencia (mínimos cuadrados)", Modulo2Activity.class);
        agregar(col, "3 · Predicción (Newton)", Modulo3Activity.class);
        agregar(col, "4 · Optimización (Newton-Raphson)", Modulo4Activity.class);
        agregar(col, "5 · TRIMP (Simpson)", Modulo5Activity.class);
        agregar(col, "6 · Riesgo (lógica difusa)", DifusaActivity.class);
        agregar(col, "7 · Agente inteligente (red + búsqueda)", IAActivity.class);
    }

    private void agregar(LinearLayout col, String texto, Class<?> destino) {
        Button b = new Button(this);
        b.setText(texto);
        b.setOnClickListener(v -> startActivity(new Intent(this, destino)));
        col.addView(b);
    }
}