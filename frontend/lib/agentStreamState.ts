export type AgentStreamFallbackEvent = "error" | "invalid_done" | "end";

export function createAgentStreamSession(currentStreamId: number) {
  return currentStreamId + 1;
}

export function isCurrentAgentStream(streamId: number, currentStreamId: number) {
  return streamId === currentStreamId;
}

export function shouldRunSyncFallback(
  event: AgentStreamFallbackEvent,
  doneReceived: boolean
) {
  if (event === "end") {
    return !doneReceived;
  }

  return true;
}
