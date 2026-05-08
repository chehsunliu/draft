package io.github.chehsunliu.itx.impl.repo.postgres;

import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
class PostgresPostRepo implements PostRepo {

  private record Row(long id, UUID authorId, String title, String body, OffsetDateTime createdAt) {}

  private static final RowMapper<Row> ROW_MAPPER =
      (rs, i) -> {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Row(
            rs.getLong("id"),
            (UUID) rs.getObject("author_id"),
            rs.getString("title"),
            rs.getString("body"),
            ts.toInstant().atOffset(ZoneOffset.UTC));
      };

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  @Override
  public List<Post> list(ListParams params) {
    int limit = params.getLimit() == 0 ? 50 : params.getLimit();
    int offset = params.getOffset();

    List<Row> rows;
    if (params.getAuthorId() != null) {
      rows =
          jdbc.query(
              "SELECT id, author_id, title, body, created_at "
                  + "FROM posts WHERE author_id = ? "
                  + "ORDER BY id DESC LIMIT ? OFFSET ?",
              ROW_MAPPER,
              params.getAuthorId(),
              limit,
              offset);
    } else {
      rows =
          jdbc.query(
              "SELECT id, author_id, title, body, created_at "
                  + "FROM posts ORDER BY id DESC LIMIT ? OFFSET ?",
              ROW_MAPPER,
              limit,
              offset);
    }

    List<Long> ids = new ArrayList<>(rows.size());
    for (Row r : rows) ids.add(r.id);
    Map<Long, List<String>> tagMap = fetchTagsFor(ids);

    List<Post> out = new ArrayList<>(rows.size());
    for (Row r : rows) {
      out.add(toPost(r, tagMap.getOrDefault(r.id, List.of())));
    }
    return out;
  }

  @Override
  public Post get(GetParams params) {
    List<Row> rows =
        jdbc.query(
            "SELECT id, author_id, title, body, created_at FROM posts WHERE id = ?",
            ROW_MAPPER,
            params.getId());
    if (rows.isEmpty()) {
      throw new RepoNotFoundException();
    }
    Row r = rows.getFirst();
    Map<Long, List<String>> tagMap = fetchTagsFor(List.of(r.id));
    return toPost(r, tagMap.getOrDefault(r.id, List.of()));
  }

  @Override
  public Post create(CreateParams params) {
    return tx.execute(
        status -> {
          Row r =
              jdbc.queryForObject(
                  "INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?) "
                      + "RETURNING id, author_id, title, body, created_at",
                  ROW_MAPPER,
                  params.getAuthorId(),
                  params.getTitle(),
                  params.getBody());
          List<Long> tagIds = upsertTags(params.getTags());
          linkPostTags(r.id, tagIds);
          return toPost(r, params.getTags());
        });
  }

  @Override
  public Post update(UpdateParams params) {
    return tx.execute(
        status -> {
          List<Row> existing =
              jdbc.query(
                  "SELECT id, author_id, title, body, created_at "
                      + "FROM posts WHERE id = ? AND author_id = ? FOR UPDATE",
                  ROW_MAPPER,
                  params.getId(),
                  params.getAuthorId());
          if (existing.isEmpty()) {
            throw new RepoNotFoundException();
          }
          Row r = existing.getFirst();
          String title = params.getTitle() != null ? params.getTitle() : r.title;
          String body = params.getBody() != null ? params.getBody() : r.body;

          jdbc.update(
              "UPDATE posts SET title = ?, body = ? WHERE id = ?", title, body, params.getId());

          List<String> tags;
          if (params.getTags() != null) {
            jdbc.update("DELETE FROM post_tags WHERE post_id = ?", params.getId());
            List<Long> tagIds = upsertTags(params.getTags());
            linkPostTags(params.getId(), tagIds);
            tags = params.getTags();
          } else {
            tags =
                jdbc.query(
                    "SELECT t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id "
                        + "WHERE pt.post_id = ? ORDER BY t.name",
                    (rs, i) -> rs.getString(1),
                    params.getId());
          }

          return Post.builder()
              .id(params.getId())
              .authorId(params.getAuthorId())
              .title(title)
              .body(body)
              .tags(tags)
              .createdAt(r.createdAt)
              .build();
        });
  }

  @Override
  public void delete(DeleteParams params) {
    int rows =
        jdbc.update(
            "DELETE FROM posts WHERE id = ? AND author_id = ?",
            params.getId(),
            params.getAuthorId());
    if (rows == 0) {
      throw new RepoNotFoundException();
    }
  }

  private Post toPost(Row r, List<String> tags) {
    return Post.builder()
        .id(r.id)
        .authorId(r.authorId)
        .title(r.title)
        .body(r.body)
        .tags(tags)
        .createdAt(r.createdAt)
        .build();
  }

  private List<Long> upsertTags(List<String> names) {
    List<Long> ids = new ArrayList<>(names.size());
    for (String name : names) {
      Long id =
          jdbc.queryForObject(
              "INSERT INTO tags (name) VALUES (?) "
                  + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name "
                  + "RETURNING id",
              Long.class,
              name);
      ids.add(id);
    }
    return ids;
  }

  private void linkPostTags(long postId, List<Long> tagIds) {
    for (Long tid : tagIds) {
      jdbc.update(
          "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
          postId,
          tid);
    }
  }

  private Map<Long, List<String>> fetchTagsFor(List<Long> postIds) {
    if (postIds.isEmpty()) return Map.of();
    String placeholders = String.join(",", java.util.Collections.nCopies(postIds.size(), "?"));
    String sql =
        "SELECT pt.post_id, t.name "
            + "FROM post_tags pt JOIN tags t ON pt.tag_id = t.id "
            + "WHERE pt.post_id IN ("
            + placeholders
            + ") ORDER BY t.name";
    Map<Long, List<String>> out = new HashMap<>();
    jdbc.query(
        sql,
        rs -> {
          long pid = rs.getLong(1);
          out.computeIfAbsent(pid, k -> new ArrayList<>()).add(rs.getString(2));
        },
        postIds.toArray());
    return out;
  }
}
