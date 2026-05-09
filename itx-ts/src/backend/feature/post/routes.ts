import { Router } from "express";
import { ItxRequest, requireUser } from "../../context.js";
import { asyncHandler, notFound, parsePostId } from "../../http.js";
import { AppState } from "../../state.js";
import { CreatePostUseCase } from "./use_case/create_post.js";
import { DeletePostUseCase } from "./use_case/delete_post.js";
import { GetPostUseCase } from "./use_case/get_post.js";
import { ListPostsUseCase } from "./use_case/list_posts.js";
import { UpdatePostUseCase } from "./use_case/update_post.js";

export function createRouter(state: AppState): Router {
  const router = Router();

  router.get(
    "/",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const limit = Number.parseInt(String(req.query.limit ?? "50"), 10) || 50;
      const offset = Number.parseInt(String(req.query.offset ?? "0"), 10) || 0;
      const useCase = new ListPostsUseCase(state.postRepo);
      const output = await useCase.execute({
        userId: itx.userId!,
        limit,
        offset,
      });
      res.json({ data: output });
    }),
  );

  router.post(
    "/",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const body = req.body as {
        title?: string;
        body?: string;
        tags?: string[];
      };
      const useCase = new CreatePostUseCase(
        state.postRepo,
        state.controlStandardQueue,
      );
      const output = await useCase.execute({
        userId: itx.userId!,
        title: body.title ?? "",
        body: body.body ?? "",
        tags: Array.isArray(body.tags) ? body.tags : [],
      });
      res.status(201).json({ data: output });
    }),
  );

  router.get(
    "/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = parsePostId(req, res);
      if (id == null) {
        return;
      }
      const itx = (req as ItxRequest).itx;
      const useCase = new GetPostUseCase(state.postRepo);
      const output = await useCase.execute({ id, userId: itx.userId! });
      if (!output) {
        notFound(res);
        return;
      }
      res.json({ data: output });
    }),
  );

  router.patch(
    "/:id",
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
      const useCase = new UpdatePostUseCase(state.postRepo);
      const output = await useCase.execute({
        id,
        userId: itx.userId!,
        title: body.title,
        body: body.body,
        tags:
          Object.hasOwn(body, "tags") && Array.isArray(body.tags)
            ? body.tags
            : undefined,
      });
      if (!output) {
        notFound(res);
        return;
      }
      res.json({ data: output });
    }),
  );

  router.delete(
    "/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = parsePostId(req, res);
      if (id == null) {
        return;
      }
      const itx = (req as ItxRequest).itx;
      const useCase = new DeletePostUseCase(state.postRepo);
      const deleted = await useCase.execute({ id, userId: itx.userId! });
      if (!deleted) {
        notFound(res);
        return;
      }
      res.status(204).end();
    }),
  );

  return router;
}
