package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        progressBarHumedad = findViewById(R.id.progressBar);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();

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
            }else if (id == R.id.nav_chat) {
           startActivity(new Intent(this, Chat.class));
           overridePendingTransition(0, 0);
           return true;
       }

            return false;
        });
    }

    private void configurarBotonesAccion() {

        findViewById(R.id.btnDetalles).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetallesActivity.class);
            startActivity(intent);
        });


        findViewById(R.id.btnVerCalendario).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardRutina).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RutinaActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardReservar).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });

        //TODO
        findViewById(R.id.fab).setOnClickListener(v ->
                Toast.makeText(this, "Asistente SportHub activado", Toast.LENGTH_SHORT).show());
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}