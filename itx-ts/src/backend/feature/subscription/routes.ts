import { Router } from "express";
import { z } from "zod";
import { ItxRequest, requireUser } from "../../context.js";
import { asyncHandler, notFound } from "../../http.js";
import { AppState } from "../../state.js";
import { SubscribeUseCase } from "./use_case/subscribe.js";
import { UnsubscribeUseCase } from "./use_case/unsubscribe.js";

const subscriptionParamsSchema = z.object({
  author_id: z.string().min(1),
});

export function createRouter(state: AppState): Router {
  const router = Router();

  router.put(
    "/:author_id",
    requireUser,
    asyncHandler(async (req, res) => {
      const { author_id: authorId } = subscriptionParamsSchema.parse(
        req.params,
      );
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
      const useCase = new SubscribeUseCase(
        state.userRepo,
        state.subscriptionRepo,
      );
      const subscribed = await useCase.execute({
        subscriberId: itx.userId!,
        subscriberEmail: itx.userEmail,
        authorId,
      });
      if (!subscribed) {
        notFound(res);
        return;
      }
      res.status(204).end();
    }),
  );

  router.delete(
    "/:author_id",
    requireUser,
    asyncHandler(async (req, res) => {
      const { author_id: authorId } = subscriptionParamsSchema.parse(
        req.params,
      );
      const itx = (req as ItxRequest).itx;
      if (itx.userId === authorId) {
        res
          .status(400)
          .json({ error: { message: "cannot unsubscribe from yourself" } });
        return;
      }
      const useCase = new UnsubscribeUseCase(
        state.userRepo,
        state.subscriptionRepo,
      );
      const unsubscribed = await useCase.execute({
        subscriberId: itx.userId!,
        authorId,
      });
      if (!unsubscribed) {
        notFound(res);
        return;
      }
      res.status(204).end();
    }),
  );

  return router;
}
