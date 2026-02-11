package com.example.sporthub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RutinaActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private Spinner spinnerDias, spinnerEjercicios;
    private Button btnAnadirEjercicio, btnExportarPdf;
    private RecyclerView recyclerEjercicios;

    private ArrayList<DiaRutina> diasRutina;
    private ArrayList<Object> itemsRecycler; // Mezcla de DiaRutina y Ejercicios
    private RutinaAdapter rutinaAdapter;

    private DBHelper db;
    private String uid; // UID del usuario de Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutina);

        spinnerDias = findViewById(R.id.spinnerDias);
        spinnerEjercicios = findViewById(R.id.spinnerEjercicios);
        btnAnadirEjercicio = findViewById(R.id.btnAnadirEjercicio);
        btnExportarPdf = findViewById(R.id.btnExportarPdf);
        recyclerEjercicios = findViewById(R.id.recyclerEjercicios);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = new DBHelper(this);

        // Spinner días
        ArrayAdapter<CharSequence> adapterDias = ArrayAdapter.createFromResource(this,
                R.array.dias_semana, android.R.layout.simple_spinner_item);
        adapterDias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDias.setAdapter(adapterDias);

        // Spinner ejercicios
        ArrayList<Ejercicio> spinnerEjerciciosList = new ArrayList<>();
        spinnerEjerciciosList.add(new Ejercicio("Press de banca", "Pecho"));
        spinnerEjerciciosList.add(new Ejercicio("Flexiones", "Pecho"));
        spinnerEjerciciosList.add(new Ejercicio("Dominadas", "Espalda"));
        spinnerEjerciciosList.add(new Ejercicio("Remo con barra", "Espalda"));
        spinnerEjerciciosList.add(new Ejercicio("Sentadillas", "Pierna"));
        spinnerEjerciciosList.add(new Ejercicio("Zancadas", "Pierna"));
        spinnerEjerciciosList.add(new Ejercicio("Curl bíceps", "Brazo"));
        spinnerEjerciciosList.add(new Ejercicio("Fondos tríceps", "Brazo"));

        ArrayAdapter<Ejercicio> adapterEjercicios = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerEjerciciosList);
        adapterEjercicios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEjercicios.setAdapter(adapterEjercicios);

        // RecyclerView
        diasRutina = db.obtenerRutinaCompleta(uid);
        itemsRecycler = new ArrayList<>();
        actualizarItemsRecycler();

        rutinaAdapter = new RutinaAdapter(itemsRecycler);
        recyclerEjercicios.setLayoutManager(new LinearLayoutManager(this));
        recyclerEjercicios.setAdapter(rutinaAdapter);

        // Botón añadir ejercicio
        btnAnadirEjercicio.setOnClickListener(v -> {
            String diaSeleccionado = spinnerDias.getSelectedItem().toString().toLowerCase();
            Ejercicio ejercicioSeleccionado = (Ejercicio) spinnerEjercicios.getSelectedItem();

            // Guardar en SQLite solo para este usuario
            db.insertarEjercicio(uid, diaSeleccionado,
                    ejercicioSeleccionado.getNombre(),
                    ejercicioSeleccionado.getGrupoMuscular());

            // Actualizar lista en memoria
            diasRutina = db.obtenerRutinaCompleta(uid);
            actualizarItemsRecycler();
            rutinaAdapter.notifyDataSetChanged();

            Toast.makeText(RutinaActivity.this,
                    ejercicioSeleccionado.getNombre() + " añadido para " + diaSeleccionado,
                    Toast.LENGTH_SHORT).show();
        });

        // Botón exportar PDF
        btnExportarPdf.setOnClickListener(v -> {
            if (diasRutina.isEmpty() || !tieneEjercicios()) {
                Toast.makeText(RutinaActivity.this,
                        "No hay ejercicios para exportar. Añade algunos primero.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar permisos de almacenamiento
            if (checkStoragePermission()) {
                exportarRutinaPdf();
            } else {
                requestStoragePermission();
            }
        });
    }

    // Convierte la lista de DiasRutina en items mezclados (header + ejercicios)
    private void actualizarItemsRecycler() {
        itemsRecycler.clear();
        for (DiaRutina dia : diasRutina) {
            itemsRecycler.add(dia); // header
            itemsRecycler.addAll(dia.getEjercicios()); // ejercicios del día
        }
    }

    // Verificar si hay al menos un ejercicio en la rutina
    private boolean tieneEjercicios() {
        for (DiaRutina dia : diasRutina) {
            if (!dia.getEjercicios().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Exportar la rutina a PDF
    private void exportarRutinaPdf() {
        PdfExporter pdfExporter = new PdfExporter(this, diasRutina);
        pdfExporter.exportarPdf();
    }

    // Verificar permisos de almacenamiento
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ no necesita WRITE_EXTERNAL_STORAGE para la carpeta Downloads
            return true;
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Solicitar permisos de almacenamiento
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }

    // Manejar la respuesta de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportarRutinaPdf();
            } else {
                Toast.makeText(this,
                        "Se necesita permiso de almacenamiento para exportar el PDF",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}