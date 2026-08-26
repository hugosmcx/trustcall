package br.com.hssoftware.trustcall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class IncomingCallNotifier {

    private static final String CHANNEL_ID = "trust_call_incoming";
    private static final int NOTIFICATION_ID = 2001;

    private static void criarCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.incoming_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.incoming_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void mostrar(Context context, String numero, @Nullable BlockReason motivo) {
        criarCanal(context);

        String numeroExibido = numero != null ? numero : context.getString(R.string.numero_oculto_label);
        String subtitulo = motivo != null
                ? context.getString(R.string.incoming_call_subtitle, context.getString(motivo.labelResId))
                : context.getString(R.string.incoming_call_subtitle_generico);

        PendingIntent atenderPendingIntent = PendingIntent.getBroadcast(
                context, 1, CallActionReceiver.criarIntentAtender(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setContentTitle(numeroExibido)
                .setContentText(subtitulo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(0, context.getString(R.string.action_answer), atenderPendingIntent);

        RoleManager roleManager = context.getSystemService(RoleManager.class);
        boolean recusarDisponivel = roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
        if (recusarDisponivel) {
            PendingIntent recusarPendingIntent = PendingIntent.getBroadcast(
                    context, 2, CallActionReceiver.criarIntentRecusar(context),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, context.getString(R.string.action_decline), recusarPendingIntent);
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        AppLogger.log(context, "IncomingCallNotifier", "Notificação exibida para " + numeroExibido
                + " (recusar disponível=" + recusarDisponivel + ")");
    }

    public static void cancelar(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }
}
