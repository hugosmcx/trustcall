package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.TelecomManager;

import androidx.core.content.ContextCompat;

public class CallActions {

    public static void aceitar(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.log(context, "CallActions", "Não foi possível atender: falta permissão ANSWER_PHONE_CALLS");
            return;
        }
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        if (telecomManager != null) {
            telecomManager.acceptRingingCall();
            AppLogger.log(context, "CallActions", "Chamada aceita via TelecomManager.acceptRingingCall()");
        }
    }

    public static void recusar(Context context) {
        TrustCallInCallService.recusarChamadaAtual();
        AppLogger.log(context, "CallActions", "Recusar solicitado (efetivo apenas se Telefone padrão estiver concedido)");
    }
}
