package br.com.hssoftware.trustcall;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class TrustCallRepository {

    private static TrustCallRepository instancia;

    private final TrustCallDbHelper dbHelper;

    private TrustCallRepository(Context context) {
        dbHelper = new TrustCallDbHelper(context);
    }

    public static synchronized TrustCallRepository getInstance(Context context) {
        if (instancia == null) {
            instancia = new TrustCallRepository(context);
        }
        return instancia;
    }

    public void addToList(String numero, ListType tipo) {
        String numeroNormalizado = PhoneUtils.normalizar(numero);
        if (numeroNormalizado.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TrustCallDbHelper.COL_LISTA_NUMERO, numeroNormalizado);
        values.put(TrustCallDbHelper.COL_LISTA_TIPO, tipo.name());
        values.put(TrustCallDbHelper.COL_LISTA_CRIADO_EM, System.currentTimeMillis());
        db.insertWithOnConflict(TrustCallDbHelper.TABLE_LISTA, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeFromList(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TrustCallDbHelper.TABLE_LISTA,
                TrustCallDbHelper.COL_LISTA_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public boolean isInList(String numeroNormalizado, ListType tipo) {
        if (numeroNormalizado == null || numeroNormalizado.isEmpty()) return false;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(TrustCallDbHelper.TABLE_LISTA,
                new String[]{TrustCallDbHelper.COL_LISTA_NUMERO},
                TrustCallDbHelper.COL_LISTA_TIPO + "=?",
                new String[]{tipo.name()},
                null, null, null)) {

            while (cursor.moveToNext()) {
                String numeroSalvo = cursor.getString(0);
                if (PhoneUtils.correspondem(numeroNormalizado, numeroSalvo)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<NumberEntry> getList(ListType tipo) {
        List<NumberEntry> resultado = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(TrustCallDbHelper.TABLE_LISTA,
                null,
                TrustCallDbHelper.COL_LISTA_TIPO + "=?",
                new String[]{tipo.name()},
                null, null,
                TrustCallDbHelper.COL_LISTA_CRIADO_EM + " DESC")) {

            int idxId = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_LISTA_ID);
            int idxNumero = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_LISTA_NUMERO);
            int idxCriadoEm = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_LISTA_CRIADO_EM);

            while (cursor.moveToNext()) {
                resultado.add(new NumberEntry(
                        cursor.getLong(idxId),
                        cursor.getString(idxNumero),
                        cursor.getLong(idxCriadoEm)));
            }
        }
        return resultado;
    }

    public void addHistoryEntry(String numero, BlockReason motivo) {
        String numeroNormalizado = numero != null ? PhoneUtils.normalizar(numero) : "";

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TrustCallDbHelper.COL_HIST_NUMERO, numeroNormalizado.isEmpty() ? null : numeroNormalizado);
        values.put(TrustCallDbHelper.COL_HIST_TIMESTAMP, System.currentTimeMillis());
        values.put(TrustCallDbHelper.COL_HIST_MOTIVO, motivo.name());
        db.insert(TrustCallDbHelper.TABLE_HISTORICO, null, values);
    }

    public long getHistoryCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return android.database.DatabaseUtils.queryNumEntries(db, TrustCallDbHelper.TABLE_HISTORICO);
    }

    public List<HistoryEntry> getHistory() {
        List<HistoryEntry> resultado = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(TrustCallDbHelper.TABLE_HISTORICO,
                null, null, null, null, null,
                TrustCallDbHelper.COL_HIST_TIMESTAMP + " DESC")) {

            int idxId = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_HIST_ID);
            int idxNumero = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_HIST_NUMERO);
            int idxTimestamp = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_HIST_TIMESTAMP);
            int idxMotivo = cursor.getColumnIndexOrThrow(TrustCallDbHelper.COL_HIST_MOTIVO);

            while (cursor.moveToNext()) {
                resultado.add(new HistoryEntry(
                        cursor.getLong(idxId),
                        cursor.getString(idxNumero),
                        cursor.getLong(idxTimestamp),
                        BlockReason.valueOf(cursor.getString(idxMotivo))));
            }
        }
        return resultado;
    }
}
