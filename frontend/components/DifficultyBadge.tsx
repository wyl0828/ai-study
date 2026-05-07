interface DifficultyBadgeProps {
  difficulty: string;
}

const colorMap: Record<string, string> = {
  EASY: "bg-emerald-50 text-emerald-700",
  MEDIUM: "bg-amber-50 text-amber-700",
  HARD: "bg-red-50 text-red-700",
};

export default function DifficultyBadge({ difficulty }: DifficultyBadgeProps) {
  const labelMap: Record<string, string> = {
    EASY: "简单",
    MEDIUM: "中等",
    HARD: "困难",
  };

  return (
    <span
      className={`inline-block text-xs font-semibold px-2 py-0.5 rounded-full ${
        colorMap[difficulty] || "bg-gray-100 text-gray-800"
      }`}
    >
      {labelMap[difficulty] || difficulty}
    </span>
  );
}
