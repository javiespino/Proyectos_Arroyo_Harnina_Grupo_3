package com.example.sporthub;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView txtRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logindsgn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUsuario());

        txtRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRole(currentUser.getUid());
        }
    }

    private void loginUsuario() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        } else {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Error inesperado de autenticación", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String uid) {

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "El perfil no existe en Firestore",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String rol = documentSnapshot.getString("rol");
                    String name = documentSnapshot.getString("name");

                    if (rol == null) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "El campo 'rol' es null",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(LoginActivity.this,
                            "Bienvenido " + name,
                            Toast.LENGTH_SHORT).show();

                    Intent intent;

                    if (rol.equals("c")) {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    } else if (rol.equals("e")) {
                        intent = new Intent(LoginActivity.this, MainActivityEntrenador.class);
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Rol no reconocido: " + rol,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Error Firestore: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!isLoading);
    }
}