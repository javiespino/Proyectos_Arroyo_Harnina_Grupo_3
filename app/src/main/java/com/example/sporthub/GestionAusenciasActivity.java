package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GestionAusenciasActivity extends AppCompatActivity
        implements ClaseEntrenadorAdapter.OnClaseActionListener {

    private ImageButton btnBack;
    private RecyclerView recyclerClases;
    private TextView tvSinClases;
    private ProgressBar progressBar;
    private ChipGroup chipGroupFiltro;

    private List<ClaseEntrenador> todasClases    = new ArrayList<>();
    private List<ClaseEntrenador> clasesMostradas = new ArrayList<>();
    private ClaseEntrenadorAdapter adapter;

    private List<String> nombresEntrenadores = new ArrayList<>();
    private List<String> uidsEntrenadores    = new ArrayList<>();

    private FirebaseFirestore db;
    private String uidActual;

    private String filtroActual = "todas";

    private static final SimpleDateFormat SDF_HOY =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_ausencias);

        db = FirebaseFirestore.getInstance();
        uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();

        inicializarVistas();
        configurarRecycler();
        configurarFiltros();

        cargarEntrenadores(() -> cargarClases());

        btnBack.setOnClickListener(v -> finish());
    }

    private void inicializarVistas() {
        btnBack         = findViewById(R.id.btnBackAusencias);
        recyclerClases  = findViewById(R.id.recyclerClasesEntrenador);
        tvSinClases     = findViewById(R.id.tvSinClasesEntrenador);
        progressBar     = findViewById(R.id.progressBarAusencias);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltroAusencias);
    }

    private void configurarRecycler() {
        adapter = new ClaseEntrenadorAdapter(clasesMostradas, this);
        recyclerClases.setLayoutManager(new LinearLayoutManager(this));
        recyclerClases.setAdapter(adapter);
    }

    private void configurarFiltros() {
        chipGroupFiltro.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipTodasAusencias)      filtroActual = "todas";
            else if (id == R.id.chipActivasAusencias)    filtroActual = "activas";
            else if (id == R.id.chipCanceladasAusencias) filtroActual = "canceladas";
            aplicarFiltro();
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar entrenadores
    // ══════════════════════════════════════════════════════════════

    private void cargarEntrenadores(Runnable onFinalizado) {
        // CORRECCIÓN: siempre inicializamos la lista con "Sin sustituto"
        // antes de la llamada a Firestore, así aunque falle el permiso
        // la lista nunca queda vacía y el spinner no devuelve -1.
        nombresEntrenadores.clear();
        uidsEntrenadores.clear();
        nombresEntrenadores.add("Sin sustituto");
        uidsEntrenadores.add("");

        db.collection("users")
                .whereEqualTo("rol", "e")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("name");
                        String uid    = doc.getId();
                        if (!uid.equals(uidActual) && nombre != null) {
                            nombresEntrenadores.add(nombre);
                            uidsEntrenadores.add(uid);
                        }
                    }
                    onFinalizado.run();
                })
                .addOnFailureListener(e -> {
                    // CORRECCIÓN: en caso de error de permisos ya tenemos
                    // "Sin sustituto" en la lista, así que no petará.
                    // Mostramos un aviso informativo pero seguimos adelante.
                    Toast.makeText(this,
                            "No se pudieron cargar otros entrenadores. " +
                                    "Solo podrás cancelar sin sustituto.",
                            Toast.LENGTH_LONG).show();
                    onFinalizado.run();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar clases
    // ══════════════════════════════════════════════════════════════

    private void cargarClases() {
        progressBar.setVisibility(View.VISIBLE);
        String hoy = SDF_HOY.format(new Date());

        db.collection("clases")
                .get()
                .addOnSuccessListener(query -> {
                    todasClases.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String fecha           = doc.getString("fecha");
                        String hora            = doc.getString("hora");
                        String nombreActividad = doc.getString("nombreActividad");
                        Boolean cancelada      = doc.getBoolean("cancelada");
                        String sustituto       = doc.getString("entrenadorSustituto");
                        String docId           = doc.getId();

                        if (fecha != null && esFechaFuturaOHoy(fecha, hoy)) {
                            ClaseEntrenador clase = new ClaseEntrenador(
                                    docId,
                                    nombreActividad != null ? nombreActividad : "",
                                    fecha,
                                    hora != null ? hora : "",
                                    cancelada != null && cancelada,
                                    sustituto != null ? sustituto : ""
                            );
                            todasClases.add(clase);
                        }
                    }

                    todasClases.sort((a, b) -> {
                        String ka = a.getFecha() + "_" + a.getHora();
                        String kb = b.getFecha() + "_" + b.getHora();
                        return ka.compareTo(kb);
                    });

                    progressBar.setVisibility(View.GONE);
                    aplicarFiltro();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar clases", Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Filtrado
    // ══════════════════════════════════════════════════════════════

    private void aplicarFiltro() {
        clasesMostradas.clear();
        for (ClaseEntrenador c : todasClases) {
            switch (filtroActual) {
                case "activas":
                    if (!c.isCancelada()) clasesMostradas.add(c);
                    break;
                case "canceladas":
                    if (c.isCancelada()) clasesMostradas.add(c);
                    break;
                default:
                    clasesMostradas.add(c);
            }
        }
        adapter.actualizarLista(new ArrayList<>(clasesMostradas));
        tvSinClases.setVisibility(clasesMostradas.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerClases.setVisibility(clasesMostradas.isEmpty() ? View.GONE  : View.VISIBLE);
    }

    // ══════════════════════════════════════════════════════════════
    // Callback adapter — marcar ausencia
    // ══════════════════════════════════════════════════════════════

    @Override
    public void onMarcarAusencia(ClaseEntrenador clase, int position) {
        if (clase.isCancelada()) {
            Toast.makeText(this, "Esta clase ya está cancelada", Toast.LENGTH_SHORT).show();
            return;
        }

        // CORRECCIÓN: garantizamos que la lista nunca esté vacía antes de mostrar el diálogo
        if (nombresEntrenadores.isEmpty()) {
            nombresEntrenadores.add("Sin sustituto");
            uidsEntrenadores.add("");
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ausencia, null);
        Spinner spinnerSustituto = dialogView.findViewById(R.id.spinnerSustituto);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, nombresEntrenadores);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSustituto.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Marcar ausencia")
                .setMessage("¿Confirmas que no podrás asistir a " +
                        clase.getNombreActividad() + " el " + clase.getFecha() +
                        " a las " + clase.getHora() + "?")
                .setView(dialogView)
                .setPositiveButton("Confirmar ausencia", (dialog, which) -> {
                    int idx = spinnerSustituto.getSelectedItemPosition();
                    // CORRECCIÓN: comprobación defensiva por si idx fuera -1
                    if (idx < 0 || idx >= nombresEntrenadores.size()) {
                        Toast.makeText(this, "Selecciona una opción", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String nombreSustituto = nombresEntrenadores.get(idx);
                    guardarAusenciaEnFirestore(clase, position, nombreSustituto);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarAusenciaEnFirestore(ClaseEntrenador clase,
                                            int position,
                                            String nombreSustituto) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("cancelada",           true);
        updates.put("entrenadorSustituto", nombreSustituto);

        db.collection("clases").document(clase.getDocId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    clase.setCancelada(true);
                    clase.setEntrenadorSustituto(nombreSustituto);
                    adapter.notifyItemChanged(position);
                    aplicarFiltro();

                    String msg = "Sin sustituto".equals(nombreSustituto)
                            ? "Clase cancelada sin sustituto"
                            : "Clase cancelada · Sustituto: " + nombreSustituto;
                    Toast.makeText(this, "✅ " + msg, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al guardar ausencia", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onReactivar(ClaseEntrenador clase, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Reactivar clase")
                .setMessage("¿Quieres reactivar la clase " + clase.getNombreActividad() +
                        " del " + clase.getFecha() + "?")
                .setPositiveButton("Reactivar", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("cancelada",           false);
                    updates.put("entrenadorSustituto", "");

                    db.collection("clases").document(clase.getDocId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                clase.setCancelada(false);
                                clase.setEntrenadorSustituto("");
                                adapter.notifyItemChanged(position);
                                aplicarFiltro();
                                Toast.makeText(this, "✅ Clase reactivada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "❌ Error al reactivar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean esFechaFuturaOHoy(String fechaClase, String hoy) {
        try {
            Date dClase = SDF_HOY.parse(fechaClase);
            Date dHoy   = SDF_HOY.parse(hoy);
            return dClase != null && !dClase.before(dHoy);
        } catch (Exception e) {
            return false;
        }
    }
}