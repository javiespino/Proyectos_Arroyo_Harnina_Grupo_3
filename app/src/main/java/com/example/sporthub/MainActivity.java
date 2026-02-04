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

        // Inicializar vistas con tus IDs originales
        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        progressBarHumedad = findViewById(R.id.progressBar);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);

        configurarMargenes();
        configurarBotones();
        cargarDatosMeteorologicos();
        obtenerDatosUsuarioFirebase();
    }

    private void cargarDatosMeteorologicos() {
        Request request = new Request.Builder().url(WEATHER_URL).build();
        cliente.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { Log.e("API", "Error red", e); }

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
                } catch (Exception e) { Log.e("API", "JSON Error", e); }
            }
        });
    }

    private void obtenerDatosUsuarioFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) tvNombreUsuario.setText(doc.getString("name"));
                    });
        }
    }

    private void configurarBotones() {
        findViewById(R.id.btnDetalles).setOnClickListener(v -> startActivity(new Intent(this, DetallesActivity.class)));
        findViewById(R.id.btnVerCalendario).setOnClickListener(v -> startActivity(new Intent(this, CalendarioActivity.class)));
        findViewById(R.id.cardRutina).setOnClickListener(v -> startActivity(new Intent(this, RutinaActivity.class)));
        findViewById(R.id.cardReservar).setOnClickListener(v -> startActivity(new Intent(this, CalendarioActivity.class)));
        findViewById(R.id.fab).setOnClickListener(v -> Toast.makeText(this, "¡Hola!", Toast.LENGTH_SHORT).show());
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}