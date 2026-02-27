package com.example.sporthub;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LesionesActivity extends AppCompatActivity implements LesionAdapter.OnLesionActionListener {

    private static final String TAG = "LesionesActivity";

    // Vistas
    private ImageButton btnBack;
    private RecyclerView recyclerLesiones;
    private MaterialButton btnAnadirLesion;
    private TextView tvSinLesiones;
    private ChipGroup chipGroupFiltro;

    // Datos
    private List<Lesion> todasLesiones = new ArrayList<>();
    private List<Lesion> listaMostrada = new ArrayList<>();
    private LesionAdapter adapter;

    // Firebase
    private FirebaseFirestore db;
    private String uid;

    // Filtro actual: "todas", "activas", "recuperadas"
    private String filtroActual = "todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesiones);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        inicializarVistas();
        configurarRecycler();
        configurarFiltros();
        cargarLesiones();

        btnBack.setOnClickListener(v -> finish());
        btnAnadirLesion.setOnClickListener(v -> mostrarDialogoAnadirLesion());
    }

    private void inicializarVistas() {
        btnBack = findViewById(R.id.btnBackLesiones);
        recyclerLesiones = findViewById(R.id.recyclerLesiones);
        btnAnadirLesion = findViewById(R.id.btnAnadirLesion);
        tvSinLesiones = findViewById(R.id.tvSinLesiones);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltro);
    }

    private void configurarRecycler() {
        adapter = new LesionAdapter(listaMostrada, this);
        recyclerLesiones.setLayoutManager(new LinearLayoutManager(this));
        recyclerLesiones.setAdapter(adapter);
    }

    private void configurarFiltros() {
        chipGroupFiltro.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTodas) filtroActual = "todas";
            else if (id == R.id.chipActivas) filtroActual = "activas";
            else if (id == R.id.chipRecuperadas) filtroActual = "recuperadas";
            aplicarFiltro();
        });
    }

    private void cargarLesiones() {
        db.collection("lesiones")
                .whereEqualTo("usuarioId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    todasLesiones.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Lesion lesion = doc.toObject(Lesion.class);
                        lesion.setId(doc.getId());
                        todasLesiones.add(lesion);
                    }
                    aplicarFiltro();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar lesiones", Toast.LENGTH_SHORT).show());
    }

    private void aplicarFiltro() {
        listaMostrada.clear();
        for (Lesion l : todasLesiones) {
            switch (filtroActual) {
                case "activas":
                    if (!l.isRecuperado()) listaMostrada.add(l);
                    break;
                case "recuperadas":
                    if (l.isRecuperado()) listaMostrada.add(l);
                    break;
                default:
                    listaMostrada.add(l);
            }
        }
        adapter.actualizarLista(new ArrayList<>(listaMostrada));
        tvSinLesiones.setVisibility(listaMostrada.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerLesiones.setVisibility(listaMostrada.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void mostrarDialogoAnadirLesion() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nueva_lesion, null);

        Spinner spinnerZona = dialogView.findViewById(R.id.spinnerZonaLesion);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcionLesion);
        TextView tvFechaInicio = dialogView.findViewById(R.id.tvFechaInicioLesion);
        TextView tvFechaFin = dialogView.findViewById(R.id.tvFechaFinLesion);

        // Spinner zonas del cuerpo
        String[] zonas = {"Hombro", "Codo", "Muñeca", "Espalda baja", "Espalda alta",
                "Rodilla", "Tobillo", "Cadera", "Cuello", "Otro"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, zonas);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZona.setAdapter(spinnerAdapter);

        // DatePickers
        final String[] fechaInicio = {""};
        final String[] fechaFin = {""};

        tvFechaInicio.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                fechaInicio[0] = String.format("%02d/%02d/%04d", d, m + 1, y);
                tvFechaInicio.setText(fechaInicio[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvFechaFin.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                fechaFin[0] = String.format("%02d/%02d/%04d", d, m + 1, y);
                tvFechaFin.setText(fechaFin[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Nueva lesión")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String zona = spinnerZona.getSelectedItem().toString();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (fechaInicio[0].isEmpty()) {
                        Toast.makeText(this, "Selecciona la fecha de inicio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    guardarLesion(zona, descripcion, fechaInicio[0], fechaFin[0]);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarLesion(String zona, String descripcion, String fechaInicio, String fechaFin) {
        Map<String, Object> data = new HashMap<>();
        data.put("usuarioId", uid);
        data.put("zona", zona);
        data.put("descripcion", descripcion);
        data.put("fechaInicio", fechaInicio);
        data.put("fechaFin", fechaFin);
        data.put("recuperado", false);

        db.collection("lesiones").add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Lesión registrada", Toast.LENGTH_SHORT).show();
                    cargarLesiones();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMarcarRecuperado(Lesion lesion, int position) {
        db.collection("lesiones").document(lesion.getId())
                .update("recuperado", true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Marcada como recuperada ✓", Toast.LENGTH_SHORT).show();
                    cargarLesiones();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEliminar(Lesion lesion, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar lesión")
                .setMessage("¿Seguro que quieres eliminar esta lesión?")
                .setPositiveButton("Eliminar", (d, w) ->
                        db.collection("lesiones").document(lesion.getId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Lesión eliminada", Toast.LENGTH_SHORT).show();
                                    cargarLesiones();
                                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}