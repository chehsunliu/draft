import { NextFunction, Request, Response } from "express";
import { z } from "zod";

export type AsyncHandler = (
  req: Request,
  res: Response,
  next: NextFunction,
) => Promise<void>;

export function asyncHandler(handler: AsyncHandler): AsyncHandler {
  return async (req, res, next) => {
    try {
      await handler(req, res, next);
    } catch (err) {
      next(err);
    }
  };
}

export function pathParam(req: Request, name: string): string {
  const value = req.params[name];
  return Array.isArray(value) ? value[0] : (value ?? "");
}

export function parsePostId(req: Request, res: Response): number | null {
  const id = Number.parseInt(pathParam(req, "id"), 10);
  if (!Number.isFinite(id)) {
    res.status(400).json({ error: { message: "invalid post id" } });
    return null;
  }
  return id;
}

export function notFound(res: Response): void {
  res.status(404).json({ error: { message: "not found" } });
}

export function parseRequest<T>(
  schema: z.ZodType<T>,
  value: unknown,
  res: Response,
): T | null {
  const result = schema.safeParse(value);
  if (!result.success) {
    res.status(400).json({
      error: {
        message: result.error.issues[0]?.message ?? "invalid request",
      },
    });
    return null;
  }
  return result.data;
}
