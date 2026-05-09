package io.github.chehsunliu.itx.impl.repo.postgres;

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
import java.sql.Timestamp;
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
final class PostgresPostRepo implements PostRepo {
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

  private static Row toRow(ResultSet rs) throws SQLException {
    Timestamp notified = rs.getTimestamp("notified_at");
    return new Row(
        rs.getLong("id"),
        rs.getObject("author_id", UUID.class),
        rs.getString("title"),
        rs.getString("body"),
        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
        notified == null ? null : notified.toInstant().atOffset(ZoneOffset.UTC));
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
              bindAll(ps, params.authorId(), limit, params.offset());
              try (var rs = ps.executeQuery()) {
                rows = mapAll(rs, PostgresPostRepo::toRow);
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
                rows = mapAll(rs, PostgresPostRepo::toRow);
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
              row =
                  firstOrNull(rs, PostgresPostRepo::toRow).orElseThrow(RepoNotFoundException::new);
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
          Row row;
          try (var ps =
              conn.prepareStatement(
                  """
                  INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?)
                  RETURNING id, author_id, title, body, created_at, notified_at
                  """)) {
            bindAll(ps, params.authorId(), params.title(), params.body());
            try (var rs = ps.executeQuery()) {
              row =
                  firstOrNull(rs, PostgresPostRepo::toRow)
                      .orElseThrow(
                          () -> new IllegalStateException("INSERT RETURNING produced no row"));
            }
          }
          List<Long> tagIds = upsertTags(conn, params.tags());
          linkPostTags(conn, row.id(), tagIds);
          return row.toPost(params.tags());
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
            bindAll(ps, params.id(), params.authorId());
            try (var rs = ps.executeQuery()) {
              existing =
                  firstOrNull(rs, PostgresPostRepo::toRow).orElseThrow(RepoNotFoundException::new);
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
            bindAll(ps, params.id(), params.authorId());
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
              conn.prepareStatement("UPDATE posts SET notified_at = now() WHERE id = ?")) {
            bindAll(ps, id);
            ps.executeUpdate();
          }
        });
  }

  private static List<Long> upsertTags(Connection conn, List<String> names) throws SQLException {
    List<Long> out = new ArrayList<>(names.size());
    for (String name : names) {
      try (var ps =
          conn.prepareStatement(
              """
              INSERT INTO tags (name) VALUES (?)
              ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
              RETURNING id
              """)) {
        bindAll(ps, name);
        try (var rs = ps.executeQuery()) {
          out.add(
              firstOrNull(rs, r -> r.getLong(1))
                  .orElseThrow(() -> new IllegalStateException("upsert tag returned no row")));
        }
      }
    }
    return out;
  }

  private static void linkPostTags(Connection conn, long postId, List<Long> tagIds)
      throws SQLException {
    for (long tid : tagIds) {
      try (var ps =
          conn.prepareStatement(
              "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
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
