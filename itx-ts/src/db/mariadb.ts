import mysql from "mysql2/promise";
import {
  NotFoundError,
  Post,
  PostRepo,
  RepoFactory,
  SubscriptionRepo,
  User,
  UserRepo,
} from "../types.js";
import { env, toDate } from "../util.js";

type Queryable = Pick<mysql.Pool | mysql.PoolConnection, "query">;

function asRows(result: unknown): mysql.RowDataPacket[] {
  return result as mysql.RowDataPacket[];
}

function rowToPost(row: mysql.RowDataPacket): Post {
  return {
    id: Number(row.id),
    authorId: String(row.author_id),
    title: String(row.title),
    body: String(row.body),
    tags: [],
    createdAt: toDate(row.created_at),
    notifiedAt: row.notified_at == null ? null : toDate(row.notified_at),
  };
}

async function fetchTagsFor(
  db: Queryable,
  ids: number[],
): Promise<Map<number, string[]>> {
  const out = new Map<number, string[]>();
  if (ids.length === 0) {
    return out;
  }
  const placeholders = ids.map(() => "?").join(",");
  const [rowsRaw] = await db.query(
    `SELECT pt.post_id, t.name
     FROM post_tags pt JOIN tags t ON pt.tag_id = t.id
     WHERE pt.post_id IN (${placeholders})
     ORDER BY t.name`,
    ids,
  );
  for (const row of asRows(rowsRaw)) {
    const postId = Number(row.post_id);
    out.set(postId, [...(out.get(postId) ?? []), String(row.name)]);
  }
  return out;
}

async function upsertTagsTx(
  conn: mysql.PoolConnection,
  names: string[],
): Promise<number[]> {
  const ids: number[] = [];
  for (const name of names) {
    await conn.query("INSERT IGNORE INTO tags (name) VALUES (?)", [name]);
    const [rowsRaw] = await conn.query("SELECT id FROM tags WHERE name = ?", [
      name,
    ]);
    ids.push(Number(asRows(rowsRaw)[0].id));
  }
  return ids;
}

async function linkPostTagsTx(
  conn: mysql.PoolConnection,
  postId: number,
  tagIds: number[],
): Promise<void> {
  for (const tagId of tagIds) {
    await conn.query(
      "INSERT IGNORE INTO post_tags (post_id, tag_id) VALUES (?, ?)",
      [postId, tagId],
    );
  }
}

class MariaDbPostRepo implements PostRepo {
  constructor(private readonly pool: mysql.Pool) {}

  async list(params: {
    authorId?: string;
    limit: number;
    offset: number;
  }): Promise<Post[]> {
    const limit = params.limit || 50;
    const offset = params.offset || 0;
    const [rowsRaw] = params.authorId
      ? await this.pool.query(
          `SELECT id, author_id, title, body, created_at, notified_at
           FROM posts WHERE author_id = ?
           ORDER BY id DESC LIMIT ? OFFSET ?`,
          [params.authorId, limit, offset],
        )
      : await this.pool.query(
          `SELECT id, author_id, title, body, created_at, notified_at
           FROM posts ORDER BY id DESC LIMIT ? OFFSET ?`,
          [limit, offset],
        );
    const posts = asRows(rowsRaw).map(rowToPost);
    const tags = await fetchTagsFor(
      this.pool,
      posts.map((p) => p.id),
    );
    return posts.map((p) => ({ ...p, tags: tags.get(p.id) ?? [] }));
  }

  async get(id: number): Promise<Post> {
    const [rowsRaw] = await this.pool.query(
      "SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = ?",
      [id],
    );
    const rows = asRows(rowsRaw);
    if (rows.length === 0) {
      throw new NotFoundError();
    }
    const post = rowToPost(rows[0]);
    const tags = await fetchTagsFor(this.pool, [post.id]);
    return { ...post, tags: tags.get(post.id) ?? [] };
  }

  async create(params: {
    authorId: string;
    title: string;
    body: string;
    tags: string[];
  }): Promise<Post> {
    const conn = await this.pool.getConnection();
    try {
      await conn.beginTransaction();
      const [resultRaw] = await conn.query(
        "INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?)",
        [params.authorId, params.title, params.body],
      );
      const id = Number((resultRaw as mysql.ResultSetHeader).insertId);
      const [createdRowsRaw] = await conn.query(
        "SELECT created_at FROM posts WHERE id = ?",
        [id],
      );
      const createdAt = toDate(asRows(createdRowsRaw)[0].created_at);
      await linkPostTagsTx(conn, id, await upsertTagsTx(conn, params.tags));
      await conn.commit();
      return {
        id,
        authorId: params.authorId,
        title: params.title,
        body: params.body,
        tags: [...params.tags],
        createdAt,
        notifiedAt: null,
      };
    } catch (err) {
      await conn.rollback();
      throw err;
    } finally {
      conn.release();
    }
  }

  async update(params: {
    id: number;
    authorId: string;
    title?: string;
    body?: string;
    tags?: string[];
  }): Promise<Post> {
    const conn = await this.pool.getConnection();
    try {
      await conn.beginTransaction();
      const [rowsRaw] = await conn.query(
        `SELECT id, author_id, title, body, created_at, notified_at
         FROM posts WHERE id = ? AND author_id = ? FOR UPDATE`,
        [params.id, params.authorId],
      );
      const rows = asRows(rowsRaw);
      if (rows.length === 0) {
        throw new NotFoundError();
      }
      const post = rowToPost(rows[0]);
      const title = params.title ?? post.title;
      const body = params.body ?? post.body;
      await conn.query("UPDATE posts SET title = ?, body = ? WHERE id = ?", [
        title,
        body,
        post.id,
      ]);
      let tags: string[];
      if (params.tags !== undefined) {
        await conn.query("DELETE FROM post_tags WHERE post_id = ?", [post.id]);
        await linkPostTagsTx(
          conn,
          post.id,
          await upsertTagsTx(conn, params.tags),
        );
        tags = [...params.tags];
      } else {
        tags = (await fetchTagsFor(conn, [post.id])).get(post.id) ?? [];
      }
      await conn.commit();
      return { ...post, title, body, tags };
    } catch (err) {
      await conn.rollback();
      throw err;
    } finally {
      conn.release();
    }
  }

  async delete(params: { id: number; authorId: string }): Promise<void> {
    const [resultRaw] = await this.pool.query(
      "DELETE FROM posts WHERE id = ? AND author_id = ?",
      [params.id, params.authorId],
    );
    if ((resultRaw as mysql.ResultSetHeader).affectedRows === 0) {
      throw new NotFoundError();
    }
  }

  async markNotified(id: number): Promise<void> {
    await this.pool.query("UPDATE posts SET notified_at = NOW() WHERE id = ?", [
      id,
    ]);
  }
}

class MariaDbUserRepo implements UserRepo {
  constructor(private readonly pool: mysql.Pool) {}

  async upsert(params: { id: string; email: string }): Promise<User> {
    await this.pool.query(
      "INSERT INTO users (id, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = id",
      [params.id, params.email],
    );
    return this.get(params.id);
  }

  async get(id: string): Promise<User> {
    const [rowsRaw] = await this.pool.query(
      "SELECT id, email FROM users WHERE id = ?",
      [id],
    );
    const rows = asRows(rowsRaw);
    if (rows.length === 0) {
      throw new NotFoundError();
    }
    return { id: String(rows[0].id), email: String(rows[0].email) };
  }
}

class MariaDbSubscriptionRepo implements SubscriptionRepo {
  constructor(private readonly pool: mysql.Pool) {}

  async subscribe(params: {
    subscriberId: string;
    authorId: string;
  }): Promise<void> {
    await this.pool.query(
      "INSERT IGNORE INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)",
      [params.subscriberId, params.authorId],
    );
  }

  async unsubscribe(params: {
    subscriberId: string;
    authorId: string;
  }): Promise<void> {
    await this.pool.query(
      "DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
      [params.subscriberId, params.authorId],
    );
  }

  async listAuthors(subscriberId: string): Promise<User[]> {
    const [rowsRaw] = await this.pool.query(
      `SELECT u.id, u.email
       FROM subscriptions s JOIN users u ON u.id = s.author_id
       WHERE s.subscriber_id = ?
       ORDER BY s.created_at DESC, u.id ASC`,
      [subscriberId],
    );
    return asRows(rowsRaw).map((row) => ({
      id: String(row.id),
      email: String(row.email),
    }));
  }

  async listSubscribers(authorId: string): Promise<User[]> {
    const [rowsRaw] = await this.pool.query(
      `SELECT u.id, u.email
       FROM subscriptions s JOIN users u ON u.id = s.subscriber_id
       WHERE s.author_id = ?
       ORDER BY s.created_at DESC, u.id ASC`,
      [authorId],
    );
    return asRows(rowsRaw).map((row) => ({
      id: String(row.id),
      email: String(row.email),
    }));
  }
}

export class MariaDbRepoFactory implements RepoFactory {
  private readonly pool: mysql.Pool;

  constructor() {
    this.pool = mysql.createPool({
      host: env("ITX_MARIADB_HOST"),
      port: Number(env("ITX_MARIADB_PORT")),
      database: env("ITX_MARIADB_DB_NAME"),
      user: env("ITX_MARIADB_USER"),
      password: env("ITX_MARIADB_PASSWORD"),
      connectionLimit: 10,
      timezone: "Z",
    });
  }

  postRepo(): PostRepo {
    return new MariaDbPostRepo(this.pool);
  }

  userRepo(): UserRepo {
    return new MariaDbUserRepo(this.pool);
  }

  subscriptionRepo(): SubscriptionRepo {
    return new MariaDbSubscriptionRepo(this.pool);
  }

  async close(): Promise<void> {
    await this.pool.end();
  }
}
