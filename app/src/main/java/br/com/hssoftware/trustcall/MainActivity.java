package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {

    MaterialSwitch switchServico;
    MaterialSwitch switchNotificacaoPersistente;

    FrameLayout iconChipStatus;
    ImageView imageViewStatusIcon;
    TextView textViewStatusTitle;
    TextView textViewStatusSubtitle;

    LinearLayout constraintLayoutBotoes;

    TextView textViewPermissao;
    Button buttonPermissaoContato;
    TextView textViewConfig;
    Button buttonConfig;

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        switchServico = findViewById(R.id.switchServico);
        switchNotificacaoPersistente = findViewById(R.id.switchNotificacaoPersistente);

        iconChipStatus = findViewById(R.id.iconChipStatus);
        imageViewStatusIcon = findViewById(R.id.imageViewStatusIcon);
        textViewStatusTitle = findViewById(R.id.textViewStatusTitle);
        textViewStatusSubtitle = findViewById(R.id.textViewStatusSubtitle);

        constraintLayoutBotoes = findViewById(R.id.constraintLayoutBotoes);

        switchServico.setOnCheckedChangeListener((compoundButton, b) -> {
            setServicoAtivo(b);
            mostrarContainer(b);
            atualizarStatusVisual(b);
            NotificationHelper.updateNotification(this);
        });

        switchNotificacaoPersistente.setOnCheckedChangeListener((compoundButton, b) -> {
            setNotificacaoPersistente(b);
            if (b) {
                solicitaPermissaoNotificacaoENotifica();
            } else {
                NotificationHelper.cancelNotification(this);
            }
        });

        textViewPermissao = findViewById(R.id.textViewPermissao);
        buttonPermissaoContato = findViewById(R.id.buttonPermissaoContato);
        buttonPermissaoContato.setOnClickListener(view -> {
            solicitaPermissao();
        });
        textViewConfig = findViewById(R.id.textViewConfig);
        buttonConfig = findViewById(R.id.buttonConfig);
        buttonConfig.setOnClickListener(view -> {
            requestCallScreeningService();
        });

        switchServico.setChecked(servicoAtivo());
        mostrarContainer(servicoAtivo());
        atualizarStatusVisual(servicoAtivo());

        switchNotificacaoPersistente.setChecked(notificacaoPersistente());

        checkAndRequestContactsPermission();

        if (notificacaoPersistente()) {
            solicitaPermissaoNotificacaoENotifica();
        }
    }

    private void atualizarStatusVisual(boolean ativo) {
        textViewStatusTitle.setText(ativo ? R.string.status_title_active : R.string.status_title_inactive);
        textViewStatusSubtitle.setText(ativo ? R.string.status_subtitle_active : R.string.status_subtitle_inactive);

        int corIcone = ContextCompat.getColor(this, ativo ? R.color.brand_success : R.color.brand_warning);
        int corFundo = ContextCompat.getColor(this, ativo ? R.color.brand_success_container : R.color.brand_warning_container);

        imageViewStatusIcon.setColorFilter(corIcone, PorterDuff.Mode.SRC_IN);

        Drawable fundoChip = iconChipStatus.getBackground().mutate();
        fundoChip.setTint(corFundo);
        iconChipStatus.setBackground(fundoChip);
    }

    private void solicitaPermissaoNotificacaoENotifica() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        } else {
            NotificationHelper.updateNotification(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        switchServico.setChecked(servicoAtivo());
        mostrarContainer(servicoAtivo());
        atualizarStatusVisual(servicoAtivo());
    }

    private void requestCallScreeningService() {
        startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
    }


    private void checkAndRequestContactsPermission() {
        mostrarBotoes(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED);
    }

    public void mostrarContainer(boolean mostrar){
        if(mostrar){
            constraintLayoutBotoes.setVisibility(View.VISIBLE);
        }else{
            constraintLayoutBotoes.setVisibility(View.GONE);
        }
    }

    public void mostrarBotoes(boolean mostrar){
        if (mostrar){
            textViewPermissao.setVisibility(View.VISIBLE);
            buttonPermissaoContato.setVisibility(View.VISIBLE);
            textViewConfig.setVisibility(View.GONE);
            buttonConfig.setVisibility(View.GONE);
        }else{
            textViewPermissao.setVisibility(View.GONE);
            buttonPermissaoContato.setVisibility(View.GONE);
            textViewConfig.setVisibility(View.VISIBLE);
            buttonConfig.setVisibility(View.VISIBLE);
        }
    }

    public void solicitaPermissao(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            mostrarBotoes(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED);
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                NotificationHelper.updateNotification(this);
            } else {
                switchNotificacaoPersistente.setChecked(false);
                setNotificacaoPersistente(false);
            }
        }
    }

    private boolean servicoAtivo(){
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        return prefs.getBoolean("BLOQUEIO_ATIVO", false);
    }

    private void setServicoAtivo(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("BLOQUEIO_ATIVO", enabled);
        editor.apply();
    }

    private boolean notificacaoPersistente() {
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        return prefs.getBoolean("NOTIFICACAO_PERSISTENTE", true);
    }

    private void setNotificacaoPersistente(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        prefs.edit().putBoolean("NOTIFICACAO_PERSISTENTE", enabled).apply();
    }

}
