package com.example.sporthub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class Chat extends AppCompatActivity {

    private EditText editMessage;
    private TextView txtResponse;
    private static final String WEBHOOK_URL =
            "https://hook.eu1.make.com/q7n6l956tkrhen73sk874pf6mmyyb4dt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        editMessage = findViewById(R.id.editMessage);
        Button btnSend = findViewById(R.id.btnSend);
        txtResponse = findViewById(R.id.txtResponse);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = editMessage.getText().toString().trim();
        if (message.isEmpty()) {
            txtResponse.setText("Escribe un mensaje primero");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        // Convertimos el mensaje a JSON en String
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
            txtResponse.setText("Error creando JSON");
            return;
        }
        final String requestBody = jsonBody.toString();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                WEBHOOK_URL,
                response -> txtResponse.setText(response), // recibimos texto plano
                error -> txtResponse.setText("Error: " + error.toString())
        ) {
            @Override
            public byte[] getBody() {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        queue.add(stringRequest);
    }
}