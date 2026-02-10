package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Habilita el diseño moderno de borde a borde
        setContentView(R.layout.activity_main);

        // 1. Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Vincular Vistas
        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        progressBarHumedad = findViewById(R.id.progressBar);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 3. Configuración inicial
        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();

        // 4. Carga de datos
        cargarDatosMeteorologicos();
        obtenerDatosUsuarioFirebase();
    }

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
                        int valHum = (int) Double.parseDouble(hum);
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

    private void configurarNavegacion() {

        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                return true; // Ya estamos aquí
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
                overridePendingTransition(0, 0); // Transición suave sin parpadeo
                return true;
            } else if (id == R.id.nav_rutinas) {
                // startActivity(new Intent(this, RutinaActivity.class));
                return true;
            }
            // Aquí puedes añadir nav_reservas o nav_chat
            return false;
        });
    }

    private void configurarBotonesAccion() {
        // 1. Botón "Detalles" (en la tarjeta de progreso) -> DetallesActivity
        findViewById(R.id.btnDetalles).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetallesActivity.class);
            startActivity(intent);
        });

        // 2. Botón "Ver Calendario" -> AHORA VA A CalendarioActivity (Reservas)
        // Según tu petición, aquí es donde el usuario gestionará sus reservas
        findViewById(R.id.btnVerCalendario).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });

        // 3. Tarjeta de Rutina (CardView) -> RutinaActivity
        findViewById(R.id.cardRutina).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RutinaActivity.class);
            startActivity(intent);
        });

        // 4. Tarjeta de Reservar (Si quieres que ambos botones lleven al mismo sitio)
        findViewById(R.id.cardReservar).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });

        // 5. FAB (Botón flotante del ChatBot)
        findViewById(R.id.fab).setOnClickListener(v ->
                Toast.makeText(this, "Asistente SportHub activado", Toast.LENGTH_SHORT).show());
    }

    private void configurarMargenes() {
        // Ajusta el padding para que el contenido no quede bajo la barra de estado o navegación
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}