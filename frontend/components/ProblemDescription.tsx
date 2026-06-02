"use client";

import { useState } from "react";
import { BookOpen, FileText, Lightbulb } from "lucide-react";
import type { ProblemDetail } from "@/lib/types";
import {
  formatText,
  knowledgePoint,
  problemDescription,
  problemTitle,
} from "@/lib/i18n";
import {
  displayExamples,
  shouldShowIoFormat,
} from "@/lib/problemPresentation";
import { getProblemPresetHints } from "@/lib/problemHints";
import DifficultyBadge from "./DifficultyBadge";
import ProblemHintPanel from "./ProblemHintPanel";
import ProblemNavigator from "./ProblemNavigator";
import ProblemSolutionPanel from "./ProblemSolutionPanel";
import ProblemExampleVisual from "./ProblemExampleVisual";

interface ProblemDescriptionProps {
  problem: ProblemDetail;
}

type ProblemTab = "description" | "hints" | "solution";

const tabs: Array<{
  key: ProblemTab;
  label: string;
  Icon: typeof FileText;
}> = [
  { key: "description", label: "题目", Icon: FileText },
  { key: "hints", label: "提示", Icon: Lightbulb },
  { key: "solution", label: "题解", Icon: BookOpen },
];

export default function ProblemDescription({
  problem,
}: ProblemDescriptionProps) {
  const [activeTab, setActiveTab] = useState<ProblemTab>("description");
  const examples = displayExamples(problem);
  const showIoFormat = shouldShowIoFormat(problem);
  const presetHints = problem.presetHints ?? getProblemPresetHints(problem.id);

  return (
    <section className="coach-panel flex w-full min-w-0 flex-col overflow-y-auto border-b md:h-full md:min-w-0 md:max-w-none md:border-b-0 md:border-r">
      <div className="flex-1 p-5">
        <div className="flex justify-between items-start mb-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <DifficultyBadge difficulty={problem.difficulty} />
              <span className="text-outline text-xs">题目 #{problem.id}</span>
            </div>
            <h1 className="text-xl font-bold text-on-surface tracking-tight">
              {problemTitle(problem.title)}
            </h1>
          </div>
          <ProblemNavigator currentProblemId={problem.id} />
        </div>

        <div className="coach-tab-list mb-4 grid-cols-3">
          {tabs.map(({ key, label, Icon }) => (
            <button
              key={key}
              type="button"
              onClick={() => setActiveTab(key)}
              className={`flex items-center justify-center gap-1.5 rounded-md px-2 py-2 text-xs font-semibold transition-colors ${
                activeTab === key
                  ? "bg-surface-container-lowest text-primary shadow-sm"
                  : "text-on-surface-variant hover:text-on-surface"
              }`}
              aria-pressed={activeTab === key}
            >
              <Icon className="h-3.5 w-3.5" />
              {label}
            </button>
          ))}
        </div>

        {activeTab === "description" && (
          <ProblemStatement
            problem={problem}
            examples={examples}
            showIoFormat={showIoFormat}
          />
        )}

        {activeTab === "hints" && <ProblemHintPanel hints={presetHints} />}

        {activeTab === "solution" && (
          <ProblemSolutionPanel solutionOutline={problem.solutionOutline} />
        )}
      </div>
    </section>
  );
}

function ProblemStatement({
  problem,
  examples,
  showIoFormat,
}: {
  problem: ProblemDetail;
  examples: ReturnType<typeof displayExamples>;
  showIoFormat: boolean;
}) {
  return (
    <>
      <div className="text-sm text-on-surface-variant space-y-3 leading-relaxed">
        <div className="whitespace-pre-wrap">
          {problemDescription(problem.description)}
        </div>
      </div>

      {showIoFormat && (
        <div className="mt-5 space-y-3">
          {problem.inputFormat && (
            <div>
              <h3 className="text-sm font-semibold text-on-surface mb-2">
                输入格式
              </h3>
              <div className="bg-surface-container rounded-lg p-3 border border-outline-variant/40 text-xs font-mono text-on-surface leading-loose">
                {formatText(problem.inputFormat)}
              </div>
            </div>
          )}
          {problem.outputFormat && (
            <div>
              <h3 className="text-sm font-semibold text-on-surface mb-2">
                输出格式
              </h3>
              <div className="bg-surface-container rounded-lg p-3 border border-outline-variant/40 text-xs font-mono text-on-surface leading-loose">
                {formatText(problem.outputFormat)}
              </div>
            </div>
          )}
        </div>
      )}

      {examples.length > 0 && (
        <div className="mt-5 space-y-3">
          {examples.map((example, i) => (
            <div key={example.id}>
              <h3 className="text-sm font-semibold text-on-surface mb-2">
                示例 {i + 1}：
              </h3>
              <div className="bg-surface-container rounded-lg p-3 border border-outline-variant/40 font-mono text-xs text-on-surface leading-loose">
                <span className="text-secondary font-sans font-medium">
                  输入：
                </span>
                {example.input}
                <br />
                <span className="text-secondary font-sans font-medium">
                  输出：
                </span>
                {example.output}
              </div>
              {example.visual && <ProblemExampleVisual visual={example.visual} />}
            </div>
          ))}
        </div>
      )}

      {problem.knowledgePoints?.length > 0 && (
        <div className="mt-5 pt-4 border-t border-outline-variant/30">
          <h3 className="text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-2">
            关联知识点
          </h3>
          <div className="flex flex-wrap gap-1.5">
            {problem.knowledgePoints.map((kp) => (
              <span
                key={kp}
                className="bg-primary/10 text-primary text-xs px-2 py-0.5 rounded-full font-medium"
              >
                {knowledgePoint(kp)}
              </span>
            ))}
          </div>
        </div>
      )}
    </>
  );
}
