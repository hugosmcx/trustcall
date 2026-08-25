package br.com.hssoftware.trustcall;

public enum BlockReason {
    LISTA_NEGRA(R.string.reason_lista_negra),
    OCULTO(R.string.reason_oculto),
    INTERNACIONAL(R.string.reason_internacional),
    DESCONHECIDO(R.string.reason_desconhecido);

    public final int labelResId;

    BlockReason(int labelResId) {
        this.labelResId = labelResId;
    }
}
