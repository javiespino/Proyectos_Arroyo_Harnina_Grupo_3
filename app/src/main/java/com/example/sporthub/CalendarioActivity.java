package com.example.sporthub;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    // Variables de reserva actual
    private String actividadSeleccionada = "";
    private String fechaSeleccionada = "";
    private String horarioSeleccionado = "";
    private int plazasDisponibles = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        inicializarVistas();
        configurarToolbar();
        configurarSpinner();
        configurarCalendario();
        configurarRecyclerView();
        configurarBotonConfirmar();

        // Establecer fecha actual por defecto
        fechaSeleccionada = obtenerFechaActual();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        spinnerActividades = findViewById(R.id.spinnerActividades);
        calendarView = findViewById(R.id.calendarView);
        recyclerHorarios = findViewById(R.id.recyclerHorarios);
        cardDetallesReserva = findViewById(R.id.cardDetallesReserva);
        tvDetallesActividad = findViewById(R.id.tvDetallesActividad);
        tvDetallesFecha = findViewById(R.id.tvDetallesFecha);
        tvDetallesHora = findViewById(R.id.tvDetallesHora);
        tvDetallesPlazas = findViewById(R.id.tvDetallesPlazas);
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        progressBar = findViewById(R.id.progressBar);
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
        // Lista de actividades
        String[] actividades = {
                "Selecciona una actividad",
                "Zumba",
                "Spinning / Bici",
                "Yoga",
                "Pilates",
                "CrossFit",
                "GAP",
                "Body Pump",
                "Aerobic"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                actividades
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActividades.setAdapter(adapter);

        spinnerActividades.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    actividadSeleccionada = actividades[position];
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
        // Establecer fecha mínima (hoy)
        calendarView.setMinDate(System.currentTimeMillis());

        // Establecer fecha máxima (3 meses desde hoy)
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 3);
        calendarView.setMaxDate(maxDate.getTimeInMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fechaSeleccionada = sdf.format(calendar.getTime());

            verificarDatosCompletos();
            Toast.makeText(this, "Fecha seleccionada: " + fechaSeleccionada, Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarRecyclerView() {
        horariosAdapter = new HorariosAdapter(this);
        recyclerHorarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerHorarios.setAdapter(horariosAdapter);
    }

    @Override
    public void onHorarioClick(String horario, int plazas, int position) {
        horarioSeleccionado = horario;
        plazasDisponibles = plazas;

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
        boolean datosCompletos = !actividadSeleccionada.isEmpty() &&
                !fechaSeleccionada.isEmpty() &&
                !horarioSeleccionado.isEmpty();

        btnConfirmarReserva.setEnabled(datosCompletos && plazasDisponibles > 0);
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
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                },
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                confirmarReserva();
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede añadir al calendario.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmarReserva() {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmarReserva.setEnabled(false);

        // Simular procesamiento
        recyclerHorarios.postDelayed(() -> {
            boolean exito = guardarEnCalendario();

            progressBar.setVisibility(View.GONE);

            if (exito) {
                Toast.makeText(this, "✅ Reserva confirmada exitosamente", Toast.LENGTH_LONG).show();
                finish(); // Volver a MainActivity
            } else {
                Toast.makeText(this, "❌ Error al confirmar reserva", Toast.LENGTH_SHORT).show();
                btnConfirmarReserva.setEnabled(true);
            }
        }, 1500);
    }

    private boolean guardarEnCalendario() {
        try {
            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();

            // Calcular tiempo de inicio
            Calendar beginTime = Calendar.getInstance();
            String[] partesFecha = fechaSeleccionada.split("/");
            String[] partesHora = horarioSeleccionado.split(" - ")[0].split(":");

            beginTime.set(
                    Integer.parseInt(partesFecha[2]), // año
                    Integer.parseInt(partesFecha[1]) - 1, // mes (0-indexed)
                    Integer.parseInt(partesFecha[0]), // día
                    Integer.parseInt(partesHora[0]), // hora
                    Integer.parseInt(partesHora[1]) // minutos
            );

            // Calcular tiempo de fin (1 hora después)
            Calendar endTime = (Calendar) beginTime.clone();
            endTime.add(Calendar.HOUR, 1);

            values.put(CalendarContract.Events.DTSTART, beginTime.getTimeInMillis());
            values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis());
            values.put(CalendarContract.Events.TITLE, "Clase de " + actividadSeleccionada);
            values.put(CalendarContract.Events.DESCRIPTION,
                    "Reserva de clase en SportHub\nPlazas disponibles: " + plazasDisponibles + "/20");
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