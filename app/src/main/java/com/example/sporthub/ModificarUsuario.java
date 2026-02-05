package com.example.sporthub;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ModificarUsuario extends AppCompatActivity {

    private EditText etNombre, etEmail;
    private TextView tvNombreCabecera;
    private Button btnGuardar, btnCerrarSesion, btnCambiarPass;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modificar_usuario);

        // 1. Configurar márgenes para diseño EdgeToEdge (Evita que las barras del sistema tapen el contenido)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Vincular vistas
        tvNombreCabecera = findViewById(R.id.tvNombreCabecera);
        etNombre = findViewById(R.id.etNombrePerfil);
        etEmail = findViewById(R.id.etEmailPerfil);
        btnGuardar = findViewById(R.id.btnSaveProfile);
        btnCerrarSesion = findViewById(R.id.btnLogout);
        btnCambiarPass = findViewById(R.id.btnChangePass);
        bottomNav = findViewById(R.id.bottomNavigationPerfil);

        // 4. Cargar datos actuales y configurar navegación
        cargarDatosUsuario();
        configurarNavegacion();

        // 5. Eventos de botones
        btnGuardar.setOnClickListener(v -> actualizarPerfil());

        btnCambiarPass.setOnClickListener(v -> enviarCorreoRestablecimiento());

        btnCerrarSesion.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ModificarUsuario.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");

                            tvNombreCabecera.setText(nombre);
                            etNombre.setText(nombre);
                            etEmail.setText(email);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show());
        }
    }

    private void actualizarPerfil() {
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevoEmail = etEmail.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || nuevoNombre.isEmpty() || nuevoEmail.isEmpty()) {
            Toast.makeText(this, "Campos vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Actualizando...");

        // Actualizar Email en Firebase Auth
        user.updateEmail(nuevoEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Sincronizar cambios en Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", nuevoNombre);
                updates.put("email", nuevoEmail);

                db.collection("users").document(user.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            tvNombreCabecera.setText(nuevoNombre);
                            Toast.makeText(this, "Perfil actualizado ✅", Toast.LENGTH_SHORT).show();
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar Cambios");
                        })
                        .addOnFailureListener(e -> {
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar Cambios");
                            Toast.makeText(this, "Error en base de datos", Toast.LENGTH_SHORT).show();
                        });
            } else {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("Guardar Cambios");
                // Nota: Si falla aquí, suele ser por seguridad de Firebase (pide login reciente)
                Toast.makeText(this, "Error: Reautenticación requerida para cambiar email", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void enviarCorreoRestablecimiento() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) return;

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Correo de restablecimiento enviado a: " + email, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_perfil); // Marcar este item como seleccionado

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_perfil) {
                return true; // Ya estamos aquí
            } else if (id == R.id.nav_rutinas) {
                // startActivity(new Intent(this, RutinaActivity.class));
                return true;
            }
            // Añadir más casos según vuestro menú (nav_reservas, nav_chat)
            return false;
        });
    }
}