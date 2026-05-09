import pg from "pg";
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

type Queryable = Pick<pg.Pool | pg.PoolClient, "query">;

function rowToPost(row: Record<string, unknown>): Post {
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
  const placeholders = ids.map((_, i) => `$${i + 1}`).join(",");
  const result = await db.query(
    `SELECT pt.post_id, t.name
     FROM post_tags pt JOIN tags t ON pt.tag_id = t.id
     WHERE pt.post_id IN (${placeholders})
     ORDER BY t.name`,
    ids,
  );
  for (const row of result.rows) {
    const postId = Number(row.post_id);
    out.set(postId, [...(out.get(postId) ?? []), String(row.name)]);
  }
  return out;
}

async function upsertTagsTx(
  client: pg.PoolClient,
  names: string[],
): Promise<number[]> {
  const ids: number[] = [];
  for (const name of names) {
    const result = await client.query(
      `INSERT INTO tags (name) VALUES ($1)
       ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
       RETURNING id`,
      [name],
    );
    ids.push(Number(result.rows[0].id));
  }
  return ids;
}

async function linkPostTagsTx(
  client: pg.PoolClient,
  postId: number,
  tagIds: number[],
): Promise<void> {
  for (const tagId of tagIds) {
    await client.query(
      "INSERT INTO post_tags (post_id, tag_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
      [postId, tagId],
    );
  }
}

class PgPostRepo implements PostRepo {
  constructor(private readonly pool: pg.Pool) {}

  async list(params: {
    authorId?: string;
    limit: number;
    offset: number;
  }): Promise<Post[]> {
    const limit = params.limit || 50;
    const offset = params.offset || 0;
    const result = params.authorId
      ? await this.pool.query(
          `SELECT id, author_id, title, body, created_at, notified_at
           FROM posts WHERE author_id = $1
           ORDER BY id DESC LIMIT $2 OFFSET $3`,
          [params.authorId, limit, offset],
        )
      : await this.pool.query(
          `SELECT id, author_id, title, body, created_at, notified_at
           FROM posts ORDER BY id DESC LIMIT $1 OFFSET $2`,
          [limit, offset],
        );
    const posts = result.rows.map(rowToPost);
    const tags = await fetchTagsFor(
      this.pool,
      posts.map((p) => p.id),
    );
    return posts.map((p) => ({ ...p, tags: tags.get(p.id) ?? [] }));
  }

  async get(id: number): Promise<Post> {
    const result = await this.pool.query(
      "SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = $1",
      [id],
    );
    if (result.rowCount === 0) {
      throw new NotFoundError();
    }
    const post = rowToPost(result.rows[0]);
    const tags = await fetchTagsFor(this.pool, [post.id]);
    return { ...post, tags: tags.get(post.id) ?? [] };
  }

  async create(params: {
    authorId: string;
    title: string;
    body: string;
    tags: string[];
  }): Promise<Post> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");
      const result = await client.query(
        "INSERT INTO posts (author_id, title, body) VALUES ($1, $2, $3) RETURNING id, created_at",
        [params.authorId, params.title, params.body],
      );
      const id = Number(result.rows[0].id);
      const createdAt = toDate(result.rows[0].created_at);
      await linkPostTagsTx(client, id, await upsertTagsTx(client, params.tags));
      await client.query("COMMIT");
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
      await client.query("ROLLBACK");
      throw err;
    } finally {
      client.release();
    }
  }

  async update(params: {
    id: number;
    authorId: string;
    title?: string;
    body?: string;
    tags?: string[];
  }): Promise<Post> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");
      const result = await client.query(
        `SELECT id, author_id, title, body, created_at, notified_at
         FROM posts WHERE id = $1 AND author_id = $2 FOR UPDATE`,
        [params.id, params.authorId],
      );
      if (result.rowCount === 0) {
        throw new NotFoundError();
      }
      const post = rowToPost(result.rows[0]);
      const title = params.title ?? post.title;
      const body = params.body ?? post.body;
      await client.query(
        "UPDATE posts SET title = $1, body = $2 WHERE id = $3",
        [title, body, post.id],
      );
      let tags: string[];
      if (params.tags !== undefined) {
        await client.query("DELETE FROM post_tags WHERE post_id = $1", [
          post.id,
        ]);
        await linkPostTagsTx(
          client,
          post.id,
          await upsertTagsTx(client, params.tags),
        );
        tags = [...params.tags];
      } else {
        tags = (await fetchTagsFor(client, [post.id])).get(post.id) ?? [];
      }
      await client.query("COMMIT");
      return { ...post, title, body, tags };
    } catch (err) {
      await client.query("ROLLBACK");
      throw err;
    } finally {
      client.release();
    }
  }

  async delete(params: { id: number; authorId: string }): Promise<void> {
    const result = await this.pool.query(
      "DELETE FROM posts WHERE id = $1 AND author_id = $2",
      [params.id, params.authorId],
    );
    if (result.rowCount === 0) {
      throw new NotFoundError();
    }
  }

  async markNotified(id: number): Promise<void> {
    await this.pool.query(
      "UPDATE posts SET notified_at = now() WHERE id = $1",
      [id],
    );
  }
}

class PgUserRepo implements UserRepo {
  constructor(private readonly pool: pg.Pool) {}

  async upsert(params: { id: string; email: string }): Promise<User> {
    const result = await this.pool.query(
      `INSERT INTO users (id, email) VALUES ($1, $2)
       ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id
       RETURNING id, email`,
      [params.id, params.email],
    );
    return {
      id: String(result.rows[0].id),
      email: String(result.rows[0].email),
    };
  }

  async get(id: string): Promise<User> {
    const result = await this.pool.query(
      "SELECT id, email FROM users WHERE id = $1",
      [id],
    );
    if (result.rowCount === 0) {
      throw new NotFoundError();
    }
    return {
      id: String(result.rows[0].id),
      email: String(result.rows[0].email),
    };
  }
}

class PgSubscriptionRepo implements SubscriptionRepo {
  constructor(private readonly pool: pg.Pool) {}

  async subscribe(params: {
    subscriberId: string;
    authorId: string;
  }): Promise<void> {
    await this.pool.query(
      `INSERT INTO subscriptions (subscriber_id, author_id) VALUES ($1, $2)
       ON CONFLICT (subscriber_id, author_id) DO NOTHING`,
      [params.subscriberId, params.authorId],
    );
  }

  async unsubscribe(params: {
    subscriberId: string;
    authorId: string;
  }): Promise<void> {
    await this.pool.query(
      "DELETE FROM subscriptions WHERE subscriber_id = $1 AND author_id = $2",
      [params.subscriberId, params.authorId],
    );
  }

  async listAuthors(subscriberId: string): Promise<User[]> {
    const result = await this.pool.query(
      `SELECT u.id, u.email
       FROM subscriptions s JOIN users u ON u.id = s.author_id
       WHERE s.subscriber_id = $1
       ORDER BY s.created_at DESC, u.id ASC`,
      [subscriberId],
    );
    return result.rows.map((row) => ({
      id: String(row.id),
      email: String(row.email),
    }));
  }

  async listSubscribers(authorId: string): Promise<User[]> {
    const result = await this.pool.query(
      `SELECT u.id, u.email
       FROM subscriptions s JOIN users u ON u.id = s.subscriber_id
       WHERE s.author_id = $1
       ORDER BY s.created_at DESC, u.id ASC`,
      [authorId],
    );
    return result.rows.map((row) => ({
      id: String(row.id),
      email: String(row.email),
    }));
  }
}

export class PostgresRepoFactory implements RepoFactory {
  private readonly pool: pg.Pool;

  constructor() {
    this.pool = new pg.Pool({
      host: env("ITX_POSTGRES_HOST"),
      port: Number(env("ITX_POSTGRES_PORT")),
      database: env("ITX_POSTGRES_DB_NAME"),
      user: env("ITX_POSTGRES_USER"),
      password: env("ITX_POSTGRES_PASSWORD"),
      max: 10,
    });
  }

  postRepo(): PostRepo {
    return new PgPostRepo(this.pool);
  }

  userRepo(): UserRepo {
    return new PgUserRepo(this.pool);
  }

  subscriptionRepo(): SubscriptionRepo {
    return new PgSubscriptionRepo(this.pool);
  }

  async close(): Promise<void> {
    await this.pool.end();
  }
}
