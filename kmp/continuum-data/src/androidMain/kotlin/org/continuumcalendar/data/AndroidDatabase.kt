package org.continuumcalendar.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createAndroidDriver(context: Context, name: String = "continuum-cmp.db"): SqlDriver =
    AndroidSqliteDriver(ContinuumDatabase.Schema, context, name)

fun createAndroidDatabase(context: Context): ContinuumDatabase =
    ContinuumDatabase(createAndroidDriver(context))
