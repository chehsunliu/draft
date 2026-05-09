import { RepoFactory } from "../types.js";
import { MariaDbRepoFactory } from "./mariadb.js";
import { PostgresRepoFactory } from "./postgres.js";

export function repoFactoryFromEnv(): RepoFactory {
  const provider = process.env.ITX_DB_PROVIDER || "postgres";
  switch (provider) {
    case "postgres":
      return new PostgresRepoFactory();
    case "mariadb":
      return new MariaDbRepoFactory();
    default:
      throw new Error(`unknown ITX_DB_PROVIDER: ${provider}`);
  }
}
