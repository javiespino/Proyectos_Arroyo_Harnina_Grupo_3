package com.example.sporthub;

/**
 * Modelo de un mensaje del chat entrenador–cliente.
 * Se almacena en Firestore: colección "mensajes", subcolección de sala.
 */
public class Mensaje {

    private String id;
    private String texto;
    private String remitenteUid;  // UID de quien envía
    private String remitenteNombre;
    private long   timestamp;     // System.currentTimeMillis()

    // Constructor vacío obligatorio para Firestore
    public Mensaje() {}

    public Mensaje(String texto, String remitenteUid, String remitenteNombre) {
        this.texto           = texto;
        this.remitenteUid    = remitenteUid;
        this.remitenteNombre = remitenteNombre;
        this.timestamp       = System.currentTimeMillis();
    }

    public String getId()                       { return id; }
    public void   setId(String id)              { this.id = id; }

    public String getTexto()                    { return texto; }
    public void   setTexto(String texto)        { this.texto = texto; }

    public String getRemitenteUid()             { return remitenteUid; }
    public void   setRemitenteUid(String uid)   { this.remitenteUid = uid; }

    public String getRemitenteNombre()                      { return remitenteNombre; }
    public void   setRemitenteNombre(String nombre)         { this.remitenteNombre = nombre; }

    public long   getTimestamp()                { return timestamp; }
    public void   setTimestamp(long ts)         { this.timestamp = ts; }
}