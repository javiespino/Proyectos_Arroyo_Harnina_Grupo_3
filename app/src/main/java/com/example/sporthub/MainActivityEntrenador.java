package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
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

public class MainActivityEntrenador extends AppCompatActivity {

    private TextView tvTemperatura, tvHumedadHeader, tvNombreEntrenador;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String WEATHER_URL = "https://api.weather.com/v2/pws/observations/current?stationId=IALMEN70&format=json&units=m&apiKey=908477f6f2b84c6c8477f6f2b80c6c03";
    private OkHttpClient cliente = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_entrenador);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTemperatura = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvNombreEntrenador = findViewById(R.id.tvNombreEntrenador);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        configurarMargenes();
        configurarNavegacion();
        // configurarBotonesAccion(); // Descomenta si ya tienes los IDs en tu XML actual

        cargarDatosMeteorologicos();
        obtenerDatosUsuarioFirebase();
    }

    private void configurarNavegacion() {
        // Asegúrate de que este ID coincida con el de tu archivo menu/bottom_nav_menu_entrenador.xml
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_clientes) {
                // startActivity(new Intent(this, ClientesActivity.class));
                return true;
            } else if (id == R.id.nav_calendario) {
                startActivity(new Intent(this, CalendarioActivity.class));
                return true;
            } else if (id == R.id.nav_chat) {
                Toast.makeText(this, "Abriendo Chat...", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_mas) {
                // ANCLA EL MENÚ AL ICONO "MÁS"
                showMoreMenu(findViewById(R.id.nav_mas));
                return false; // False para que no se quede seleccionado el botón Más
            }
            return false;
        });
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_mas_opciones, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_rutinas_extra) {
                startActivity(new Intent(this, RutinaActivity.class));
                return true;
            } else if (id == R.id.nav_reportes) {
                Toast.makeText(this, "Abriendo Reportes...", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- MÉTODOS DE APOYO (CLIMA Y FIREBASE) ---

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
                    });
                } catch (Exception e) { Log.e("API", "Error JSON", e); }
            }
        });
    }

    private void obtenerDatosUsuarioFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            tvNombreEntrenador.setText(doc.getString("name"));
                        }
                    });
        }
    }

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}