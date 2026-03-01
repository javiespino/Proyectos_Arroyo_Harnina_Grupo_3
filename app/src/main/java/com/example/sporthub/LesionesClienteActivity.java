package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del entrenador: ver las lesiones de un cliente concreto (solo lectura).
 */
public class LesionesClienteActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTituloCliente, tvSinLesiones;
    private RecyclerView recyclerLesiones;
    private ChipGroup chipGroupFiltro;

    private FirebaseFirestore db;
    private String clienteUid;
    private String clienteNombre;

    private List<Lesion> todasLesiones = new ArrayList<>();
    private List<Lesion> listaMostrada = new ArrayList<>();
    private LesionAdapterSoloLectura adapter;

    private String filtroActual = "todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesiones_cliente);

        db             = FirebaseFirestore.getInstance();
        clienteUid     = getIntent().getStringExtra("clienteUid");
        clienteNombre  = getIntent().getStringExtra("clienteNombre");

        btnBack          = findViewById(R.id.btnBackLesionesCliente);
        tvTituloCliente  = findViewById(R.id.tvTituloLesionesCliente);
        tvSinLesiones    = findViewById(R.id.tvSinLesionesCliente);
        recyclerLesiones = findViewById(R.id.recyclerLesionesCliente);
        chipGroupFiltro  = findViewById(R.id.chipGroupFiltroCliente);

        tvTituloCliente.setText("Lesiones de " + clienteNombre);

        adapter = new LesionAdapterSoloLectura(listaMostrada);
        recyclerLesiones.setLayoutManager(new LinearLayoutManager(this));
        recyclerLesiones.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        chipGroupFiltro.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipClienteTodas)       filtroActual = "todas";
            else if (id == R.id.chipClienteActivas)     filtroActual = "activas";
            else if (id == R.id.chipClienteRecuperadas) filtroActual = "recuperadas";
            aplicarFiltro();
        });

        cargarLesiones();
    }

    private void cargarLesiones() {
        db.collection("lesiones")
                .whereEqualTo("usuarioId", clienteUid)
                .get()
                .addOnSuccessListener(query -> {
                    todasLesiones.clear();
                    for (QueryDocumentSnapshot doc : query) {
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
}