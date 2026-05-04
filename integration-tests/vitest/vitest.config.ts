import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globalSetup: ["./src/global-setup.ts"],
    // Run all tests in a single fork so module-level singletons (daemon, db pool)
    // are shared. Mirrors pytest's session scope.
    pool: "forks",
    poolOptions: { forks: { singleFork: true } },
    testTimeout: 30000,
    hookTimeout: 30000,
  },
});
