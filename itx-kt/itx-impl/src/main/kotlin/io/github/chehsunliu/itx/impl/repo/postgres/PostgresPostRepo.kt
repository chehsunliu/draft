package io.github.chehsunliu.itx.impl.repo.postgres

import io.github.chehsunliu.itx.contract.repo.Post
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException
import io.github.chehsunliu.itx.impl.repo.bindAll
import io.github.chehsunliu.itx.impl.repo.firstOrNull
import io.github.chehsunliu.itx.impl.repo.mapAll
import io.github.chehsunliu.itx.impl.repo.transactionally
import io.github.chehsunliu.itx.impl.repo.useConnection
import java.sql.Connection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

internal class PostgresPostRepo(
    private val ds: DataSource,
) : PostRepo {
    private data class Row(
        val id: Long,
        val authorId: UUID,
        val title: String,
        val body: String,
        val createdAt: OffsetDateTime,
        val notifiedAt: OffsetDateTime?,
    )

    private fun java.sql.ResultSet.toRow(): Row =
        Row(
            id = getLong("id"),
            authorId = getObject("author_id", UUID::class.java),
            title = getString("title"),
            body = getString("body"),
            createdAt = getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            notifiedAt = getTimestamp("notified_at")?.toInstant()?.atOffset(ZoneOffset.UTC),
        )

    private fun Row.toPost(tags: List<String>): Post = Post(id, authorId, title, body, tags, createdAt, notifiedAt)

    override suspend fun list(params: PostRepo.ListParams): List<Post> =
        ds.useConnection { conn ->
            val limit = if (params.limit == 0) 50 else params.limit
            val rows =
                if (params.authorId != null) {
                    conn
                        .prepareStatement(
                            """
                        SELECT id, author_id, title, body, created_at, notified_at
                        FROM posts WHERE author_id = ?
                        ORDER BY id DESC LIMIT ? OFFSET ?
                        """,
                        ).use { ps ->
                            ps.bindAll(params.authorId, limit, params.offset)
                            ps.executeQuery().use { rs -> rs.mapAll { it.toRow() } }
                        }
                } else {
                    conn
                        .prepareStatement(
                            """
                        SELECT id, author_id, title, body, created_at, notified_at
                        FROM posts ORDER BY id DESC LIMIT ? OFFSET ?
                        """,
                        ).use { ps ->
                            ps.bindAll(limit, params.offset)
                            ps.executeQuery().use { rs -> rs.mapAll { it.toRow() } }
                        }
                }
            val tagMap = fetchTagsFor(conn, rows.map { it.id })
            rows.map { it.toPost(tagMap[it.id].orEmpty()) }
        }

    override suspend fun get(params: PostRepo.GetParams): Post =
        ds.useConnection { conn ->
            val row =
                conn
                    .prepareStatement(
                        "SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = ?",
                    ).use { ps ->
                        ps.bindAll(params.id)
                        ps.executeQuery().use { rs -> rs.firstOrNull { it.toRow() } }
                    } ?: throw RepoNotFoundException()
            val tags = fetchTagsFor(conn, listOf(row.id))[row.id].orEmpty()
            row.toPost(tags)
        }

    override suspend fun create(params: PostRepo.CreateParams): Post =
        ds.transactionally { conn ->
            val row =
                conn
                    .prepareStatement(
                        """
                    INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?)
                    RETURNING id, author_id, title, body, created_at, notified_at
                    """,
                    ).use { ps ->
                        ps.bindAll(params.authorId, params.title, params.body)
                        ps.executeQuery().use { rs ->
                            rs.firstOrNull { it.toRow() } ?: error("INSERT RETURNING produced no row")
                        }
                    }
            val tagIds = upsertTags(conn, params.tags)
            linkPostTags(conn, row.id, tagIds)
            row.toPost(params.tags)
        }

    override suspend fun update(params: PostRepo.UpdateParams): Post =
        ds.transactionally { conn ->
            val existing =
                conn
                    .prepareStatement(
                        """
                    SELECT id, author_id, title, body, created_at, notified_at
                    FROM posts WHERE id = ? AND author_id = ? FOR UPDATE
                    """,
                    ).use { ps ->
                        ps.bindAll(params.id, params.authorId)
                        ps.executeQuery().use { rs -> rs.firstOrNull { it.toRow() } }
                    } ?: throw RepoNotFoundException()

            val title = params.title ?: existing.title
            val body = params.body ?: existing.body

            conn.prepareStatement("UPDATE posts SET title = ?, body = ? WHERE id = ?").use {
                it.bindAll(title, body, params.id)
                it.executeUpdate()
            }

            val newTags = params.tags
            val tags =
                if (newTags != null) {
                    conn.prepareStatement("DELETE FROM post_tags WHERE post_id = ?").use {
                        it.bindAll(params.id)
                        it.executeUpdate()
                    }
                    val tagIds = upsertTags(conn, newTags)
                    linkPostTags(conn, params.id, tagIds)
                    newTags
                } else {
                    conn
                        .prepareStatement(
                            """
                        SELECT t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id
                        WHERE pt.post_id = ? ORDER BY t.name
                        """,
                        ).use { ps ->
                            ps.bindAll(params.id)
                            ps.executeQuery().use { rs -> rs.mapAll { it.getString(1) } }
                        }
                }

            Post(params.id, params.authorId, title, body, tags, existing.createdAt, existing.notifiedAt)
        }

    override suspend fun delete(params: PostRepo.DeleteParams) {
        ds.useConnection { conn ->
            val rows =
                conn.prepareStatement("DELETE FROM posts WHERE id = ? AND author_id = ?").use {
                    it.bindAll(params.id, params.authorId)
                    it.executeUpdate()
                }
            if (rows == 0) throw RepoNotFoundException()
        }
    }

    override suspend fun markNotified(id: Long) {
        ds.useConnection { conn ->
            conn.prepareStatement("UPDATE posts SET notified_at = now() WHERE id = ?").use {
                it.bindAll(id)
                it.executeUpdate()
            }
        }
    }

    private fun upsertTags(
        conn: Connection,
        names: List<String>,
    ): List<Long> =
        names.map { name ->
            conn
                .prepareStatement(
                    """
                INSERT INTO tags (name) VALUES (?)
                ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """,
                ).use { ps ->
                    ps.bindAll(name)
                    ps.executeQuery().use { rs ->
                        rs.firstOrNull { it.getLong(1) } ?: error("upsert tag returned no row")
                    }
                }
        }

    private fun linkPostTags(
        conn: Connection,
        postId: Long,
        tagIds: List<Long>,
    ) {
        for (tid in tagIds) {
            conn
                .prepareStatement(
                    "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                ).use {
                    it.bindAll(postId, tid)
                    it.executeUpdate()
                }
        }
    }

    private fun fetchTagsFor(
        conn: Connection,
        postIds: List<Long>,
    ): Map<Long, List<String>> {
        if (postIds.isEmpty()) return emptyMap()
        val placeholders = postIds.joinToString(",") { "?" }
        return conn
            .prepareStatement(
                """
            SELECT pt.post_id, t.name
            FROM post_tags pt JOIN tags t ON pt.tag_id = t.id
            WHERE pt.post_id IN ($placeholders)
            ORDER BY t.name
            """,
            ).use { ps ->
                postIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val out = HashMap<Long, MutableList<String>>()
                    while (rs.next()) out.getOrPut(rs.getLong(1)) { mutableListOf() }.add(rs.getString(2))
                    out.mapValues { it.value.toList() }
                }
            }
    }
}
