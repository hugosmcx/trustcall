package br.com.hssoftware.trustcall;

import android.net.Uri;
import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.PhoneAccountHandle;

public class TrustCallInCallService extends InCallService {

    private static TrustCallInCallService instancia;

    private Call chamadaAtual;
    private final CallDecisionEngine decisionEngine = new CallDecisionEngine();

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            tratarEstado(call, state);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instancia = this;
        AppLogger.log(this, "TrustCallInCallService", "Serviço criado (Telecom vinculou o app como InCallService)");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instancia == this) instancia = null;
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        AppLogger.log(this, "TrustCallInCallService", "onCallAdded estado=" + call.getState());
        call.registerCallback(callCallback);
        tratarEstado(call, call.getState());
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        if (call == chamadaAtual) {
            encerrarInteracao();
        }
    }

    private void tratarEstado(Call call, int state) {
        if (state == Call.STATE_RINGING) {
            chamadaAtual = call;
            avaliarChamadaRingente(call);
        } else if (state == Call.STATE_ACTIVE) {
            chamadaAtual = call;
            IncomingCallNotifier.cancelar(this);
            FloatingBubbleService.esconder(this);
            OngoingCallNotifier.mostrar(this, numeroDoCall(call));
        } else if (state == Call.STATE_DISCONNECTED) {
            if (call == chamadaAtual) {
                encerrarInteracao();
            }
        }
    }

    private String numeroDoCall(Call call) {
        Uri handle = call.getDetails().getHandle();
        String numero = handle != null ? handle.getSchemeSpecificPart() : null;
        return (numero == null || numero.isEmpty()) ? null : numero;
    }

    private void avaliarChamadaRingente(Call call) {
        String numeroOriginal = numeroDoCall(call);
        boolean numeroOculto = numeroOriginal == null;
        String numeroNormalizado = numeroOculto ? "" : PhoneUtils.normalizar(numeroOriginal);
        PhoneAccountHandle accountHandle = call.getDetails().getAccountHandle();
        String contaId = accountHandle != null ? accountHandle.getId() : null;

        CallDecisionEngine.Decisao decisao = decisionEngine.decidir(this, numeroOriginal, numeroNormalizado, numeroOculto, contaId);

        AppLogger.log(this, "TrustCallInCallService", "Chamada tocando " + (numeroOculto ? "oculta" : numeroOriginal)
                + " conta=" + contaId + " -> " + decisao.acao);

        if (decisao.acao == CallDecisionEngine.Acao.PERGUNTAR) {
            IncomingCallNotifier.mostrar(this, numeroOriginal, decisao.motivo);
            FloatingBubbleService.mostrar(this, numeroOriginal);
        } else {
            IncomingCallNotifier.mostrar(this, numeroOriginal, null);
        }
    }

    private void encerrarInteracao() {
        chamadaAtual = null;
        IncomingCallNotifier.cancelar(this);
        FloatingBubbleService.esconder(this);
        OngoingCallNotifier.cancelar(this);
    }

    public static void aceitarChamadaAtual() {
        if (instancia != null && instancia.chamadaAtual != null) {
            instancia.chamadaAtual.answer(0);
        }
    }

    public static void recusarChamadaAtual() {
        if (instancia != null && instancia.chamadaAtual != null) {
            instancia.chamadaAtual.reject(false, null);
        }
    }

    public static void encerrarChamadaAtual() {
        if (instancia != null && instancia.chamadaAtual != null) {
            instancia.chamadaAtual.disconnect();
        }
    }
}
