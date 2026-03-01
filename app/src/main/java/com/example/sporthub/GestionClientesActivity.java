package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionClientesActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView recyclerClientes;
    private ProgressBar progressBar;
    private TextView tvVacio;

    private FirebaseFirestore db;
    private List<ClienteInfo> listaClientes = new ArrayList<>();
    private ClienteInfoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_clientes);

        db = FirebaseFirestore.getInstance();

        btnBack          = findViewById(R.id.btnBackClientes);
        recyclerClientes = findViewById(R.id.recyclerClientes);
        progressBar      = findViewById(R.id.progressBarClientes);
        tvVacio          = findViewById(R.id.tvVacioClientes);

        adapter = new ClienteInfoAdapter(listaClientes, cliente -> {
            Intent intent = new Intent(this, LesionesClienteActivity.class);
            intent.putExtra("clienteUid",    cliente.getUid());
            intent.putExtra("clienteNombre", cliente.getNombre());
            startActivity(intent);
        });

        recyclerClientes.setLayoutManager(new LinearLayoutManager(this));
        recyclerClientes.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        cargarClientes();
    }

    private void cargarClientes() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("rol", "c")
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    listaClientes.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("name");
                        String uid    = doc.getId();
                        listaClientes.add(new ClienteInfo(uid, nombre != null ? nombre : "Usuario"));
                    }

                    // Para cada cliente, cargar cuántas lesiones activas tiene
                    if (listaClientes.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    cargarLesionesResumen();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar clientes", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarLesionesResumen() {
        final int[] pendientes = {listaClientes.size()};

        for (ClienteInfo cliente : listaClientes) {
            db.collection("lesiones")
                    .whereEqualTo("usuarioId", cliente.getUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        int activas     = 0;
                        int recuperadas = 0;
                        String zonas    = "";

                        for (QueryDocumentSnapshot doc : query) {
                            Boolean recuperado = doc.getBoolean("recuperado");
                            String zona        = doc.getString("zona");
                            if (recuperado != null && recuperado) {
                                recuperadas++;
                            } else {
                                activas++;
                                if (!zonas.isEmpty()) zonas += ", ";
                                if (zona != null) zonas += zona;
                            }
                        }

                        cliente.setLesionesActivas(activas);
                        cliente.setLesionesRecuperadas(recuperadas);
                        cliente.setZonasActivas(zonas.isEmpty() ? "Sin lesiones activas" : zonas);

                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            tvVacio.setVisibility(listaClientes.isEmpty() ? View.VISIBLE : View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}