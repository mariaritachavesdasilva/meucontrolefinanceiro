package com.meucontrolefinanceiro.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

 data class FinanceEntry(
    val id: Long,
    val type: String,
    val description: String,
    val amount: Double,
    val date: String,
    val category: String
)

class FinanceDatabase(context: Context) : SQLiteOpenHelper(context, "financeiro.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, description TEXT NOT NULL, amount REAL NOT NULL, date TEXT NOT NULL, category TEXT NOT NULL)")
    db.execSQL("CREATE INDEX idx_entries_date ON entries(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { }

    fun add(type: String, description: String, amount: Double, date: String, category: String): Long {
        val values = ContentValues().apply {
            put("type", type); put("description", description); put("amount", amount); put("date", date); put("category", category)
        }
        return writableDatabase.insert("entries", null, values)
    }

    fun delete(id: Long) { writableDatabase.delete("entries", "id=?", arrayOf(id.toString())) }

    fun all(): List<FinanceEntry> {
        val result = mutableListOf<FinanceEntry>()
        readableDatabase.rawQuery("SELECT id,type,description,amount,date,category FROM entries ORDER BY date DESC, id DESC", null).use { cursor ->
            while (cursor.moveToNext()) result.add(FinanceEntry(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5)))
        }
        return result
    }
}
