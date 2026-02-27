package com.example.sporthub;

import android.graphics.Color;
import android.util.Log;
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

    private static final String TAG = "HorariosAdapter";
    private List<Reserva> listaReservas = new ArrayList<>();
    private OnHorarioClickListener listener;
    private int selectedPosition = -1;

    public interface OnHorarioClickListener {
        void onHorarioClick(Reserva reserva, int position);
    }

    public HorariosAdapter(OnHorarioClickListener listener) {
        this.listener = listener;
        Log.d(TAG, "Adaptador creado");
    }

    public void actualizarLista(List<Reserva> nuevaLista) {
        this.listaReservas = nuevaLista;
        this.selectedPosition = -1;
        notifyDataSetChanged();
        Log.d(TAG, "Lista actualizada con " + nuevaLista.size() + " elementos");
    }

    @NonNull
    @Override
    public HorarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario, parent, false);
        Log.d(TAG, "ViewHolder creado");
        return new HorarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorarioViewHolder holder, int position) {
        Reserva reserva = listaReservas.get(position);

        holder.tvHora.setText(reserva.getHora());
        holder.tvPlazas.setText(reserva.getPlazasDisponibles() + "/" + reserva.getPlazasTotales() + " plazas");

        boolean isSelected = selectedPosition == position;
        holder.cardHorario.setCardBackgroundColor(isSelected ?
                Color.parseColor("#E3F2FD") : Color.WHITE);

        if (reserva.getPlazasDisponibles() == 0) {
            holder.cardHorario.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.tvPlazas.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            holder.tvPlazas.setTextColor(Color.parseColor("#757575"));
        }

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition != RecyclerView.NO_POSITION) {
                selectedPosition = clickedPosition;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onHorarioClick(reserva, selectedPosition);
                }
                Log.d(TAG, "Click en posición: " + clickedPosition + ", Hora: " + reserva.getHora());
            }
        });

        Log.d(TAG, "Bind posición " + position + ": " + reserva.getHora() + " - " +
                reserva.getPlazasDisponibles() + " plazas");
    }

    @Override
    public int getItemCount() {
        int count = listaReservas.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class HorarioViewHolder extends RecyclerView.ViewHolder {
        CardView cardHorario;
        TextView tvHora, tvPlazas;

        public HorarioViewHolder(@NonNull View itemView) {
            super(itemView);
            // Referencias a tu layout item_horario.xml
            cardHorario = itemView.findViewById(R.id.cardHorario);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvPlazas = itemView.findViewById(R.id.tvPlazas);
        }
    }
}