package com.example.sporthub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarioActivity extends AppCompatActivity
        implements HorariosAdapter.OnHorarioClickListener {

    private static final String TAG = "CalendarioActivity";

    private FirebaseFirestore db;
    private HorariosAdapter horariosAdapter;
    private ListenerRegistration listenerRegistration;

    private String actividadSeleccionada = "";
    private String fechaSeleccionada = "";
    private Reserva reservaSeleccionadaObj = null;

    // UI
    private Spinner spinnerActividades;
    private CalendarView calendarView;
    private View cardDetallesReserva;
    private TextView tvDetallesActividad, tvDetallesFecha, tvDetallesHora, tvDetallesPlazas;
    private Button btnConfirmarReserva;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        configurarToolbar();     // 👈 IMPORTANTE
        configurarRecyclerView();
        configurarSpinner();
        configurarCalendario();
        configurarBotonConfirmar();

        fechaSeleccionada = new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        spinnerActividades = findViewById(R.id.spinnerActividades);
        calendarView = findViewById(R.id.calendarView);
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
        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
    }

    private void sincronizarConFirestore() {

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (actividadSeleccionada.isEmpty()
                || actividadSeleccionada.equals("Selecciona una actividad")) {

            horariosAdapter.actualizarLista(new ArrayList<>());
            return;
        }

        listenerRegistration = db.collection("clases")
                .whereEqualTo("nombreActividad", actividadSeleccionada)
                .whereEqualTo("fecha", fechaSeleccionada)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Error: " + error.getMessage());
                        return;
                    }

                    List<Reserva> lista = new ArrayList<>();

                    if (value != null && !value.isEmpty()) {

                        for (QueryDocumentSnapshot doc : value) {
                            Reserva r = doc.toObject(Reserva.class);
                            r.setDocumentId(doc.getId());
                            lista.add(r);
                        }

                        horariosAdapter.actualizarLista(lista);

                    } else {
                        generarHorariosAuto();
                    }
                });
    }

    private void generarHorariosAuto() {

        String[] horas = {
                "09:00 - 10:00",
                "12:00 - 13:00",
                "18:00 - 19:00",
                "20:00 - 21:00"
        };

        for (String h : horas) {

            String idDoc = (actividadSeleccionada + "_"
                    + fechaSeleccionada + "_"
                    + h.split(" ")[0])
                    .replace("/", "-")
                    .replace(" ", "");

            Reserva nueva = new Reserva(
                    actividadSeleccionada,
                    fechaSeleccionada,
                    h,
                    idDoc,
                    20
            );

            db.collection("clases")
                    .document(idDoc)
                    .set(nueva);
        }
    }

    @Override
    public void onHorarioClick(Reserva reserva, int position) {

        reservaSeleccionadaObj = reserva;
        cardDetallesReserva.setVisibility(View.VISIBLE);

        tvDetallesActividad.setText("Actividad: " + reserva.getNombreActividad());
        tvDetallesFecha.setText("Fecha: " + reserva.getFecha());
        tvDetallesHora.setText("Hora: " + reserva.getHora());
        tvDetallesPlazas.setText("Plazas disponibles: "
                + reserva.getPlazasDisponibles() + "/20");

        btnConfirmarReserva.setEnabled(reserva.getPlazasDisponibles() > 0);
    }

    private void configurarBotonConfirmar() {

        btnConfirmarReserva.setOnClickListener(v -> {

            if (reservaSeleccionadaObj == null) {
                Toast.makeText(this,
                        "Selecciona un horario primero",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnConfirmarReserva.setEnabled(false);

            String docId = reservaSeleccionadaObj.getDocumentId();

            db.runTransaction(transaction -> {

                DocumentReference ref =
                        db.collection("clases").document(docId);

                DocumentSnapshot snapshot = transaction.get(ref);

                Long plazasActuales =
                        snapshot.getLong("plazasDisponibles");

                if (plazasActuales == null || plazasActuales <= 0) {
                    throw new FirebaseFirestoreException(
                            "No hay plazas disponibles",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                transaction.update(ref,
                        "plazasDisponibles",
                        plazasActuales - 1);

                return null;

            }).addOnSuccessListener(aVoid -> {

                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                        "¡Reserva confirmada!",
                        Toast.LENGTH_LONG).show();

                cardDetallesReserva.setVisibility(View.GONE);
                reservaSeleccionadaObj = null;

            }).addOnFailureListener(e -> {

                progressBar.setVisibility(View.GONE);
                btnConfirmarReserva.setEnabled(true);

                Toast.makeText(this,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();

                Log.e(TAG, "Error transacción: " + e.getMessage());
            });
        });
    }

    private void configurarSpinner() {

        String[] actividades = {
                "Selecciona una actividad",
                "Zumba",
                "Spinning",
                "Yoga",
                "CrossFit"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        actividades);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerActividades.setAdapter(adapter);

        spinnerActividades.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> p,
                                               View v,
                                               int pos,
                                               long id) {

                        actividadSeleccionada = actividades[pos];
                        cardDetallesReserva.setVisibility(View.GONE);
                        reservaSeleccionadaObj = null;

                        sincronizarConFirestore();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {}
                });
    }

    private void configurarCalendario() {

        calendarView.setOnDateChangeListener((v, year, month, day) -> {

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);

            fechaSeleccionada = new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            ).format(cal.getTime());

            cardDetallesReserva.setVisibility(View.GONE);
            reservaSeleccionadaObj = null;

            sincronizarConFirestore();
        });
    }

    private void configurarRecyclerView() {

        horariosAdapter = new HorariosAdapter(this);

        RecyclerView rv = findViewById(R.id.recyclerHorarios);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(horariosAdapter);
        rv.setNestedScrollingEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
