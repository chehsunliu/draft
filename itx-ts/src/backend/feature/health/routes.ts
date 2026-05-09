import { Router } from "express";

export function createRouter(): Router {
  const router = Router();
  router.get("/", (_req, res) => res.json({ data: { status: "ok" } }));
  router.all("/", (_req, res) =>
    res.status(405).json({ error: { message: "Method Not Allowed" } }),
  );
  return router;
}
