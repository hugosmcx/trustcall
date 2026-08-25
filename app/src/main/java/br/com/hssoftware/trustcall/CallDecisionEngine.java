package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

public class CallDecisionEngine {

    public enum Acao { PERMITIR, BLOQUEAR, PERGUNTAR }

    public static class Decisao {
        public final Acao acao;
        public final BlockReason motivo;

        private Decisao(Acao acao, BlockReason motivo) {
            this.acao = acao;
            this.motivo = motivo;
        }

        static Decisao permitir() {
            return new Decisao(Acao.PERMITIR, null);
        }

        static Decisao of(Acao acao, BlockReason motivo) {
            return new Decisao(acao, motivo);
        }
    }

    public Decisao decidir(Context context, String numeroOriginal, String numeroNormalizado, boolean numeroOculto, String contaId) {
        if (!SimAccountsHelper.linhaAtiva(context, contaId)) {
            return Decisao.permitir();
        }

        SharedPreferences prefs = context.getSharedPreferences("TRUST_CALL_PREFS", Context.MODE_PRIVATE);
        TrustCallRepository repository = TrustCallRepository.getInstance(context);

        if (!numeroOculto && repository.isInList(numeroNormalizado, ListType.BRANCA)) {
            return Decisao.permitir();
        }

        if (!numeroOculto && repository.isInList(numeroNormalizado, ListType.NEGRA)) {
            return Decisao.of(Acao.BLOQUEAR, BlockReason.LISTA_NEGRA);
        }

        if (numeroOculto) {
            if (prefs.getBoolean("FILTRO_OCULTOS", false)) {
                return Decisao.of(acaoDoModo(prefs, "MODO_OCULTOS"), BlockReason.OCULTO);
            }
            return Decisao.permitir();
        }

        if (prefs.getBoolean("FILTRO_INTERNACIONAL", false) && PhoneUtils.isInternacional(numeroOriginal)) {
            return Decisao.of(acaoDoModo(prefs, "MODO_INTERNACIONAL"), BlockReason.INTERNACIONAL);
        }

        if (prefs.getBoolean("FILTRO_DESCONHECIDOS", true)
                && temPermissaoContatos(context)
                && !isNumberInContacts(context, numeroOriginal)) {
            return Decisao.of(acaoDoModo(prefs, "MODO_DESCONHECIDOS"), BlockReason.DESCONHECIDO);
        }

        return Decisao.permitir();
    }

    private Acao acaoDoModo(SharedPreferences prefs, String chave) {
        return "PERGUNTAR".equals(prefs.getString(chave, "BLOQUEAR")) ? Acao.PERGUNTAR : Acao.BLOQUEAR;
    }

    private boolean temPermissaoContatos(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isNumberInContacts(Context context, String phoneNumber) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);

        if (cursor != null) {
            String numeroNormalizado = PhoneUtils.normalizar(phoneNumber);
            while (cursor.moveToNext()) {
                String contactNumber = PhoneUtils.normalizar(cursor.getString(0));
                if (PhoneUtils.correspondem(numeroNormalizado, contactNumber)) {
                    cursor.close();
                    return true;
                }
            }
            cursor.close();
        }
        return false;
    }
}
