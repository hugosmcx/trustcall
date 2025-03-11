package br.com.hssoftware.trustcall;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

public class CallFilterService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details callDetails) {
        String incomingNumber = callDetails.getHandle() != null ? callDetails.getHandle().getSchemeSpecificPart() : null;

        if(incomingNumber != null) {
            Log.d("HUGO", incomingNumber);
        }else{
            Log.d("HUGO", "Número nulo");
        }

        if (incomingNumber != null && !isNumberInContacts(this, incomingNumber)) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(true)
                    .setSkipNotification(true)
                    .build();

            respondToCall(callDetails, response);
        }
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
                    Log.d("HUGO", "Encontrou");
                    cursor.close();
                    return true;
                }
            }
            Log.d("HUGO", "Não encontrou");
            cursor.close();
        }
        return false;
    }

}
