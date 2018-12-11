package io.shortbread.apps.recon

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper

class DatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "database", null, 1) {
    companion object {
        private var instance: DatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseOpenHelper {
            if (instance == null) {
                instance = DatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}

// Access property for Context
val Context.database: DatabaseOpenHelper
    get() = DatabaseOpenHelper.getInstance(getApplicationContext())
