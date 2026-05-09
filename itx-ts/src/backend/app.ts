import express, { NextFunction, Request, Response, Router } from "express";
import { extractContext, ItxRequest, requireUser } from "./context.js";
import { AppState } from "./state.js";
import { NotFoundError } from "../types.js";
import { toPostDto } from "../util.js";

type AsyncHandler = (
  req: Request,
  res: Response,
  next: NextFunction,
) => Promise<void>;

function asyncHandler(handler: AsyncHandler): AsyncHandler {
  return async (req, res, next) => {
    try {
      await handler(req, res, next);
    } catch (err) {
      next(err);
    }
  };
}

function pathParam(req: Request, name: string): string {
  const value = req.params[name];
  return Array.isArray(value) ? value[0] : (value ?? "");
}

function parsePostId(req: Request, res: Response): number | null {
  const id = Number.parseInt(pathParam(req, "id"), 10);
  if (!Number.isFinite(id)) {
    res.status(400).json({ error: { message: "invalid post id" } });
    return null;
  }
  return id;
}

function notFound(res: Response): void {
  res.status(404).json({ error: { message: "not found" } });
}

function registerPostRoutes(router: Router, state: AppState): void {
  router.get(
    "/posts",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const limit = Number.parseInt(String(req.query.limit ?? "50"), 10) || 50;
      const offset = Number.parseInt(String(req.query.offset ?? "0"), 10) || 0;
      const posts = await state.postRepo.list({
        authorId: itx.userId,
        limit,
        offset,
      });
      res.json({ data: { items: posts.map(toPostDto) } });
    }),
  );

  router.post(
    "/posts",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const body = req.body as {
        title?: string;
        body?: string;
        tags?: string[];
      };
      const tags = Array.isArray(body.tags) ? body.tags : [];
      const post = await state.postRepo.create({
        authorId: itx.userId!,
        title: body.title ?? "",
        body: body.body ?? "",
        tags,
      });
      await state.controlStandardQueue.publish(
        JSON.stringify({
          type: "post.created",
          postId: post.id,
          authorId: post.authorId,
        }),
      );
      res.status(201).json({ data: toPostDto(post) });
    }),
  );

  router.get(
    "/posts/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = parsePostId(req, res);
      if (id == null) {
        return;
      }
      const itx = (req as ItxRequest).itx;
      const post = await state.postRepo.get(id).catch((err: unknown) => {
        if (err instanceof NotFoundError) {
          return null;
        }
        throw err;
      });
      if (!post || post.authorId !== itx.userId) {
        notFound(res);
        return;
      }
      res.json({ data: toPostDto(post) });
    }),
  );

  router.patch(
    "/posts/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = parsePostId(req, res);
      if (id == null) {
        return;
      }
      const itx = (req as ItxRequest).itx;
      const body = req.body as {
        title?: string;
        body?: string;
        tags?: string[];
      };
      const post = await state.postRepo
        .update({
          id,
          authorId: itx.userId!,
          title: body.title,
          body: body.body,
          tags:
            Object.hasOwn(body, "tags") && Array.isArray(body.tags)
              ? body.tags
              : undefined,
        })
        .catch((err: unknown) => {
          if (err instanceof NotFoundError) {
            return null;
          }
          throw err;
        });
      if (!post) {
        notFound(res);
        return;
      }
      res.json({ data: toPostDto(post) });
    }),
  );

  router.delete(
    "/posts/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = parsePostId(req, res);
      if (id == null) {
        return;
      }
      const itx = (req as ItxRequest).itx;
      const deleted = await state.postRepo
        .delete({ id, authorId: itx.userId! })
        .then(
          () => true,
          (err: unknown) => {
            if (err instanceof NotFoundError) {
              return false;
            }
            throw err;
          },
        );
      if (!deleted) {
        notFound(res);
        return;
      }
      res.status(204).end();
    }),
  );
}

function registerUserRoutes(router: Router, state: AppState): void {
  router.get(
    "/users/me",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      if (!itx.userEmail) {
        res
          .status(500)
          .json({ error: { message: "missing X-Itx-User-Email" } });
        return;
      }
      const user = await state.userRepo.upsert({
        id: itx.userId!,
        email: itx.userEmail,
      });
      res.json({ data: user });
    }),
  );

  router.get(
    "/users/:id/subscriptions",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = pathParam(req, "id");
      if (!id) {
        res.status(400).json({ error: { message: "invalid user id" } });
        return;
      }
      const user = await state.userRepo.get(id).catch((err: unknown) => {
        if (err instanceof NotFoundError) {
          return null;
        }
        throw err;
      });
      if (!user) {
        notFound(res);
        return;
      }
      const authors = await state.subscriptionRepo.listAuthors(id);
      res.json({ data: { items: authors } });
    }),
  );
}

function registerSubscriptionRoutes(router: Router, state: AppState): void {
  router.put(
    "/subscriptions/:author_id",
    requireUser,
    asyncHandler(async (req, res) => {
      const authorId = pathParam(req, "author_id");
      const itx = (req as ItxRequest).itx;
      if (itx.userId === authorId) {
        res
          .status(400)
          .json({ error: { message: "cannot subscribe to yourself" } });
        return;
      }
      if (!itx.userEmail) {
        res
          .status(500)
          .json({ error: { message: "missing X-Itx-User-Email" } });
        return;
      }
      const author = await state.userRepo
        .get(authorId)
        .catch((err: unknown) => {
          if (err instanceof NotFoundError) {
            return null;
          }
          throw err;
        });
      if (!author) {
        notFound(res);
        return;
      }
      await state.userRepo.upsert({ id: itx.userId!, email: itx.userEmail });
      await state.subscriptionRepo.subscribe({
        subscriberId: itx.userId!,
        authorId,
      });
      res.status(204).end();
    }),
  );

  router.delete(
    "/subscriptions/:author_id",
    requireUser,
    asyncHandler(async (req, res) => {
      const authorId = pathParam(req, "author_id");
      const itx = (req as ItxRequest).itx;
      if (itx.userId === authorId) {
        res
          .status(400)
          .json({ error: { message: "cannot unsubscribe from yourself" } });
        return;
      }
      const author = await state.userRepo
        .get(authorId)
        .catch((err: unknown) => {
          if (err instanceof NotFoundError) {
            return null;
          }
          throw err;
        });
      if (!author) {
        notFound(res);
        return;
      }
      await state.subscriptionRepo.unsubscribe({
        subscriberId: itx.userId!,
        authorId,
      });
      res.status(204).end();
    }),
  );
}

export function createApp(state: AppState): express.Express {
  const app = express();
  app.use(express.json());
  app.use(extractContext);

  app.get("/api/v1/health", (_req, res) =>
    res.json({ data: { status: "ok" } }),
  );
  app.all("/api/v1/health", (_req, res) =>
    res.status(405).json({ error: { message: "Method Not Allowed" } }),
  );

  const protectedRouter = Router();
  registerPostRoutes(protectedRouter, state);
  registerUserRoutes(protectedRouter, state);
  registerSubscriptionRoutes(protectedRouter, state);
  app.use("/api/v1", protectedRouter);

  app.use((_req, res) =>
    res.status(404).json({ error: { message: "Not Found" } }),
  );
  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (res.headersSent) {
      return;
    }
    console.error(err);
    res.status(500).json({
      error: { message: err instanceof Error ? err.message : String(err) },
    });
  });
  return app;
}
