package com.example.sporthub;

/**
 * Modelo que representa una clase dirigida tal y como la ve el entrenador.
 *
 * Campos extra respecto a Reserva:
 *   cancelada            → true si el entrenador marcó ausencia
 *   entrenadorSustituto  → nombre del sustituto asignado (o "" si no hay)
 */
public class ClaseEntrenador {

    private String docId;               // ID del documento en "clases"
    private String nombreActividad;
    private String fecha;               // "dd/MM/yyyy"
    private String hora;                // "09:00 - 10:00"
    private boolean cancelada;
    private String entrenadorSustituto; // nombre o ""

    // Constructor vacío para Firestore
    public ClaseEntrenador() {}

    public ClaseEntrenador(String docId, String nombreActividad,
                           String fecha, String hora,
                           boolean cancelada, String entrenadorSustituto) {
        this.docId               = docId;
        this.nombreActividad     = nombreActividad;
        this.fecha               = fecha;
        this.hora                = hora;
        this.cancelada           = cancelada;
        this.entrenadorSustituto = entrenadorSustituto;
    }

    // ── Getters y Setters ──────────────────────────────────────────

    public String getDocId()                     { return docId; }
    public void   setDocId(String docId)         { this.docId = docId; }

    public String getNombreActividad()                          { return nombreActividad; }
    public void   setNombreActividad(String nombreActividad)    { this.nombreActividad = nombreActividad; }

    public String getFecha()               { return fecha; }
    public void   setFecha(String fecha)   { this.fecha = fecha; }

    public String getHora()              { return hora; }
    public void   setHora(String hora)   { this.hora = hora; }

    public boolean isCancelada()                 { return cancelada; }
    public void    setCancelada(boolean c)       { this.cancelada = c; }

    public String getEntrenadorSustituto()                              { return entrenadorSustituto; }
    public void   setEntrenadorSustituto(String entrenadorSustituto)   { this.entrenadorSustituto = entrenadorSustituto; }
}