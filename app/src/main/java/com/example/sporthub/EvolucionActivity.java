package com.example.sporthub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EvolucionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private BarChart barChart;
    private ProgressBar progressBar;
    private TextView tvTotalCompletadas, tvRacha, tvMejorSemana, tvPromedio;
    private TextView tvVacio;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Etiquetas de las últimas 6 semanas
    private final String[] etiquetasSemanas = new String[6];
    // Contadores de clases completadas por semana
    private final int[] clasesPorSemana = new int[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evolucion);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        btnBack             = findViewById(R.id.btnBackEvolucion);
        barChart            = findViewById(R.id.barChartEvolucion);
        progressBar         = findViewById(R.id.progressBarEvolucion);
        tvTotalCompletadas  = findViewById(R.id.tvTotalCompletadas);
        tvRacha             = findViewById(R.id.tvRacha);
        tvMejorSemana       = findViewById(R.id.tvMejorSemana);
        tvPromedio          = findViewById(R.id.tvPromedio);
        tvVacio             = findViewById(R.id.tvVacioEvolucion);

        btnBack.setOnClickListener(v -> finish());

        prepararEtiquetasSemanas();
        cargarDatos();
    }

    // ══════════════════════════════════════════════════════════════
    // Prepara las etiquetas "S1" a "S6" con fecha de inicio de semana
    // ══════════════════════════════════════════════════════════════

    private void prepararEtiquetasSemanas() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Ir al lunes de la semana actual
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int i = 5; i >= 0; i--) {
            etiquetasSemanas[i] = sdf.format(cal.getTime());
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Carga reservas completadas del usuario desde Firestore
    // ══════════════════════════════════════════════════════════════

    private void cargarDatos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("reservas")
                .whereEqualTo("usuarioId", user.getUid())
                .whereEqualTo("completada", true)
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);

                    if (query.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        barChart.setVisibility(View.GONE);
                        return;
                    }

                    // Reiniciamos contadores
                    for (int i = 0; i < 6; i++) clasesPorSemana[i] = 0;

                    // Calculamos las fechas de inicio de cada semana
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);

                    // inicioDeSemana[i] = lunes de esa semana
                    Date[] inicioDeSemana = new Date[6];
                    Date[] finDeSemana    = new Date[6];

                    for (int i = 5; i >= 0; i--) {
                        inicioDeSemana[i] = cal.getTime();
                        cal.add(Calendar.DAY_OF_YEAR, 6);
                        finDeSemana[i] = cal.getTime();
                        cal.add(Calendar.DAY_OF_YEAR, -6);
                        cal.add(Calendar.WEEK_OF_YEAR, -1);
                    }

                    SimpleDateFormat sdfClase = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : query) {
                        String claseId = doc.getString("claseId");
                        if (claseId == null) continue;

                        // claseId formato: "Actividad_dd-MM-yyyy_HH:mm"
                        String[] partes = claseId.split("_");
                        if (partes.length < 2) continue;

                        try {
                            Date fechaClase = sdfClase.parse(partes[1]);
                            if (fechaClase == null) continue;

                            for (int i = 0; i < 6; i++) {
                                if (!fechaClase.before(inicioDeSemana[i]) &&
                                        !fechaClase.after(finDeSemana[i])) {
                                    clasesPorSemana[i]++;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // fecha mal formada, ignoramos
                        }
                    }

                    mostrarGrafica();
                    calcularEstadisticas(query.size());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvVacio.setVisibility(View.VISIBLE);
                    barChart.setVisibility(View.GONE);
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Construye y muestra la gráfica de barras
    // ══════════════════════════════════════════════════════════════

    private void mostrarGrafica() {
        tvVacio.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        List<BarEntry> entradas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entradas.add(new BarEntry(i, clasesPorSemana[i]));
        }

        BarDataSet dataSet = new BarDataSet(entradas, "Clases completadas");
        dataSet.setColor(getColor(R.color.gym_blue));
        dataSet.setValueTextColor(getColor(R.color.gym_blue));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.animateY(800);
        barChart.setExtraBottomOffset(10f);

        // Eje X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(etiquetasSemanas));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getColor(R.color.gym_gray));
        xAxis.setTextSize(11f);

        // Eje Y izquierdo
        YAxis yAxisLeft = barChart.getAxisLeft();
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setTextColor(getColor(R.color.gym_gray));

        // Eje Y derecho desactivado
        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }

    // ══════════════════════════════════════════════════════════════
    // Calcula y muestra las estadísticas resumen
    // ══════════════════════════════════════════════════════════════

    private void calcularEstadisticas(int totalCompletadas) {
        // Total completadas
        tvTotalCompletadas.setText(String.valueOf(totalCompletadas));

        // Mejor semana
        int mejorIdx = 0;
        for (int i = 1; i < 6; i++) {
            if (clasesPorSemana[i] > clasesPorSemana[mejorIdx]) mejorIdx = i;
        }
        tvMejorSemana.setText(clasesPorSemana[mejorIdx] + " clases (" + etiquetasSemanas[mejorIdx] + ")");

        // Promedio por semana
        double promedio = 0;
        for (int c : clasesPorSemana) promedio += c;
        promedio /= 6.0;
        tvPromedio.setText(String.format(Locale.getDefault(), "%.1f clases/semana", promedio));

        // Racha de semanas consecutivas con al menos 1 clase
        int racha = 0;
        for (int i = 5; i >= 0; i--) {
            if (clasesPorSemana[i] > 0) racha++;
            else break;
        }
        tvRacha.setText(racha + (racha == 1 ? " semana" : " semanas"));
    }
}