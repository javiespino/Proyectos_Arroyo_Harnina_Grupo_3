package com.example.sporthub;

public class ClienteInfo {
    private String uid;
    private String nombre;
    private int lesionesActivas;
    private int lesionesRecuperadas;
    private String zonasActivas;

    public ClienteInfo(String uid, String nombre) {
        this.uid    = uid;
        this.nombre = nombre;
        this.lesionesActivas     = 0;
        this.lesionesRecuperadas = 0;
        this.zonasActivas        = "Cargando...";
    }

    public String getUid()                              { return uid; }
    public String getNombre()                           { return nombre; }
    public int    getLesionesActivas()                  { return lesionesActivas; }
    public void   setLesionesActivas(int n)             { this.lesionesActivas = n; }
    public int    getLesionesRecuperadas()              { return lesionesRecuperadas; }
    public void   setLesionesRecuperadas(int n)         { this.lesionesRecuperadas = n; }
    public String getZonasActivas()                     { return zonasActivas; }
    public void   setZonasActivas(String z)             { this.zonasActivas = z; }
}