import { useState } from 'react';
import { X, ChevronDown, ChevronRight, Database, Code, Play, Layers, Clock, Bot, RefreshCw } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { StepIndicator } from './StepIndicator';
import { SqlBlock } from './SqlBlock';

function Section({
  title,
  icon,
  children,
  defaultOpen = true,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const sectionId = `inspection-section-${title.toLowerCase().replace(/\s+/g, '-')}`;

  return (
    <div className="border-b border-stone-200 last:border-b-0">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-controls={sectionId}
        className="w-full flex items-center gap-2 px-4 py-3 text-sm font-medium text-stone-700 hover:bg-stone-200/50 transition-colors"
      >
        {open ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        {icon}
        <span>{title}</span>
      </button>
      {open && (
        <div id={sectionId} role="region" aria-label={title} className="px-4 pb-3">
          {children}
        </div>
      )}
    </div>
  );
}

export function InspectionPanel() {
  const { state, dispatch } = useApp();
  const { currentInspection: data, inspectionOpen } = state;

  return (
    <aside
      className={`fixed top-0 right-0 h-full w-80 bg-stone-100 border-l border-stone-200 overflow-y-auto z-40 transition-transform duration-300 ease-in-out md:relative md:translate-x-0 ${
        inspectionOpen ? 'translate-x-0' : 'translate-x-full'
      } max-md:shadow-lg`}
      aria-label="Query Inspection"
    >
      <header className="sticky top-0 bg-stone-100 z-10 flex items-center justify-between px-4 py-3 border-b border-stone-200">
        <h2 className="text-sm font-semibold text-stone-800">Query Inspection</h2>
        <button
          type="button"
          onClick={() => dispatch({ type: 'TOGGLE_INSPECTION' })}
          className="p-1 rounded hover:bg-stone-200 text-stone-500"
          aria-label="Close inspection panel"
        >
          <X size={16} />
        </button>
      </header>

      {!data ? (
        <div className="flex flex-col items-center justify-center h-64 px-6 text-center">
          <Layers size={32} className="text-stone-400 mb-3" />
          <p className="text-sm text-stone-500">
            Submit a query to see what happens under the hood
          </p>
        </div>
      ) : (
        <div className="divide-y divide-stone-200">
          <Section title="Step Progress" icon={<Play size={14} />}>
            <StepIndicator steps={data.steps} />
          </Section>

          <Section title="Schema Chunks Retrieved" icon={<Database size={14} />}>
            {data.chunks.length === 0 ? (
              <p className="text-xs text-stone-500 italic">No chunks retrieved</p>
            ) : (
              <ul className="space-y-1.5">
                {data.chunks.map((chunk, i) => (
                  <li
                    key={i}
                    className="text-xs text-stone-600 bg-white rounded px-2 py-1.5 border border-stone-200 line-clamp-2"
                  >
                    {chunk}
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <Section title="Generated SQL" icon={<Code size={14} />}>
            <SqlBlock sql={data.sql} />
          </Section>

          <Section title="Execution Result" icon={<Play size={14} />}>
            <div className="bg-white rounded border border-stone-200 p-2 overflow-auto max-h-48">
              <pre className="text-xs text-stone-700 font-mono whitespace-pre-wrap">
                {data.rawResult || 'No result'}
              </pre>
            </div>
          </Section>

          <Section title="Metadata" icon={<Clock size={14} />} defaultOpen={false}>
            <dl className="space-y-2 text-xs">
              <div className="flex items-center gap-2">
                <RefreshCw size={12} className="text-stone-400" />
                <dt className="text-stone-500">Iterations:</dt>
                <dd className="text-stone-700 font-medium">{data.iterations}</dd>
              </div>
              <div className="flex items-center gap-2">
                <Clock size={12} className="text-stone-400" />
                <dt className="text-stone-500">Latency:</dt>
                <dd className="text-stone-700 font-medium">{data.latencyMs}ms</dd>
              </div>
              <div className="flex items-start gap-2">
                <Bot size={12} className="text-stone-400 mt-0.5" />
                <dt className="text-stone-500">Agents:</dt>
                <dd className="text-stone-700 font-medium">
                  {data.agentsUsed.join(', ') || 'None'}
                </dd>
              </div>
            </dl>
          </Section>
        </div>
      )}
    </aside>
  );
}
