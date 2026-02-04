package com.example.sporthub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HorariosAdapter extends RecyclerView.Adapter<HorariosAdapter.HorarioViewHolder> {

    private List<String> horarios;
    private List<Integer> plazasDisponibles;
    private OnHorarioClickListener listener;
    private int selectedPosition = -1;

    public interface OnHorarioClickListener {
        void onHorarioClick(String horario, int plazas, int position);
    }

    public HorariosAdapter(OnHorarioClickListener listener) {
        this.horarios = new ArrayList<>();
        this.plazasDisponibles = new ArrayList<>();
        this.listener = listener;
        cargarHorariosDefault();
    }

    private void cargarHorariosDefault() {
        // Horarios disponibles por defecto
        String[] horariosArray = {"09:00 - 10:00", "10:30 - 11:30", "12:00 - 13:00",
                "18:00 - 19:00", "19:00 - 20:00", "20:00 - 21:00"};
        Integer[] plazasArray = {15, 18, 12, 20, 17, 14}; // Plazas disponibles simuladas

        for (int i = 0; i < horariosArray.length; i++) {
            horarios.add(horariosArray[i]);
            plazasDisponibles.add(plazasArray[i]);
        }
    }

    @NonNull
    @Override
    public HorarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario, parent, false);
        return new HorarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorarioViewHolder holder, int position) {
        String horario = horarios.get(position);
        int plazas = plazasDisponibles.get(position);

        holder.tvHora.setText(horario);
        holder.tvPlazas.setText(plazas + "/20 plazas");

        // Cambiar color si está seleccionado
        if (selectedPosition == position) {
            holder.cardHorario.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            holder.cardHorario.setCardBackgroundColor(Color.WHITE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onHorarioClick(horario, plazas, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return horarios.size();
    }

    public void actualizarHorarios(List<String> nuevosHorarios, List<Integer> nuevasPlazas) {
        this.horarios = nuevosHorarios;
        this.plazasDisponibles = nuevasPlazas;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    static class HorarioViewHolder extends RecyclerView.ViewHolder {
        CardView cardHorario;
        TextView tvHora;
        TextView tvPlazas;

        public HorarioViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHorario = itemView.findViewById(R.id.cardHorario);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvPlazas = itemView.findViewById(R.id.tvPlazas);
        }
    }
}
