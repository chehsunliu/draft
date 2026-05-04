// Module-level singletons. Constructed lazily on first use inside the test
// worker (vitest runs all tests in a single fork).
//
// `globalSetup` (separate process) builds the artifact, spawns the backend
// daemon, and exposes its base URL + DB host/port via env vars; this module
// reads those env vars and stands up the seeder + connection pool.

import { PostgresDbSeeder } from "./itx-testkit/seeder/db/postgres.ts";

const excludedTables = ["flyway_schema_history"];

let _seeder: PostgresDbSeeder | null = null;

export function baseUrl(): string {
  const url = process.env.ITX_VITEST_BASE_URL;
  if (!url) throw new Error("ITX_VITEST_BASE_URL not set — global-setup did not run");
  return url;
}

export async function dbSeeder(): Promise<PostgresDbSeeder> {
  if (_seeder) return _seeder;
  const host = process.env.ITX_VITEST_PG_HOST;
  const port = process.env.ITX_VITEST_PG_PORT;
  if (!host || !port) throw new Error("ITX_VITEST_PG_HOST/PORT not set — global-setup did not run");
  const seeder = new PostgresDbSeeder(
    {
      host,
      port: Number(port),
      database: "itx-db",
      user: "itx-admin",
      password: "itx-admin",
    },
    excludedTables,
  );
  await seeder.enter();
  _seeder = seeder;
  return seeder;
}
