package br.com.hssoftware.trustcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ToggleBlockerReceiver extends BroadcastReceiver {

    public static final String ACTION_TOGGLE_BLOCKER = "br.com.hssoftware.trustcall.ACTION_TOGGLE_BLOCKER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TOGGLE_BLOCKER.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
        boolean bloqueioAtivo = prefs.getBoolean("BLOQUEIO_ATIVO", false);
        prefs.edit().putBoolean("BLOQUEIO_ATIVO", !bloqueioAtivo).apply();

        NotificationHelper.updateNotification(context);
    }
}
