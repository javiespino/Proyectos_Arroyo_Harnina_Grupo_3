package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AbonosActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;

    // Card estado actual
    private TextView tvTipoAbono, tvEstadoAbono, tvFechaVencimiento, tvPrecioActual;
    private CardView cardAbonoActual;

    // Cards de contratación
    private CardView cardMensual, cardTrimestral, cardAnual;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String COL_ABONOS = "abonos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abonos);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        inicializarVistas();
        configurarToolbar();
        configurarBotonesAbono();
        cargarAbonoActual();
    }

    private void inicializarVistas() {
        toolbar           = findViewById(R.id.toolbar);
        progressBar       = findViewById(R.id.progressBar);
        tvTipoAbono       = findViewById(R.id.tvTipoAbono);
        tvEstadoAbono     = findViewById(R.id.tvEstadoAbono);
        tvFechaVencimiento = findViewById(R.id.tvFechaVencimiento);
        tvPrecioActual    = findViewById(R.id.tvPrecioActual);
        cardAbonoActual   = findViewById(R.id.cardAbonoActual);
        cardMensual       = findViewById(R.id.cardMensual);
        cardTrimestral    = findViewById(R.id.cardTrimestral);
        cardAnual         = findViewById(R.id.cardAnual);
    }

    private void configurarToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Abonos");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ══════════════════════════════════════════════════════════════
    // Cargar abono actual desde Firestore
    // ══════════════════════════════════════════════════════════════

    private void cargarAbonoActual() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection(COL_ABONOS).document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);

                    if (doc.exists()) {
                        String tipo       = doc.getString("tipo");
                        String estado     = doc.getString("estado");
                        String vencimiento = doc.getString("fechaVencimiento");
                        String precio     = doc.getString("precio");

                        tvTipoAbono.setText(tipo != null ? tipo : "Sin abono");
                        tvFechaVencimiento.setText("Vence: " + (vencimiento != null ? vencimiento : "-"));
                        tvPrecioActual.setText(precio != null ? precio : "-");

                        if ("activo".equals(estado)) {
                            tvEstadoAbono.setText("✅ Activo");
                            tvEstadoAbono.setTextColor(getColor(android.R.color.holo_green_dark));
                        } else {
                            tvEstadoAbono.setText("❌ Vencido");
                            tvEstadoAbono.setTextColor(getColor(android.R.color.holo_red_dark));
                        }

                        cardAbonoActual.setVisibility(View.VISIBLE);
                    } else {
                        tvTipoAbono.setText("Sin abono activo");
                        tvEstadoAbono.setText("—");
                        tvFechaVencimiento.setText("Contrata un abono para empezar");
                        tvPrecioActual.setText("");
                        cardAbonoActual.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar abono", Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════════════════════════
    // Configurar botones de cada tipo de abono
    // ══════════════════════════════════════════════════════════════

    private void configurarBotonesAbono() {
        cardMensual.setOnClickListener(v ->
                mostrarConfirmacion("Mensual", "29,99 €", 1));

        cardTrimestral.setOnClickListener(v ->
                mostrarConfirmacion("Trimestral", "74,99 €", 3));

        cardAnual.setOnClickListener(v ->
                mostrarConfirmacion("Anual", "249,99 €", 12));
    }

    private void mostrarConfirmacion(String tipo, String precio, int meses) {
        new AlertDialog.Builder(this)
                .setTitle("Contratar abono " + tipo)
                .setMessage("¿Confirmas la contratación del abono " + tipo +
                        " por " + precio + "?\n\n" +
                        "(Pago simulado — no se realizará ningún cargo real)")
                .setPositiveButton("✅ Confirmar", (dialog, which) ->
                        contratarAbono(tipo, precio, meses))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ══════════════════════════════════════════════════════════════
    // Guardar abono en Firestore (simulación de pago)
    // ══════════════════════════════════════════════════════════════

    private void contratarAbono(String tipo, String precio, int meses) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // Calcular fecha de vencimiento
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, meses);
        String fechaVencimiento = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(cal.getTime());

        String fechaContratacion = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date());

        Map<String, Object> abonoMap = new HashMap<>();
        abonoMap.put("tipo",              tipo);
        abonoMap.put("precio",            precio);
        abonoMap.put("estado",            "activo");
        abonoMap.put("fechaContratacion", fechaContratacion);
        abonoMap.put("fechaVencimiento",  fechaVencimiento);
        abonoMap.put("usuarioId",         user.getUid());

        // El documento se llama igual que el UID del usuario
        db.collection(COL_ABONOS).document(user.getUid())
                .set(abonoMap)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "✅ Abono " + tipo + " activado hasta " + fechaVencimiento,
                            Toast.LENGTH_LONG).show();
                    cargarAbonoActual(); // Refresca la UI
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "❌ Error al contratar abono: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}