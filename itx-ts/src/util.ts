import { Post, PostDto } from "./types.js";

export function env(name: string): string {
  return process.env[name] ?? "";
}

export function parsePositiveInt(
  raw: string | undefined,
  fallback: number,
): number {
  if (!raw) {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
    value,
  );
}

export function formatInstant(value: Date): string {
  const out = value.toISOString();
  return out.endsWith(".000Z") ? out.replace(".000Z", "Z") : out;
}

export function toPostDto(post: Post): PostDto {
  return {
    id: post.id,
    authorId: post.authorId,
    title: post.title,
    body: post.body,
    tags: post.tags ?? [],
    createdAt: formatInstant(post.createdAt),
  };
}

export function toDate(value: unknown): Date {
  if (value instanceof Date) {
    return value;
  }
  return new Date(String(value));
}
