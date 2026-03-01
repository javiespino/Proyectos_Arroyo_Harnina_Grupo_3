package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivityAdmin extends AppCompatActivity {

    private TextView tvNombreAdmin;
    private TextView tvTotalUsuarios, tvTotalEntrenadores, tvTotalModeradores;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvNombreAdmin        = findViewById(R.id.tvNombreAdmin);
        tvTotalUsuarios      = findViewById(R.id.tvTotalUsuarios);
        tvTotalEntrenadores  = findViewById(R.id.tvTotalEntrenadores);
        tvTotalModeradores   = findViewById(R.id.tvTotalModeradores);
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);

        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();
        verificarRolYCargarDatos();
    }

    // ══════════════════════════════════════════════════════════════
    // Verificar que es admin antes de mostrar la pantalla
    // ══════════════════════════════════════════════════════════════

    private void verificarRolYCargarDatos() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    String rol = doc.getString("rol");

                    if (!"a".equals(rol)) {
                        redirigirPorRol(rol);
                        return;
                    }

                    String name = doc.getString("name");
                    if (name != null) tvNombreAdmin.setText(name);

                    cargarEstadisticas();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al verificar rol admin", e);
                    cargarEstadisticas();
                });
    }

    private void redirigirPorRol(String rol) {
        Intent intent;
        if ("c".equals(rol)) {
            intent = new Intent(this, MainActivity.class);
        } else if ("e".equals(rol)) {
            intent = new Intent(this, MainActivityEntrenador.class);
        } else if ("m".equals(rol)) {
            intent = new Intent(this, MainActivityModerador.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }

    // ══════════════════════════════════════════════════════════════
    // Estadísticas del dashboard
    // ══════════════════════════════════════════════════════════════

    private void cargarEstadisticas() {
        // Total clientes
        db.collection("users")
                .whereEqualTo("rol", "c")
                .get()
                .addOnSuccessListener(q -> tvTotalUsuarios.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> tvTotalUsuarios.setText("--"));

        // Total entrenadores
        db.collection("users")
                .whereEqualTo("rol", "e")
                .get()
                .addOnSuccessListener(q -> tvTotalEntrenadores.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> tvTotalEntrenadores.setText("--"));

        // Total moderadores
        db.collection("users")
                .whereEqualTo("rol", "m")
                .get()
                .addOnSuccessListener(q -> tvTotalModeradores.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> tvTotalModeradores.setText("--"));
    }

    // ══════════════════════════════════════════════════════════════
    // Botones de acción del dashboard
    // ══════════════════════════════════════════════════════════════

    private void configurarBotonesAccion() {
        // Card Gestión de roles (exclusivo admin) ← CORREGIDO
        findViewById(R.id.cardRolesAdmin).setOnClickListener(v ->
                startActivity(new Intent(this, GestionRolesAdminActivity.class)));

        // Card Ver todos los usuarios
        findViewById(R.id.cardUsuariosAdmin).setOnClickListener(v ->
                startActivity(new Intent(this, GestionClientesActivity.class)));

        // Card Abonos
        findViewById(R.id.cardAbonosAdmin).setOnClickListener(v ->
                startActivity(new Intent(this, AbonosActivity.class)));

        // Card Calendario global
        findViewById(R.id.cardCalendarioAdmin).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarioActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════
    // Navegación inferior
    // ══════════════════════════════════════════════════════════════

    private void configurarNavegacion() {
        bottomNavigationView.setSelectedItemId(R.id.nav_admin_inicio);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_inicio) {
                return true;
            } else if (id == R.id.nav_admin_roles) {
                // ← CORREGIDO
                startActivity(new Intent(this, GestionRolesAdminActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_admin_abonos) {
                startActivity(new Intent(this, AbonosActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_admin_calendario) {
                startActivity(new Intent(this, CalendarioActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_admin_mas) {
                startActivity(new Intent(this, GestionClientesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Cerrar sesión
    // ══════════════════════════════════════════════════════════════

    public void cerrarSesion(View view) {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // ══════════════════════════════════════════════════════════════
    // Márgenes
    // ══════════════════════════════════════════════════════════════

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAdmin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}