package br.com.hssoftware.trustcall;

import android.content.SharedPreferences;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.PhoneAccountHandle;

public class CallFilterService extends CallScreeningService {

    private final CallDecisionEngine decisionEngine = new CallDecisionEngine();

    @Override
    public void onScreenCall(Call.Details callDetails) {
        if (!servicoAtivo()) {
            AppLogger.log(this, "CallFilterService", "Chamada ignorada: serviço desativado");
            return;
        }

        Uri handle = callDetails.getHandle();
        String numeroOriginal = handle != null ? handle.getSchemeSpecificPart() : null;
        boolean numeroOculto = numeroOriginal == null || numeroOriginal.isEmpty();
        String numeroNormalizado = numeroOculto ? "" : PhoneUtils.normalizar(numeroOriginal);
        PhoneAccountHandle accountHandle = callDetails.getAccountHandle();
        String contaId = accountHandle != null ? accountHandle.getId() : null;

        CallDecisionEngine.Decisao decisao = decisionEngine.decidir(this, numeroOriginal, numeroNormalizado, numeroOculto, contaId);

        AppLogger.log(this, "CallFilterService", "Chamada " + (numeroOculto ? "oculta" : numeroOriginal)
                + " conta=" + contaId + " -> " + decisao.acao + (decisao.motivo != null ? " (" + decisao.motivo + ")" : ""));

        if (decisao.acao == CallDecisionEngine.Acao.BLOQUEAR) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(true)
                    .setSkipNotification(true)
                    .build();

            respondToCall(callDetails, response);

            TrustCallRepository repository = TrustCallRepository.getInstance(this);
            repository.addHistoryEntry(numeroOculto ? null : numeroOriginal, decisao.motivo);
            NotificationHelper.updateNotification(this);
            TrustCallEvents.notificarHistoricoAtualizado();
        } else {
            respondToCall(callDetails, new CallResponse.Builder().setDisallowCall(false).build());

            if (decisao.acao == CallDecisionEngine.Acao.PERGUNTAR) {
                IncomingCallNotifier.mostrar(this, numeroOculto ? null : numeroOriginal, decisao.motivo);
                FloatingBubbleService.mostrar(this, numeroOculto ? null : numeroOriginal, decisao.motivo);
            }
        }
    }

    private boolean servicoAtivo(){
        SharedPreferences prefs = getSharedPreferences("TRUST_CALL_PREFS", MODE_PRIVATE);
        return prefs.getBoolean("BLOQUEIO_ATIVO", false);
    }

}
