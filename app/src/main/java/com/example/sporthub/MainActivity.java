package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configurarMargenes();
        configurarBotones();
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void configurarBotones() {
        Button btnDetalles = findViewById(R.id.btnDetalles);
        Button btnVerCalendario = findViewById(R.id.btnVerCalendario);
        CardView cardRutina = findViewById(R.id.cardRutina);
        CardView cardReservar = findViewById(R.id.cardReservar);
        FloatingActionButton fab = findViewById(R.id.fab);

        btnDetalles.setOnClickListener(v -> {
            startActivity(new Intent(this, DetallesActivity.class));
        });

        btnVerCalendario.setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarioActivity.class));
        });

        cardRutina.setOnClickListener(v -> {
            startActivity(new Intent(this, RutinaActivity.class));
        });

        cardReservar.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo Reservas...", Toast.LENGTH_SHORT).show();
        });

        fab.setOnClickListener(v -> {
            Toast.makeText(this, "Hola Carlos, ¿en qué puedo ayudarte?", Toast.LENGTH_SHORT).show();
        });
    }
}