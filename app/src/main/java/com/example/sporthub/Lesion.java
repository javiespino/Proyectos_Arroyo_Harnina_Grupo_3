package com.example.sporthub;

public class Lesion {
    private String id;
    private String usuarioId;
    private String zona;          // Zona del cuerpo
    private String descripcion;   // Notas / descripción
    private String fechaInicio;   // "dd/MM/yyyy"
    private String fechaFin;      // "dd/MM/yyyy" - puede ser null
    private boolean recuperado;

    public Lesion() {}

    public Lesion(String usuarioId, String zona, String descripcion,
                  String fechaInicio, String fechaFin) {
        this.usuarioId = usuarioId;
        this.zona = zona;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.recuperado = false;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public boolean isRecuperado() { return recuperado; }
    public void setRecuperado(boolean recuperado) { this.recuperado = recuperado; }
}