package com.example.sporthub;

public class Ejercicio {
    private String nombre;
    private String grupoMuscular; // pecho, espalda, pierna, brazo

    public Ejercicio() {
        // Constructor vacío necesario para Firebase / SQLite
    }

    public Ejercicio(String nombre, String grupoMuscular) {
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    @Override
    public String toString() {
        // Esto se muestra en el Spinner: "pecho - Press de banca"
        return grupoMuscular + " - " + nombre;
    }
}
