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

public class LesionAdapterSoloLectura extends RecyclerView.Adapter<LesionAdapterSoloLectura.ViewHolder> {

    private List<Lesion> lista;

    public LesionAdapterSoloLectura(List<Lesion> lista) {
        this.lista = lista;
    }

    public void actualizarLista(List<Lesion> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesion_readonly, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lesion lesion = lista.get(position);

        holder.tvZona.setText(lesion.getZona());
        holder.tvDescripcion.setText(
                lesion.getDescripcion() != null && !lesion.getDescripcion().isEmpty()
                        ? lesion.getDescripcion() : "Sin notas");

        String fechas = "Inicio: " + lesion.getFechaInicio();
        if (lesion.getFechaFin() != null && !lesion.getFechaFin().isEmpty()) {
            fechas += "  ·  Fin estimado: " + lesion.getFechaFin();
        }
        holder.tvFechas.setText(fechas);

        if (lesion.isRecuperado()) {
            holder.tvEstado.setText("✓ Recuperado");
            holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"));
            holder.card.setCardBackgroundColor(Color.parseColor("#F1F8E9"));
        } else {
            holder.tvEstado.setText("● Lesión activa");
            holder.tvEstado.setTextColor(Color.parseColor("#F44336"));
            holder.card.setCardBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvZona, tvDescripcion, tvFechas, tvEstado;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card          = itemView.findViewById(R.id.cardLesionReadonly);
            tvZona        = itemView.findViewById(R.id.tvZonaLesionReadonly);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionLesionReadonly);
            tvFechas      = itemView.findViewById(R.id.tvFechasLesionReadonly);
            tvEstado      = itemView.findViewById(R.id.tvEstadoLesionReadonly);
        }
    }
}