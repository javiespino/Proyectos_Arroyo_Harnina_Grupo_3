package com.example.sporthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RutinaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DIA = 0;
    private static final int TYPE_EJERCICIO = 1;

    private ArrayList<Object> items; // Mezcla de DiaRutina y Ejercicio

    public RutinaAdapter(ArrayList<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof DiaRutina) {
            return TYPE_DIA;
        } else {
            return TYPE_EJERCICIO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DIA) {
            // Asegúrate de que item_dia.xml tenga el TextView con id: tvNombreDia
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dia, parent, false);
            return new DiaViewHolder(view);
        } else {
            // Asegúrate de que item_ejercicio.xml tenga los IDs: tvNombreEjercicio e imgGrupoMuscular
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ejercicio, parent, false);
            return new EjercicioViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_DIA) {
            DiaRutina dia = (DiaRutina) items.get(position);
            ((DiaViewHolder) holder).tvNombreDia.setText(dia.getNombreDia());
        } else {
            Ejercicio ejercicio = (Ejercicio) items.get(position);
            EjercicioViewHolder evh = (EjercicioViewHolder) holder;

            evh.tvNombreEjercicio.setText(ejercicio.getNombre());

            // Normalizamos el texto: quitamos espacios y pasamos a minúsculas
            String grupo = ejercicio.getGrupoMuscular().toLowerCase().trim();

            // Selección de imagen dinámica
            switch (grupo) {
                case "pecho":
                    evh.imgGrupoMuscular.setImageResource(R.drawable.ic_pecho);
                    break;
                case "espalda":
                    evh.imgGrupoMuscular.setImageResource(R.drawable.ic_espalda);
                    break;
                case "pierna":
                    evh.imgGrupoMuscular.setImageResource(R.drawable.ic_pierna);
                    break;
                case "brazo":
                    evh.imgGrupoMuscular.setImageResource(R.drawable.ic_brazo);
                    break;
                default:
                    // Imagen genérica si el grupo no coincide
                    evh.imgGrupoMuscular.setImageResource(R.drawable.ic_brazo);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder para los Headers de los días
    static class DiaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreDia;
        public DiaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreDia = itemView.findViewById(R.id.tvNombreDia);
        }
    }

    // ViewHolder para los ejercicios individuales
    static class EjercicioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreEjercicio;
        ImageView imgGrupoMuscular;

        public EjercicioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
            imgGrupoMuscular = itemView.findViewById(R.id.imgGrupoMuscular);
        }
    }
}