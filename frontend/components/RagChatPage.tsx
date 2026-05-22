"use client";

import { FormEvent, useState } from "react";
import { Send, Sparkles } from "lucide-react";
import { formatApiError, ragChatApi } from "@/lib/api";
import type { RagChatSource } from "@/lib/types";
import RagChatMessage from "@/components/RagChatMessage";
import RagChatSources from "@/components/RagChatSources";

const DEMO_USER_ID = 1;

const exampleQuestions = [
  "HashMap 查询和写入顺序为什么会出错？",
  "反转链表为什么最后要返回 prev？",
  "我的最近错题主要集中在哪些知识点？",
  "Spring Bean 生命周期怎么回答更像面试？",
];

interface ChatMessage {
  id: number;
  role: "user" | "assistant";
  content: string;
  sources?: RagChatSource[];
}

export default function RagChatPage() {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [selectedMessageId, setSelectedMessageId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = question.trim().length > 0 && !loading;
  const lastAssistantWithSources = [...messages]
    .reverse()
    .find((message) => message.role === "assistant" && (message.sources?.length || 0) > 0);
  const selectedMessage = messages.find(
    (message) => message.id === selectedMessageId && (message.sources?.length || 0) > 0
  );
  const selectedSources = selectedMessage?.sources || lastAssistantWithSources?.sources || [];

  const ask = async (value?: string) => {
    const text = (value ?? question).trim();
    if (!text || loading) {
      return;
    }

    setLoading(true);
    setError(null);
    setQuestion("");
    setMessages((current) => [
      ...current,
      { id: Date.now(), role: "user", content: text },
    ]);

    try {
      const response = await ragChatApi.ask({
        userId: DEMO_USER_ID,
        question: text,
      });
      const assistantId = Date.now() + 1;
      const assistantSources = response.data.sources || [];
      setMessages((current) => [
        ...current,
        {
          id: assistantId,
          role: "assistant",
          content: response.data.answer,
          sources: response.data.sources || [],
        },
      ]);
      if (assistantSources.length > 0) {
        setSelectedMessageId(assistantId);
      }
    } catch (err) {
      setError(formatApiError(err, "rag"));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    ask();
  };

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6">
        <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-primary">
          <Sparkles className="h-4 w-4" />
          学习资料问答 V1
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-on-surface">知识库问答</h1>
        <p className="mt-2 max-w-3xl text-sm leading-relaxed text-on-surface-variant">
          围绕题目、知识卡、历史诊断和错题卡提问。这里用于复习资料与面试表达，不替代做题页的提交诊断主流程。
        </p>
      </div>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="rounded-xl border border-outline-variant/30 bg-surface p-5">
          {error && (
            <div className="mb-4 rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
              {error}
            </div>
          )}

          <div className="mb-4 flex flex-wrap gap-2">
            {exampleQuestions.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => ask(item)}
                disabled={loading}
                className="rounded-full border border-outline-variant/50 px-3 py-1.5 text-xs font-medium text-on-surface-variant transition-colors hover:border-primary/50 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
              >
                {item}
              </button>
            ))}
          </div>

          <div className="mb-4 min-h-[360px] space-y-4 rounded-lg border border-outline-variant/20 bg-surface-container-lowest p-4">
            {messages.length === 0 ? (
              <div className="flex h-[320px] items-center justify-center text-center text-sm leading-relaxed text-on-surface-variant">
                选择一个示例问题，或输入你想复习的题目、知识点、历史错题。
              </div>
            ) : (
              messages.map((message) => (
                <RagChatMessage
                  key={message.id}
                  role={message.role}
                  content={message.content}
                  sources={message.sources}
                  selected={message.id === selectedMessageId}
                  onSelect={() => setSelectedMessageId(message.id)}
                />
              ))
            )}
            {loading && (
              <RagChatMessage role="assistant" content="正在检索知识库并组织回答..." />
            )}
          </div>

          <form onSubmit={handleSubmit} className="flex gap-3">
            <textarea
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              rows={2}
              placeholder="例如：HashMap 查询和写入顺序为什么会导致 Two Sum 出错？"
              className="min-h-[52px] flex-1 resize-none rounded-lg border border-outline-variant/50 bg-surface px-4 py-3 text-sm text-on-surface outline-none transition-colors placeholder:text-on-surface-variant focus:border-primary"
            />
            <button
              type="submit"
              disabled={!canSubmit}
              className="inline-flex h-[52px] items-center gap-2 rounded-lg bg-primary px-5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Send className="h-4 w-4" />
              发送
            </button>
          </form>
        </div>

        <RagChatSources sources={selectedSources} />
      </section>
    </main>
  );
}
