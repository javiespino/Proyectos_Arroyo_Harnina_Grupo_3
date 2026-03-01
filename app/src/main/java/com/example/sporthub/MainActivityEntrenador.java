package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    // ── Botones acciones rápidas ───────────────────────────────────
    private Button btnGestionar;    // CLIENTES → (toast por ahora, sin activity de clientes)
    private Button btnCrearEditar;  // RUTINAS  → RutinaActivity
    private Button btnConfigurar;   // HORARIO/AUSENCIAS → GestionAusenciasActivity
    private Button btnVerAnaliticas;// REPORTES → (toast por ahora)
    // ──────────────────────────────────────────────────────────────

    private final String WEATHER_URL =
            "https://api.weather.com/v2/pws/observations/current?stationId=IALMEN70&format=json&units=m&apiKey=908477f6f2b84c6c8477f6f2b80c6c03";
    private OkHttpClient cliente = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_entrenador);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Vistas cabecera
        tvTemperatura      = findViewById(R.id.tvClima);
        tvHumedadHeader    = findViewById(R.id.tvHumedadHeader);
        tvNombreEntrenador = findViewById(R.id.tvNombreEntrenador);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Botones acciones rápidas
        btnGestionar     = findViewById(R.id.btnGestionar);
        btnCrearEditar   = findViewById(R.id.btnCrearEditar);
        btnConfigurar    = findViewById(R.id.btnConfigurar);
        btnVerAnaliticas = findViewById(R.id.btnVerAnaliticas);

        configurarMargenes();
        configurarNavegacion();
        configurarAccionesRapidas();

        cargarDatosMeteorologicos();
        obtenerDatosUsuarioFirebase();
    }

    // ══════════════════════════════════════════════════════════════
    // Acciones rápidas — botones del dashboard
    // ══════════════════════════════════════════════════════════════

    private void configurarAccionesRapidas() {

        // CLIENTES — sin activity específica todavía
        btnGestionar.setOnClickListener(v ->
                Toast.makeText(this, "Gestión de clientes — próximamente", Toast.LENGTH_SHORT).show()
        );

        // RUTINAS → RutinaActivity
        btnCrearEditar.setOnClickListener(v ->
                startActivity(new Intent(this, RutinaActivity.class))
        );

        // MI HORARIO / AUSENCIAS → GestionAusenciasActivity
        btnConfigurar.setOnClickListener(v ->
                startActivity(new Intent(this, GestionAusenciasActivity.class))
        );

        // REPORTES / SEGUIMIENTO — sin activity específica todavía
        btnVerAnaliticas.setOnClickListener(v ->
                Toast.makeText(this, "Reportes — próximamente", Toast.LENGTH_SHORT).show()
        );
    }

    // ══════════════════════════════════════════════════════════════
    // Navegación inferior
    // ══════════════════════════════════════════════════════════════

    private void configurarNavegacion() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_clientes) {
                Toast.makeText(this, "Gestión de clientes — próximamente", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_calendario) {
                startActivity(new Intent(this, CalendarioActivity.class));
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ListaChatsActivity.class));
                return true;
            } else if (id == R.id.nav_mas) {
                showMoreMenu(findViewById(R.id.nav_mas));
                return false;
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
                Toast.makeText(this, "Reportes — próximamente", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
                return true;
            } else if (id == R.id.nav_ausencias) {
                startActivity(new Intent(this, GestionAusenciasActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ══════════════════════════════════════════════════════════════
    // Datos meteorológicos
    // ══════════════════════════════════════════════════════════════

    private void cargarDatosMeteorologicos() {
        Request request = new Request.Builder().url(WEATHER_URL).build();
        cliente.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "Error red", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String jsonResponse = response.body().string();
                    JSONObject root = new JSONObject(jsonResponse);
                    JSONObject obs  = root.getJSONArray("observations").getJSONObject(0);
                    JSONObject metric = obs.getJSONObject("metric");
                    String temp = metric.getString("temp");
                    String hum  = obs.getString("humidity");
                    runOnUiThread(() -> {
                        tvTemperatura.setText(temp + "°C");
                        tvHumedadHeader.setText(hum + "%");
                    });
                } catch (Exception e) {
                    Log.e("API", "Error JSON", e);
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Datos del usuario (nombre)
    // ══════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════
    // Márgenes sistema
    // ══════════════════════════════════════════════════════════════

    private void configurarMargenes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}