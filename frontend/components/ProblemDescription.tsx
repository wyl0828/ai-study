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

interface ProblemDescriptionProps {
  problem: ProblemDetail;
}

export default function ProblemDescription({
  problem,
}: ProblemDescriptionProps) {
  const examples = displayExamples(problem);
  const showIoFormat = shouldShowIoFormat(problem);
  const presetHints = problem.presetHints ?? getProblemPresetHints(problem.id);

  return (
    <section className="w-[25%] min-w-[280px] max-w-[420px] h-full border-r border-outline-variant/30 flex flex-col overflow-y-auto bg-surface-container-lowest">
      <div className="p-5 flex-1">
        {/* 难度/题号/标题 + 上下题导航 */}
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

        {/* 题目描述 */}
        <div className="text-sm text-on-surface-variant space-y-3 leading-relaxed">
          <div className="whitespace-pre-wrap">
            {problemDescription(problem.description)}
          </div>
        </div>

        {/* 输入输出格式 */}
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

        {/* 示例用例 */}
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
              </div>
            ))}
          </div>
        )}

        {/* 知识点标签 */}
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

        <ProblemHintPanel hints={presetHints} />
      </div>
    </section>
  );
}
