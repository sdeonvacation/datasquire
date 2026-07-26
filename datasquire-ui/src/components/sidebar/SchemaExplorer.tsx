import { useState } from 'react';
import { ChevronRight, Database, MessageSquare } from 'lucide-react';

interface SchemaExplorerProps {
  onTableClick: (query: string) => void;
}

type ColumnType = 'text' | 'numeric' | 'date';

interface Column {
  name: string;
  type: ColumnType;
}

const SCHEMA: Record<string, Column[]> = {
  business_units: [
    { name: 'id', type: 'numeric' },
    { name: 'name', type: 'text' },
    { name: 'region', type: 'text' },
    { name: 'cost_center', type: 'text' },
    { name: 'head_count', type: 'numeric' },
  ],
  chart_of_accounts: [
    { name: 'account_id', type: 'numeric' },
    { name: 'account_name', type: 'text' },
    { name: 'account_type', type: 'text' },
    { name: 'parent_account_id', type: 'numeric' },
  ],
  journal_entries: [
    { name: 'entry_id', type: 'numeric' },
    { name: 'entry_date', type: 'date' },
    { name: 'description', type: 'text' },
    { name: 'status', type: 'text' },
  ],
  journal_lines: [
    { name: 'line_id', type: 'numeric' },
    { name: 'entry_id', type: 'numeric' },
    { name: 'account_id', type: 'numeric' },
    { name: 'business_unit_id', type: 'numeric' },
    { name: 'debit_amount', type: 'numeric' },
    { name: 'credit_amount', type: 'numeric' },
  ],
  budgets: [
    { name: 'budget_id', type: 'numeric' },
    { name: 'fiscal_year', type: 'numeric' },
    { name: 'business_unit_id', type: 'numeric' },
    { name: 'q1_amount', type: 'numeric' },
    { name: 'q2_amount', type: 'numeric' },
    { name: 'q3_amount', type: 'numeric' },
    { name: 'q4_amount', type: 'numeric' },
    { name: 'annual_total', type: 'numeric' },
  ],
  invoices: [
    { name: 'invoice_id', type: 'numeric' },
    { name: 'vendor_name', type: 'text' },
    { name: 'amount', type: 'numeric' },
    { name: 'status', type: 'text' },
    { name: 'due_date', type: 'date' },
  ],
  cash_flow: [
    { name: 'flow_id', type: 'numeric' },
    { name: 'flow_date', type: 'date' },
    { name: 'flow_type', type: 'text' },
    { name: 'amount', type: 'numeric' },
  ],
};

const TYPE_COLORS: Record<ColumnType, string> = {
  text: 'bg-blue-900/40 text-blue-300',
  numeric: 'bg-emerald-900/40 text-emerald-300',
  date: 'bg-amber-900/40 text-amber-300',
};

export function SchemaExplorer({ onTableClick }: SchemaExplorerProps) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const toggle = (table: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(table) ? next.delete(table) : next.add(table);
      return next;
    });
  };

  return (
    <div className="flex flex-col gap-1 overflow-y-auto px-2">
      <div className="flex items-center gap-1.5 px-2 pb-2 text-xs font-medium text-stone-500 uppercase tracking-wider">
        <Database className="h-3.5 w-3.5" />
        Finance Schema
      </div>
      {Object.entries(SCHEMA).map(([table, columns]) => {
        const isOpen = expanded.has(table);
        return (
          <div key={table}>
            <div
              className="flex w-full items-center gap-1.5 rounded-md px-2 py-1.5 text-sm text-stone-300 hover:bg-stone-800 transition-colors"
              role="treeitem"
              aria-expanded={isOpen}
            >
              <button
                onClick={() => toggle(table)}
                onDoubleClick={() => onTableClick(`Tell me about the ${table} table`)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    onTableClick(`Tell me about the ${table} table`);
                  }
                }}
                className="flex flex-1 items-center gap-1.5"
                aria-label={`${isOpen ? 'Collapse' : 'Expand'} ${table}`}
              >
                <ChevronRight
                  className={`h-3.5 w-3.5 text-stone-500 transition-transform duration-200 ${isOpen ? 'rotate-90' : ''}`}
                />
                <span className="font-mono text-xs">{table}</span>
              </button>
              <button
                onClick={() => onTableClick(`Tell me about the ${table} table`)}
                className="rounded p-0.5 text-stone-500 hover:text-emerald-400 transition-colors"
                aria-label={`Ask about ${table}`}
              >
                <MessageSquare className="h-3 w-3" />
              </button>
            </div>
            <div
              className={`overflow-hidden transition-all duration-200 ${isOpen ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'}`}
            >
              <ul className="ml-5 border-l border-stone-700 pl-3 py-1">
                {columns.map((col) => (
                  <li key={col.name} className="flex items-center gap-2 py-0.5">
                    <span className="font-mono text-xs text-stone-400">{col.name}</span>
                    <span className={`rounded px-1 py-px text-[10px] font-medium ${TYPE_COLORS[col.type]}`}>
                      {col.type}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        );
      })}
    </div>
  );
}
