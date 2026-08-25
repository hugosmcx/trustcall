package br.com.hssoftware.trustcall;

import android.content.Context;
import android.content.SharedPreferences;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimAccountsHelper {

    private static final String PREF_LINHAS_ATIVAS = "LINHAS_ATIVAS";

    public static List<SimAccountEntry> listar(Context context) {
        List<SimAccountEntry> resultado = new ArrayList<>();

        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        if (telecomManager == null) {
            AppLogger.log(context, "SimAccountsHelper", "TelecomManager indisponível");
            return resultado;
        }

        List<PhoneAccountHandle> handles;
        try {
            handles = telecomManager.getCallCapablePhoneAccounts();
        } catch (SecurityException e) {
            AppLogger.logErro(context, "SimAccountsHelper", "Sem permissão para listar handles de conta", e);
            return resultado;
        }

        AppLogger.log(context, "SimAccountsHelper", "getCallCapablePhoneAccounts() retornou " + handles.size() + " conta(s)");

        int indice = 1;
        for (PhoneAccountHandle handle : handles) {
            String label = "SIM " + indice;
            String subtitulo = "SIM " + indice;
            try {
                PhoneAccount account = telecomManager.getPhoneAccount(handle);
                if (account != null && account.getLabel() != null) {
                    label = account.getLabel().toString();
                }
                if (account != null && account.getShortDescription() != null) {
                    subtitulo = account.getShortDescription().toString();
                }
            } catch (SecurityException e) {
                AppLogger.logErro(context, "SimAccountsHelper", "Sem permissão para detalhes da conta " + handle.getId() + ", usando nome genérico", e);
            }
            resultado.add(new SimAccountEntry(handle.getId(), label, subtitulo));
            indice++;
        }
        return resultado;
    }

    public static boolean linhaAtiva(Context context, String contaId) {
        if (contaId == null) return true;
        SharedPreferences prefs = context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
        Set<String> linhasAtivas = prefs.getStringSet(PREF_LINHAS_ATIVAS, null);
        return linhasAtivas == null || linhasAtivas.contains(contaId);
    }

    public static void setLinhaAtiva(Context context, List<SimAccountEntry> todasAsContas, String contaId, boolean ativa) {
        SharedPreferences prefs = context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
        Set<String> linhasAtivas = prefs.getStringSet(PREF_LINHAS_ATIVAS, null);

        Set<String> novoConjunto = new HashSet<>();
        if (linhasAtivas != null) {
            novoConjunto.addAll(linhasAtivas);
        } else {
            for (SimAccountEntry conta : todasAsContas) {
                novoConjunto.add(conta.id);
            }
        }

        if (ativa) {
            novoConjunto.add(contaId);
        } else {
            novoConjunto.remove(contaId);
        }

        prefs.edit().putStringSet(PREF_LINHAS_ATIVAS, novoConjunto).apply();
    }
}
