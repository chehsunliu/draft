package io.github.chehsunliu.itx.impl.repo.postgres

import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException
import io.github.chehsunliu.itx.contract.repo.User
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.github.chehsunliu.itx.impl.repo.bindAll
import io.github.chehsunliu.itx.impl.repo.firstOrNull
import io.github.chehsunliu.itx.impl.repo.useConnection
import java.util.UUID
import javax.sql.DataSource

internal class PostgresUserRepo(
    private val ds: DataSource,
) : UserRepo {
    override suspend fun upsert(params: UserRepo.UpsertParams): User =
        ds.useConnection { conn ->
            conn
                .prepareStatement(
                    """
                INSERT INTO users (id, email) VALUES (?, ?)
                ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id
                RETURNING id, email
                """,
                ).use { ps ->
                    ps.bindAll(params.id, params.email)
                    ps.executeQuery().use { rs ->
                        rs.firstOrNull { User(it.getObject("id", UUID::class.java), it.getString("email")) }
                            ?: error("upsert returned no row")
                    }
                }
        }

    override suspend fun get(id: UUID): User =
        ds.useConnection { conn ->
            conn.prepareStatement("SELECT id, email FROM users WHERE id = ?").use { ps ->
                ps.bindAll(id)
                ps.executeQuery().use { rs ->
                    rs.firstOrNull { User(it.getObject("id", UUID::class.java), it.getString("email")) }
                        ?: throw RepoNotFoundException()
                }
            }
        }
}
