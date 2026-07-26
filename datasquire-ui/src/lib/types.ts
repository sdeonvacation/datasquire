export type Role = 'user' | 'assistant' | 'system';

export interface Message {
  id: string;
  role: Role;
  content: string;
  timestamp: Date;
  inspection?: InspectionData;
}

export interface InspectionData {
  chunks: string[];
  sql: string;
  rawResult: string;
  steps: StepInfo[];
  latencyMs: number;
  agentsUsed: string[];
  iterations: number;
}

export interface StepInfo {
  step: 'rag' | 'sql' | 'execute' | 'format';
  status: 'pending' | 'active' | 'done';
  detail?: string;
}

export interface SessionSummary {
  id: string;
  firstQuery: string;
  lastAccess: Date;
  messageCount: number;
}

export interface AgentInfo {
  name: string;
  description: string;
  capabilities: string[];
}

export interface SSEEvent {
  type: 'progress' | 'data' | 'done' | 'error';
  payload: ProgressPayload | DataPayload | DonePayload | ErrorPayload;
}

export interface ProgressPayload {
  step: StepInfo['step'];
  detail: string;
}

export interface DataPayload {
  chunk: string;
}

export interface DonePayload {
  sessionId: string;
  agentsUsed: string[];
  iterations: number;
  qualityScore: number;
  metadata: Record<string, unknown>;
}

export interface ErrorPayload {
  code: string;
  message: string;
}

export interface AppState {
  sessions: SessionSummary[];
  activeSessionId: string | null;
  messages: Message[];
  isStreaming: boolean;
  currentInspection: InspectionData | null;
  sidebarOpen: boolean;
  inspectionOpen: boolean;
  theme: 'light' | 'dark';
}

export type AppAction =
  | { type: 'SEND_QUERY'; payload: { id: string; content: string } }
  | { type: 'STREAM_CHUNK'; payload: { chunk: string } }
  | { type: 'STREAM_PROGRESS'; payload: ProgressPayload }
  | { type: 'QUERY_COMPLETE'; payload: { inspection: InspectionData } }
  | { type: 'QUERY_ERROR'; payload: { message: string } }
  | { type: 'SET_SESSION'; payload: { sessionId: string; messages: Message[] } }
  | { type: 'NEW_SESSION'; payload: { session: SessionSummary } }
  | { type: 'TOGGLE_SIDEBAR' }
  | { type: 'TOGGLE_INSPECTION' }
  | { type: 'SET_THEME'; payload: { theme: 'light' | 'dark' } };
