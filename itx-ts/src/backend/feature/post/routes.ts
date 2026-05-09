import { Router } from "express";
import { z } from "zod";
import { ItxRequest, requireUser } from "../../context.js";
import { asyncHandler } from "../../http.js";
import { AppState } from "../../state.js";
import { CreatePostUseCase } from "./use_case/create_post.js";
import { DeletePostUseCase } from "./use_case/delete_post.js";
import { GetPostUseCase } from "./use_case/get_post.js";
import { ListPostsUseCase } from "./use_case/list_posts.js";
import { UpdatePostUseCase } from "./use_case/update_post.js";

const listPostsQuerySchema = z.object({
  limit: z.coerce.number().int().positive().catch(50),
  offset: z.coerce.number().int().nonnegative().catch(0),
});

const postParamsSchema = z.object({
  id: z.coerce.number().int(),
});

const createPostBodySchema = z.object({
  title: z.string().default(""),
  body: z.string().default(""),
  tags: z.array(z.string()).default([]),
});

const updatePostBodySchema = z.object({
  title: z.string().optional(),
  body: z.string().optional(),
  tags: z.array(z.string()).optional(),
});

export function createRouter(state: AppState): Router {
  const router = Router();

  router.get(
    "/",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const query = listPostsQuerySchema.parse(req.query);
      const useCase = new ListPostsUseCase(state.postRepo);
      const output = await useCase.execute({
        userId: itx.userId!,
        limit: query.limit,
        offset: query.offset,
      });
      res.json({ data: output });
    }),
  );

  router.post(
    "/",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      const body = createPostBodySchema.parse(req.body ?? {});
      const useCase = new CreatePostUseCase(
        state.postRepo,
        state.controlStandardQueue,
      );
      const output = await useCase.execute({
        userId: itx.userId!,
        title: body.title,
        body: body.body,
        tags: body.tags,
      });
      res.status(201).json({ data: output });
    }),
  );

  router.get(
    "/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const { id } = postParamsSchema.parse(req.params);
      const itx = (req as ItxRequest).itx;
      const useCase = new GetPostUseCase(state.postRepo);
      const output = await useCase.execute({ id, userId: itx.userId! });
      res.json({ data: output });
    }),
  );

  router.patch(
    "/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const { id } = postParamsSchema.parse(req.params);
      const itx = (req as ItxRequest).itx;
      const body = updatePostBodySchema.parse(req.body ?? {});
      const useCase = new UpdatePostUseCase(state.postRepo);
      const output = await useCase.execute({
        id,
        userId: itx.userId!,
        title: body.title,
        body: body.body,
        tags: body.tags,
      });
      res.json({ data: output });
    }),
  );

  router.delete(
    "/:id",
    requireUser,
    asyncHandler(async (req, res) => {
      const { id } = postParamsSchema.parse(req.params);
      const itx = (req as ItxRequest).itx;
      const useCase = new DeletePostUseCase(state.postRepo);
      await useCase.execute({ id, userId: itx.userId! });
      res.status(204).end();
    }),
  );

  return router;
}
