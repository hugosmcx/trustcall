package br.com.hssoftware.trustcall;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface Listener {
        void onAdicionarListaBranca(HistoryEntry entry);
        void onAdicionarListaNegra(HistoryEntry entry);
    }

    private final List<HistoryEntry> itens;
    private final Listener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public HistoryAdapter(List<HistoryEntry> itens, Listener listener) {
        this.itens = itens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry entry = itens.get(position);
        android.content.Context context = holder.itemView.getContext();

        boolean oculto = entry.numero == null || entry.numero.isEmpty();
        holder.textViewNumero.setText(oculto ? context.getString(R.string.numero_oculto_label) : entry.numero);

        String motivoLabel = context.getString(entry.motivo.labelResId);
        holder.textViewMeta.setText(motivoLabel + " · " + dateFormat.format(entry.timestamp));

        int corIcone = ContextCompat.getColor(context,
                entry.motivo == BlockReason.LISTA_NEGRA ? R.color.brand_warning : R.color.brand_on_surface_variant);
        holder.imageViewMotivo.setColorFilter(corIcone, PorterDuff.Mode.SRC_IN);

        boolean semNumero = oculto;
        holder.buttonWhitelist.setEnabled(!semNumero);
        holder.buttonBlacklist.setEnabled(!semNumero);
        holder.buttonWhitelist.setAlpha(semNumero ? 0.3f : 1f);
        holder.buttonBlacklist.setAlpha(semNumero ? 0.3f : 1f);

        holder.buttonWhitelist.setOnClickListener(v -> listener.onAdicionarListaBranca(entry));
        holder.buttonBlacklist.setOnClickListener(v -> listener.onAdicionarListaNegra(entry));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewNumero;
        final TextView textViewMeta;
        final ImageView imageViewMotivo;
        final ImageButton buttonWhitelist;
        final ImageButton buttonBlacklist;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNumero = itemView.findViewById(R.id.textViewNumero);
            textViewMeta = itemView.findViewById(R.id.textViewMeta);
            imageViewMotivo = itemView.findViewById(R.id.imageViewMotivo);
            buttonWhitelist = itemView.findViewById(R.id.buttonWhitelist);
            buttonBlacklist = itemView.findViewById(R.id.buttonBlacklist);
        }
    }
}
