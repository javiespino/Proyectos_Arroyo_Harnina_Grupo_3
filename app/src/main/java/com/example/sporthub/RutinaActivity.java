package com.example.sporthub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RutinaActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private Spinner spinnerDias, spinnerEjercicios;
    private MaterialButton btnAnadirEjercicio, btnExportarPdf;
    private ImageButton btnBack;
    private RecyclerView recyclerEjercicios;

    private ArrayList<DiaRutina> diasRutina;
    private ArrayList<Object> itemsRecycler;
    private RutinaAdapter rutinaAdapter;

    private DBHelper db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutina);

        // 1. Inicializar Vistas
        btnBack = findViewById(R.id.btnBack);
        spinnerDias = findViewById(R.id.spinnerDias);
        spinnerEjercicios = findViewById(R.id.spinnerEjercicios);
        btnAnadirEjercicio = findViewById(R.id.btnAnadirEjercicio);
        btnExportarPdf = findViewById(R.id.btnExportarPdf);
        recyclerEjercicios = findViewById(R.id.recyclerEjercicios);

        // 2. Firebase y DB
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = new DBHelper(this);

        // 3. Configurar Botón Atrás
        btnBack.setOnClickListener(v -> finish());

        // 4. Configurar Spinners
        configurarSpinners();

        // 5. Configurar RecyclerView
        diasRutina = db.obtenerRutinaCompleta(uid);
        itemsRecycler = new ArrayList<>();
        actualizarItemsRecycler();

        rutinaAdapter = new RutinaAdapter(itemsRecycler);
        recyclerEjercicios.setLayoutManager(new LinearLayoutManager(this));
        recyclerEjercicios.setAdapter(rutinaAdapter);

        // 6. Eventos
        btnAnadirEjercicio.setOnClickListener(v -> añadirEjercicio());

        btnExportarPdf.setOnClickListener(v -> {
            if (tieneEjercicios()) {
                if (checkStoragePermission()) exportarRutinaPdf();
                else requestStoragePermission();
            } else {
                Toast.makeText(this, "Añade ejercicios primero", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarSpinners() {
        // Días
        ArrayAdapter<CharSequence> adapterDias = ArrayAdapter.createFromResource(this,
                R.array.dias_semana, android.R.layout.simple_spinner_item);
        adapterDias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDias.setAdapter(adapterDias);

        // Ejercicios predefinidos con sus grupos musculares
        ArrayList<Ejercicio> listaBase = new ArrayList<>();
        listaBase.add(new Ejercicio("Press de banca", "Pecho"));
        listaBase.add(new Ejercicio("Flexiones", "Pecho"));
        listaBase.add(new Ejercicio("Dominadas", "Espalda"));
        listaBase.add(new Ejercicio("Remo con barra", "Espalda"));
        listaBase.add(new Ejercicio("Sentadillas", "Pierna"));
        listaBase.add(new Ejercicio("Zancadas", "Pierna"));
        listaBase.add(new Ejercicio("Curl bíceps", "Brazo"));
        listaBase.add(new Ejercicio("Fondos tríceps", "Brazo"));

        ArrayAdapter<Ejercicio> adapterEj = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaBase);
        adapterEj.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEjercicios.setAdapter(adapterEj);
    }

    private void añadirEjercicio() {
        String dia = spinnerDias.getSelectedItem().toString().toLowerCase();
        Ejercicio ej = (Ejercicio) spinnerEjercicios.getSelectedItem();

        db.insertarEjercicio(uid, dia, ej.getNombre(), ej.getGrupoMuscular());

        // Refrescar interfaz
        diasRutina = db.obtenerRutinaCompleta(uid);
        actualizarItemsRecycler();
        rutinaAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Añadido: " + ej.getNombre(), Toast.LENGTH_SHORT).show();
    }

    private void actualizarItemsRecycler() {
        itemsRecycler.clear();
        for (DiaRutina dia : diasRutina) {
            itemsRecycler.add(dia); // Objeto tipo Dia (Header)
            itemsRecycler.addAll(dia.getEjercicios()); // Objetos tipo Ejercicio
        }
    }

    private boolean tieneEjercicios() {
        for (DiaRutina dia : diasRutina) {
            if (!dia.getEjercicios().isEmpty()) return true;
        }
        return false;
    }

    private void exportarRutinaPdf() {
        new PdfExporter(this, diasRutina).exportarPdf();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
    }
}