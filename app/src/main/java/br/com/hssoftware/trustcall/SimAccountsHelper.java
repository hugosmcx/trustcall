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
        try {
            TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
            if (telecomManager == null) return resultado;

            List<PhoneAccountHandle> handles = telecomManager.getCallCapablePhoneAccounts();
            int indice = 1;
            for (PhoneAccountHandle handle : handles) {
                PhoneAccount account = telecomManager.getPhoneAccount(handle);
                String label = (account != null && account.getLabel() != null)
                        ? account.getLabel().toString()
                        : ("SIM " + indice);
                String subtitulo = (account != null && account.getShortDescription() != null)
                        ? account.getShortDescription().toString()
                        : "SIM " + indice;
                resultado.add(new SimAccountEntry(handle.getId(), label, subtitulo));
                indice++;
            }
        } catch (SecurityException ignored) {
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
