import { Router } from "express";
import { ItxRequest, requireUser } from "../../context.js";
import { asyncHandler, notFound, pathParam } from "../../http.js";
import { AppState } from "../../state.js";
import { GetMeUseCase } from "./use_case/get_me.js";
import { ListSubscriptionsUseCase } from "./use_case/list_subscriptions.js";

export function createRouter(state: AppState): Router {
  const router = Router();

  router.get(
    "/me",
    requireUser,
    asyncHandler(async (req, res) => {
      const itx = (req as ItxRequest).itx;
      if (!itx.userEmail) {
        res
          .status(500)
          .json({ error: { message: "missing X-Itx-User-Email" } });
        return;
      }
      const useCase = new GetMeUseCase(state.userRepo);
      const output = await useCase.execute({
        userId: itx.userId!,
        userEmail: itx.userEmail,
      });
      res.json({ data: output });
    }),
  );

  router.get(
    "/:id/subscriptions",
    requireUser,
    asyncHandler(async (req, res) => {
      const id = pathParam(req, "id");
      if (!id) {
        res.status(400).json({ error: { message: "invalid user id" } });
        return;
      }
      const useCase = new ListSubscriptionsUseCase(
        state.userRepo,
        state.subscriptionRepo,
      );
      const output = await useCase.execute({ subscriberId: id });
      if (!output) {
        notFound(res);
        return;
      }
      res.json({ data: output });
    }),
  );

  return router;
}
