import { TrendingUp, AlertTriangle, FileWarning, BarChart3 } from 'lucide-react';

interface SuggestedQueriesProps {
  onSelect: (query: string) => void;
}

const suggestions = [
  {
    text: "What's our total revenue YTD by business unit?",
    icon: TrendingUp,
  },
  {
    text: 'Which departments are over budget for Q3?',
    icon: AlertTriangle,
  },
  {
    text: 'Show me all overdue invoices over $10,000',
    icon: FileWarning,
  },
  {
    text: "What's our operating cash flow trend?",
    icon: BarChart3,
  },
] as const;

export function SuggestedQueries({ onSelect }: SuggestedQueriesProps) {
  return (
    <div className="flex flex-col items-center justify-center h-full px-4 py-12">
      <h1 className="text-2xl font-semibold text-stone-800 mb-2">
        Finance Controller Dashboard
      </h1>
      <p className="text-stone-500 mb-8">
        Ask questions about your financial data
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-xl">
        {suggestions.map((item) => (
          <button
            key={item.text}
            onClick={() => onSelect(item.text)}
            className="flex items-start gap-3 border border-stone-200 rounded-xl p-4 text-left hover:shadow-md hover:border-stone-300 transition-all bg-white group"
          >
            <item.icon className="w-5 h-5 text-green-600 mt-0.5 shrink-0 group-hover:scale-110 transition-transform" />
            <span className="text-sm text-stone-700">{item.text}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
