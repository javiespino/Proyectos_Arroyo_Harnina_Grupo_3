package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class PerfilUsuario extends AppCompatActivity {

    private TextView tvNombreMenu;
    private LinearLayout optionEditProfile, optionSubscription;
    private Button btnLogout;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_usuario);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vinculación de vistas
        tvNombreMenu = findViewById(R.id.tvNombreMenu);
        optionEditProfile = findViewById(R.id.optionEditProfile);
        optionSubscription = findViewById(R.id.optionSubscription);
        btnLogout = findViewById(R.id.btnMenuLogout);
        bottomNav = findViewById(R.id.bottomNavigationPerfil);

        // Ajuste de márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        obtenerNombreUsuario();
        configurarListeners();
        configurarBottomNav();
    }

    private void obtenerNombreUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            tvNombreMenu.setText(doc.getString("name"));
                        }
                    });
        }
    }

    private void configurarListeners() {
        // LANZAR MODIFICAR USUARIO (Corregido con PerfilUsuario.this)
        optionEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilUsuario.this, ModificarUsuario.class);
            startActivity(intent);
        });

        optionSubscription.setOnClickListener(v ->
                Toast.makeText(this, "Próximamente...", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(PerfilUsuario.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void configurarBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_perfil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                // VOLVER AL MAIN (Corregido)
                startActivity(new Intent(PerfilUsuario.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_perfil) {
                return true; // Ya estamos aquí
            }
            return false;
        });
    }
}