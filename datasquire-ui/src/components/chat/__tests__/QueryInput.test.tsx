import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryInput } from '../QueryInput';

describe('QueryInput', () => {
  const onSend = vi.fn();
  const onCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders textarea with placeholder', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    expect(screen.getByPlaceholderText('Ask about revenue, budgets, invoices...')).toBeTruthy();
  });

  it('disables send button when input is empty', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    const btn = screen.getByLabelText('Send message');
    expect(btn).toBeDisabled();
  });

  it('enables send button when input has text', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'test query' } });
    expect(screen.getByLabelText('Send message')).not.toBeDisabled();
  });

  it('calls onSend and clears input on submit', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'my query' } });
    fireEvent.click(screen.getByLabelText('Send message'));
    expect(onSend).toHaveBeenCalledWith('my query');
  });

  it('submits on Cmd+Enter', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'keyboard submit' } });
    fireEvent.keyDown(textarea, { key: 'Enter', metaKey: true });
    expect(onSend).toHaveBeenCalledWith('keyboard submit');
  });

  it('submits on Ctrl+Enter', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={false} />);
    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'ctrl submit' } });
    fireEvent.keyDown(textarea, { key: 'Enter', ctrlKey: true });
    expect(onSend).toHaveBeenCalledWith('ctrl submit');
  });

  it('shows cancel button and generating text during streaming', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={true} />);
    expect(screen.getByText('Generating...')).toBeTruthy();
    expect(screen.getByLabelText('Cancel generation')).toBeTruthy();
  });

  it('calls onCancel when cancel button clicked', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={true} />);
    fireEvent.click(screen.getByLabelText('Cancel generation'));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('does not call onSend when streaming', () => {
    render(<QueryInput onSend={onSend} onCancel={onCancel} isStreaming={true} />);
    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'blocked' } });
    fireEvent.keyDown(textarea, { key: 'Enter', metaKey: true });
    expect(onSend).not.toHaveBeenCalled();
  });
});
