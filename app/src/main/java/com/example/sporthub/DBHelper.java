package com.example.sporthub;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rutina.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_EJERCICIOS = "ejercicios_rutina";
    private static final String COL_ID = "id";
    private static final String COL_UID = "uid_usuario"; // UID de Firebase
    private static final String COL_DIA = "dia";
    private static final String COL_NOMBRE = "nombre";
    private static final String COL_GRUPO = "grupoMuscular";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_EJERCICIOS + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_UID + " TEXT," +
                COL_DIA + " TEXT," +
                COL_NOMBRE + " TEXT," +
                COL_GRUPO + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EJERCICIOS);
        onCreate(db);
    }

    // Insertar ejercicio para un usuario específico
    public void insertarEjercicio(String uid, String dia, String nombre, String grupo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_UID, uid);
        values.put(COL_DIA, dia);
        values.put(COL_NOMBRE, nombre);
        values.put(COL_GRUPO, grupo);
        db.insert(TABLE_EJERCICIOS, null, values);
        db.close();
    }

    // Obtener ejercicios por día para un usuario
    public ArrayList<Ejercicio> obtenerEjerciciosPorDia(String uid, String dia) {
        ArrayList<Ejercicio> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EJERCICIOS,
                new String[]{COL_NOMBRE, COL_GRUPO},
                COL_UID + "=? AND " + COL_DIA + "=?",
                new String[]{uid, dia},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String nombre = cursor.getString(0);
                String grupo = cursor.getString(1);
                lista.add(new Ejercicio(nombre, grupo));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    // Obtener toda la rutina del usuario
    public ArrayList<DiaRutina> obtenerRutinaCompleta(String uid) {
        ArrayList<DiaRutina> diasRutina = new ArrayList<>();
        String[] nombresDias = {"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};
        for (String dia : nombresDias) {
            DiaRutina dr = new DiaRutina(dia);
            dr.getEjercicios().addAll(obtenerEjerciciosPorDia(uid, dia));
            diasRutina.add(dr);
        }
        return diasRutina;
    }
}

