package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class CallFilterService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details callDetails) {
        String incomingNumber = callDetails.getHandle() != null ? callDetails.getHandle().getSchemeSpecificPart() : null;

        if (servicoAtivo() && temPermissao() && incomingNumber != null && !isNumberInContacts(this, incomingNumber)) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(true)
                    .setSkipNotification(true)
                    .build();

            respondToCall(callDetails, response);
        }
    }

    private boolean temPermissao() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean servicoAtivo(){
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        return prefs.getBoolean("BLOQUEIO_ATIVO", false);
    }

    private boolean isNumberInContacts(Context context, String phoneNumber) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null,
                null,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String contactNumber = cursor.getString(0);

                // Remove espaços, traços e outros caracteres não numéricos
                contactNumber = contactNumber.replaceAll("[^0-9]", "");
                phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

                // Verifica se o final do número do contato corresponde ao número recebido
                if (phoneNumber.endsWith(contactNumber) || contactNumber.endsWith(phoneNumber)) {
                    cursor.close();
                    return true;
                }
            }
            cursor.close();
        }
        return false;
    }

}
