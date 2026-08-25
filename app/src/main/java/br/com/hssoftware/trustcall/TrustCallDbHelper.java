package br.com.hssoftware.trustcall;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TrustCallDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "trust_call.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_LISTA = "numero_lista";
    public static final String COL_LISTA_ID = "id";
    public static final String COL_LISTA_NUMERO = "numero";
    public static final String COL_LISTA_TIPO = "tipo";
    public static final String COL_LISTA_CRIADO_EM = "criado_em";

    public static final String TABLE_HISTORICO = "historico_bloqueios";
    public static final String COL_HIST_ID = "id";
    public static final String COL_HIST_NUMERO = "numero";
    public static final String COL_HIST_TIMESTAMP = "timestamp";
    public static final String COL_HIST_MOTIVO = "motivo";

    public TrustCallDbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LISTA + " (" +
                COL_LISTA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LISTA_NUMERO + " TEXT NOT NULL, " +
                COL_LISTA_TIPO + " TEXT NOT NULL, " +
                COL_LISTA_CRIADO_EM + " INTEGER NOT NULL, " +
                "UNIQUE(" + COL_LISTA_NUMERO + ", " + COL_LISTA_TIPO + "))");

        db.execSQL("CREATE TABLE " + TABLE_HISTORICO + " (" +
                COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HIST_NUMERO + " TEXT NOT NULL, " +
                COL_HIST_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_HIST_MOTIVO + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LISTA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORICO);
        onCreate(db);
    }
}
