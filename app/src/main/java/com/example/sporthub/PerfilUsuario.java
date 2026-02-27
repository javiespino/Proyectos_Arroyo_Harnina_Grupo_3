package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.View;

public class PerfilUsuario extends AppCompatActivity {

    private TextView tvNombreMenu, tvEstadoAbono;
    private View optionEditProfile, optionSubscription, optionLesiones;
    private View btnMenuLogout;
    private BottomNavigationView bottomNav;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_usuario);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        cargarDatosUsuario();
        cargarEstadoAbono();
        configurarNavegacion();
        configurarBotones();
    }

    private void inicializarVistas() {
        tvNombreMenu = findViewById(R.id.tvNombreMenu);
        tvEstadoAbono = findViewById(R.id.tvEstadoAbono);
        optionEditProfile = findViewById(R.id.optionEditProfile);
        optionSubscription = findViewById(R.id.optionSubscription);
        optionLesiones = findViewById(R.id.optionLesiones);
        btnMenuLogout = findViewById(R.id.btnMenuLogout);
        bottomNav = findViewById(R.id.bottomNavigationPerfil);
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("name");
                        if (nombre != null) tvNombreMenu.setText(nombre);
                    }
                });
    }

    private void cargarEstadoAbono() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("abonos")
                .whereEqualTo("usuarioId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        String estado = doc.getString("estado");
                        String tipo = doc.getString("tipo");
                        String vencimiento = doc.getString("fechaVencimiento");
                        if ("activo".equals(estado) && tipo != null) {
                            tvEstadoAbono.setText("● Abono " + tipo + " · Vence: " + vencimiento);
                            tvEstadoAbono.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                        } else {
                            tvEstadoAbono.setText("Sin abono activo");
                            tvEstadoAbono.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                        }
                    } else {
                        tvEstadoAbono.setText("Sin abono activo");
                        tvEstadoAbono.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                    }
                })
                .addOnFailureListener(e -> tvEstadoAbono.setText("Sin abono activo"));
    }

    private void configurarBotones() {
        optionEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ModificarUsuario.class)));

        optionSubscription.setOnClickListener(v ->
                startActivity(new Intent(this, AbonosActivity.class)));

        optionLesiones.setOnClickListener(v ->
                startActivity(new Intent(this, LesionesActivity.class)));

        btnMenuLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_perfil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinaActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_reservas) {
                startActivity(new Intent(this, CalendarioActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, Chat.class));
                finish();
                return true;
            } else if (id == R.id.nav_perfil) {
                return true;
            }
            return false;
        });
    }
}