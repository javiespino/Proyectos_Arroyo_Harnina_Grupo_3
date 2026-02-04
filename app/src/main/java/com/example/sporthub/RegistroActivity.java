package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private static final String TAG = "RegistroActivity";

    private EditText edtName, edtEmail, edtPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registrolayout);

        // Vistas
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Validaciones
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        // Crear usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Usuario creado en Firebase Auth");

                        FirebaseUser user = task.getResult().getUser();

                        if (user == null) {
                            Toast.makeText(this, "Error inesperado al crear usuario", Toast.LENGTH_LONG).show();
                            resetButton();
                            return;
                        }

                        String userId = user.getUid();

                        // Datos a guardar en Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("rol", "c"); // 👈 rol por defecto c== cliente
                        userMap.put("createdAt", System.currentTimeMillis());


                        // Guardar en Firestore
                        db.collection("users")
                                .document(userId)
                                .set(userMap)
                                .addOnSuccessListener(unused -> {
                                    Log.d(TAG, "✅ Datos guardados en Firestore");
                                    Toast.makeText(this, "Registro exitoso ✅", Toast.LENGTH_SHORT).show();

                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error Firestore", e);
                                    Toast.makeText(this,
                                            "Error al guardar datos: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    resetButton();
                                });

                    } else {
                        Log.e(TAG, "❌ Error Auth", task.getException());
                        Toast.makeText(this,
                                "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Registrar");
    }
}
