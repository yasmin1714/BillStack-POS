package com.example.billstack

import Bill
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BillDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "BillingSystem.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createBillTable = """
            CREATE TABLE IF NOT EXISTS Bills (
                bill_id INTEGER PRIMARY KEY AUTOINCREMENT,
                items TEXT(500),
                prices TEXT(500),
                bill_total TEXT,
                bill_date DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createBillTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Bills")
        onCreate(db)
    }

    // INSERT: Save a new bill
    fun insertBill(itemsCsv: String, pricesCsv: String, total: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("items", itemsCsv)
            put("prices", pricesCsv)
            put("bill_total", total)
        }
        return db.insert("Bills", null, values)
    }

    fun getAllBills(): List<Bill> {
        val billList = mutableListOf<Bill>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Bills ORDER BY bill_date DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val bill = Bill(
                    id = cursor.getInt(0),
                    items = cursor.getString(1),
                    prices = cursor.getString(2),
                    billTotal = cursor.getString(3),
                    date = cursor.getString(4)
                )
                billList.add(bill)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return billList
    }
}