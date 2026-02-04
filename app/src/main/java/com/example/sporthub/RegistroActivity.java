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

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegistroActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length() < 6){
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()){
                        Log.d(TAG, "✅ Usuario creado en Firebase Auth");

                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);

                        db.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Datos guardados en Firestore");
                                    Toast.makeText(this, "Registro exitoso ✅", Toast.LENGTH_SHORT).show();

                                    // Ahora SÍ navegar a MainActivity
                                    Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error al guardar en Firestore", e);
                                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();

                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar");
                                });

                    } else {
                        Log.e(TAG, "❌ Error al crear usuario", task.getException());
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrar");
                    }
                });
    }
}