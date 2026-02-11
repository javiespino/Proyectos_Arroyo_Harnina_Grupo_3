package com.example.sporthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EjercicioAdapter extends RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder> {

    private ArrayList<Ejercicio> ejercicios;

    public EjercicioAdapter(ArrayList<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    @NonNull
    @Override
    public EjercicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio, parent, false);
        return new EjercicioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EjercicioViewHolder holder, int position) {
        Ejercicio ejercicio = ejercicios.get(position);
        holder.tvNombreEjercicio.setText(ejercicio.getNombre());

        // Cambiar imagen según el grupo muscular
        switch (ejercicio.getGrupoMuscular().toLowerCase()) {
            case "pecho":
                holder.imgGrupoMuscular.setImageResource(R.drawable.ic_pecho);
                break;
            case "espalda":
                holder.imgGrupoMuscular.setImageResource(R.drawable.ic_espalda);
                break;
            case "pierna":
                holder.imgGrupoMuscular.setImageResource(R.drawable.ic_pierna);
                break;
            case "brazo":
                holder.imgGrupoMuscular.setImageResource(R.drawable.ic_brazo);
                break;
            default:
                holder.imgGrupoMuscular.setImageResource(R.drawable.ic_brazo);
        }
    }

    @Override
    public int getItemCount() {
        return ejercicios.size();
    }

    public static class EjercicioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreEjercicio;
        ImageView imgGrupoMuscular;

        public EjercicioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
            imgGrupoMuscular = itemView.findViewById(R.id.imgGrupoMuscular);
        }
    }
}
