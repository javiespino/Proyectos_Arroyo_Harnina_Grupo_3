package com.example.sporthub;

public class Reserva {

    // Campos para Firestore (colección `reservas`)
    private String id;
    private String claseId;
    private String usuarioId;
    private String nombreActividad;
    private boolean completada;

    // Campos para mostrar en UI (HorariosAdapter / DetallesActivity)
    private String fecha;
    private String hora;
    private int plazasDisponibles;
    private int plazasTotales;
    private String documentId;

    // Constructor vacío OBLIGATORIO para Firestore
    public Reserva() {
        this.plazasTotales = 20;
    }

    // Constructor para HorariosAdapter (CalendarioActivity)
    public Reserva(String nombreActividad, String fecha, String hora,
                   String documentId, int plazasDisponibles) {
        this.nombreActividad   = nombreActividad;
        this.fecha             = fecha;
        this.hora              = hora;
        this.documentId        = documentId;
        this.claseId           = documentId;
        this.plazasDisponibles = plazasDisponibles;
        this.plazasTotales     = 20;
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

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public int getPlazasDisponibles() { return plazasDisponibles; }
    public void setPlazasDisponibles(int plazasDisponibles) { this.plazasDisponibles = plazasDisponibles; }

    public int getPlazasTotales() { return plazasTotales; }
    public void setPlazasTotales(int plazasTotales) { this.plazasTotales = plazasTotales; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}