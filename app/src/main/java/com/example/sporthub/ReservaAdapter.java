package com.example.sporthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    public interface OnReservaActionListener {
        void onMarcarCompletada(String reservaId, String claseId);
        void onCancelar(String reservaId, int position);
    }

    private List<Reserva> reservas;
    private OnReservaActionListener listener;

    public ReservaAdapter(List<Reserva> reservas, OnReservaActionListener listener) {
        this.reservas = reservas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = reservas.get(position);

        holder.tvNombreActividad.setText(reserva.getNombreActividad());

        // Extraemos fecha y hora del claseId: "Zumba_28-02-2026_09:00"
        // Formato visible: "28/02/2026 · 09:00 - 10:00"
        String claseId = reserva.getClaseId();
        String fechaHora = parsearFechaHora(claseId);
        holder.tvFechaHora.setText("🕒 " + fechaHora);

        // Estado y color del badge
        if (reserva.isCompletada()) {
            holder.tvEstado.setText("✅ Completada");
            holder.tvEstado.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            holder.checkCompletada.setChecked(true);
            holder.checkCompletada.setEnabled(false); // No se puede desmarcar
        } else {
            holder.tvEstado.setText("⏳ Pendiente");
            holder.tvEstado.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
            holder.checkCompletada.setChecked(false);
            holder.checkCompletada.setEnabled(true);
        }

        // Checkbox → marcar como completada
        holder.checkCompletada.setOnCheckedChangeListener(null); // Evitar disparos al reciclar
        holder.checkCompletada.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !reserva.isCompletada()) {
                listener.onMarcarCompletada(reserva.getId(), reserva.getClaseId());
            }
        });

        // Botón cancelar
        holder.btnCancelar.setOnClickListener(v ->
                listener.onCancelar(reserva.getId(), holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    // Elimina un item de la lista localmente tras cancelar
    public void eliminarItem(int position) {
        reservas.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, reservas.size());
    }

    // Actualiza el estado de una reserva a completada localmente
    public void marcarCompletada(String reservaId) {
        for (int i = 0; i < reservas.size(); i++) {
            if (reservas.get(i).getId().equals(reservaId)) {
                reservas.get(i).setCompletada(true);
                notifyItemChanged(i);
                break;
            }
        }
    }

    // Parsea "Zumba_28-02-2026_09:00" → "28/02/2026 · 09:00"
    private String parsearFechaHora(String claseId) {
        try {
            String[] partes = claseId.split("_");
            // partes[0] = "Zumba", partes[1] = "28-02-2026", partes[2] = "09:00"
            String fecha = partes[1].replace("-", "/"); // "28/02/2026"
            String hora  = partes[2];                   // "09:00"
            return fecha + " · " + hora;
        } catch (Exception e) {
            return claseId;
        }
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreActividad, tvFechaHora, tvEstado, btnCancelar;
        CheckBox checkCompletada;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreActividad = itemView.findViewById(R.id.tvNombreActividad);
            tvFechaHora       = itemView.findViewById(R.id.tvFechaHora);
            tvEstado          = itemView.findViewById(R.id.tvEstado);
            btnCancelar       = itemView.findViewById(R.id.btnCancelar);
            checkCompletada   = itemView.findViewById(R.id.checkCompletada);
        }
    }
}