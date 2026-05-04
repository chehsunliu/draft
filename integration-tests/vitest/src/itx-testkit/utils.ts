import { exec } from "node:child_process";
import { Socket } from "node:net";
import { promisify } from "node:util";

const execAsync = promisify(exec);

export async function getHostPort(service: string, containerPort: number, composeDir: string): Promise<string> {
  for (const cmd of ["docker compose", "docker-compose"]) {
    try {
      const { stdout } = await execAsync(`${cmd} port ${service} ${containerPort}`, { cwd: composeDir });
      const line = stdout.trim();
      if (line) {
        const parts = line.split(":");
        return parts[parts.length - 1];
      }
    } catch {
      // try next form
    }
  }
  throw new Error(`failed to get host port for ${service}:${containerPort} — is docker compose running?`);
}

export async function waitForPort(host: string, port: number, timeoutMs = 5000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await tryConnect(host, port)) return;
    await new Promise((r) => setTimeout(r, 100));
  }
  throw new Error(`${host}:${port} is not available`);
}

function tryConnect(host: string, port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const sock = new Socket();
    sock.setTimeout(100);
    sock.once("connect", () => {
      sock.destroy();
      resolve(true);
    });
    sock.once("error", () => resolve(false));
    sock.once("timeout", () => {
      sock.destroy();
      resolve(false);
    });
    sock.connect(port, host);
  });
}
