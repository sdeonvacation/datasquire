import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface StreamingMessageProps {
  content: string;
}

export function StreamingMessage({ content }: StreamingMessageProps) {
  return (
    <div className="flex justify-start mb-4 animate-[fadeIn_0.3s_ease-in]">
      <div className="w-full max-w-full md:max-w-[85%]">
        <div className="bg-white border border-stone-200 px-4 py-3 rounded-2xl rounded-bl-sm">
          <div className="prose prose-sm prose-stone max-w-none">
            {content ? (
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {content}
              </ReactMarkdown>
            ) : (
              <span className="text-stone-400 text-sm">Thinking...</span>
            )}
            <span className="inline-block w-2 h-4 bg-green-500 ml-0.5 align-middle animate-[blink_1s_step-end_infinite]" />
          </div>
        </div>
      </div>
    </div>
  );
}
