package br.com.hssoftware.trustcall;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class AppLogger {

    private static final int MAX_ENTRADAS = 300;
    private static final Deque<String> buffer = new ArrayDeque<>();
    private static final SimpleDateFormat FORMATO = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());
    private static boolean carregado = false;

    public static synchronized void log(Context context, String tag, String mensagem) {
        garantirCarregado(context);
        String linha = FORMATO.format(new Date()) + "  [" + tag + "]  " + mensagem;
        adicionar(linha);
        persistir(context, linha);
    }

    public static void logErro(Context context, String tag, String mensagem, Throwable t) {
        if (t == null) {
            log(context, tag, mensagem);
            return;
        }
        String stackTrace = android.util.Log.getStackTraceString(t).trim().replace("\n", " | ");
        log(context, tag, mensagem + " — " + stackTrace);
    }

    public static synchronized List<String> obterEntradas(Context context) {
        garantirCarregado(context);
        return new ArrayList<>(buffer);
    }

    public static synchronized void limpar(Context context) {
        buffer.clear();
        File arquivo = arquivoLog(context);
        if (arquivo.exists()) {
            arquivo.delete();
        }
    }

    private static void adicionar(String linha) {
        buffer.addLast(linha);
        if (buffer.size() > MAX_ENTRADAS) {
            buffer.removeFirst();
        }
    }

    private static void garantirCarregado(Context context) {
        if (carregado) return;
        carregado = true;

        File arquivo = arquivoLog(context);
        if (!arquivo.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                adicionar(linha);
            }
        } catch (IOException ignored) {
        }
    }

    private static void persistir(Context context, String linha) {
        try (FileWriter writer = new FileWriter(arquivoLog(context), true)) {
            writer.write(linha + "\n");
        } catch (IOException ignored) {
        }
    }

    private static File arquivoLog(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), "trustcall_log.txt");
    }
}
