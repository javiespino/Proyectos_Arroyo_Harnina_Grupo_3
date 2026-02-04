package com.example.sporthub;

public class Reserva {
    private String nombreActividad;
    private String fecha;
    private String hora;
    private String nombreUsuario;
    private int plazasDisponibles;
    private int plazasTotales;

    // Constructor vacío
    public Reserva() {
        this.plazasTotales = 20; // Capacidad por defecto
    }

    // Constructor completo
    public Reserva(String nombreActividad, String fecha, String hora, String nombreUsuario, int plazasDisponibles) {
        this.nombreActividad = nombreActividad;
        this.fecha = fecha;
        this.hora = hora;
        this.nombreUsuario = nombreUsuario;
        this.plazasDisponibles = plazasDisponibles;
        this.plazasTotales = 20;
    }

    // Getters y Setters
    public String getNombreActividad() {
        return nombreActividad;
    }

    public void setNombreActividad(String nombreActividad) {
        this.nombreActividad = nombreActividad;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public int getPlazasDisponibles() {
        return plazasDisponibles;
    }

    public void setPlazasDisponibles(int plazasDisponibles) {
        this.plazasDisponibles = plazasDisponibles;
    }

    public int getPlazasTotales() {
        return plazasTotales;
    }

    public void setPlazasTotales(int plazasTotales) {
        this.plazasTotales = plazasTotales;
    }

    public boolean hayPlazasDisponibles() {
        return plazasDisponibles > 0;
    }

    @Override
    public String toString() {
        return nombreActividad + " - " + fecha + " a las " + hora +
                " (Plazas: " + plazasDisponibles + "/" + plazasTotales + ")";
    }
}
