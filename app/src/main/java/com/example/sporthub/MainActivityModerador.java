package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivityModerador extends AppCompatActivity {

    private TextView tvNombreModerador;
    private TextView tvTotalReservas, tvTotalClientes, tvTotalAusencias;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_moderador);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvNombreModerador   = findViewById(R.id.tvNombreModerador);
        tvTotalReservas     = findViewById(R.id.tvTotalReservas);
        tvTotalClientes     = findViewById(R.id.tvTotalClientes);
        tvTotalAusencias    = findViewById(R.id.tvTotalAusencias);
        bottomNavigationView = findViewById(R.id.bottomNavigationModerador);

        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();
        verificarRolYCargarDatos();
    }

    // ══════════════════════════════════════════════════════════════
    // Verificar rol antes de mostrar la pantalla
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

                    // Seguridad: si no es moderador, lo mandamos a donde corresponde
                    if (!"m".equals(rol)) {
                        redirigirPorRol(rol);
                        return;
                    }

                    String name = doc.getString("name");
                    if (name != null) tvNombreModerador.setText(name);

                    cargarEstadisticas();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al verificar rol moderador", e);
                    cargarEstadisticas();
                });
    }

    private void redirigirPorRol(String rol) {
        Intent intent;
        if ("c".equals(rol)) {
            intent = new Intent(this, MainActivity.class);
        } else if ("e".equals(rol)) {
            intent = new Intent(this, MainActivityEntrenador.class);
        } else if ("a".equals(rol)) {
            intent = new Intent(this, MainActivityAdmin.class);
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
        // Total de reservas activas
        db.collection("reservas")
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(query -> {
                    tvTotalReservas.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvTotalReservas.setText("--"));

        // Total de clientes (rol "c")
        db.collection("users")
                .whereEqualTo("rol", "c")
                .get()
                .addOnSuccessListener(query -> {
                    tvTotalClientes.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvTotalClientes.setText("--"));

        // Total de ausencias registradas hoy
        String hoy = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                .format(new java.util.Date());

        db.collection("ausencias")
                .whereEqualTo("fecha", hoy)
                .get()
                .addOnSuccessListener(query -> {
                    tvTotalAusencias.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvTotalAusencias.setText("--"));
    }

    // ══════════════════════════════════════════════════════════════
    // Botones de acción del dashboard
    // ══════════════════════════════════════════════════════════════

    private void configurarBotonesAccion() {
        // Card Reservas
        findViewById(R.id.cardReservasMod).setOnClickListener(v ->
                startActivity(new Intent(this, GestionReservasModeradorActivity.class)));

        // Card Clientes
        findViewById(R.id.cardClientesMod).setOnClickListener(v ->
                startActivity(new Intent(this, GestionClientesActivity.class)));

        // Card Ausencias
        findViewById(R.id.cardAusenciasMod).setOnClickListener(v ->
                startActivity(new Intent(this, GestionAusenciasActivity.class)));

        // Card Chat
        findViewById(R.id.cardChatMod).setOnClickListener(v ->
                startActivity(new Intent(this, ListaChatsActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════
    // Navegación inferior
    // ══════════════════════════════════════════════════════════════

    private void configurarNavegacion() {
        bottomNavigationView.setSelectedItemId(R.id.nav_mod_inicio);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_mod_inicio) {
                return true;
            } else if (id == R.id.nav_mod_reservas) {
                startActivity(new Intent(this, GestionReservasModeradorActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_mod_clientes) {
                startActivity(new Intent(this, GestionClientesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_mod_chat) {
                startActivity(new Intent(this, ListaChatsActivity.class));
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainModerador), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}