package com.example.sporthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClienteChatAdapter extends RecyclerView.Adapter<ClienteChatAdapter.ViewHolder> {

    public interface OnClienteClickListener {
        void onClienteClick(ClienteChat cliente);
    }

    private final List<ClienteChat>      lista;
    private final OnClienteClickListener listener;

    public ClienteChatAdapter(List<ClienteChat> lista, OnClienteClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClienteChat cliente = lista.get(position);
        holder.tvNombre.setText(cliente.getNombre());
        holder.tvSubtitulo.setText("Toca para abrir el chat");
        holder.itemView.setOnClickListener(v -> listener.onClienteClick(cliente));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSubtitulo;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre    = itemView.findViewById(R.id.tvNombreCliente);
            tvSubtitulo = itemView.findViewById(R.id.tvSubtituloCliente);
        }
    }
}