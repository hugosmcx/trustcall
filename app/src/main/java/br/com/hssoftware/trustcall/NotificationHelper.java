package br.com.hssoftware.trustcall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class NotificationHelper {

    private static final String CHANNEL_ID = "trust_call_status";
    public static final int NOTIFICATION_ID = 1001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void updateNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
        boolean bloqueioAtivo = prefs.getBoolean("BLOQUEIO_ATIVO", false);
        boolean persistente = prefs.getBoolean("NOTIFICACAO_PERSISTENTE", true);

        if (!persistente) {
            cancelNotification(context);
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createNotificationChannel(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggleIntent = new Intent(context, ToggleBlockerReceiver.class);
        toggleIntent.setAction(ToggleBlockerReceiver.ACTION_TOGGLE_BLOCKER);
        PendingIntent togglePendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String statusText = bloqueioAtivo
                ? context.getString(R.string.notification_text_active)
                : context.getString(R.string.notification_text_inactive);
        String actionLabel = bloqueioAtivo
                ? context.getString(R.string.action_disable_blocker)
                : context.getString(R.string.action_enable_blocker);

        int accentColor = ContextCompat.getColor(context,
                bloqueioAtivo ? R.color.brand_success : R.color.brand_warning);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setColor(accentColor)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(statusText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(statusText))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(contentPendingIntent)
                .addAction(0, actionLabel, togglePendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }
}
