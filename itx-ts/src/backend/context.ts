import { NextFunction, Request, Response } from "express";
import { randomUUID } from "node:crypto";
import { isUuid } from "../util.js";

export type ItxContext = {
  requestId: string;
  userId?: string;
  userEmail: string;
};

export type ItxRequest = Request & {
  itx: ItxContext;
};

function getHeader(req: Request, name: string): string {
  const value = req.header(name);
  return value ?? "";
}

export function extractContext(
  req: Request,
  res: Response,
  next: NextFunction,
): void {
  const requestId = getHeader(req, "X-Itx-Request-Id");
  if (requestId && !isUuid(requestId)) {
    res.status(400).json({ error: { message: "invalid X-Itx-Request-Id" } });
    return;
  }

  const userId = getHeader(req, "X-Itx-User-Id");
  if (userId && !isUuid(userId)) {
    res.status(400).json({ error: { message: "invalid X-Itx-User-Id" } });
    return;
  }

  (req as ItxRequest).itx = {
    requestId: requestId || randomUUID(),
    userId: userId || undefined,
    userEmail: getHeader(req, "X-Itx-User-Email"),
  };
  next();
}

export function requireUser(
  req: Request,
  res: Response,
  next: NextFunction,
): void {
  if (!(req as ItxRequest).itx.userId) {
    res.status(401).json({ error: { message: "Unauthorized" } });
    return;
  }
  next();
}
