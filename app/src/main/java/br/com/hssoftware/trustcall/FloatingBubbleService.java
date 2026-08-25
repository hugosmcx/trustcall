package br.com.hssoftware.trustcall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class FloatingBubbleService extends Service {

    private static final String EXTRA_NUMERO = "numero";
    private static final String CHANNEL_ID = "trust_call_bubble";
    private static final int FOREGROUND_ID = 3001;

    private WindowManager windowManager;
    private View overlayView;

    public static boolean temPermissao(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static Intent criarIntentPermissao(Context context) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
    }

    public static void mostrar(Context context, String numero) {
        if (!temPermissao(context)) return;
        Intent intent = new Intent(context, FloatingBubbleService.class);
        intent.putExtra(EXTRA_NUMERO, numero);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void esconder(Context context) {
        context.stopService(new Intent(context, FloatingBubbleService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        criarCanal();
        startForeground(FOREGROUND_ID, criarNotificacaoSilenciosa(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String numero = intent != null ? intent.getStringExtra(EXTRA_NUMERO) : null;
        exibirBolha(numero);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private void exibirBolha(String numero) {
        if (overlayView != null) return;

        overlayView = LayoutInflater.from(this).inflate(R.layout.view_call_bubble, null);

        View bubbleColapsada = overlayView.findViewById(R.id.bubbleColapsada);
        View bubbleExpandida = overlayView.findViewById(R.id.bubbleExpandida);
        TextView textViewNumero = overlayView.findViewById(R.id.textViewNumeroBubble);
        textViewNumero.setText(numero != null ? numero : getString(R.string.numero_oculto_label));

        int tipoOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipoOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 16;
        params.y = 160;

        windowManager.addView(overlayView, params);

        bubbleColapsada.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean arrastou = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        arrastou = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (initialTouchX - event.getRawX());
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) arrastou = true;
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!arrastou) {
                            bubbleColapsada.setVisibility(View.GONE);
                            bubbleExpandida.setVisibility(View.VISIBLE);
                        }
                        return true;
                }
                return false;
            }
        });

        overlayView.findViewById(R.id.buttonAtenderBubble).setOnClickListener(v -> {
            TrustCallInCallService.aceitarChamadaAtual();
            IncomingCallNotifier.cancelar(this);
            stopSelf();
        });

        overlayView.findViewById(R.id.buttonRecusarBubble).setOnClickListener(v -> {
            TrustCallInCallService.recusarChamadaAtual();
            IncomingCallNotifier.cancelar(this);
            stopSelf();
        });
    }

    private void criarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.bubble_channel_name), NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private android.app.Notification criarNotificacaoSilenciosa() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setContentTitle(getString(R.string.bubble_notification_title))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
