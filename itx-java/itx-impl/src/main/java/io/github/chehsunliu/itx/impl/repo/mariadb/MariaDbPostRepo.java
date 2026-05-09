package io.github.chehsunliu.itx.impl.repo.mariadb;

import static io.github.chehsunliu.itx.impl.repo.Jdbc.bindAll;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.firstOrNull;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.mapAll;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.transactionally;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.useConnection;
import static io.github.chehsunliu.itx.impl.repo.Jdbc.useConnectionVoid;

import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class MariaDbPostRepo implements PostRepo {
  private final DataSource ds;

  private record Row(
      long id,
      UUID authorId,
      String title,
      String body,
      OffsetDateTime createdAt,
      OffsetDateTime notifiedAt) {
    Post toPost(List<String> tags) {
      return new Post(id, authorId, title, body, tags, createdAt, notifiedAt);
    }
  }

  // MariaDB TIMESTAMP is wall-clock; reading as Timestamp would route through the JVM default
  // zone. Pull the literal LocalDateTime and stamp UTC explicitly.
  private static Row toRow(ResultSet rs) throws SQLException {
    LocalDateTime notified = rs.getObject("notified_at", LocalDateTime.class);
    return new Row(
        rs.getLong("id"),
        UUID.fromString(rs.getString("author_id")),
        rs.getString("title"),
        rs.getString("body"),
        rs.getObject("created_at", LocalDateTime.class).atOffset(ZoneOffset.UTC),
        notified == null ? null : notified.atOffset(ZoneOffset.UTC));
  }

  @Override
  public List<Post> list(ListParams params) {
    return useConnection(
        ds,
        conn -> {
          int limit = params.limit() == 0 ? 50 : params.limit();
          List<Row> rows;
          if (params.authorId() != null) {
            try (var ps =
                conn.prepareStatement(
                    """
                    SELECT id, author_id, title, body, created_at, notified_at
                    FROM posts WHERE author_id = ?
                    ORDER BY id DESC LIMIT ? OFFSET ?
                    """)) {
              bindAll(ps, params.authorId().toString(), limit, params.offset());
              try (var rs = ps.executeQuery()) {
                rows = mapAll(rs, MariaDbPostRepo::toRow);
              }
            }
          } else {
            try (var ps =
                conn.prepareStatement(
                    """
                    SELECT id, author_id, title, body, created_at, notified_at
                    FROM posts ORDER BY id DESC LIMIT ? OFFSET ?
                    """)) {
              bindAll(ps, limit, params.offset());
              try (var rs = ps.executeQuery()) {
                rows = mapAll(rs, MariaDbPostRepo::toRow);
              }
            }
          }
          List<Long> ids = new ArrayList<>(rows.size());
          for (Row r : rows) ids.add(r.id());
          Map<Long, List<String>> tagMap = fetchTagsFor(conn, ids);
          List<Post> out = new ArrayList<>(rows.size());
          for (Row r : rows) out.add(r.toPost(tagMap.getOrDefault(r.id(), List.of())));
          return out;
        });
  }

  @Override
  public Post get(GetParams params) {
    return useConnection(
        ds,
        conn -> {
          Row row;
          try (var ps =
              conn.prepareStatement(
                  "SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = ?")) {
            bindAll(ps, params.id());
            try (var rs = ps.executeQuery()) {
              row = firstOrNull(rs, MariaDbPostRepo::toRow).orElseThrow(RepoNotFoundException::new);
            }
          }
          List<String> tags =
              fetchTagsFor(conn, List.of(row.id())).getOrDefault(row.id(), List.of());
          return row.toPost(tags);
        });
  }

  @Override
  public Post create(CreateParams params) {
    return transactionally(
        ds,
        conn -> {
          long id;
          try (var ps =
              conn.prepareStatement(
                  "INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS)) {
            bindAll(ps, params.authorId().toString(), params.title(), params.body());
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
              if (!rs.next())
                throw new IllegalStateException("INSERT did not return generated key");
              id = rs.getLong(1);
            }
          }
          OffsetDateTime createdAt;
          try (var ps = conn.prepareStatement("SELECT created_at FROM posts WHERE id = ?")) {
            bindAll(ps, id);
            try (var rs = ps.executeQuery()) {
              if (!rs.next()) throw new IllegalStateException("inserted post not found");
              createdAt = rs.getObject(1, LocalDateTime.class).atOffset(ZoneOffset.UTC);
            }
          }
          List<Long> tagIds = upsertTags(conn, params.tags());
          linkPostTags(conn, id, tagIds);
          return new Post(
              id, params.authorId(), params.title(), params.body(), params.tags(), createdAt, null);
        });
  }

  @Override
  public Post update(UpdateParams params) {
    return transactionally(
        ds,
        conn -> {
          Row existing;
          try (var ps =
              conn.prepareStatement(
                  """
                  SELECT id, author_id, title, body, created_at, notified_at
                  FROM posts WHERE id = ? AND author_id = ? FOR UPDATE
                  """)) {
            bindAll(ps, params.id(), params.authorId().toString());
            try (var rs = ps.executeQuery()) {
              existing =
                  firstOrNull(rs, MariaDbPostRepo::toRow).orElseThrow(RepoNotFoundException::new);
            }
          }

          String title = params.title() != null ? params.title() : existing.title();
          String body = params.body() != null ? params.body() : existing.body();

          try (var ps =
              conn.prepareStatement("UPDATE posts SET title = ?, body = ? WHERE id = ?")) {
            bindAll(ps, title, body, params.id());
            ps.executeUpdate();
          }

          List<String> tags;
          if (params.tags() != null) {
            try (var ps = conn.prepareStatement("DELETE FROM post_tags WHERE post_id = ?")) {
              bindAll(ps, params.id());
              ps.executeUpdate();
            }
            List<Long> tagIds = upsertTags(conn, params.tags());
            linkPostTags(conn, params.id(), tagIds);
            tags = params.tags();
          } else {
            try (var ps =
                conn.prepareStatement(
                    """
                    SELECT t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id
                    WHERE pt.post_id = ? ORDER BY t.name
                    """)) {
              bindAll(ps, params.id());
              try (var rs = ps.executeQuery()) {
                tags = mapAll(rs, r -> r.getString(1));
              }
            }
          }

          return new Post(
              params.id(),
              params.authorId(),
              title,
              body,
              tags,
              existing.createdAt(),
              existing.notifiedAt());
        });
  }

  @Override
  public void delete(DeleteParams params) {
    useConnectionVoid(
        ds,
        conn -> {
          int rows;
          try (var ps = conn.prepareStatement("DELETE FROM posts WHERE id = ? AND author_id = ?")) {
            bindAll(ps, params.id(), params.authorId().toString());
            rows = ps.executeUpdate();
          }
          if (rows == 0) throw new RepoNotFoundException();
        });
  }

  @Override
  public void markNotified(long id) {
    useConnectionVoid(
        ds,
        conn -> {
          try (var ps =
              conn.prepareStatement("UPDATE posts SET notified_at = NOW() WHERE id = ?")) {
            bindAll(ps, id);
            ps.executeUpdate();
          }
        });
  }

  private static List<Long> upsertTags(Connection conn, List<String> names) throws SQLException {
    List<Long> out = new ArrayList<>(names.size());
    for (String name : names) {
      try (var ps = conn.prepareStatement("INSERT IGNORE INTO tags (name) VALUES (?)")) {
        bindAll(ps, name);
        ps.executeUpdate();
      }
      try (var ps = conn.prepareStatement("SELECT id FROM tags WHERE name = ?")) {
        bindAll(ps, name);
        try (var rs = ps.executeQuery()) {
          if (!rs.next())
            throw new IllegalStateException("tag '" + name + "' missing after upsert");
          out.add(rs.getLong(1));
        }
      }
    }
    return out;
  }

  private static void linkPostTags(Connection conn, long postId, List<Long> tagIds)
      throws SQLException {
    for (long tid : tagIds) {
      try (var ps =
          conn.prepareStatement("INSERT IGNORE INTO post_tags (post_id, tag_id) VALUES (?, ?)")) {
        bindAll(ps, postId, tid);
        ps.executeUpdate();
      }
    }
  }

  private static Map<Long, List<String>> fetchTagsFor(Connection conn, List<Long> postIds)
      throws SQLException {
    if (postIds.isEmpty()) return Map.of();
    StringBuilder placeholders = new StringBuilder();
    for (int i = 0; i < postIds.size(); i++) {
      if (i > 0) placeholders.append(',');
      placeholders.append('?');
    }
    try (var ps =
        conn.prepareStatement(
            "SELECT pt.post_id, t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id"
                + " WHERE pt.post_id IN ("
                + placeholders
                + ") ORDER BY t.name")) {
      for (int i = 0; i < postIds.size(); i++) {
        ps.setLong(i + 1, postIds.get(i));
      }
      try (var rs = ps.executeQuery()) {
        Map<Long, List<String>> out = new LinkedHashMap<>();
        while (rs.next()) {
          out.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2));
        }
        Map<Long, List<String>> immut = new HashMap<>();
        for (var e : out.entrySet()) immut.put(e.getKey(), List.copyOf(e.getValue()));
        return immut;
      }
    }
  }
}
