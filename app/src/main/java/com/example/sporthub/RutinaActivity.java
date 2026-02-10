package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RutinaActivity extends AppCompatActivity {

    private Spinner spinnerDias, spinnerEjercicios;
    private Button btnAnadirEjercicio;
    private RecyclerView recyclerEjercicios;

    private ArrayList<DiaRutina> diasRutina;
    private ArrayList<Object> itemsRecycler; // Mezcla de DiaRutina y Ejercicios
    private RutinaAdapter rutinaAdapter;

    private DBHelper db;
    private String uid; // UID del usuario de Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutina);

        spinnerDias = findViewById(R.id.spinnerDias);
        spinnerEjercicios = findViewById(R.id.spinnerEjercicios);
        btnAnadirEjercicio = findViewById(R.id.btnAnadirEjercicio);
        recyclerEjercicios = findViewById(R.id.recyclerEjercicios);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = new DBHelper(this);

        // Spinner días
        ArrayAdapter<CharSequence> adapterDias = ArrayAdapter.createFromResource(this,
                R.array.dias_semana, android.R.layout.simple_spinner_item);
        adapterDias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDias.setAdapter(adapterDias);

        // Spinner ejercicios
        ArrayList<Ejercicio> spinnerEjerciciosList = new ArrayList<>();
        spinnerEjerciciosList.add(new Ejercicio("Press de banca", "pecho"));
        spinnerEjerciciosList.add(new Ejercicio("Flexiones", "pecho"));
        spinnerEjerciciosList.add(new Ejercicio("Dominadas", "espalda"));
        spinnerEjerciciosList.add(new Ejercicio("Remo con barra", "espalda"));
        spinnerEjerciciosList.add(new Ejercicio("Sentadillas", "pierna"));
        spinnerEjerciciosList.add(new Ejercicio("Zancadas", "pierna"));
        spinnerEjerciciosList.add(new Ejercicio("Curl bíceps", "brazo"));
        spinnerEjerciciosList.add(new Ejercicio("Fondos tríceps", "brazo"));

        ArrayAdapter<Ejercicio> adapterEjercicios = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerEjerciciosList);
        adapterEjercicios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEjercicios.setAdapter(adapterEjercicios);

        // RecyclerView
        diasRutina = db.obtenerRutinaCompleta(uid);
        itemsRecycler = new ArrayList<>();
        actualizarItemsRecycler();

        rutinaAdapter = new RutinaAdapter(itemsRecycler);
        recyclerEjercicios.setLayoutManager(new LinearLayoutManager(this));
        recyclerEjercicios.setAdapter(rutinaAdapter);

        // Botón añadir ejercicio
        btnAnadirEjercicio.setOnClickListener(v -> {
            String diaSeleccionado = spinnerDias.getSelectedItem().toString().toLowerCase();
            Ejercicio ejercicioSeleccionado = (Ejercicio) spinnerEjercicios.getSelectedItem();

            // Guardar en SQLite solo para este usuario
            db.insertarEjercicio(uid, diaSeleccionado,
                    ejercicioSeleccionado.getNombre(),
                    ejercicioSeleccionado.getGrupoMuscular());

            // Actualizar lista en memoria
            diasRutina = db.obtenerRutinaCompleta(uid);
            actualizarItemsRecycler();
            rutinaAdapter.notifyDataSetChanged();

            Toast.makeText(RutinaActivity.this,
                    ejercicioSeleccionado.getNombre() + " añadido para " + diaSeleccionado,
                    Toast.LENGTH_SHORT).show();
        });
    }

    // Convierte la lista de DiasRutina en items mezclados (header + ejercicios)
    private void actualizarItemsRecycler() {
        itemsRecycler.clear();
        for (DiaRutina dia : diasRutina) {
            itemsRecycler.add(dia); // header
            itemsRecycler.addAll(dia.getEjercicios()); // ejercicios del día
        }
    }
}
