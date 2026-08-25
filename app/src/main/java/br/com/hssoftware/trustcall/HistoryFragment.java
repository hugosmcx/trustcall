package br.com.hssoftware.trustcall;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class HistoryFragment extends Fragment {

    private enum Modo { BLOQUEADAS, BRANCA, NEGRA }

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private FloatingActionButton fabAdd;
    private Modo modoAtual = Modo.BLOQUEADAS;

    private TrustCallRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = TrustCallRepository.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        textViewEmpty = view.findViewById(R.id.textViewEmpty);
        fabAdd = view.findViewById(R.id.fabAdd);

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroupModo);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.buttonModoBloqueadas) modoAtual = Modo.BLOQUEADAS;
            else if (checkedId == R.id.buttonModoBranca) modoAtual = Modo.BRANCA;
            else modoAtual = Modo.NEGRA;
            atualizarLista();
        });

        fabAdd.setOnClickListener(v -> abrirDialogoAdicionar());
    }

    @Override
    public void onResume() {
        super.onResume();
        atualizarLista();
    }

    private void atualizarLista() {
        fabAdd.setVisibility(modoAtual == Modo.BLOQUEADAS ? View.GONE : View.VISIBLE);

        if (modoAtual == Modo.BLOQUEADAS) {
            List<HistoryEntry> historico = repository.getHistory();
            textViewEmpty.setText(R.string.history_empty);
            textViewEmpty.setVisibility(historico.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setAdapter(new HistoryAdapter(historico, new HistoryAdapter.Listener() {
                @Override
                public void onAdicionarListaBranca(HistoryEntry entry) {
                    repository.addToList(entry.numero, ListType.BRANCA);
                }

                @Override
                public void onAdicionarListaNegra(HistoryEntry entry) {
                    repository.addToList(entry.numero, ListType.NEGRA);
                }
            }));
        } else {
            ListType tipo = modoAtual == Modo.BRANCA ? ListType.BRANCA : ListType.NEGRA;
            List<NumberEntry> numeros = repository.getList(tipo);
            textViewEmpty.setText(modoAtual == Modo.BRANCA ? R.string.list_empty_branca : R.string.list_empty_negra);
            textViewEmpty.setVisibility(numeros.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setAdapter(new NumberListAdapter(numeros, entry -> {
                repository.removeFromList(entry.id);
                atualizarLista();
            }));
        }
    }

    private void abrirDialogoAdicionar() {
        ListType tipo = modoAtual == Modo.BRANCA ? ListType.BRANCA : ListType.NEGRA;

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.add_number_hint);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle(tipo == ListType.BRANCA ? R.string.add_number_title_branca : R.string.add_number_title_negra)
                .setView(input)
                .setPositiveButton(R.string.add_number_confirm, (dialog, which) -> {
                    String numero = input.getText().toString();
                    if (!numero.trim().isEmpty()) {
                        repository.addToList(numero, tipo);
                        atualizarLista();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
