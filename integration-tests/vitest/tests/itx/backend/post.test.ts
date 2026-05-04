import * as path from "node:path";

import { beforeAll, beforeEach, describe, expect, test } from "vitest";

import { baseUrl, dbSeeder } from "../../../src/setup.ts";

const userId = "11111111-1111-1111-1111-111111111111";
const fixtures = path.resolve(import.meta.dirname, "post");

let seeder: Awaited<ReturnType<typeof dbSeeder>>;

beforeAll(async () => {
  seeder = await dbSeeder();
});

beforeEach(async () => {
  await seeder.resetTables();
  await seeder.writeData(path.join(fixtures, "20260503_baseline"));
});

describe("list posts", () => {
  test("returns the caller's posts in DESC id order", async () => {
    await seeder.writeData(path.join(fixtures, "20260502_simple"));

    const r = await fetch(`${baseUrl()}/api/v1/posts`, {
      headers: { "X-Itx-User-Id": userId },
    });
    expect(r.status).toBe(200);
    const body = await r.json();
    expect(body).toEqual({
      data: {
        items: [
          {
            id: 3,
            authorId: userId,
            title: "Weekend recap",
            body: "Coffee and code.",
            tags: ["life"],
            createdAt: "2026-03-17T12:00:00Z",
          },
          {
            id: 2,
            authorId: userId,
            title: "Rust adventures",
            body: "Talking about traits.",
            tags: ["design", "rust"],
            createdAt: "2026-03-16T11:00:00Z",
          },
          {
            id: 1,
            authorId: userId,
            title: "Yeah",
            body: "Blah blah blah...",
            tags: [],
            createdAt: "2026-03-15T10:00:00Z",
          },
        ],
      },
    });
  });
});
