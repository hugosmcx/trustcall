package br.com.hssoftware.trustcall;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class IncomingCallActivity extends AppCompatActivity {

    private static final String EXTRA_NUMERO = "numero";
    private static final String EXTRA_MOTIVO = "motivo";

    public static Intent criarIntent(Context context, String numero, @Nullable BlockReason motivo) {
        Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.putExtra(EXTRA_NUMERO, numero);
        if (motivo != null) {
            intent.putExtra(EXTRA_MOTIVO, motivo.name());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        setContentView(R.layout.activity_incoming_call);

        String numero = getIntent().getStringExtra(EXTRA_NUMERO);
        String motivoNome = getIntent().getStringExtra(EXTRA_MOTIVO);

        TextView textViewNumero = findViewById(R.id.textViewNumero);
        textViewNumero.setText(numero != null ? numero : getString(R.string.numero_oculto_label));

        TextView textViewMotivo = findViewById(R.id.textViewMotivo);
        if (motivoNome != null) {
            BlockReason motivo = BlockReason.valueOf(motivoNome);
            textViewMotivo.setText(getString(R.string.incoming_call_subtitle, getString(motivo.labelResId)));
        } else {
            textViewMotivo.setText(R.string.incoming_call_subtitle_generico);
        }

        findViewById(R.id.buttonAtender).setOnClickListener(v -> {
            TrustCallInCallService.aceitarChamadaAtual();
            IncomingCallNotifier.cancelar(this);
            FloatingBubbleService.esconder(this);
            finish();
        });

        findViewById(R.id.buttonRecusar).setOnClickListener(v -> {
            TrustCallInCallService.recusarChamadaAtual();
            IncomingCallNotifier.cancelar(this);
            FloatingBubbleService.esconder(this);
            finish();
        });
    }
}
