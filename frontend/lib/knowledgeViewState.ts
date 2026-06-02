type StorageLike = Pick<Storage, "getItem" | "setItem" | "removeItem">;

const LAST_CARD_KEY_PREFIX = "ai-study.knowledge.last-card";

function browserStorage(): StorageLike | null {
  if (typeof window === "undefined") return null;
  return window.localStorage;
}

export function knowledgeLastCardKey(userId: number): string {
  return `${LAST_CARD_KEY_PREFIX}.${userId}`;
}

export function readLastKnowledgeCardId(
  userId: number,
  storage: StorageLike | null = browserStorage()
): number | null {
  if (!storage) return null;

  try {
    const raw = storage.getItem(knowledgeLastCardKey(userId));
    const cardId = Number(raw);
    return Number.isInteger(cardId) && cardId > 0 ? cardId : null;
  } catch {
    return null;
  }
}

export function saveLastKnowledgeCardId(
  userId: number,
  cardId: number,
  storage: StorageLike | null = browserStorage()
) {
  if (!storage || !Number.isInteger(cardId) || cardId <= 0) return;

  try {
    storage.setItem(knowledgeLastCardKey(userId), String(cardId));
  } catch {
    // View state is best-effort; learning data stays in MySQL.
  }
}

export function clearLastKnowledgeCardId(
  userId: number,
  storage: StorageLike | null = browserStorage()
) {
  if (!storage) return;

  try {
    storage.removeItem(knowledgeLastCardKey(userId));
  } catch {
    // Ignore local view-state cleanup failures.
  }
}
