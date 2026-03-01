package com.example.sporthub;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF 3.8 — Contactar con entrenador
 * El cliente tiene guardado su entrenadorUid en Firestore para garantizar
 * que siempre se conecte a la misma sala, independientemente de cuántos
 * entrenadores haya registrados.
 */
public class ChatEntrenador extends AppCompatActivity {

    private RecyclerView  recyclerMensajes;
    private EditText      editMessage;
    private ImageButton   btnSend, btnBack;
    private TextView      tvNombreChat;

    private MensajeAdapter adapter;
    private List<Mensaje>  listaMensajes = new ArrayList<>();

    private FirebaseFirestore db;
    private String miUid;
    private String miNombre;
    private String salaId;
    private String uidInterlocutor;

    private ListenerRegistration listenerMensajes;

    private static final String COL_MENSAJES = "mensajes";
    private static final String COL_CHATS    = "chats";
    private static final String COL_USERS    = "users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_entrenador);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        miUid = user.getUid();

        inicializarVistas();
        cargarPerfilYConectar();
    }

    private void inicializarVistas() {
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMessage      = findViewById(R.id.editMessage);
        btnSend          = findViewById(R.id.btnSend);
        btnBack          = findViewById(R.id.btnBackChat);
        tvNombreChat     = findViewById(R.id.tvNombreChat);

        adapter = new MensajeAdapter(listaMensajes, miUid);
        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        recyclerMensajes.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> enviarMensaje());
    }

    private void cargarPerfilYConectar() {
        db.collection(COL_USERS).document(miUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    miNombre     = doc.getString("name");
                    String miRol = doc.getString("rol");

                    if ("e".equals(miRol)) {
                        // Soy entrenador: el intent trae el UID del cliente
                        String clienteUid = getIntent().getStringExtra("clienteUid");
                        if (clienteUid == null || clienteUid.isEmpty()) {
                            Toast.makeText(this,
                                    "Abre este chat desde la lista de clientes.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        uidInterlocutor = clienteUid;
                        // El entrenador construye la sala usando el entrenadorUid guardado en el cliente
                        cargarEntrenadorUidDelCliente(clienteUid);
                    } else {
                        // Soy cliente: verificar si ya tengo entrenador asignado
                        String entrenadorGuardado = doc.getString("entrenadorUid");
                        if (entrenadorGuardado != null && !entrenadorGuardado.isEmpty()) {
                            uidInterlocutor = entrenadorGuardado;
                            cargarNombreEntrenador(entrenadorGuardado);
                        } else {
                            asignarEntrenador();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Cliente: primera vez, asigna entrenador y lo guarda en su documento
    private void asignarEntrenador() {
        db.collection(COL_USERS)
                .whereEqualTo("rol", "e")
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this, "No hay entrenadores disponibles.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    QueryDocumentSnapshot docE = (QueryDocumentSnapshot) query.getDocuments().get(0);
                    uidInterlocutor = docE.getId();

                    // Guardar en Firestore para siempre usar el mismo
                    db.collection(COL_USERS).document(miUid)
                            .update("entrenadorUid", uidInterlocutor);

                    String nombre = docE.getString("name");
                    tvNombreChat.setText(nombre != null ? nombre : "Entrenador");
                    construirSalaYEscuchar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al buscar entrenador", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Cliente: ya tiene entrenador asignado, solo carga su nombre
    private void cargarNombreEntrenador(String uid) {
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String nombre = doc.getString("name");
                    tvNombreChat.setText(nombre != null ? nombre : "Entrenador");
                    construirSalaYEscuchar();
                })
                .addOnFailureListener(e -> construirSalaYEscuchar());
    }

    // Entrenador: lee el entrenadorUid guardado en el documento del cliente
    // para garantizar que la sala sea la misma que construyó el cliente
    private void cargarEntrenadorUidDelCliente(String clienteUid) {
        db.collection(COL_USERS).document(clienteUid).get()
                .addOnSuccessListener(doc -> {
                    String nombre = doc.getString("name");
                    tvNombreChat.setText(nombre != null ? nombre : "Cliente");

                    String entrenadorUidDelCliente = doc.getString("entrenadorUid");
                    if (entrenadorUidDelCliente != null && !entrenadorUidDelCliente.isEmpty()) {
                        // Usar el entrenadorUid que guardó el cliente para construir la misma sala
                        String entrenadorUidParaSala = entrenadorUidDelCliente;
                        salaId = clienteUid.compareTo(entrenadorUidParaSala) < 0
                                ? clienteUid + "_" + entrenadorUidParaSala
                                : entrenadorUidParaSala + "_" + clienteUid;
                    } else {
                        // El cliente nunca abrió el chat, construir sala con miUid (este entrenador)
                        salaId = clienteUid.compareTo(miUid) < 0
                                ? clienteUid + "_" + miUid
                                : miUid + "_" + clienteUid;
                    }
                    escucharMensajes();
                })
                .addOnFailureListener(e -> {
                    // Fallback
                    salaId = clienteUid.compareTo(miUid) < 0
                            ? clienteUid + "_" + miUid
                            : miUid + "_" + clienteUid;
                    escucharMensajes();
                });
    }

    private void construirSalaYEscuchar() {
        salaId = miUid.compareTo(uidInterlocutor) < 0
                ? miUid + "_" + uidInterlocutor
                : uidInterlocutor + "_" + miUid;
        escucharMensajes();
    }

    private void escucharMensajes() {
        listenerMensajes = db.collection(COL_MENSAJES)
                .document(salaId)
                .collection(COL_CHATS)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    listaMensajes.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Mensaje msg = doc.toObject(Mensaje.class);
                        msg.setId(doc.getId());
                        listaMensajes.add(msg);
                    }
                    adapter.actualizarLista(new ArrayList<>(listaMensajes));
                    if (!listaMensajes.isEmpty()) {
                        recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);
                    }
                });
    }

    private void enviarMensaje() {
        String texto = editMessage.getText().toString().trim();
        if (texto.isEmpty()) return;
        if (salaId == null) {
            Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("texto",           texto);
        data.put("remitenteUid",    miUid);
        data.put("remitenteNombre", miNombre != null ? miNombre : "Usuario");
        data.put("timestamp",       System.currentTimeMillis());

        db.collection(COL_MENSAJES)
                .document(salaId)
                .collection(COL_CHATS)
                .add(data)
                .addOnSuccessListener(ref -> editMessage.setText(""))
                .addOnFailureListener(ex ->
                        Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerMensajes != null) listenerMensajes.remove();
    }
}