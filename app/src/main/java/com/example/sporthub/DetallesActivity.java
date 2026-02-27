package com.example.sporthub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DetallesActivity extends AppCompatActivity implements ReservaAdapter.OnReservaActionListener {

    private MaterialToolbar toolbar;
    private ProgressBar progressBarDetalles;
    private TextView tvResumenClases, tvPorcentajeDetalles, tvVacio;
    private RecyclerView recyclerReservas;

    private ReservaAdapter adapter;
    private List<Reserva> listaReservas = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ReservasManager reservasManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalles);

        mAuth            = FirebaseAuth.getInstance();
        db               = FirebaseFirestore.getInstance();
        reservasManager  = new ReservasManager();

        // Vistas
        toolbar                = findViewById(R.id.toolbar);
        progressBarDetalles    = findViewById(R.id.progressBarDetalles);
        tvResumenClases        = findViewById(R.id.tvResumenClases);
        tvPorcentajeDetalles   = findViewById(R.id.tvPorcentajeDetalles);
        tvVacio                = findViewById(R.id.tvVacio);
        recyclerReservas       = findViewById(R.id.recyclerReservas);

        // Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Márgenes sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // RecyclerView
        adapter = new ReservaAdapter(listaReservas, this);
        recyclerReservas.setLayoutManager(new LinearLayoutManager(this));
        recyclerReservas.setAdapter(adapter);

        cargarReservas();
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar reservas del usuario desde Firestore
    // ══════════════════════════════════════════════════════════════

    private void cargarReservas() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("reservas")
                .whereEqualTo("usuarioId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaReservas.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId()); // Guardamos el ID del documento
                        listaReservas.add(reserva);
                    }

                    adapter.notifyDataSetChanged();
                    actualizarResumen();

                    // Mostrar mensaje si no hay reservas
                    if (listaReservas.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        recyclerReservas.setVisibility(View.GONE);
                    } else {
                        tvVacio.setVisibility(View.GONE);
                        recyclerReservas.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("Detalles", "Error al cargar reservas", e));
    }

    // ══════════════════════════════════════════════════════════════
    // Actualizar card de resumen (progressBar + textos)
    // ══════════════════════════════════════════════════════════════

    private void actualizarResumen() {
        int total = listaReservas.size();
        int completadas = 0;
        for (Reserva r : listaReservas) {
            if (r.isCompletada()) completadas++;
        }

        int porcentaje = total > 0 ? (int) Math.round((completadas * 100.0) / total) : 0;

        progressBarDetalles.setProgress(porcentaje);
        tvPorcentajeDetalles.setText(porcentaje + "%");
        tvResumenClases.setText(completadas + " de " + total + " clases completadas");
    }

    // ══════════════════════════════════════════════════════════════
    // Callbacks del adapter
    // ══════════════════════════════════════════════════════════════

    @Override
    public void onMarcarCompletada(String reservaId, String claseId) {
        reservasManager.marcarComoCompletada(reservaId, claseId, new ReservasManager.OperacionCallback() {
            @Override
            public void onExito() {
                adapter.marcarCompletada(reservaId); // Actualiza la UI localmente
                actualizarResumen();
                Toast.makeText(DetallesActivity.this,
                        "✅ Clase marcada como completada", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(DetallesActivity.this,
                        "⚠️ " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCancelar(String reservaId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("¿Seguro que quieres cancelar esta reserva?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> {

                    // Obtenemos el claseId antes de borrar
                    String claseId = listaReservas.get(position).getClaseId();

                    db.collection("reservas")
                            .document(reservaId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                // Devolver la plaza a Firestore
                                db.collection("clases")
                                        .document(claseId)
                                        .update("plazasDisponibles", FieldValue.increment(1))
                                        .addOnFailureListener(e -> Log.e("Firestore", "Error al devolver plaza", e));

                                adapter.eliminarItem(position);
                                actualizarResumen();
                                Toast.makeText(this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al cancelar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }
}