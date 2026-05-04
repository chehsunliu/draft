import { spawn, type ChildProcess } from "node:child_process";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import * as path from "node:path";

const execFileAsync = promisify(execFile);

export interface ArtifactProfile {
  cwd: string;
  buildCmd: [string, ...string[]];
  backendBinary: string;
}

export const artifactProfiles: Record<string, ArtifactProfile> = {
  rust: {
    cwd: path.resolve(import.meta.dirname, "../../../../itx-rs"),
    buildCmd: ["cargo", "build"],
    backendBinary: "target/debug/itx-backend",
  },
  golang: {
    cwd: path.resolve(import.meta.dirname, "../../../../itx-go"),
    buildCmd: ["make", "build"],
    backendBinary: "bin/itx-backend",
  },
};

export async function build(profile: ArtifactProfile): Promise<void> {
  const [cmd, ...args] = profile.buildCmd;
  await execFileAsync(cmd, args, { cwd: profile.cwd });
}

export function spawnBackend(
  profile: ArtifactProfile,
  procEnv: Record<string, string>,
  host: string,
  port: number,
): ChildProcess {
  const binary = path.join(profile.cwd, profile.backendBinary);
  return spawn(binary, ["--host", host, "--port", String(port)], {
    cwd: profile.cwd,
    env: procEnv,
    stdio: ["ignore", "inherit", "inherit"],
  });
}
