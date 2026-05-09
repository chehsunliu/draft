import express, { NextFunction, Request, Response } from "express";
import { extractContext } from "./context.js";
import * as health from "./feature/health/routes.js";
import * as post from "./feature/post/routes.js";
import * as subscription from "./feature/subscription/routes.js";
import * as user from "./feature/user/routes.js";
import { AppState } from "./state.js";
import { NotFoundError } from "../types.js";
import { ZodError } from "zod";

export function createApp(state: AppState): express.Express {
  const app = express();
  app.use(express.json());
  app.use(extractContext);

  app.use("/api/v1/health", health.createRouter());
  app.use("/api/v1/posts", post.createRouter(state));
  app.use("/api/v1/users", user.createRouter(state));
  app.use("/api/v1/subscriptions", subscription.createRouter(state));

  app.use((_req, res) =>
    res.status(404).json({ error: { message: "Not Found" } }),
  );
  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (res.headersSent) {
      return;
    }
    if (err instanceof ZodError) {
      res.status(400).json({
        error: {
          message: err.issues[0]?.message ?? "invalid request",
        },
      });
      return;
    }
    if (err instanceof NotFoundError) {
      res.status(404).json({ error: { message: err.message } });
      return;
    }
    console.error(err);
    res.status(500).json({
      error: { message: err instanceof Error ? err.message : String(err) },
    });
  });
  return app;
}
