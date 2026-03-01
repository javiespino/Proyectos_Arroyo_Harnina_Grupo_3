package com.example.sporthub;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder> {

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<Mensaje> lista;
    private final String  miUid; // UID del usuario actual para distinguir enviado/recibido

    public MensajeAdapter(List<Mensaje> lista, String miUid) {
        this.lista = lista;
        this.miUid = miUid;
    }

    public void actualizarLista(List<Mensaje> nueva) {
        this.lista = nueva;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mensaje, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje msg = lista.get(position);
        boolean esMio = miUid.equals(msg.getRemitenteUid());

        // Texto y hora
        holder.tvTexto.setText(msg.getTexto());
        holder.tvHora.setText(SDF.format(new Date(msg.getTimestamp())));

        // Nombre del remitente solo en mensajes recibidos
        if (!esMio && msg.getRemitenteNombre() != null) {
            holder.tvNombre.setVisibility(View.VISIBLE);
            holder.tvNombre.setText(msg.getRemitenteNombre());
        } else {
            holder.tvNombre.setVisibility(View.GONE);
        }

        // Alinear burbuja: derecha si es mío, izquierda si es del otro
        LinearLayout layoutMensaje = (LinearLayout) holder.itemView;
        layoutMensaje.setGravity(esMio ? Gravity.END : Gravity.START);

        // Color de burbuja
        if (esMio) {
            holder.bubbleContainer.setBackgroundResource(R.drawable.bg_bubble_sent);
            holder.tvTexto.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
            holder.tvHora.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        } else {
            holder.bubbleContainer.setBackgroundResource(R.drawable.bg_bubble_received);
            holder.tvTexto.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
            holder.tvHora.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class MensajeViewHolder extends RecyclerView.ViewHolder {
        LinearLayout bubbleContainer;
        TextView tvNombre, tvTexto, tvHora;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            tvNombre        = itemView.findViewById(R.id.tvNombreRemitente);
            tvTexto         = itemView.findViewById(R.id.tvTextoMensaje);
            tvHora          = itemView.findViewById(R.id.tvHoraMensaje);
        }
    }
}