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

    private TextView tvTemperatura, tvHumedadHeader, tvPorcentajeCentral, tvNombreUsuario;
    private ProgressBar progressBarHumedad;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String WEATHER_URL = "https://api.weather.com/v2/pws/observations/current?stationId=IALMEN70&format=json&units=m&apiKey=908477f6f2b84c6c8477f6f2b80c6c03";
    private OkHttpClient cliente = new OkHttpClient();

    // --- Chat flotante ---
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

        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        progressBarHumedad = findViewById(R.id.progressBar);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // --- Inicializar chat flotante ---
        chatContainer = findViewById(R.id.chatContainer);
        editMessage = findViewById(R.id.editMessage);
        txtResponse = findViewById(R.id.txtResponse);
        btnSend = findViewById(R.id.btnSend);

        // Mostrar/ocultar chat al tocar el FAB
        findViewById(R.id.fab).setOnClickListener(v -> {
            if (chatContainer.getVisibility() == View.GONE) {
                chatContainer.setVisibility(View.VISIBLE);
            } else {
                chatContainer.setVisibility(View.GONE);
            }
        });

        // Botón enviar mensaje
        btnSend.setOnClickListener(v -> {
            String message = editMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToWebhook(message);
                editMessage.setText("");
            }
        });

        configurarMargenes();
        configurarNavegacion();

        cargarDatosMeteorologicos();
        obtenerDatosUsuarioFirebase();
    }

    // --- Función para enviar mensaje al webhook de Make/OpenAI ---
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
                jsonBody.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8")
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
                        // Parseamos el JSON que nos devuelve Make
                        JSONObject json = new JSONObject(res);

                        // Extraemos el texto del chatbot (según el path de tu webhook)
                        String chatbotResponse = json.optString("message"); // Si tu webhook devuelve {"message":"texto"}

                        // Si tu webhook devuelve solo texto plano, usa directamente 'res'
                        if(chatbotResponse.isEmpty()) {
                            chatbotResponse = res;
                        }

                        txtResponse.setText(chatbotResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        txtResponse.setText(res); // fallback en caso de error
                    }
                });
            }
        });
    }

    // --- Funciones existentes ---
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
                    JSONObject root = new JSONObject(jsonResponse);
                    JSONObject obs = root.getJSONArray("observations").getJSONObject(0);
                    JSONObject metric = obs.getJSONObject("metric");

                    String temp = metric.getString("temp");
                    String hum = obs.getString("humidity");

                    runOnUiThread(() -> {
                        tvTemperatura.setText(temp + "°C");
                        tvHumedadHeader.setText(hum + "%");
                        int valHum;

                        try {
                            valHum = (int) Double.parseDouble(hum);
                        } catch (Exception e) {
                            valHum = 0;
                        }

                        progressBarHumedad.setProgress(valHum);
                        tvPorcentajeCentral.setText(valHum + "%");
                    });
                } catch (Exception e) {
                    Log.e("API", "Error al procesar JSON", e);
                }
            }
        });
    }

    private void obtenerDatosUsuarioFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String nombre = doc.getString("name");
                            tvNombreUsuario.setText(nombre);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error al leer usuario", e));
        }
    }

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
            }
            // Eliminamos nav_chat que abría otra activity
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