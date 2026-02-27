package com.example.sporthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProximasClasesAdapter extends RecyclerView.Adapter<ProximasClasesAdapter.ViewHolder> {

    private List<Reserva> lista;

    public ProximasClasesAdapter(List<Reserva> lista) {
        this.lista = lista;
    }

    public void actualizar(List<Reserva> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proxima_clase, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reserva reserva = lista.get(position);

        holder.tvNombreActividad.setText(reserva.getNombreActividad());

        // Extraer fecha y hora del claseId: "Pilates_28-02-2026_12:00"
        String claseId = reserva.getClaseId();
        String fecha = "";
        String hora = "";
        if (claseId != null) {
            String[] partes = claseId.split("_");
            if (partes.length >= 3) {
                fecha = partes[1]; // "28-02-2026"
                hora  = partes[2]; // "12:00"
                // Convertir guiones a barras para mostrar
                fecha = fecha.replace("-", "/");
            }
        }
        holder.tvFecha.setText("📅 " + fecha);
        holder.tvHora.setText("🕒 " + hora);
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreActividad, tvFecha, tvHora;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreActividad = itemView.findViewById(R.id.tvNombreActividadProxima);
            tvFecha           = itemView.findViewById(R.id.tvFechaProxima);
            tvHora            = itemView.findViewById(R.id.tvHoraProxima);
        }
    }
}