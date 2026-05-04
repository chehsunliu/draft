import * as fs from "node:fs/promises";
import * as path from "node:path";
import { Pool, type PoolConfig } from "pg";

import type { DbSeeder } from "./base.js";

interface ColumnInfo {
  name: string;
  type: string;
}

interface TableInfo {
  name: string;
  columns: ColumnInfo[];
  hasIdColumn: boolean;
}

export class PostgresDbSeeder implements DbSeeder {
  private pool: Pool;
  private excluded: Set<string>;
  private tables: TableInfo[] = [];

  constructor(config: PoolConfig, excludedTables: string[] = []) {
    this.pool = new Pool(config);
    this.excluded = new Set(excludedTables);
  }

  async enter(): Promise<void> {
    if (this.tables.length > 0) return;
    const tables = await this.pool.query<{ table_name: string }>(
      "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
    );
    for (const { table_name } of tables.rows) {
      if (this.excluded.has(table_name)) continue;
      const cols = await this.pool.query<ColumnInfo>(
        "SELECT column_name AS name, data_type AS type FROM information_schema.columns " +
          "WHERE table_schema = 'public' AND table_name = $1 ORDER BY ordinal_position",
        [table_name],
      );
      this.tables.push({
        name: table_name,
        columns: cols.rows,
        hasIdColumn: cols.rows.some((c) => c.name === "id"),
      });
    }
  }

  async close(): Promise<void> {
    await this.pool.end();
  }

  async resetTables(): Promise<void> {
    if (this.tables.length === 0) return;
    const list = this.tables.map((t) => `"${t.name}"`).join(",");
    await this.pool.query(`TRUNCATE TABLE ${list} RESTART IDENTITY CASCADE`);
  }

  async writeData(folder: string): Promise<void> {
    const client = await this.pool.connect();
    try {
      await client.query("SET session_replication_role = 'replica'");
      for (const table of this.tables) {
        const file = path.join(folder, "postgres", `${table.name}.json`);
        let raw: string;
        try {
          raw = await fs.readFile(file, "utf-8");
        } catch (e) {
          if ((e as NodeJS.ErrnoException).code === "ENOENT") continue;
          throw e;
        }
        const rows = JSON.parse(raw) as Record<string, unknown>[];
        if (rows.length === 0) continue;

        const cols = Object.keys(rows[0]);
        const colList = cols.map((c) => `"${c}"`).join(",");
        for (const row of rows) {
          const placeholders = cols.map((_, i) => `$${i + 1}`).join(",");
          const values = cols.map((c) => row[c]);
          await client.query(`INSERT INTO "${table.name}" (${colList}) VALUES (${placeholders})`, values);
        }
      }
      // Bump IDENTITY sequences past explicit-id inserts.
      for (const table of this.tables) {
        if (!table.hasIdColumn) continue;
        const seq = await client.query<{ seq: string | null }>(`SELECT pg_get_serial_sequence($1, 'id') AS seq`, [
          table.name,
        ]);
        const seqName = seq.rows[0]?.seq;
        if (!seqName) continue;
        await client.query(`SELECT setval($1, COALESCE((SELECT MAX(id) FROM "${table.name}"), 1), true)`, [seqName]);
      }
    } finally {
      client.release();
    }
  }
}
