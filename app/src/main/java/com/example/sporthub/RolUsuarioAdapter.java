package com.example.sporthub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RolUsuarioAdapter extends RecyclerView.Adapter<RolUsuarioAdapter.ViewHolder> {

    public interface OnRolChangeListener {
        void onRolChange(ClienteInfo usuario, String nuevoRol);
    }

    private final List<ClienteInfo>   lista;
    private final OnRolChangeListener listener;

    public RolUsuarioAdapter(List<ClienteInfo> lista, OnRolChangeListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rol_usuario, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClienteInfo usuario = lista.get(position);
        String rol = usuario.getRol();

        holder.tvNombre.setText(usuario.getNombre());

        // Badge de rol actual con color
        switch (rol) {
            case "e":
                holder.tvRolActual.setText("Entrenador");
                holder.tvRolActual.setTextColor(Color.parseColor("#1565C0"));
                holder.card.setCardBackgroundColor(Color.parseColor("#F0F4FF"));
                break;
            case "m":
                holder.tvRolActual.setText("Moderador");
                holder.tvRolActual.setTextColor(Color.parseColor("#E65100"));
                holder.card.setCardBackgroundColor(Color.parseColor("#FFF8F0"));
                break;
            default: // "c" cliente
                holder.tvRolActual.setText("Cliente");
                holder.tvRolActual.setTextColor(Color.parseColor("#4CAF50"));
                holder.card.setCardBackgroundColor(Color.WHITE);
                break;
        }

        // Botón "Hacer Entrenador" — solo visible si NO es ya entrenador
        if ("e".equals(rol)) {
            holder.btnEntrenador.setVisibility(View.GONE);
        } else {
            holder.btnEntrenador.setVisibility(View.VISIBLE);
            holder.btnEntrenador.setOnClickListener(v ->
                    listener.onRolChange(usuario, "e"));
        }

        // Botón "Hacer Moderador" — solo visible si NO es ya moderador
        if ("m".equals(rol)) {
            holder.btnModerador.setVisibility(View.GONE);
        } else {
            holder.btnModerador.setVisibility(View.VISIBLE);
            holder.btnModerador.setOnClickListener(v ->
                    listener.onRolChange(usuario, "m"));
        }

        // Botón "Quitar rol" — solo visible si tiene un rol especial (no es cliente)
        if ("c".equals(rol)) {
            holder.btnQuitarRol.setVisibility(View.GONE);
        } else {
            holder.btnQuitarRol.setVisibility(View.VISIBLE);
            holder.btnQuitarRol.setOnClickListener(v ->
                    listener.onRolChange(usuario, "c"));
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvNombre, tvRolActual;
        Button   btnEntrenador, btnModerador, btnQuitarRol;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card          = itemView.findViewById(R.id.cardRolUsuario);
            tvNombre      = itemView.findViewById(R.id.tvNombreRolUsuario);
            tvRolActual   = itemView.findViewById(R.id.tvRolActual);
            btnEntrenador = itemView.findViewById(R.id.btnHacerEntrenador);
            btnModerador  = itemView.findViewById(R.id.btnHacerModerador);
            btnQuitarRol  = itemView.findViewById(R.id.btnQuitarRol);
        }
    }
}