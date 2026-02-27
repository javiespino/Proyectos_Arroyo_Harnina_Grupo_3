package com.example.sporthub;

public class Reserva {

    private String id;               // ID del documento en Firestore
    private String claseId;          // ID de la clase (doc en colección `clases`)
    private String usuarioId;        // UID de Firebase Auth
    private String nombreActividad;  // Nombre de la actividad (Zumba, Yoga...)
    private boolean completada;      // true si el usuario ya asistió

    // Constructor vacío obligatorio para Firestore
    public Reserva() {}

    // Constructor completo
    public Reserva(String claseId, String usuarioId, String nombreActividad) {
        this.claseId = claseId;
        this.usuarioId = usuarioId;
        this.nombreActividad = nombreActividad;
        this.completada = false;
    }

    // ── Getters y Setters ──────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClaseId() { return claseId; }
    public void setClaseId(String claseId) { this.claseId = claseId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreActividad() { return nombreActividad; }
    public void setNombreActividad(String nombreActividad) { this.nombreActividad = nombreActividad; }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
}