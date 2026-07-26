import { Check } from 'lucide-react';
import type { StepInfo } from '../../lib/types';

interface StepIndicatorProps {
  steps: StepInfo[];
}

const STEP_LABELS: Record<StepInfo['step'], string> = {
  rag: 'RAG',
  sql: 'SQL',
  execute: 'Execute',
  format: 'Format',
};

function StepCircle({ status }: { status: StepInfo['status'] }) {
  if (status === 'done') {
    return (
      <div className="w-7 h-7 rounded-full bg-green-600 flex items-center justify-center">
        <Check size={14} className="text-white" />
      </div>
    );
  }

  if (status === 'active') {
    return (
      <div className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center animate-pulse">
        <div className="w-2.5 h-2.5 rounded-full bg-white" />
      </div>
    );
  }

  return (
    <div className="w-7 h-7 rounded-full bg-stone-300 flex items-center justify-center">
      <div className="w-2.5 h-2.5 rounded-full bg-stone-500" />
    </div>
  );
}

function ConnectorLine({ status }: { status: 'done' | 'pending' }) {
  return (
    <div
      className={`flex-1 h-0.5 mx-1 ${
        status === 'done' ? 'bg-green-600' : 'bg-stone-300'
      }`}
    />
  );
}

export function StepIndicator({ steps }: StepIndicatorProps) {
  return (
    <div className="space-y-2">
      <div className="flex items-center">
        {steps.map((step, i) => (
          <div key={step.step} className="contents">
            <div className="flex flex-col items-center gap-1">
              <StepCircle status={step.status} />
              <span className="text-xs font-medium text-stone-600">
                {STEP_LABELS[step.step]}
              </span>
            </div>
            {i < steps.length - 1 && (
              <ConnectorLine
                status={step.status === 'done' ? 'done' : 'pending'}
              />
            )}
          </div>
        ))}
      </div>

      {steps
        .filter((s) => s.status === 'active' && s.detail)
        .map((s) => (
          <p key={s.step} className="text-xs text-blue-600 mt-1 text-center">
            {s.detail}
          </p>
        ))}
    </div>
  );
}
