package com.example.sporthub;

import java.util.ArrayList;

public class DiaRutina {
    private String nombreDia; // lunes, martes...
    private ArrayList<Ejercicio> ejercicios;

    public DiaRutina(String nombreDia) {
        this.nombreDia = nombreDia;
        this.ejercicios = new ArrayList<>();
    }

    public String getNombreDia() {
        return nombreDia;
    }

    public ArrayList<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void addEjercicio(Ejercicio e) {
        ejercicios.add(e);
    }
}
