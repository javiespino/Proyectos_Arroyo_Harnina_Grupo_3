package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
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

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Header: temperatura y humedad (sensores físicos)
    private TextView tvTemperatura, tvHumedadHeader;

    // Card azul: progreso de reservas completadas
    private ProgressBar progressBar;
    private TextView tvPorcentajeCentral;

    private TextView tvNombreUsuario;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String WEATHER_URL = "https://api.weather.com/v2/pws/observations/current?stationId=IALMEN70&format=json&units=m&apiKey=908477f6f2b84c6c8477f6f2b80c6c03";
    private OkHttpClient cliente = new OkHttpClient();

    // Chat flotante
    private FrameLayout chatContainer;
    private EditText editMessage;
    private TextView txtResponse;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Header (sensores)
        tvTemperatura   = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);

        // Card azul (reservas)
        progressBar         = findViewById(R.id.progressBar);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Chat flotante
        chatContainer = findViewById(R.id.chatContainer);
        editMessage   = findViewById(R.id.editMessage);
        txtResponse   = findViewById(R.id.txtResponse);
        btnSend       = findViewById(R.id.btnSend);

        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();
        configurarChat();

        cargarDatosMeteorologicos();    // → tvClima + tvHumedadHeader (header)
        obtenerDatosUsuarioFirebase();  // → tvNombreUsuario
        cargarProgresoReservas();       // → progressBar + tvPorcentajeCentral (card azul)
    }

    // ══════════════════════════════════════════════════════════════
    // Chat flotante (FAB)
    // ══════════════════════════════════════════════════════════════

    private void configurarChat() {
        // Mostrar/ocultar el panel de chat al pulsar el FAB
        findViewById(R.id.fab).setOnClickListener(v -> {
            if (chatContainer.getVisibility() == View.GONE) {
                chatContainer.setVisibility(View.VISIBLE);
            } else {
                chatContainer.setVisibility(View.GONE);
            }
        });

        // Botón enviar mensaje al webhook
        btnSend.setOnClickListener(v -> {
            String message = editMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToWebhook(message);
                editMessage.setText("");
            }
        });
    }

    private void sendMessageToWebhook(String message) {
        String WEBHOOK_URL = "https://hook.eu1.make.com/q7n6l956tkrhen73sk874pf6mmyyb4dt";

        OkHttpClient client = new OkHttpClient();
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", message);
        } catch (Exception e) {
            e.printStackTrace();
            txtResponse.setText("Error creando JSON");
            return;
        }

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                jsonBody.toString(),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(WEBHOOK_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> txtResponse.setText("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body() != null ? response.body().string() : "Sin respuesta";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(res);
                        String chatbotResponse = json.optString("message");
                        if (chatbotResponse.isEmpty()) {
                            chatbotResponse = res;
                        }
                        txtResponse.setText(chatbotResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        txtResponse.setText(res);
                    }
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Meteorología → header (tvClima + tvHumedadHeader)
    // ══════════════════════════════════════════════════════════════

    private void cargarDatosMeteorologicos() {
        Request request = new Request.Builder().url(WEATHER_URL).build();
        cliente.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "Error de red", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String jsonResponse = response.body().string();
                    JSONObject root   = new JSONObject(jsonResponse);
                    JSONObject obs    = root.getJSONArray("observations").getJSONObject(0);
                    JSONObject metric = obs.getJSONObject("metric");

                    String temp = metric.getString("temp");
                    String hum  = obs.getString("humidity");

                    runOnUiThread(() -> {
                        tvTemperatura.setText(temp + "°C");
                        tvHumedadHeader.setText(hum + "%");
                    });
                } catch (Exception e) {
                    Log.e("API", "Error al procesar JSON", e);
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Reservas → card azul (progressBar + tvPorcentajeCentral)
    // ══════════════════════════════════════════════════════════════

    private void cargarProgresoReservas() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        new ReservasManager().calcularProgreso(user.getUid(), new ReservasManager.ProgresoCallback() {
            @Override
            public void onProgreso(int porcentaje, int completadas, int total) {
                progressBar.setProgress(porcentaje);
                tvPorcentajeCentral.setText(porcentaje + "%");
            }

            @Override
            public void onError(Exception e) {
                Log.e("Reservas", "Error al calcular progreso", e);
            }
        });
    }

    public void marcarReservaCompletada(String reservaId, String claseId) {
        new ReservasManager().marcarComoCompletada(reservaId, claseId, new ReservasManager.OperacionCallback() {
            @Override
            public void onExito() {
                Toast.makeText(MainActivity.this, "✅ Clase completada", Toast.LENGTH_SHORT).show();
                cargarProgresoReservas();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "⚠️ " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Firebase → nombre usuario
    // ══════════════════════════════════════════════════════════════

    private void obtenerDatosUsuarioFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            tvNombreUsuario.setText(doc.getString("name"));
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error al leer usuario", e));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Botones de acción (cards del main)
    // ══════════════════════════════════════════════════════════════

    private void configurarBotonesAccion() {
        findViewById(R.id.btnDetalles).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DetallesActivity.class)));

        findViewById(R.id.btnVerCalendario).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CalendarioActivity.class)));

        findViewById(R.id.cardRutina).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RutinaActivity.class)));

        findViewById(R.id.cardReservar).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CalendarioActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════
    // Navegación y márgenes
    // ══════════════════════════════════════════════════════════════

    void configurarNavegacion() {
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinaActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_reservas) {
                startActivity(new Intent(this, CalendarioActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, Chat.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}