package io.github.chehsunliu.itx.impl.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

internal suspend inline fun <T> DataSource.useConnection(crossinline block: (Connection) -> T): T =
    withContext(Dispatchers.IO) { connection.use { block(it) } }

internal suspend inline fun <T> DataSource.transactionally(crossinline block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        connection.use { conn ->
            val originalAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                val result = block(conn)
                conn.commit()
                result
            } catch (t: Throwable) {
                conn.rollback()
                throw t
            } finally {
                conn.autoCommit = originalAutoCommit
            }
        }
    }

internal fun PreparedStatement.bindAll(vararg args: Any?) {
    args.forEachIndexed { i, value -> setObject(i + 1, value) }
}

internal fun <T> ResultSet.mapAll(transform: (ResultSet) -> T): List<T> {
    val out = mutableListOf<T>()
    while (next()) out.add(transform(this))
    return out
}

internal fun <T> ResultSet.firstOrNull(transform: (ResultSet) -> T): T? = if (next()) transform(this) else null
