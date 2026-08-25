package br.com.hssoftware.trustcall;

public class HistoryEntry {
    public final long id;
    public final String numero;
    public final long timestamp;
    public final BlockReason motivo;

    public HistoryEntry(long id, String numero, long timestamp, BlockReason motivo) {
        this.id = id;
        this.numero = numero;
        this.timestamp = timestamp;
        this.motivo = motivo;
    }
}
