package com.example.sporthub;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarioActivity extends AppCompatActivity implements HorariosAdapter.OnHorarioClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private MaterialToolbar toolbar;
    private Spinner spinnerActividades;
    private CalendarView calendarView;
    private RecyclerView recyclerHorarios;
    private CardView cardDetallesReserva;
    private TextView tvDetallesActividad, tvDetallesFecha, tvDetallesHora, tvDetallesPlazas;
    private Button btnConfirmarReserva;
    private ProgressBar progressBar;

    // Adapter
    private HorariosAdapter horariosAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Variables de reserva actual
    private String actividadSeleccionada = "";
    private String fechaSeleccionada = "";
    private String horarioSeleccionado = "";
    private int plazasDisponibles = 0;

    // Horarios disponibles
    private static final String[] HORAS_INICIO   = {"09:00", "12:00", "18:00", "20:00"};
    private static final String[] HORAS_COMPLETAS = {"09:00 - 10:00", "12:00 - 13:00", "18:00 - 19:00", "20:00 - 21:00"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        inicializarVistas();
        configurarToolbar();
        configurarSpinner();
        configurarCalendario();
        configurarRecyclerView();
        configurarBotonConfirmar();

        fechaSeleccionada = obtenerFechaActual();
    }

    private void inicializarVistas() {
        toolbar             = findViewById(R.id.toolbar);
        spinnerActividades  = findViewById(R.id.spinnerActividades);
        calendarView        = findViewById(R.id.calendarView);
        recyclerHorarios    = findViewById(R.id.recyclerHorarios);
        cardDetallesReserva = findViewById(R.id.cardDetallesReserva);
        tvDetallesActividad = findViewById(R.id.tvDetallesActividad);
        tvDetallesFecha     = findViewById(R.id.tvDetallesFecha);
        tvDetallesHora      = findViewById(R.id.tvDetallesHora);
        tvDetallesPlazas    = findViewById(R.id.tvDetallesPlazas);
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        progressBar         = findViewById(R.id.progressBar);
    }

    private void configurarToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void configurarSpinner() {
        String[] actividades = {
                "Selecciona una actividad",
                "Zumba", "Spinning", "Yoga", "Pilates",
                "CrossFit", "GAP", "Body Pump", "Aerobic"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, actividades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActividades.setAdapter(adapter);

        spinnerActividades.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    actividadSeleccionada = actividades[position];
                    cargarHorariosDesdeFirestore(); // ← carga plazas reales
                    verificarDatosCompletos();
                } else {
                    actividadSeleccionada = "";
                    btnConfirmarReserva.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                actividadSeleccionada = "";
            }
        });
    }

    private void configurarCalendario() {
        calendarView.setMinDate(System.currentTimeMillis());

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 3);
        calendarView.setMaxDate(maxDate.getTimeInMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fechaSeleccionada = sdf.format(calendar.getTime());

            cargarHorariosDesdeFirestore(); // ← carga plazas reales al cambiar fecha
            verificarDatosCompletos();
            Toast.makeText(this, "Fecha: " + fechaSeleccionada, Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarRecyclerView() {
        horariosAdapter = new HorariosAdapter(this);
        recyclerHorarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerHorarios.setAdapter(horariosAdapter);
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar horarios con plazas reales desde Firestore
    // ══════════════════════════════════════════════════════════════

    private void cargarHorariosDesdeFirestore() {
        if (actividadSeleccionada.isEmpty() || fechaSeleccionada.isEmpty()) return;

        String fechaGuion = fechaSeleccionada.replace("/", "-");

        List<String> horarios = new ArrayList<>();
        List<Integer> plazas  = new ArrayList<>();

        int[] contador = {0};

        for (int i = 0; i < HORAS_INICIO.length; i++) {
            String docId        = actividadSeleccionada + "_" + fechaGuion + "_" + HORAS_INICIO[i];
            String horaCompleta = HORAS_COMPLETAS[i];

            db.collection("clases").document(docId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Long plazasDisp = doc.getLong("plazasDisponibles");
                            horarios.add(horaCompleta);
                            plazas.add(plazasDisp != null ? plazasDisp.intValue() : 0);
                        }
                        contador[0]++;
                        if (contador[0] == HORAS_INICIO.length) {
                            // Ordenar según HORAS_COMPLETAS antes de mostrar
                            List<String> horariosOrdenados = new ArrayList<>();
                            List<Integer> plazasOrdenadas  = new ArrayList<>();
                            for (String horaOrden : HORAS_COMPLETAS) {
                                int idx = horarios.indexOf(horaOrden);
                                if (idx != -1) {
                                    horariosOrdenados.add(horarios.get(idx));
                                    plazasOrdenadas.add(plazas.get(idx));
                                }
                            }
                            horariosAdapter.actualizarHorarios(horariosOrdenados, plazasOrdenadas);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error al cargar horario: " + docId, e);
                        contador[0]++;
                        if (contador[0] == HORAS_INICIO.length) {
                            List<String> horariosOrdenados = new ArrayList<>();
                            List<Integer> plazasOrdenadas  = new ArrayList<>();
                            for (String horaOrden : HORAS_COMPLETAS) {
                                int idx = horarios.indexOf(horaOrden);
                                if (idx != -1) {
                                    horariosOrdenados.add(horarios.get(idx));
                                    plazasOrdenadas.add(plazas.get(idx));
                                }
                            }
                            horariosAdapter.actualizarHorarios(horariosOrdenados, plazasOrdenadas);
                        }
                    });
        }
    }

    @Override
    public void onHorarioClick(String horario, int plazas, int position) {
        horarioSeleccionado = horario;
        plazasDisponibles   = plazas;
        mostrarDetallesReserva();
        verificarDatosCompletos();
    }

    private void mostrarDetallesReserva() {
        if (!actividadSeleccionada.isEmpty() && !fechaSeleccionada.isEmpty() && !horarioSeleccionado.isEmpty()) {
            cardDetallesReserva.setVisibility(View.VISIBLE);
            tvDetallesActividad.setText("Actividad: " + actividadSeleccionada);
            tvDetallesFecha.setText("Fecha: " + fechaSeleccionada);
            tvDetallesHora.setText("Hora: " + horarioSeleccionado);
            tvDetallesPlazas.setText("Plazas disponibles: " + plazasDisponibles + "/20");
        }
    }

    private void verificarDatosCompletos() {
        boolean datosCompletos = !actividadSeleccionada.isEmpty()
                && !fechaSeleccionada.isEmpty()
                && !horarioSeleccionado.isEmpty();

        // Si la fecha es hoy, comprobamos que la hora no haya pasado
        if (datosCompletos && esHoy(fechaSeleccionada)) {
            datosCompletos = !horarioPasado(horarioSeleccionado);
            if (!datosCompletos) {
                Toast.makeText(this, "⚠️ Este horario ya ha pasado hoy", Toast.LENGTH_SHORT).show();
            }
        }

        btnConfirmarReserva.setEnabled(datosCompletos && plazasDisponibles > 0);
    }

    private boolean esHoy(String fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return fecha.equals(sdf.format(new Date()));
    }

    private boolean horarioPasado(String horario) {
        try {
            String horaInicio = horario.split(" - ")[0];
            String[] partes   = horaInicio.split(":");

            Calendar claseTime = Calendar.getInstance();
            claseTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(partes[0]));
            claseTime.set(Calendar.MINUTE,      Integer.parseInt(partes[1]));
            claseTime.set(Calendar.SECOND, 0);

            return Calendar.getInstance().after(claseTime);
        } catch (Exception e) {
            return false;
        }
    }

    private void configurarBotonConfirmar() {
        btnConfirmarReserva.setOnClickListener(v -> {
            if (verificarPermisos()) {
                confirmarReserva();
            } else {
                solicitarPermisos();
            }
        });
    }

    private boolean verificarPermisos() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisos() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                confirmarReserva();
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede añadir al calendario.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Confirmar reserva: Firestore + Calendario del dispositivo
    // ══════════════════════════════════════════════════════════════

    private void confirmarReserva() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: sesión no iniciada", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmarReserva.setEnabled(false);

        String horaInicio = horarioSeleccionado.split(" - ")[0];
        String fechaGuion = fechaSeleccionada.replace("/", "-");
        String claseId    = actividadSeleccionada + "_" + fechaGuion + "_" + horaInicio;

        Map<String, Object> reservaMap = new HashMap<>();
        reservaMap.put("claseId",         claseId);
        reservaMap.put("usuarioId",       user.getUid());
        reservaMap.put("nombreActividad", actividadSeleccionada);
        reservaMap.put("completada",      false);

        db.collection("reservas")
                .add(reservaMap)
                .addOnSuccessListener(documentReference -> {

                    // Restar 1 plaza en la clase
                    db.collection("clases")
                            .document(claseId)
                            .update("plazasDisponibles", FieldValue.increment(-1))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error al restar plaza", e));

                    boolean exitoCalendario = guardarEnCalendario();

                    progressBar.setVisibility(View.GONE);

                    if (exitoCalendario) {
                        Toast.makeText(this, "✅ Reserva confirmada", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "✅ Reserva guardada (no se pudo añadir al calendario)",
                                Toast.LENGTH_LONG).show();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmarReserva.setEnabled(true);
                    Toast.makeText(this, "❌ Error al guardar reserva: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Guardar en calendario del dispositivo
    // ══════════════════════════════════════════════════════════════

    private boolean guardarEnCalendario() {
        try {
            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();

            Calendar beginTime = Calendar.getInstance();
            String[] partesFecha = fechaSeleccionada.split("/");
            String[] partesHora  = horarioSeleccionado.split(" - ")[0].split(":");

            beginTime.set(
                    Integer.parseInt(partesFecha[2]),
                    Integer.parseInt(partesFecha[1]) - 1,
                    Integer.parseInt(partesFecha[0]),
                    Integer.parseInt(partesHora[0]),
                    Integer.parseInt(partesHora[1])
            );

            Calendar endTime = (Calendar) beginTime.clone();
            endTime.add(Calendar.HOUR, 1);

            values.put(CalendarContract.Events.DTSTART, beginTime.getTimeInMillis());
            values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis());
            values.put(CalendarContract.Events.TITLE, "Clase de " + actividadSeleccionada);
            values.put(CalendarContract.Events.DESCRIPTION,
                    "Reserva SportHub\nPlazas disponibles: " + plazasDisponibles + "/20");
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Madrid");

            cr.insert(CalendarContract.Events.CONTENT_URI, values);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}