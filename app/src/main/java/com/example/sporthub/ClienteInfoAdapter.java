package com.example.sporthub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClienteInfoAdapter extends RecyclerView.Adapter<ClienteInfoAdapter.ViewHolder> {

    public interface OnClienteClickListener {
        void onClienteClick(ClienteInfo cliente);
    }

    private final List<ClienteInfo>      lista;
    private final OnClienteClickListener listener;

    public ClienteInfoAdapter(List<ClienteInfo> lista, OnClienteClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente_info, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClienteInfo cliente = lista.get(position);

        holder.tvNombre.setText(cliente.getNombre());
        holder.tvZonas.setText("📍 " + cliente.getZonasActivas());

        int activas = cliente.getLesionesActivas();
        if (activas > 0) {
            holder.tvBadgeLesiones.setText(activas + " lesión" + (activas > 1 ? "es activas" : " activa"));
            holder.tvBadgeLesiones.setTextColor(Color.parseColor("#F44336"));
            holder.card.setCardBackgroundColor(Color.parseColor("#FFF8F8"));
        } else {
            holder.tvBadgeLesiones.setText("Sin lesiones activas ✓");
            holder.tvBadgeLesiones.setTextColor(Color.parseColor("#4CAF50"));
            holder.card.setCardBackgroundColor(Color.WHITE);
        }

        int recuperadas = cliente.getLesionesRecuperadas();
        holder.tvRecuperadas.setText(recuperadas > 0
                ? recuperadas + " recuperada" + (recuperadas > 1 ? "s" : "")
                : "");
        holder.tvRecuperadas.setVisibility(recuperadas > 0 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onClienteClick(cliente));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvNombre, tvZonas, tvBadgeLesiones, tvRecuperadas;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card              = itemView.findViewById(R.id.cardClienteInfo);
            tvNombre          = itemView.findViewById(R.id.tvNombreClienteInfo);
            tvZonas           = itemView.findViewById(R.id.tvZonasCliente);
            tvBadgeLesiones   = itemView.findViewById(R.id.tvBadgeLesiones);
            tvRecuperadas     = itemView.findViewById(R.id.tvRecuperadasCliente);
        }
    }
}