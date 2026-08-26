package br.com.hssoftware.trustcall;

import android.Manifest;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;

public class HomeFragment extends Fragment {

    private MaterialSwitch switchServico;
    private MaterialSwitch switchNotificacaoPersistente;

    private FrameLayout iconChipStatus;
    private ImageView imageViewStatusIcon;
    private TextView textViewStatusTitle;
    private TextView textViewStatusSubtitle;

    private Chip chipDesconhecidos;
    private Chip chipOcultos;
    private Chip chipInternacionais;

    private LinearLayout constraintLayoutBotoes;
    private MaterialCardView cardPermissao;
    private MaterialCardView cardConfig;
    private MaterialCardView cardTelefonePadrao;
    private MaterialCardView cardSobreposicao;
    private MaterialCardView cardLinhas;
    private LinearLayout linhasContainer;
    private boolean permissaoTelefoniaSolicitada = false;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private final ActivityResultLauncher<String> contactsPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                AppLogger.log(requireContext(), "HomeFragment", "Permissão contatos: " + (granted ? "concedida" : "negada"));
                atualizarCardsConfiguracao();
            });

    private final ActivityResultLauncher<Intent> callScreeningRoleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                AppLogger.log(requireContext(), "HomeFragment", "Retorno pedido ROLE_CALL_SCREENING: resultCode=" + result.getResultCode()
                        + " concedido=" + papelIdentificadorConcedido());
                atualizarCardsConfiguracao();
            });

    private final ActivityResultLauncher<Intent> dialerRoleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                AppLogger.log(requireContext(), "HomeFragment", "Retorno pedido ROLE_DIALER: resultCode=" + result.getResultCode()
                        + " concedido=" + papelTelefonePadraoConcedido());
                atualizarCardsConfiguracao();
            });

    private final ActivityResultLauncher<String[]> telefoniaPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            resultados -> {
                for (Map.Entry<String, Boolean> entrada : resultados.entrySet()) {
                    AppLogger.log(requireContext(), "HomeFragment", "Permissão " + entrada.getKey() + ": "
                            + (entrada.getValue() ? "concedida" : "negada"));
                }
                configurarLinhas();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchServico = view.findViewById(R.id.switchServico);
        switchNotificacaoPersistente = view.findViewById(R.id.switchNotificacaoPersistente);

        iconChipStatus = view.findViewById(R.id.iconChipStatus);
        imageViewStatusIcon = view.findViewById(R.id.imageViewStatusIcon);
        textViewStatusTitle = view.findViewById(R.id.textViewStatusTitle);
        textViewStatusSubtitle = view.findViewById(R.id.textViewStatusSubtitle);

        chipDesconhecidos = view.findViewById(R.id.chipDesconhecidos);
        chipOcultos = view.findViewById(R.id.chipOcultos);
        chipInternacionais = view.findViewById(R.id.chipInternacionais);

        constraintLayoutBotoes = view.findViewById(R.id.constraintLayoutBotoes);
        cardPermissao = view.findViewById(R.id.cardPermissao);
        cardConfig = view.findViewById(R.id.cardConfig);
        cardTelefonePadrao = view.findViewById(R.id.cardTelefonePadrao);
        cardSobreposicao = view.findViewById(R.id.cardSobreposicao);
        cardLinhas = view.findViewById(R.id.cardLinhas);
        linhasContainer = view.findViewById(R.id.linhasContainer);

        switchServico.setOnCheckedChangeListener((compoundButton, b) -> {
            setServicoAtivo(b);
            atualizarStatusVisual(b);
            NotificationHelper.updateNotification(requireContext());
        });

        switchNotificacaoPersistente.setOnCheckedChangeListener((compoundButton, b) -> {
            setNotificacaoPersistente(b);
            if (b) {
                solicitaPermissaoNotificacaoENotifica();
            } else {
                NotificationHelper.cancelNotification(requireContext());
            }
        });

        configurarCriterio(view, chipDesconhecidos, "FILTRO_DESCONHECIDOS", true,
                R.id.rowModoDesconhecidos, R.id.toggleModoDesconhecidos,
                R.id.buttonModoDesconhecidosBloquear, R.id.buttonModoDesconhecidosPerguntar, "MODO_DESCONHECIDOS");
        configurarCriterio(view, chipOcultos, "FILTRO_OCULTOS", false,
                R.id.rowModoOcultos, R.id.toggleModoOcultos,
                R.id.buttonModoOcultosBloquear, R.id.buttonModoOcultosPerguntar, "MODO_OCULTOS");
        configurarCriterio(view, chipInternacionais, "FILTRO_INTERNACIONAL", false,
                R.id.rowModoInternacionais, R.id.toggleModoInternacionais,
                R.id.buttonModoInternacionaisBloquear, R.id.buttonModoInternacionaisPerguntar, "MODO_INTERNACIONAL");

        view.findViewById(R.id.buttonPermissaoContato).setOnClickListener(v ->
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS));

        view.findViewById(R.id.buttonConfig).setOnClickListener(v -> solicitaPapelIdentificador());

        view.findViewById(R.id.buttonTelefonePadrao).setOnClickListener(v -> solicitaPapelTelefonePadrao());

        view.findViewById(R.id.linkAjudaTelefone).setOnClickListener(v ->
                mostrarAjudaPermissaoRestrita(getString(R.string.role_dialer_button)));

        view.findViewById(R.id.buttonTelefonePadraoFallback).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));

        view.findViewById(R.id.buttonSobreposicao).setOnClickListener(v ->
                startActivity(FloatingBubbleService.criarIntentPermissao(requireContext())));

        view.findViewById(R.id.linkAjudaSobreposicao).setOnClickListener(v ->
                mostrarAjudaPermissaoRestrita(getString(R.string.overlay_button)));

        solicitarPermissoesTelefonia();
        configurarLinhas();

        switchServico.setChecked(servicoAtivo());
        atualizarStatusVisual(servicoAtivo());

        switchNotificacaoPersistente.setChecked(notificacaoPersistente());

        atualizarCardsConfiguracao();

        if (notificacaoPersistente()) {
            solicitaPermissaoNotificacaoENotifica();
        }
    }

    private void configurarCriterio(View root, Chip chip, String chaveFiltro, boolean defaultFiltro,
                                     int idRow, int idToggle, int idBloquear, int idPerguntar, String chaveModo) {
        View row = root.findViewById(idRow);
        MaterialButtonToggleGroup toggle = root.findViewById(idToggle);
        int buttonBloquear = idBloquear;
        int buttonPerguntar = idPerguntar;

        SharedPreferences prefs = prefs();
        chip.setChecked(prefs.getBoolean(chaveFiltro, defaultFiltro));
        row.setVisibility(chip.isChecked() ? View.VISIBLE : View.GONE);
        toggle.check("PERGUNTAR".equals(prefs.getString(chaveModo, "BLOQUEAR")) ? buttonPerguntar : buttonBloquear);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs().edit().putBoolean(chaveFiltro, isChecked).apply();
            row.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            atualizarStatusVisual(servicoAtivo());
        });

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            prefs().edit().putString(chaveModo, checkedId == buttonPerguntar ? "PERGUNTAR" : "BLOQUEAR").apply();
            atualizarStatusVisual(servicoAtivo());
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        switchServico.setChecked(servicoAtivo());
        atualizarStatusVisual(servicoAtivo());
        atualizarCardsConfiguracao();
        configurarLinhas();
    }

    private void solicitarPermissoesTelefonia() {
        boolean temEstado = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean temNumeros = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED;
        boolean temAtender = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED;

        if ((!temEstado || !temNumeros || !temAtender) && !permissaoTelefoniaSolicitada) {
            permissaoTelefoniaSolicitada = true;
            telefoniaPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.ANSWER_PHONE_CALLS});
        }
    }

    private void configurarLinhas() {
        boolean temEstado = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

        if (!temEstado) {
            cardLinhas.setVisibility(View.GONE);
            return;
        }

        List<SimAccountEntry> contas = SimAccountsHelper.listar(requireContext());
        AppLogger.log(requireContext(), "HomeFragment", "Linhas SIM encontradas: " + contas.size());

        if (contas.size() < 2) {
            cardLinhas.setVisibility(View.GONE);
            return;
        }

        cardLinhas.setVisibility(View.VISIBLE);
        linhasContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (SimAccountEntry conta : contas) {
            View row = inflater.inflate(R.layout.item_sim_line, linhasContainer, false);
            TextView label = row.findViewById(R.id.textViewLinhaLabel);
            TextView subtitulo = row.findViewById(R.id.textViewLinhaSubtitulo);
            MaterialSwitch switchLinha = row.findViewById(R.id.switchLinha);

            label.setText(conta.label);
            subtitulo.setText(conta.subtitulo);
            switchLinha.setChecked(SimAccountsHelper.linhaAtiva(requireContext(), conta.id));
            switchLinha.setOnCheckedChangeListener((buttonView, isChecked) ->
                    SimAccountsHelper.setLinhaAtiva(requireContext(), contas, conta.id, isChecked));

            linhasContainer.addView(row);
        }
    }

    private void mostrarAjudaPermissaoRestrita(String nomeBotao) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ajuda_restrita_titulo)
                .setMessage(getString(R.string.ajuda_restrita_mensagem, nomeBotao))
                .setPositiveButton(R.string.ajuda_botao_abrir_config, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.ajuda_fechar, null)
                .show();
    }

    private void atualizarStatusVisual(boolean ativo) {
        textViewStatusTitle.setText(ativo ? R.string.status_title_active : R.string.status_title_inactive);
        textViewStatusSubtitle.setText(ativo ? calcularSubtituloStatus() : getString(R.string.status_subtitle_inactive));

        int corIcone = ContextCompat.getColor(requireContext(), ativo ? R.color.brand_success : R.color.brand_warning);
        int corFundo = ContextCompat.getColor(requireContext(), ativo ? R.color.brand_success_container : R.color.brand_warning_container);

        imageViewStatusIcon.setColorFilter(corIcone, PorterDuff.Mode.SRC_IN);

        Drawable fundoChip = iconChipStatus.getBackground().mutate();
        fundoChip.setTint(corFundo);
        iconChipStatus.setBackground(fundoChip);
    }

    private String calcularSubtituloStatus() {
        SharedPreferences prefs = prefs();
        String[][] criterios = {
                {"FILTRO_DESCONHECIDOS", "MODO_DESCONHECIDOS", "true"},
                {"FILTRO_OCULTOS", "MODO_OCULTOS", "false"},
                {"FILTRO_INTERNACIONAL", "MODO_INTERNACIONAL", "false"},
        };

        boolean algumBloquear = false;
        boolean algumPerguntar = false;
        for (String[] criterio : criterios) {
            boolean habilitado = prefs.getBoolean(criterio[0], Boolean.parseBoolean(criterio[2]));
            if (!habilitado) continue;
            if ("PERGUNTAR".equals(prefs.getString(criterio[1], "BLOQUEAR"))) {
                algumPerguntar = true;
            } else {
                algumBloquear = true;
            }
        }

        if (algumPerguntar && algumBloquear) return getString(R.string.status_subtitle_misto);
        if (algumPerguntar) return getString(R.string.status_subtitle_identificando);
        return getString(R.string.status_subtitle_active);
    }

    private void solicitaPermissaoNotificacaoENotifica() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        } else {
            NotificationHelper.updateNotification(requireContext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                NotificationHelper.updateNotification(requireContext());
            } else {
                switchNotificacaoPersistente.setChecked(false);
                setNotificacaoPersistente(false);
            }
        }
    }

    private void solicitaPapelIdentificador() {
        RoleManager roleManager = requireContext().getSystemService(RoleManager.class);
        callScreeningRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING));
    }

    private void solicitaPapelTelefonePadrao() {
        RoleManager roleManager = requireContext().getSystemService(RoleManager.class);
        boolean disponivel = roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER);
        AppLogger.log(requireContext(), "HomeFragment", "Solicitando ROLE_DIALER — isRoleAvailable=" + disponivel);

        if (!disponivel) {
            Toast.makeText(requireContext(), R.string.role_dialer_indisponivel, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            dialerRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER));
        } catch (Exception e) {
            AppLogger.logErro(requireContext(), "HomeFragment", "Falha ao lançar pedido de ROLE_DIALER", e);
            Toast.makeText(requireContext(), R.string.role_dialer_indisponivel, Toast.LENGTH_LONG).show();
        }
    }

    private boolean papelIdentificadorConcedido() {
        RoleManager roleManager = requireContext().getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private boolean papelTelefonePadraoConcedido() {
        RoleManager roleManager = requireContext().getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
    }

    private void atualizarCardsConfiguracao() {
        boolean temContatos = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        boolean temPapel = papelIdentificadorConcedido();
        boolean temPapelTelefone = papelTelefonePadraoConcedido();
        boolean temSobreposicao = FloatingBubbleService.temPermissao(requireContext());

        cardPermissao.setVisibility(temContatos ? View.GONE : View.VISIBLE);
        cardConfig.setVisibility(temPapel ? View.GONE : View.VISIBLE);
        cardTelefonePadrao.setVisibility(temPapelTelefone ? View.GONE : View.VISIBLE);
        cardSobreposicao.setVisibility(temSobreposicao ? View.GONE : View.VISIBLE);
        constraintLayoutBotoes.setVisibility(
                (!temContatos || !temPapel || !temPapelTelefone || !temSobreposicao) ? View.VISIBLE : View.GONE);
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("TRUST_CALL_PREFS", 0);
    }

    private boolean servicoAtivo() {
        return prefs().getBoolean("BLOQUEIO_ATIVO", false);
    }

    private void setServicoAtivo(boolean enabled) {
        prefs().edit().putBoolean("BLOQUEIO_ATIVO", enabled).apply();
    }

    private boolean notificacaoPersistente() {
        return prefs().getBoolean("NOTIFICACAO_PERSISTENTE", true);
    }

    private void setNotificacaoPersistente(boolean enabled) {
        prefs().edit().putBoolean("NOTIFICACAO_PERSISTENTE", enabled).apply();
    }

}
