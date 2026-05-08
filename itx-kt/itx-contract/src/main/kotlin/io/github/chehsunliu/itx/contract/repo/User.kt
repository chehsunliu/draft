package io.github.chehsunliu.itx.contract.repo

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
)

interface UserRepo {
    /** Inserts the user if missing, then returns the current row. */
    suspend fun upsert(params: UpsertParams): User

    /** Throws [RepoNotFoundException] if no user with [id] exists. */
    suspend fun get(id: UUID): User

    data class UpsertParams(
        val id: UUID,
        val email: String,
    )
}
