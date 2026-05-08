package io.github.chehsunliu.itx.impl.repo.mariadb

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException
import io.github.chehsunliu.itx.contract.repo.User
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.github.chehsunliu.itx.impl.repo.bindAll
import io.github.chehsunliu.itx.impl.repo.firstOrNull
import io.github.chehsunliu.itx.impl.repo.useConnection
import java.util.UUID
import javax.sql.DataSource

internal class MariaDbUserRepo(
    private val ds: DataSource,
) : UserRepo {
    override suspend fun upsert(params: UserRepo.UpsertParams): User =
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    "INSERT INTO users (id, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = id",
                ).use {
                    it.bindAll(params.id.toString(), params.email)
                    it.executeUpdate()
                }
            conn.prepareStatement("SELECT id, email FROM users WHERE id = ?").use { ps ->
                ps.bindAll(params.id.toString())
                ps.executeQuery().use { rs ->
                    rs.firstOrNull { User(UUID.fromString(it.getString("id")), it.getString("email")) }
                        ?: error("upsert returned no row")
                }
            }
        }

    override suspend fun get(id: UUID): User =
        ds.useConnection { conn ->
            conn.prepareStatement("SELECT id, email FROM users WHERE id = ?").use { ps ->
                ps.bindAll(id.toString())
                ps.executeQuery().use { rs ->
                    rs.firstOrNull { User(UUID.fromString(it.getString("id")), it.getString("email")) }
                        ?: throw RepoNotFoundException()
                }
            }
        }
}
