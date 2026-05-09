import { ControlDispatcher } from "../worker/dispatcher.js";
import { runQueues } from "../worker/run.js";
import { workerStateFromEnv } from "../worker/state.js";

const state = await workerStateFromEnv();
const dispatcher = new ControlDispatcher(state);

try {
  await runQueues(
    [state.controlStandardQueue, state.controlPremiumQueue],
    (body) => dispatcher.handle(body),
  );
} finally {
  await state.close();
}
