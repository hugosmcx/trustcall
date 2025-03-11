package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Switch switchServico;

    ConstraintLayout constraintLayoutBotoes;

    TextView textViewPermissao;
    Button buttonPermissaoContato;
    TextView textViewConfig;
    Button buttonConfig;

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 100;

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
        constraintLayoutBotoes = findViewById(R.id.constraintLayoutBotoes);

        switchServico.setOnCheckedChangeListener((compoundButton, b) -> {
            setServicoAtivo(b);
            mostrarContainer(b);
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

        checkAndRequestContactsPermission();
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

}