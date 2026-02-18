package com.example.sporthub;

public class Reserva {
    private String documentId; // ID único de la clase en Firestore
    private String nombreActividad;
    private String fecha;
    private String hora;
    private String nombreUsuario; // Para el nombre de la persona que reserva
    private int plazasDisponibles;
    private int plazasTotales;

    // Constructor vacío (NECESARIO para Firestore)
    public Reserva() {
        this.plazasTotales = 20;
    }

    // Constructor con parámetros - ORDEN CORREGIDO
    public Reserva(String nombreActividad, String fecha, String hora, String documentId, int plazasDisponibles) {
        this.nombreActividad = nombreActividad;
        this.fecha = fecha;
        this.hora = hora;
        this.documentId = documentId;
        this.plazasDisponibles = plazasDisponibles;
        this.plazasTotales = 20;
    }

    // Getters y Setters necesarios
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getNombreActividad() { return nombreActividad; }
    public void setNombreActividad(String nombreActividad) { this.nombreActividad = nombreActividad; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public int getPlazasDisponibles() { return plazasDisponibles; }
    public void setPlazasDisponibles(int plazasDisponibles) { this.plazasDisponibles = plazasDisponibles; }

    public int getPlazasTotales() { return plazasTotales; }
    public void setPlazasTotales(int plazasTotales) { this.plazasTotales = plazasTotales; }
}