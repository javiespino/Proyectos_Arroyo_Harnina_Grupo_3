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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RutinaActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private Spinner spinnerDias, spinnerEjercicios;
    private MaterialButton btnAnadirEjercicio, btnExportarPdf;
    private ImageButton btnBack;
    private RecyclerView recyclerEjercicios;

    // ── RF 3.10 · filtro ──────────────────────────────────────────
    private ChipGroup chipGroupFiltroRutina;
    private String filtroActual = "todos"; // "todos","pecho","espalda","pierna","brazo"
    // ──────────────────────────────────────────────────────────────

    private ArrayList<DiaRutina> diasRutina;      // datos completos de SQLite
    private ArrayList<Object>   itemsRecycler;    // lo que ve el RecyclerView
    private RutinaAdapter rutinaAdapter;

    private DBHelper db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutina);

        // 1. Inicializar Vistas
        btnBack              = findViewById(R.id.btnBack);
        spinnerDias          = findViewById(R.id.spinnerDias);
        spinnerEjercicios    = findViewById(R.id.spinnerEjercicios);
        btnAnadirEjercicio   = findViewById(R.id.btnAnadirEjercicio);
        btnExportarPdf       = findViewById(R.id.btnExportarPdf);
        recyclerEjercicios   = findViewById(R.id.recyclerEjercicios);
        chipGroupFiltroRutina = findViewById(R.id.chipGroupFiltroRutina); // RF 3.10

        // 2. Firebase y DB
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db  = new DBHelper(this);

        // 3. Botón Atrás
        btnBack.setOnClickListener(v -> finish());

        // 4. Spinners
        configurarSpinners();

        // 5. RecyclerView
        diasRutina    = db.obtenerRutinaCompleta(uid);
        itemsRecycler = new ArrayList<>();
        actualizarItemsRecycler();

        rutinaAdapter = new RutinaAdapter(itemsRecycler);
        recyclerEjercicios.setLayoutManager(new LinearLayoutManager(this));
        recyclerEjercicios.setAdapter(rutinaAdapter);

        // 6. RF 3.10 · Configurar chips de filtro
        configurarFiltros();

        // 7. Eventos
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

    // ══════════════════════════════════════════════════════════════
    // RF 3.10 · Configurar chips y lógica de filtrado
    // ══════════════════════════════════════════════════════════════

    private void configurarFiltros() {
        chipGroupFiltroRutina.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if      (id == R.id.chipRutinaTodos)   filtroActual = "todos";
            else if (id == R.id.chipRutinaPecho)   filtroActual = "pecho";
            else if (id == R.id.chipRutinaEspalda) filtroActual = "espalda";
            else if (id == R.id.chipRutinaPierna)  filtroActual = "pierna";
            else if (id == R.id.chipRutinaBrazo)   filtroActual = "brazo";

            aplicarFiltro();
        });
    }

    /**
     * Aplica el filtro actual sobre diasRutina y recarga el RecyclerView.
     *
     * - "todos"   → muestra todos los días y ejercicios (comportamiento original)
     * - cualquier grupo → muestra solo los días que tengan al menos un ejercicio
     *   de ese grupo, y dentro de cada día solo los ejercicios que coincidan.
     */
    private void aplicarFiltro() {
        itemsRecycler.clear();

        if ("todos".equals(filtroActual)) {
            // Sin filtro: comportamiento original
            for (DiaRutina dia : diasRutina) {
                itemsRecycler.add(dia);
                itemsRecycler.addAll(dia.getEjercicios());
            }
        } else {
            // Con filtro: solo días que tengan ejercicios del grupo seleccionado
            for (DiaRutina dia : diasRutina) {
                ArrayList<Ejercicio> ejerciciosFiltrados = new ArrayList<>();
                for (Ejercicio e : dia.getEjercicios()) {
                    if (filtroActual.equalsIgnoreCase(e.getGrupoMuscular().trim())) {
                        ejerciciosFiltrados.add(e);
                    }
                }
                if (!ejerciciosFiltrados.isEmpty()) {
                    itemsRecycler.add(dia);
                    itemsRecycler.addAll(ejerciciosFiltrados);
                }
            }
        }

        rutinaAdapter.notifyDataSetChanged();
    }

    // ══════════════════════════════════════════════════════════════
    // Resto de métodos sin cambios
    // ══════════════════════════════════════════════════════════════

    private void configurarSpinners() {
        // Días
        ArrayAdapter<CharSequence> adapterDias = ArrayAdapter.createFromResource(this,
                R.array.dias_semana, android.R.layout.simple_spinner_item);
        adapterDias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDias.setAdapter(adapterDias);

        // Ejercicios predefinidos con sus grupos musculares
        ArrayList<Ejercicio> listaBase = new ArrayList<>();
        listaBase.add(new Ejercicio("Press de banca",  "Pecho"));
        listaBase.add(new Ejercicio("Flexiones",       "Pecho"));
        listaBase.add(new Ejercicio("Dominadas",       "Espalda"));
        listaBase.add(new Ejercicio("Remo con barra",  "Espalda"));
        listaBase.add(new Ejercicio("Sentadillas",     "Pierna"));
        listaBase.add(new Ejercicio("Zancadas",        "Pierna"));
        listaBase.add(new Ejercicio("Curl bíceps",     "Brazo"));
        listaBase.add(new Ejercicio("Fondos tríceps",  "Brazo"));

        ArrayAdapter<Ejercicio> adapterEj = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaBase);
        adapterEj.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEjercicios.setAdapter(adapterEj);
    }

    private void añadirEjercicio() {
        String dia = spinnerDias.getSelectedItem().toString().toLowerCase();
        Ejercicio ej = (Ejercicio) spinnerEjercicios.getSelectedItem();

        db.insertarEjercicio(uid, dia, ej.getNombre(), ej.getGrupoMuscular());

        // Refrescar datos desde SQLite y volver a aplicar el filtro activo
        diasRutina = db.obtenerRutinaCompleta(uid);
        aplicarFiltro();

        Toast.makeText(this, "Añadido: " + ej.getNombre(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Recarga itemsRecycler sin filtro (usado en onCreate).
     * Después de configurarFiltros() siempre se usa aplicarFiltro().
     */
    private void actualizarItemsRecycler() {
        itemsRecycler.clear();
        for (DiaRutina dia : diasRutina) {
            itemsRecycler.add(dia);
            itemsRecycler.addAll(dia.getEjercicios());
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
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }
}