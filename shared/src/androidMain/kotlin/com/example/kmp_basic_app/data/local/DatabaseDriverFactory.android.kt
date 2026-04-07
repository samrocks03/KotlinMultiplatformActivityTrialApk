package com.example.kmp_basic_app.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.kmp_basic_app.db.AppDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "kmp_app.db")
    }
}
