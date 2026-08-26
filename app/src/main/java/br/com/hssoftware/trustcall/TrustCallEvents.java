package br.com.hssoftware.trustcall;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrustCallEvents {

    public interface Listener {
        void onEvento();
    }

    private static final List<Listener> historicoListeners = new CopyOnWriteArrayList<>();
    private static final List<Listener> servicoListeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void registrarHistorico(Listener listener) {
        historicoListeners.add(listener);
    }

    public static void removerHistorico(Listener listener) {
        historicoListeners.remove(listener);
    }

    public static void notificarHistoricoAtualizado() {
        mainHandler.post(() -> {
            for (Listener listener : historicoListeners) {
                listener.onEvento();
            }
        });
    }

    public static void registrarServico(Listener listener) {
        servicoListeners.add(listener);
    }

    public static void removerServico(Listener listener) {
        servicoListeners.remove(listener);
    }

    public static void notificarServicoAtualizado() {
        mainHandler.post(() -> {
            for (Listener listener : servicoListeners) {
                listener.onEvento();
            }
        });
    }
}
