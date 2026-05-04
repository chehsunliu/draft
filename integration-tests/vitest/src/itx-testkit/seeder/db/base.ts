export interface DbSeeder {
  /** Open broker connections + reflect schema. Idempotent. */
  enter(): Promise<void>;
  /** Release connections. */
  close(): Promise<void>;
  /** Truncate every reflected table so each test starts clean. */
  resetTables(): Promise<void>;
  /** Read JSON files under `<folder>/<provider>/<table>.json` and insert rows. */
  writeData(folder: string): Promise<void>;
}
