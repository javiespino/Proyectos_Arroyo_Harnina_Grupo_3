package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionReservasModeradorActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView recyclerReservas;
    private ProgressBar progressBar;
    private TextView tvVacio;

    private FirebaseFirestore db;
    private List<Reserva> listaReservas = new ArrayList<>();
    private ReservaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_reservas_moderador);

        db = FirebaseFirestore.getInstance();

        btnBack          = findViewById(R.id.btnBackReservasMod);
        recyclerReservas = findViewById(R.id.recyclerReservasMod);
        progressBar      = findViewById(R.id.progressBarReservasMod);
        tvVacio          = findViewById(R.id.tvVacioReservasMod);

        // Usamos la interfaz correcta de ReservaAdapter
        adapter = new ReservaAdapter(listaReservas, new ReservaAdapter.OnReservaActionListener() {
            @Override
            public void onMarcarCompletada(String reservaId, String claseId) {
                // El moderador no marca clases como completadas, solo cancela
            }

            @Override
            public void onCancelar(String reservaId, int position) {
                for (Reserva r : listaReservas) {
                    if (reservaId.equals(r.getId())) {
                        confirmarCancelacion(r, position);
                        break;
                    }
                }
            }
        });

        recyclerReservas.setLayoutManager(new LinearLayoutManager(this));
        recyclerReservas.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        cargarReservas();
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar todas las reservas activas (no completadas)
    // ══════════════════════════════════════════════════════════════

    private void cargarReservas() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("reservas")
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    listaReservas.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        Reserva r = doc.toObject(Reserva.class);
                        r.setId(doc.getId());
                        listaReservas.add(r);
                    }

                    tvVacio.setVisibility(listaReservas.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Confirmar y ejecutar cancelación
    // ══════════════════════════════════════════════════════════════

    private void confirmarCancelacion(Reserva reserva, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("¿Seguro que quieres cancelar la reserva de\n" +
                        reserva.getNombreActividad() + "?")
                .setPositiveButton("Sí, cancelar", (dialog, which) ->
                        cancelarReserva(reserva, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelarReserva(Reserva reserva, int position) {
        db.collection("reservas").document(reserva.getId())
                .update("completada", true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                    adapter.eliminarItem(position);
                    if (listaReservas.isEmpty()) tvVacio.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cancelar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}