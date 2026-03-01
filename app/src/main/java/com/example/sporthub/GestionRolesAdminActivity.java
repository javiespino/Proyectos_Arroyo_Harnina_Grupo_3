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

public class GestionRolesAdminActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView recyclerUsuarios;
    private ProgressBar progressBar;
    private TextView tvVacio;

    private FirebaseFirestore db;
    private List<ClienteInfo> listaUsuarios = new ArrayList<>();
    private RolUsuarioAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_roles_admin);

        db = FirebaseFirestore.getInstance();

        btnBack          = findViewById(R.id.btnBackRoles);
        recyclerUsuarios = findViewById(R.id.recyclerRoles);
        progressBar      = findViewById(R.id.progressBarRoles);
        tvVacio          = findViewById(R.id.tvVacioRoles);

        adapter = new RolUsuarioAdapter(listaUsuarios, (usuario, nuevoRol) ->
                confirmarCambioRol(usuario, nuevoRol)
        );

        recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsuarios.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        cargarUsuarios();
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar todos los usuarios (excepto admins)
    // ══════════════════════════════════════════════════════════════

    private void cargarUsuarios() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    listaUsuarios.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String rol    = doc.getString("rol");
                        String nombre = doc.getString("name");
                        String uid    = doc.getId();

                        // No mostramos administradores
                        if ("a".equals(rol)) continue;

                        ClienteInfo usuario = new ClienteInfo(uid, nombre != null ? nombre : "Usuario");
                        usuario.setRol(rol != null ? rol : "c");
                        listaUsuarios.add(usuario);
                    }

                    tvVacio.setVisibility(listaUsuarios.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Confirmar cambio de rol
    // ══════════════════════════════════════════════════════════════

    private void confirmarCambioRol(ClienteInfo usuario, String nuevoRol) {
        String rolTexto = rolATexto(nuevoRol);
        String rolActualTexto = rolATexto(usuario.getRol());

        new AlertDialog.Builder(this)
                .setTitle("Cambiar rol")
                .setMessage("¿Cambiar a " + usuario.getNombre() +
                        " de " + rolActualTexto +
                        " a " + rolTexto + "?")
                .setPositiveButton("Confirmar", (dialog, which) ->
                        ejecutarCambioRol(usuario, nuevoRol))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCambioRol(ClienteInfo usuario, String nuevoRol) {
        db.collection("users").document(usuario.getUid())
                .update("rol", nuevoRol)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            usuario.getNombre() + " ahora es " + rolATexto(nuevoRol),
                            Toast.LENGTH_SHORT).show();
                    // Actualizamos localmente sin recargar todo
                    usuario.setRol(nuevoRol);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cambiar rol: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private String rolATexto(String rol) {
        switch (rol) {
            case "e": return "Entrenador";
            case "m": return "Moderador";
            case "c": return "Cliente";
            default:  return "Desconocido";
        }
    }
}