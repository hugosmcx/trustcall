package br.com.hssoftware.trustcall;

import android.content.Context;

import androidx.annotation.Nullable;

public class PendingBubbleHolder {

    private static volatile String numero;
    private static volatile BlockReason motivo;
    private static volatile boolean pendente;

    public static synchronized void definir(@Nullable String numero, @Nullable BlockReason motivo) {
        PendingBubbleHolder.numero = numero;
        PendingBubbleHolder.motivo = motivo;
        pendente = true;
    }

    public static synchronized void consumir(Context context) {
        if (!pendente) return;
        pendente = false;
        FloatingBubbleService.mostrar(context, numero, motivo);
    }
}
