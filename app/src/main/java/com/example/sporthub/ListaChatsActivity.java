package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla exclusiva del entrenador.
 * Muestra todos los usuarios con rol "u" y permite abrir el chat con cada uno.
 */
public class ListaChatsActivity extends AppCompatActivity {

    private RecyclerView     recyclerClientes;
    private ProgressBar      progressBar;
    private TextView         tvVacio;
    private ImageButton      btnBack;

    private FirebaseFirestore db;
    private String            miUid;

    private final List<ClienteChat> listaClientes = new ArrayList<>();
    private ClienteChatAdapter      adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_chats);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        miUid = user.getUid();

        recyclerClientes = findViewById(R.id.recyclerClientes);
        progressBar      = findViewById(R.id.progressBar);
        tvVacio          = findViewById(R.id.tvVacio);
        btnBack          = findViewById(R.id.btnBackLista);

        btnBack.setOnClickListener(v -> finish());

        adapter = new ClienteChatAdapter(listaClientes, cliente -> {
            Intent intent = new Intent(this, ChatEntrenador.class);
            intent.putExtra("clienteUid", cliente.getUid());
            startActivity(intent);
        });

        recyclerClientes.setLayoutManager(new LinearLayoutManager(this));
        recyclerClientes.setAdapter(adapter);

        cargarClientes();
    }

    private void cargarClientes() {
        progressBar.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("rol", "c")
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    listaClientes.clear();

                    Log.d("ListaChats", "Documentos encontrados: " + query.size());

                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("name");
                        String uid    = doc.getId();
                        Log.d("ListaChats", "Cliente: " + uid + " - " + nombre);
                        listaClientes.add(new ClienteChat(uid, nombre != null ? nombre : "Usuario"));
                    }

                    if (listaClientes.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("ListaChats", "Error Firestore: " + e.getMessage(), e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}