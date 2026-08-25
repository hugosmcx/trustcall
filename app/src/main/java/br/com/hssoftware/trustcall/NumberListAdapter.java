package br.com.hssoftware.trustcall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NumberListAdapter extends RecyclerView.Adapter<NumberListAdapter.ViewHolder> {

    public interface Listener {
        void onRemover(NumberEntry entry);
    }

    private final List<NumberEntry> itens;
    private final Listener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public NumberListAdapter(List<NumberEntry> itens, Listener listener) {
        this.itens = itens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_number_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NumberEntry entry = itens.get(position);
        holder.textViewNumero.setText(entry.numero);
        holder.textViewData.setText(dateFormat.format(entry.criadoEm));
        holder.buttonRemover.setOnClickListener(v -> listener.onRemover(entry));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewNumero;
        final TextView textViewData;
        final ImageButton buttonRemover;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNumero = itemView.findViewById(R.id.textViewNumero);
            textViewData = itemView.findViewById(R.id.textViewData);
            buttonRemover = itemView.findViewById(R.id.buttonRemover);
        }
    }
}
