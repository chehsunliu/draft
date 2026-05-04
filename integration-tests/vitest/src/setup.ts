// Loaded as a vitest setupFile (see vitest.config.ts). Runs once per worker;
// because we set `singleFork: true` that's effectively once per vitest run,
// and the same process where every test file executes — so module-level
// singletons here are visible to all tests via direct imports.

import * as path from "node:path";
import { afterAll, beforeAll } from "vitest";
import type { ChildProcess } from "node:child_process";

import { artifactProfiles, build, spawnBackend, type ArtifactProfile } from "./itx-testkit/profile.js";
import { PostgresDbSeeder } from "./itx-testkit/seeder/db/postgres.js";
import { getHostPort, waitForPort } from "./itx-testkit/utils.js";

const composeDir = path.resolve(import.meta.dirname, "../../");
const excludedTables = ["flyway_schema_history"];

let daemon: ChildProcess | null = null;
let _baseUrl: string | null = null;
let _seeder: PostgresDbSeeder | null = null;

export function baseUrl(): string {
  if (!_baseUrl) throw new Error("baseUrl() called before beforeAll setup");
  return _baseUrl;
}

export function dbSeeder(): PostgresDbSeeder {
  if (!_seeder) throw new Error("dbSeeder() called before beforeAll setup");
  return _seeder;
}

beforeAll(async () => {
  const itxLang = process.env.ITX_LANG ?? "rust";
  const itxTestProfile = process.env.ITX_TEST_PROFILE ?? "aws";
  if (!(itxLang in artifactProfiles)) {
    throw new Error(`unknown ITX_LANG: ${itxLang}`);
  }
  if (itxTestProfile !== "aws") {
    throw new Error(`only the 'aws' profile is wired up so far; got ${itxTestProfile}`);
  }
  const profile: ArtifactProfile = artifactProfiles[itxLang];

  await build(profile);

  const pgHost = "127.0.0.1";
  const pgPort = await getHostPort("postgres", 5432, composeDir);
  const sqsPort = await getHostPort("sqs", 9324, composeDir);
  const sqsEndpoint = `http://127.0.0.1:${sqsPort}`;
  const sqsQueueUrl = (name: string) => `${sqsEndpoint}/000000000000/${name}`;

  const serverHost = "127.0.0.1";
  const serverPort = 18082;
  const env: Record<string, string> = {
    ITX_DB_PROVIDER: "postgres",
    ITX_POSTGRES_HOST: pgHost,
    ITX_POSTGRES_PORT: pgPort,
    ITX_POSTGRES_DB_NAME: "itx-db",
    ITX_POSTGRES_USER: "itx-admin",
    ITX_POSTGRES_PASSWORD: "itx-admin",
    ITX_QUEUE_PROVIDER: "sqs",
    AWS_REGION: "us-east-1",
    AWS_ACCESS_KEY_ID: "x",
    AWS_SECRET_ACCESS_KEY: "x",
    ITX_SQS_LOCAL_ENDPOINT_URL: sqsEndpoint,
    ITX_SQS_CONTROL_STANDARD_QUEUE_URL: sqsQueueUrl("test-itx-control-standard"),
    ITX_SQS_CONTROL_PREMIUM_QUEUE_URL: sqsQueueUrl("test-itx-control-premium"),
    ITX_SQS_COMPUTE_STANDARD_QUEUE_URL: sqsQueueUrl("test-itx-compute-standard"),
    ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL: sqsQueueUrl("test-itx-compute-premium"),
  };

  daemon = spawnBackend(profile, env, serverHost, serverPort);
  try {
    await waitForPort(serverHost, serverPort, 10000);
  } catch (e) {
    daemon.kill("SIGKILL");
    throw e;
  }
  _baseUrl = `http://${serverHost}:${serverPort}`;

  _seeder = new PostgresDbSeeder(
    {
      host: pgHost,
      port: Number(pgPort),
      database: "itx-db",
      user: "itx-admin",
      password: "itx-admin",
    },
    excludedTables,
  );
  await _seeder.enter();
});

afterAll(async () => {
  if (_seeder) {
    await _seeder.close();
    _seeder = null;
  }
  if (daemon) {
    daemon.kill("SIGINT");
    await new Promise<void>((resolve) => {
      const timer = setTimeout(() => {
        daemon?.kill("SIGKILL");
        resolve();
      }, 5000);
      daemon!.once("exit", () => {
        clearTimeout(timer);
        resolve();
      });
    });
    daemon = null;
  }
});
