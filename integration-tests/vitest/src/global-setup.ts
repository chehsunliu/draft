import * as path from "node:path";
import type { ChildProcess } from "node:child_process";

import { artifactProfiles, build, spawnBackend } from "./itx-testkit/profile.ts";
import { getHostPort, waitForPort } from "./itx-testkit/utils.ts";

const composeDir = path.resolve(import.meta.dirname, "../../");

export default async function globalSetup() {
  const itxLang = process.env.ITX_LANG ?? "rust";
  const itxTestProfile = process.env.ITX_TEST_PROFILE ?? "aws";
  if (!(itxLang in artifactProfiles)) {
    throw new Error(`unknown ITX_LANG: ${itxLang}`);
  }
  if (itxTestProfile !== "aws") {
    throw new Error(`only the 'aws' profile is wired up so far; got ${itxTestProfile}`);
  }

  const profile = artifactProfiles[itxLang];

  // Build artifact (cargo build / make build).
  await build(profile);

  // Resolve docker compose ports.
  const pgPort = await getHostPort("postgres", 5432, composeDir);
  const pgHost = "127.0.0.1";
  const sqsPort = await getHostPort("sqs", 9324, composeDir);
  const sqsEndpoint = `http://127.0.0.1:${sqsPort}`;
  const sqsQueueUrl = (name: string) => `${sqsEndpoint}/000000000000/${name}`;

  // Spawn the backend.
  const serverHost = "127.0.0.1";
  const serverPort = 18082; // distinct from pytest's 18080/18081
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
  const proc: ChildProcess = spawnBackend(profile, env, serverHost, serverPort);

  try {
    await waitForPort(serverHost, serverPort, 10000);
  } catch (e) {
    proc.kill("SIGKILL");
    throw e;
  }

  // Hand off to the worker process.
  process.env.ITX_VITEST_BASE_URL = `http://${serverHost}:${serverPort}`;
  process.env.ITX_VITEST_PG_HOST = pgHost;
  process.env.ITX_VITEST_PG_PORT = pgPort;

  return async () => {
    proc.kill("SIGINT");
    await new Promise<void>((resolve) => {
      const timer = setTimeout(() => {
        proc.kill("SIGKILL");
        resolve();
      }, 5000);
      proc.once("exit", () => {
        clearTimeout(timer);
        resolve();
      });
    });
  };
}
