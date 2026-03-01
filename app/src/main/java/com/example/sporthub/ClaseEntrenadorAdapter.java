package com.example.sporthub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter para mostrar las clases del entrenador en GestionAusenciasActivity.
 *
 * Cada item muestra:
 *  - Nombre de actividad, fecha y hora
 *  - Estado: Activa / Cancelada
 *  - Sustituto (si hay)
 *  - Botón "Marcar ausencia" (si activa) o "Reactivar" (si cancelada)
 */
public class ClaseEntrenadorAdapter
        extends RecyclerView.Adapter<ClaseEntrenadorAdapter.ClaseViewHolder> {

    // ── Interfaz de callbacks ──────────────────────────────────────
    public interface OnClaseActionListener {
        void onMarcarAusencia(ClaseEntrenador clase, int position);
        void onReactivar(ClaseEntrenador clase, int position);
    }

    private List<ClaseEntrenador> lista;
    private final OnClaseActionListener listener;

    public ClaseEntrenadorAdapter(List<ClaseEntrenador> lista, OnClaseActionListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    /** Reemplaza la lista completa y refresca el RecyclerView */
    public void actualizarLista(List<ClaseEntrenador> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clase_entrenador, parent, false);
        return new ClaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClaseViewHolder holder, int position) {
        ClaseEntrenador clase = lista.get(position);

        // Texto principal
        holder.tvNombreActividad.setText(clase.getNombreActividad());
        holder.tvFechaHora.setText("📅 " + clase.getFecha() + "   🕒 " + clase.getHora());

        if (clase.isCancelada()) {
            // ── Estado CANCELADA ─────────────────────────────────
            holder.card.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // rojo suave
            holder.tvEstado.setText("❌ Cancelada");
            holder.tvEstado.setTextColor(Color.parseColor("#D32F2F"));

            // Mostrar sustituto si existe
            String sust = clase.getEntrenadorSustituto();
            if (sust != null && !sust.isEmpty() && !"Sin sustituto".equals(sust)) {
                holder.tvSustituto.setVisibility(View.VISIBLE);
                holder.tvSustituto.setText("👤 Sustituto: " + sust);
            } else {
                holder.tvSustituto.setVisibility(View.VISIBLE);
                holder.tvSustituto.setText("👤 Sin sustituto asignado");
            }

            holder.btnAccion.setText("Reactivar clase");
            holder.btnAccion.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.btnAccion.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onReactivar(clase, pos);
            });

        } else {
            // ── Estado ACTIVA ────────────────────────────────────
            holder.card.setCardBackgroundColor(Color.WHITE);
            holder.tvEstado.setText("✅ Activa");
            holder.tvEstado.setTextColor(Color.parseColor("#388E3C"));
            holder.tvSustituto.setVisibility(View.GONE);

            holder.btnAccion.setText("Marcar ausencia");
            holder.btnAccion.setBackgroundColor(Color.parseColor("#F44336"));
            holder.btnAccion.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onMarcarAusencia(clase, pos);
            });
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────
    static class ClaseViewHolder extends RecyclerView.ViewHolder {
        CardView     card;
        TextView     tvNombreActividad, tvFechaHora, tvEstado, tvSustituto;
        MaterialButton btnAccion;

        public ClaseViewHolder(@NonNull View itemView) {
            super(itemView);
            card               = itemView.findViewById(R.id.cardClaseEntrenador);
            tvNombreActividad  = itemView.findViewById(R.id.tvNombreActividadEntrenador);
            tvFechaHora        = itemView.findViewById(R.id.tvFechaHoraEntrenador);
            tvEstado           = itemView.findViewById(R.id.tvEstadoClaseEntrenador);
            tvSustituto        = itemView.findViewById(R.id.tvSustitutoEntrenador);
            btnAccion          = itemView.findViewById(R.id.btnAccionAusencia);
        }
    }
}