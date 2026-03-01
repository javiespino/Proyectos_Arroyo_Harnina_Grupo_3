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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Header: temperatura y humedad
    private TextView tvTemperatura, tvHumedadHeader;

    // Card azul: progreso de reservas completadas
    private ProgressBar progressBar;
    private TextView tvPorcentajeCentral;

    private TextView tvNombreUsuario;
    private BottomNavigationView bottomNavigationView;

    // Próximas clases
    private RecyclerView recyclerProximasClases;
    private TextView tvSinClases;
    private ProximasClasesAdapter proximasAdapter;
    private List<Reserva> proximasList = new ArrayList<>();

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

        tvTemperatura   = findViewById(R.id.tvClima);
        tvHumedadHeader = findViewById(R.id.tvHumedadHeader);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        progressBar         = findViewById(R.id.progressBar);
        tvPorcentajeCentral = findViewById(R.id.tvPorcentajeCentral);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Próximas clases
        recyclerProximasClases = findViewById(R.id.recyclerProximasClases);
        tvSinClases = findViewById(R.id.tvSinClases);
        proximasAdapter = new ProximasClasesAdapter(proximasList);
        recyclerProximasClases.setLayoutManager(new LinearLayoutManager(this));
        recyclerProximasClases.setAdapter(proximasAdapter);

        // Chat flotante
        chatContainer = findViewById(R.id.chatContainer);
        editMessage   = findViewById(R.id.editMessage);
        txtResponse   = findViewById(R.id.txtResponse);
        btnSend       = findViewById(R.id.btnSend);

        configurarMargenes();
        configurarNavegacion();
        configurarBotonesAccion();
        configurarChat();

        // CORRECCIÓN: primero verificamos el rol antes de cargar datos.
        // Si el usuario no es cliente (rol "c"), lo redirigimos a su pantalla correcta.
        verificarRolYCargarDatos();
    }

    // ══════════════════════════════════════════════════════════════
    // CORRECCIÓN: verificar rol antes de mostrar la pantalla
    // ══════════════════════════════════════════════════════════════

    private void verificarRolYCargarDatos() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            // Sin sesión activa → volver al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    String rol = doc.getString("rol");

                    if ("e".equals(rol)) {
                        // Es entrenador → redirigir a su pantalla
                        startActivity(new Intent(this, MainActivityEntrenador.class));
                        finish();
                        return;
                    }

                    // Es cliente (rol "c") o cualquier otro → cargar datos normalmente
                    String name = doc.getString("name");
                    if (name != null) tvNombreUsuario.setText(name);

                    cargarDatosMeteorologicos();
                    cargarProgresoReservas();
                    cargarProximasClases();
                    cargarAvisoClasesCanceladas(); // RF 3.19
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al verificar rol", e);
                    // Si falla Firestore cargamos igualmente para no bloquear al usuario
                    cargarDatosMeteorologicos();
                    cargarProgresoReservas();
                    cargarProximasClases();
                    cargarAvisoClasesCanceladas();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Próximas clases desde Firestore
    // ══════════════════════════════════════════════════════════════

    private void cargarProximasClases() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String hoy = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        db.collection("reservas")
                .whereEqualTo("usuarioId", user.getUid())
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(query -> {
                    List<Reserva> futuras = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        String claseId = doc.getString("claseId");
                        String nombreActividad = doc.getString("nombreActividad");

                        if (claseId == null) continue;

                        String[] partes = claseId.split("_");
                        if (partes.length >= 2) {
                            String fechaClase = partes[1];
                            if (esFechaFuturaOHoy(fechaClase, hoy)) {
                                Reserva r = new Reserva();
                                r.setClaseId(claseId);
                                r.setNombreActividad(nombreActividad);
                                futuras.add(r);
                            }
                        }
                    }

                    futuras.sort((a, b) -> {
                        String fa = a.getClaseId() != null ? a.getClaseId() : "";
                        String fb = b.getClaseId() != null ? b.getClaseId() : "";
                        return fa.compareTo(fb);
                    });

                    List<Reserva> top3 = futuras.size() > 3 ? futuras.subList(0, 3) : futuras;
                    proximasAdapter.actualizar(new ArrayList<>(top3));

                    if (top3.isEmpty()) {
                        tvSinClases.setVisibility(View.VISIBLE);
                        recyclerProximasClases.setVisibility(View.GONE);
                    } else {
                        tvSinClases.setVisibility(View.GONE);
                        recyclerProximasClases.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error al cargar próximas clases", e));
    }

    private boolean esFechaFuturaOHoy(String fechaClase, String hoy) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date dClase = sdf.parse(fechaClase);
            Date dHoy   = sdf.parse(hoy);
            return dClase != null && !dClase.before(dHoy);
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // RF 3.19 — Aviso al cliente si alguna clase está cancelada
    // ══════════════════════════════════════════════════════════════

    private void cargarAvisoClasesCanceladas() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("reservas")
                .whereEqualTo("usuarioId", user.getUid())
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;

                    List<String> claseIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        String claseId = doc.getString("claseId");
                        if (claseId != null) claseIds.add(claseId);
                    }

                    final int[] consultadas = {0};
                    final List<String> avisos = new ArrayList<>();

                    for (String claseId : claseIds) {
                        db.collection("clases").document(claseId).get()
                                .addOnSuccessListener(claseDoc -> {
                                    consultadas[0]++;

                                    if (claseDoc.exists()) {
                                        Boolean cancelada = claseDoc.getBoolean("cancelada");
                                        if (cancelada != null && cancelada) {
                                            String actividad = claseDoc.getString("nombreActividad");
                                            String fecha     = claseDoc.getString("fecha");
                                            String hora      = claseDoc.getString("hora");
                                            String sustituto = claseDoc.getString("entrenadorSustituto");

                                            String linea = "• " + actividad + " — " + fecha + " " + hora;
                                            if (sustituto != null && !sustituto.isEmpty()
                                                    && !"Sin sustituto".equals(sustituto)) {
                                                linea += "\n  👤 Sustituto: " + sustituto;
                                            } else {
                                                linea += "\n  ⚠️ Sin sustituto asignado";
                                            }
                                            avisos.add(linea);
                                        }
                                    }

                                    if (consultadas[0] == claseIds.size() && !avisos.isEmpty()) {
                                        runOnUiThread(() -> mostrarDialogoAvisosCancelaciones(avisos));
                                    }
                                })
                                .addOnFailureListener(e -> consultadas[0]++);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("MainActivity", "Error al comprobar clases canceladas", e));
    }

    private void mostrarDialogoAvisosCancelaciones(List<String> avisos) {
        StringBuilder sb = new StringBuilder("Las siguientes clases han sido canceladas:\n\n");
        for (String aviso : avisos) {
            sb.append(aviso).append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Clases canceladas")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Entendido", null)
                .show();
    }

    // ══════════════════════════════════════════════════════════════
    // Chat flotante
    // ══════════════════════════════════════════════════════════════

    private void configurarChat() {
        findViewById(R.id.fab).setOnClickListener(v -> {
            if (chatContainer.getVisibility() == View.GONE) {
                chatContainer.setVisibility(View.VISIBLE);
            } else {
                chatContainer.setVisibility(View.GONE);
            }
        });

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
        Request request = new Request.Builder().url(WEBHOOK_URL).post(body).build();
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
                        txtResponse.setText(chatbotResponse.isEmpty() ? res : chatbotResponse);
                    } catch (Exception e) {
                        txtResponse.setText(res);
                    }
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Meteorología
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
    // Progreso reservas
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
    // Botones de acción
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
                startActivity(new Intent(this, ChatEntrenador.class));
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