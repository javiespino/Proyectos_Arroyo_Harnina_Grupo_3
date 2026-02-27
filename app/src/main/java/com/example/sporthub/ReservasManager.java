package com.example.sporthub;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Gestiona las operaciones de reservas del usuario en Firestore.
 *
 * Uso desde MainActivity:
 *   ReservasManager manager = new ReservasManager();
 *   manager.calcularProgreso(uid, progressBar, tvPorcentajeCentral);
 */
public class ReservasManager {

    private final FirebaseFirestore db;
    private static final String COL_RESERVAS = "reservas";
    private static final String COL_CLASES   = "clases";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ── Callbacks ──────────────────────────────────────────────────

    public interface ProgresoCallback {
        /** @param porcentaje 0-100 listo para setProgress()
         *  @param completadas número de reservas con completada==true
         *  @param total       número total de reservas del usuario */
        void onProgreso(int porcentaje, int completadas, int total);
        void onError(Exception e);
    }

    public interface OperacionCallback {
        void onExito();
        void onError(Exception e);
    }

    // ── Constructor ────────────────────────────────────────────────

    public ReservasManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ══════════════════════════════════════════════════════════════
    // 1 + 4 · Marcar reserva como completada (con validación de fecha)
    // ══════════════════════════════════════════════════════════════

    /**
     * Cambia el campo `completada` a true en la reserva indicada.
     * Antes valida que la fecha de la clase (String "dd/MM/yyyy" en
     * la colección `clases`) sea igual o anterior a hoy.
     *
     * @param reservaId ID del documento en la colección `reservas`
     * @param claseId   ID del documento en la colección `clases`
     * @param callback  resultado de la operación
     */
    public void marcarComoCompletada(String reservaId,
                                     String claseId,
                                     OperacionCallback callback) {

        // 1. Leemos la clase para obtener su fecha
        db.collection(COL_CLASES)
                .document(claseId)
                .get()
                .addOnSuccessListener(claseSnapshot -> {

                    if (!claseSnapshot.exists()) {
                        callback.onError(new Exception("La clase no existe: " + claseId));
                        return;
                    }

                    // 2. Leemos el campo fecha como String "dd/MM/yyyy"
                    String fechaStr = claseSnapshot.getString("fecha");
                    if (fechaStr == null || fechaStr.isEmpty()) {
                        callback.onError(new Exception("La clase no tiene campo 'fecha'"));
                        return;
                    }

                    // 3. Validamos que la fecha de la clase <= hoy
                    if (!esFechaPasadaOHoy(fechaStr)) {
                        callback.onError(
                                new Exception("No puedes completar una clase futura.")
                        );
                        return;
                    }

                    // 4. Fecha válida → actualizamos Firestore
                    db.collection(COL_RESERVAS)
                            .document(reservaId)
                            .update("completada", true)
                            .addOnSuccessListener(aVoid -> callback.onExito())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // ══════════════════════════════════════════════════════════════
    // 2 + 3 · Calcular progreso del usuario y devolver porcentaje
    // ══════════════════════════════════════════════════════════════

    /**
     * Consulta todas las reservas del usuario, cuenta cuántas están
     * completadas y devuelve el porcentaje por callback.
     *
     * @param usuarioId UID de Firebase Auth del usuario
     * @param callback  devuelve (porcentaje, completadas, total)
     */
    public void calcularProgreso(String usuarioId, ProgresoCallback callback) {

        db.collection(COL_RESERVAS)
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int total       = querySnapshot.size();
                    int completadas = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Boolean comp = doc.getBoolean("completada");
                        if (comp != null && comp) {
                            completadas++;
                        }
                    }

                    // Evitamos división por cero
                    int porcentaje = (total > 0)
                            ? (int) Math.round((completadas * 100.0) / total)
                            : 0;

                    callback.onProgreso(porcentaje, completadas, total);
                })
                .addOnFailureListener(callback::onError);
    }

    // ══════════════════════════════════════════════════════════════
    // Helper privado · Validación de fecha
    // ══════════════════════════════════════════════════════════════

    /**
     * Devuelve true si la fecha (String "dd/MM/yyyy") es hoy o anterior.
     */
    private boolean esFechaPasadaOHoy(String fechaStr) {
        try {
            // Normalizamos ambas fechas a medianoche para comparar solo el día
            Date fechaClase = SDF.parse(fechaStr);
            Date hoy        = SDF.parse(SDF.format(new Date())); // hoy a 00:00:00

            // fechaClase <= hoy  →  !fechaClase.after(hoy)
            return fechaClase != null && !fechaClase.after(hoy);

        } catch (ParseException e) {
            // Si el formato es incorrecto, bloqueamos por seguridad
            return false;
        }
    }
}