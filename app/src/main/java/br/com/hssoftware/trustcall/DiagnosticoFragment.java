package br.com.hssoftware.trustcall;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telecom.TelecomManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DiagnosticoFragment extends Fragment {

    private TextView textViewRelatorio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diagnostico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewRelatorio = view.findViewById(R.id.textViewRelatorio);

        view.findViewById(R.id.buttonAtualizar).setOnClickListener(v -> atualizarRelatorio());
        view.findViewById(R.id.buttonCopiar).setOnClickListener(v -> copiarRelatorio());
        view.findViewById(R.id.buttonBaixar).setOnClickListener(v -> baixarRelatorio());
        view.findViewById(R.id.buttonCompartilhar).setOnClickListener(v -> compartilharRelatorio());
        view.findViewById(R.id.buttonLimparLog).setOnClickListener(v -> {
            AppLogger.limpar(requireContext());
            atualizarRelatorio();
        });

        atualizarRelatorio();
    }

    @Override
    public void onResume() {
        super.onResume();
        atualizarRelatorio();
    }

    private void atualizarRelatorio() {
        textViewRelatorio.setText(gerarRelatorio());
    }

    private String gerarRelatorio() {
        Context context = requireContext();
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

        sb.append("=== Trust Call — Diagnóstico ===\n");
        sb.append("Gerado em: ").append(df.format(new java.util.Date())).append("\n\n");

        sb.append("--- App ---\n");
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sb.append("Versão: ").append(info.versionName).append(" (").append(info.versionCode).append(")\n");
        } catch (Exception ignored) {
        }
        sb.append("Pacote: ").append(context.getPackageName()).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Aparelho: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n\n");

        sb.append("--- Permissões ---\n");
        sb.append("Contatos: ").append(statusPermissao(Manifest.permission.READ_CONTACTS)).append("\n");
        sb.append("Notificações: ").append(statusPermissao(Manifest.permission.POST_NOTIFICATIONS)).append("\n");
        sb.append("Telefone (READ_PHONE_STATE): ").append(statusPermissao(Manifest.permission.READ_PHONE_STATE)).append("\n");
        sb.append("Ligar (CALL_PHONE): ").append(statusPermissao(Manifest.permission.CALL_PHONE)).append("\n");
        sb.append("Sobrepor outros apps: ").append(FloatingBubbleService.temPermissao(context) ? "Concedida" : "Negada").append("\n\n");

        sb.append("--- Papéis do sistema ---\n");
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        boolean identificador = roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        boolean telefone = roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
        boolean telefoneDisponivel = roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER);
        sb.append("Identificador de chamadas: ").append(identificador ? "Concedido" : "Não concedido").append("\n");
        sb.append("Telefone padrão: ").append(telefone ? "Concedido" : "Não concedido").append("\n");
        sb.append("  Papel disponível neste aparelho: ").append(telefoneDisponivel ? "Sim" : "Não").append("\n");

        String discadorAtual = "desconhecido";
        try {
            TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
            if (telecomManager != null) {
                discadorAtual = telecomManager.getDefaultDialerPackage();
            }
        } catch (Exception ignored) {
        }
        sb.append("Discador padrão atual: ").append(discadorAtual).append("\n\n");

        sb.append("--- Linhas SIM detectadas ---\n");
        List<SimAccountEntry> contas = SimAccountsHelper.listar(context);
        sb.append("Total: ").append(contas.size()).append("\n");
        for (SimAccountEntry conta : contas) {
            sb.append("  • ").append(conta.label).append(" (").append(conta.subtitulo).append(") — id=").append(conta.id).append("\n");
        }
        if (contas.isEmpty()) {
            sb.append("  (nenhuma linha detectada)\n");
        }
        sb.append("\n");

        sb.append("--- Log de eventos recentes ---\n");
        List<String> entradas = AppLogger.obterEntradas(context);
        if (entradas.isEmpty()) {
            sb.append("(nenhum evento registrado ainda)\n");
        } else {
            for (String linha : entradas) {
                sb.append(linha).append("\n");
            }
        }

        return sb.toString();
    }

    private String statusPermissao(String permissao) {
        return ContextCompat.checkSelfPermission(requireContext(), permissao) == PackageManager.PERMISSION_GRANTED
                ? "Concedida" : "Negada";
    }

    private void copiarRelatorio() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Trust Call - Diagnóstico", textViewRelatorio.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.diagnostico_copiado, Toast.LENGTH_SHORT).show();
    }

    private void compartilharRelatorio() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, textViewRelatorio.getText().toString());
        startActivity(Intent.createChooser(intent, getString(R.string.diagnostico_compartilhar)));
    }

    private void baixarRelatorio() {
        String nomeArquivo = "trustcall_diagnostico_" + System.currentTimeMillis() + ".txt";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, nomeArquivo);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Toast.makeText(requireContext(), R.string.diagnostico_erro_salvar, Toast.LENGTH_LONG).show();
            return;
        }

        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                out.write(textViewRelatorio.getText().toString().getBytes(StandardCharsets.UTF_8));
            }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            requireContext().getContentResolver().update(uri, values, null, null);
            Toast.makeText(requireContext(), getString(R.string.diagnostico_salvo, nomeArquivo), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), R.string.diagnostico_erro_salvar, Toast.LENGTH_LONG).show();
        }
    }
}
