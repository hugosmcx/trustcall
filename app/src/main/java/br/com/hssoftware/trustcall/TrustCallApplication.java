package br.com.hssoftware.trustcall;

import android.app.Application;

public class TrustCallApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.UncaughtExceptionHandler anterior = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppLogger.logErro(this, "CRASH", "Exceção não tratada em " + thread.getName(), throwable);
            if (anterior != null) {
                anterior.uncaughtException(thread, throwable);
            }
        });
    }
}
