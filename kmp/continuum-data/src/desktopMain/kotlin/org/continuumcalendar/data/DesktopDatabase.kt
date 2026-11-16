package org.continuumcalendar.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun createDesktopDriver(path: String = JdbcSqliteDriver.IN_MEMORY): SqlDriver {
    val driver = JdbcSqliteDriver(path)
    ContinuumDatabase.Schema.create(driver)
    return driver
}

fun createDesktopDatabase(path: String = JdbcSqliteDriver.IN_MEMORY): ContinuumDatabase =
    ContinuumDatabase(createDesktopDriver(path))
