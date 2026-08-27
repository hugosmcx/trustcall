package br.com.hssoftware.trustcall;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Preferências de bloqueio por linha (chip). Quando contaId é null (aparelho com 1 linha só,
 * ou linha não identificada pelo Telecom), cai nas chaves antigas sem sufixo — mantém
 * compatibilidade com configs salvas antes dessa feature existir.
 */
public class ChipConfig {

    public static final String CRITERIO_DESCONHECIDOS = "DESCONHECIDOS";
    public static final String CRITERIO_OCULTOS = "OCULTOS";
    public static final String CRITERIO_INTERNACIONAL = "INTERNACIONAL";
    public static final String CRITERIO_DDD = "DDD";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
    }

    public static boolean isFiltroAtivo(Context context, String criterio, @Nullable String contaId, boolean padrao) {
        SharedPreferences prefs = prefs(context);
        if (contaId != null) {
            String chaveEspecifica = "FILTRO_" + criterio + "_" + contaId;
            if (prefs.contains(chaveEspecifica)) {
                return prefs.getBoolean(chaveEspecifica, padrao);
            }
        }
        return prefs.getBoolean("FILTRO_" + criterio, padrao);
    }

    public static void setFiltroAtivo(Context context, String criterio, @Nullable String contaId, boolean ativo) {
        String chave = contaId != null ? "FILTRO_" + criterio + "_" + contaId : "FILTRO_" + criterio;
        prefs(context).edit().putBoolean(chave, ativo).apply();
    }

    public static CallDecisionEngine.Acao getModo(Context context, String criterio, @Nullable String contaId) {
        SharedPreferences prefs = prefs(context);
        String valor;
        if (contaId != null && prefs.contains("MODO_" + criterio + "_" + contaId)) {
            valor = prefs.getString("MODO_" + criterio + "_" + contaId, "BLOQUEAR");
        } else {
            valor = prefs.getString("MODO_" + criterio, "BLOQUEAR");
        }
        return "PERGUNTAR".equals(valor) ? CallDecisionEngine.Acao.PERGUNTAR : CallDecisionEngine.Acao.BLOQUEAR;
    }

    public static void setModo(Context context, String criterio, @Nullable String contaId, CallDecisionEngine.Acao acao) {
        String chave = contaId != null ? "MODO_" + criterio + "_" + contaId : "MODO_" + criterio;
        prefs(context).edit().putString(chave, acao == CallDecisionEngine.Acao.PERGUNTAR ? "PERGUNTAR" : "BLOQUEAR").apply();
    }

    public static Set<String> getDddsBloqueados(Context context, @Nullable String contaId) {
        String chave = "DDD_LISTA_" + (contaId != null ? contaId : "GERAL");
        Set<String> valor = prefs(context).getStringSet(chave, null);
        return valor != null ? new HashSet<>(valor) : new HashSet<>();
    }

    public static void setDddsBloqueados(Context context, @Nullable String contaId, Set<String> ddds) {
        String chave = "DDD_LISTA_" + (contaId != null ? contaId : "GERAL");
        prefs(context).edit().putStringSet(chave, ddds).apply();
    }
}
