package com.example.sporthub;

public class ClienteChat {
    private String uid;
    private String nombre;

    public ClienteChat(String uid, String nombre) {
        this.uid    = uid;
        this.nombre = nombre;
    }

    public String getUid()    { return uid; }
    public String getNombre() { return nombre; }
}