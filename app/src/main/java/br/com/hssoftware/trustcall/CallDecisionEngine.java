package br.com.hssoftware.trustcall;

import android.Manifest;
import android.content.Context;
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

        TrustCallRepository repository = TrustCallRepository.getInstance(context);

        if (!numeroOculto && repository.isInList(numeroNormalizado, ListType.BRANCA)) {
            return Decisao.permitir();
        }

        if (!numeroOculto && repository.isInList(numeroNormalizado, ListType.NEGRA)) {
            return Decisao.of(Acao.BLOQUEAR, BlockReason.LISTA_NEGRA);
        }

        if (numeroOculto) {
            if (ChipConfig.isFiltroAtivo(context, ChipConfig.CRITERIO_OCULTOS, contaId, false)) {
                return Decisao.of(ChipConfig.getModo(context, ChipConfig.CRITERIO_OCULTOS, contaId), BlockReason.OCULTO);
            }
            return Decisao.permitir();
        }

        if (ChipConfig.isFiltroAtivo(context, ChipConfig.CRITERIO_INTERNACIONAL, contaId, false)
                && PhoneUtils.isInternacional(numeroOriginal)) {
            return Decisao.of(ChipConfig.getModo(context, ChipConfig.CRITERIO_INTERNACIONAL, contaId), BlockReason.INTERNACIONAL);
        }

        if (ChipConfig.isFiltroAtivo(context, ChipConfig.CRITERIO_DDD, contaId, false)) {
            String ddd = PhoneUtils.extrairDDD(numeroOriginal);
            if (ddd != null && ChipConfig.getDddsBloqueados(context, contaId).contains(ddd)) {
                return Decisao.of(ChipConfig.getModo(context, ChipConfig.CRITERIO_DDD, contaId), BlockReason.DDD_BLOQUEADO);
            }
        }

        if (ChipConfig.isFiltroAtivo(context, ChipConfig.CRITERIO_DESCONHECIDOS, contaId, true)
                && temPermissaoContatos(context)
                && !isNumberInContacts(context, numeroOriginal)) {
            return Decisao.of(ChipConfig.getModo(context, ChipConfig.CRITERIO_DESCONHECIDOS, contaId), BlockReason.DESCONHECIDO);
        }

        return Decisao.permitir();
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
