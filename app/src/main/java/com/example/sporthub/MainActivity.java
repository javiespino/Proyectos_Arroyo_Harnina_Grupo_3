package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemperatura, tvHumedadHeader, tvPorcentajeCentral;
    private ProgressBar progressBarHumedad;

    // URL de la estación meteorológica IALMEN70
    private final String WEATHER_URL = "https://api.weather.com/v2/pws/observations/current?stationId=IALMEN70&format=json&units=m&apiKey=908477f6f2b84c6c8477f6f2b80c6c03";
    private OkHttpClient cliente = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configurarMargenes();

        // Inicializar vistas
        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        progressBarHumedad = findViewById(R.id.progressBar);

        configurarBotones();

        // Cargar los datos nada más iniciar la pantalla
        cargarDatosMeteorologicos();
    }

    private void cargarDatosMeteorologicos() {
        Request request = new Request.Builder().url(WEATHER_URL).build();

        cliente.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "ERROR de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                String jsonResponse = response.body().string();

                try {
                    JSONObject root = new JSONObject(jsonResponse);
                    JSONArray obsArr = root.getJSONArray("observations");
                    JSONObject obs = obsArr.getJSONObject(0);
                    JSONObject metric = obs.getJSONObject("metric");

                    // Extraemos los datos de la API
                    String temp = metric.getString("temp");
                    String humedad = obs.getString("humidity");

                    runOnUiThread(() -> {
                        // 1. Actualizamos los chips de la cabecera con etiquetas descriptivas
                        tvTemperatura.setText("Temperatura: " + temp + "°C");
                        tvHumedadHeader.setText("Humedad: " + humedad + "%");

                        // 2. Actualizamos el progreso circular con el dato de humedad
                        int valorHumedad = (int) Double.parseDouble(humedad);
                        progressBarHumedad.setProgress(valorHumedad);

                        // 3. El porcentaje central también muestra la humedad
                        tvPorcentajeCentral.setText(valorHumedad + "%");
                    });

                } catch (Exception e) {
                    Log.e("API", "Error procesando JSON: " + e.getMessage());
                }
            }
        });
    }

    private void configurarBotones() {
        findViewById(R.id.btnDetalles).setOnClickListener(v ->
                startActivity(new Intent(this, DetallesActivity.class)));

        findViewById(R.id.btnVerCalendario).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarioActivity.class)));

        findViewById(R.id.cardRutina).setOnClickListener(v ->
                startActivity(new Intent(this, RutinaActivity.class)));

        // AQUÍ ESTABA EL ERROR: Llave y paréntesis corregidos
        findViewById(R.id.cardReservar).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarioActivity.class));
        });

        findViewById(R.id.fab).setOnClickListener(v ->
                Toast.makeText(this, "Hola Carlos, ¿cómo va el entreno?", Toast.LENGTH_SHORT).show());
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}