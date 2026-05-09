import http from "node:http";
import { createApp } from "../backend/app.js";
import { appStateFromEnv } from "../backend/state.js";

function argValue(name: string, fallback: string): string {
  const index = process.argv.indexOf(name);
  if (index >= 0 && process.argv[index + 1]) {
    return process.argv[index + 1];
  }
  const prefix = `${name}=`;
  const inline = process.argv.find((arg) => arg.startsWith(prefix));
  return inline ? inline.slice(prefix.length) : fallback;
}

const host = argValue("--host", "127.0.0.1");
const port = Number(argValue("--port", "8080"));
const state = await appStateFromEnv();
const server = http.createServer(createApp(state));

async function shutdown(): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    server.close((err) => (err ? reject(err) : resolve()));
  });
  await state.close();
}

process.once("SIGINT", () => void shutdown().then(() => process.exit(0)));
process.once("SIGTERM", () => void shutdown().then(() => process.exit(0)));

await new Promise<void>((resolve) => {
  server.listen(port, host, () => resolve());
});
