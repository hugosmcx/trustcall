package br.com.hssoftware.trustcall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class FloatingBubbleService extends Service {

    private static final String EXTRA_NUMERO = "numero";
    private static final String EXTRA_MOTIVO = "motivo";
    private static final String CHANNEL_ID = "trust_call_bubble";
    private static final int FOREGROUND_ID = 3001;
    private static final int MARGEM_BORDA_DP = 56;
    private static final int ARRASTO_MINIMO_DP = 72;
    private static final int TAMANHO_BOLHA_DP = 72;

    private WindowManager windowManager;
    private View bubbleView;
    private View expandedView;
    private WindowManager.LayoutParams bubbleParams;

    private String numeroAtual;
    private BlockReason motivoAtual;
    private boolean recusarDisponivel;

    public static boolean temPermissao(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static Intent criarIntentPermissao(Context context) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
    }

    public static void mostrar(Context context, String numero, @Nullable BlockReason motivo) {
        if (!temPermissao(context)) {
            AppLogger.log(context, "FloatingBubbleService", "Bolha não exibida: sem permissão de sobreposição");
            return;
        }
        try {
            Intent intent = new Intent(context, FloatingBubbleService.class);
            intent.putExtra(EXTRA_NUMERO, numero);
            if (motivo != null) intent.putExtra(EXTRA_MOTIVO, motivo.name());
            ContextCompat.startForegroundService(context, intent);
            AppLogger.log(context, "FloatingBubbleService", "Solicitado exibir bolha para " + numero);
        } catch (Exception e) {
            AppLogger.logErro(context, "FloatingBubbleService", "Falha ao iniciar serviço da bolha", e);
        }
    }

    public static void esconder(Context context) {
        context.stopService(new Intent(context, FloatingBubbleService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            criarCanal();
            startForeground(FOREGROUND_ID, criarNotificacaoSilenciosa(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            RoleManager roleManager = getSystemService(RoleManager.class);
            recusarDisponivel = roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
        } catch (Exception e) {
            AppLogger.logErro(this, "FloatingBubbleService", "Falha ao iniciar serviço (onCreate)", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (windowManager == null) {
            return START_NOT_STICKY;
        }
        numeroAtual = intent != null ? intent.getStringExtra(EXTRA_NUMERO) : null;
        String motivoNome = intent != null ? intent.getStringExtra(EXTRA_MOTIVO) : null;
        motivoAtual = motivoNome != null ? BlockReason.valueOf(motivoNome) : null;

        try {
            exibirBolha();
        } catch (Exception e) {
            AppLogger.logErro(this, "FloatingBubbleService", "Falha ao exibir bolha (onStartCommand)", e);
            stopSelf();
        }
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
        removerViewComSeguranca(expandedView);
        removerViewComSeguranca(bubbleView);
        expandedView = null;
        bubbleView = null;
    }

    private void removerViewComSeguranca(View view) {
        if (view != null && windowManager != null) {
            try {
                windowManager.removeView(view);
            } catch (Exception ignored) {
            }
        }
    }

    private void exibirBolha() {
        if (bubbleView != null) return;

        bubbleView = LayoutInflater.from(this).inflate(R.layout.view_call_bubble, null);
        View bubbleColapsada = bubbleView.findViewById(R.id.bubbleColapsada);

        int tipoOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipoOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;

        DisplayMetrics metricas = getResources().getDisplayMetrics();
        int tamanhoBolhaPx = (int) (TAMANHO_BOLHA_DP * metricas.density);
        bubbleParams.x = metricas.widthPixels - tamanhoBolhaPx - (int) (16 * metricas.density);
        bubbleParams.y = (int) (160 * metricas.density);

        windowManager.addView(bubbleView, bubbleParams);

        configurarGestoBolha(bubbleColapsada);
    }

    private void configurarGestoBolha(View bubbleColapsada) {
        DisplayMetrics metricas = getResources().getDisplayMetrics();
        int margemBordaPx = (int) (MARGEM_BORDA_DP * metricas.density);
        int tamanhoBolhaPx = (int) (TAMANHO_BOLHA_DP * metricas.density);
        int arrastoMinimoPx = (int) (ARRASTO_MINIMO_DP * metricas.density);
        int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        Handler handler = new Handler(Looper.getMainLooper());

        bubbleColapsada.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean arrastou = false;
            private boolean longPressDisparado = false;

            private final Runnable longPressRunnable = () -> {
                if (arrastou) return;
                longPressDisparado = true;
                bubbleColapsada.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                acaoRecusarOuFechar();
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        arrastou = false;
                        longPressDisparado = false;
                        handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (longPressDisparado) return true;
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (!arrastou && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            arrastou = true;
                            handler.removeCallbacks(longPressRunnable);
                        }
                        if (!arrastou) return true;

                        bubbleParams.x = initialX + dx;
                        bubbleParams.y = initialY + dy;
                        windowManager.updateViewLayout(bubbleView, bubbleParams);

                        boolean arrastoSignificativo = distanciaArrasto(dx, dy) >= arrastoMinimoPx;
                        boolean pertoDaBorda = arrastoSignificativo
                                && pertoDaBorda(bubbleParams, margemBordaPx, tamanhoBolhaPx, metricas);
                        v.setAlpha(pertoDaBorda ? 0.55f : 1f);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPressRunnable);
                        v.setAlpha(1f);
                        if (longPressDisparado) return true;

                        if (arrastou) {
                            int dxFinal = (int) (event.getRawX() - initialTouchX);
                            int dyFinal = (int) (event.getRawY() - initialTouchY);
                            boolean arrastoSignificativo = distanciaArrasto(dxFinal, dyFinal) >= arrastoMinimoPx;
                            if (arrastoSignificativo && pertoDaBorda(bubbleParams, margemBordaPx, tamanhoBolhaPx, metricas)) {
                                acaoRecusarOuFechar();
                            }
                        } else {
                            mostrarExpandido();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private double distanciaArrasto(int dx, int dy) {
        return Math.hypot(dx, dy);
    }

    private boolean pertoDaBorda(WindowManager.LayoutParams params, int margemPx, int tamanhoBolhaPx, DisplayMetrics metricas) {
        boolean pertoEsquerda = params.x <= margemPx;
        boolean pertoDireita = params.x + tamanhoBolhaPx >= metricas.widthPixels - margemPx;
        boolean pertoTopo = params.y <= margemPx;
        boolean pertoFundo = params.y + tamanhoBolhaPx >= metricas.heightPixels - margemPx;
        return pertoEsquerda || pertoDireita || pertoTopo || pertoFundo;
    }

    private void mostrarExpandido() {
        if (expandedView != null) return;

        expandedView = LayoutInflater.from(this).inflate(R.layout.view_call_expanded, null);

        TextView textViewNumero = expandedView.findViewById(R.id.textViewNumeroExpandido);
        textViewNumero.setText(numeroAtual != null ? numeroAtual : getString(R.string.numero_oculto_label));

        TextView textViewMotivo = expandedView.findViewById(R.id.textViewMotivoExpandido);
        textViewMotivo.setText(motivoAtual != null
                ? getString(R.string.incoming_call_subtitle, getString(motivoAtual.labelResId))
                : getString(R.string.incoming_call_subtitle_generico));

        expandedView.findViewById(R.id.buttonAtenderExpandido).setOnClickListener(v -> {
            CallActions.aceitar(this);
            IncomingCallNotifier.cancelar(this);
            stopSelf();
        });

        View containerRecusar = expandedView.findViewById(R.id.containerRecusarExpandido);
        if (recusarDisponivel) {
            expandedView.findViewById(R.id.buttonRecusarExpandido).setOnClickListener(v -> acaoRecusarOuFechar());
        } else {
            containerRecusar.setVisibility(View.GONE);
        }

        expandedView.findViewById(R.id.textViewFecharExpandido).setOnClickListener(v -> esconderExpandido());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        try {
            windowManager.addView(expandedView, params);
        } catch (Exception e) {
            AppLogger.logErro(this, "FloatingBubbleService", "Falha ao expandir bolha", e);
            expandedView = null;
        }
    }

    private void esconderExpandido() {
        removerViewComSeguranca(expandedView);
        expandedView = null;
    }

    private void acaoRecusarOuFechar() {
        if (recusarDisponivel) {
            CallActions.recusar(this);
        }
        IncomingCallNotifier.cancelar(this);
        stopSelf();
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
