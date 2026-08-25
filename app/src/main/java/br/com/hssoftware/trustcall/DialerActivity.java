package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DialerActivity extends AppCompatActivity {

    private EditText editTextNumero;

    private final ActivityResultLauncher<String> callPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> { if (granted) ligar(); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);

        editTextNumero = findViewById(R.id.editTextNumero);

        Uri data = getIntent().getData();
        if (data != null && data.getSchemeSpecificPart() != null) {
            editTextNumero.setText(data.getSchemeSpecificPart());
        }

        findViewById(R.id.buttonLigar).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                ligar();
            } else {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
            }
        });
    }

    private void ligar() {
        String numero = editTextNumero.getText().toString().trim();
        if (numero.isEmpty()) return;

        TelecomManager telecomManager = getSystemService(TelecomManager.class);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            telecomManager.placeCall(Uri.fromParts("tel", numero, null), null);
            finish();
        }
    }
}
