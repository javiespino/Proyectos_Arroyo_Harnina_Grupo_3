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

public class LesionAdapter extends RecyclerView.Adapter<LesionAdapter.LesionViewHolder> {

    public interface OnLesionActionListener {
        void onMarcarRecuperado(Lesion lesion, int position);
        void onEliminar(Lesion lesion, int position);
    }

    private List<Lesion> lista;
    private OnLesionActionListener listener;

    public LesionAdapter(List<Lesion> lista, OnLesionActionListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public void actualizarLista(List<Lesion> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LesionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesion, parent, false);
        return new LesionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LesionViewHolder holder, int position) {
        Lesion lesion = lista.get(position);

        holder.tvZona.setText(lesion.getZona());
        holder.tvDescripcion.setText(lesion.getDescripcion() != null && !lesion.getDescripcion().isEmpty()
                ? lesion.getDescripcion() : "Sin notas");

        String fechas = "Inicio: " + lesion.getFechaInicio();
        if (lesion.getFechaFin() != null && !lesion.getFechaFin().isEmpty()) {
            fechas += "  ·  Fin: " + lesion.getFechaFin();
        }
        holder.tvFechas.setText(fechas);

        if (lesion.isRecuperado()) {
            holder.tvEstado.setText("✓ Recuperado");
            holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"));
            holder.card.setCardBackgroundColor(Color.parseColor("#F1F8E9"));
            holder.btnRecuperado.setVisibility(View.GONE);
        } else {
            holder.tvEstado.setText("● Activa");
            holder.tvEstado.setTextColor(Color.parseColor("#F44336"));
            holder.card.setCardBackgroundColor(Color.WHITE);
            holder.btnRecuperado.setVisibility(View.VISIBLE);
        }

        holder.btnRecuperado.setOnClickListener(v -> {
            if (listener != null) listener.onMarcarRecuperado(lesion, holder.getAdapterPosition());
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminar(lesion, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class LesionViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvZona, tvDescripcion, tvFechas, tvEstado;
        android.widget.Button btnRecuperado, btnEliminar;

        public LesionViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardLesion);
            tvZona = itemView.findViewById(R.id.tvZonaLesion);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionLesion);
            tvFechas = itemView.findViewById(R.id.tvFechasLesion);
            tvEstado = itemView.findViewById(R.id.tvEstadoLesion);
            btnRecuperado = itemView.findViewById(R.id.btnMarcarRecuperado);
            btnEliminar = itemView.findViewById(R.id.btnEliminarLesion);
        }
    }
}