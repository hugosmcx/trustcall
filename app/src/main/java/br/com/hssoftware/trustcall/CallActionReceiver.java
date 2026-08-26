package br.com.hssoftware.trustcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallActionReceiver extends BroadcastReceiver {

    private static final String ACTION_ATENDER = "br.com.hssoftware.trustcall.ACTION_ATENDER_CHAMADA";
    private static final String ACTION_RECUSAR = "br.com.hssoftware.trustcall.ACTION_RECUSAR_CHAMADA";
    private static final String ACTION_ENCERRAR = "br.com.hssoftware.trustcall.ACTION_ENCERRAR_CHAMADA";

    public static Intent criarIntentAtender(Context context) {
        return new Intent(context, CallActionReceiver.class).setAction(ACTION_ATENDER);
    }

    public static Intent criarIntentRecusar(Context context) {
        return new Intent(context, CallActionReceiver.class).setAction(ACTION_RECUSAR);
    }

    public static Intent criarIntentEncerrar(Context context) {
        return new Intent(context, CallActionReceiver.class).setAction(ACTION_ENCERRAR);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        AppLogger.log(context, "CallActionReceiver", "onReceive action=" + action);
        if (ACTION_ATENDER.equals(action)) {
            CallActions.aceitar(context);
            IncomingCallNotifier.cancelar(context);
            FloatingBubbleService.esconder(context);
        } else if (ACTION_RECUSAR.equals(action)) {
            CallActions.recusar(context);
            IncomingCallNotifier.cancelar(context);
            FloatingBubbleService.esconder(context);
        } else if (ACTION_ENCERRAR.equals(action)) {
            TrustCallInCallService.encerrarChamadaAtual();
            OngoingCallNotifier.cancelar(context);
        }
    }
}
