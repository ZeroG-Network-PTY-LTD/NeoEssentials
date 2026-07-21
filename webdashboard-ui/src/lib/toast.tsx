import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { CheckCircle2, XCircle } from 'lucide-react';

interface Toast {
  id: number;
  message: string;
  isError: boolean;
}

interface ToastContextValue {
  /** Replaces the Laravel app's redirect-carried session flash — fired directly from each
   * mutation's own fetch response instead of a server-computed prop. */
  showToast: (message: string, isError?: boolean) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, isError = false) => {
    const id = nextId++;
    setToasts((prev) => [...prev, { id, message, isError }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 5000);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`flex items-center gap-2 px-4 py-3 rounded-[var(--radius-lg)] border text-[13px] shadow-lg ${
              t.isError
                ? 'bg-[var(--mc-ember-50)] border-[var(--mc-ember-400)] text-[var(--mc-ember-500)]'
                : 'bg-[var(--mc-moss-50)] border-[var(--mc-moss-400,var(--mc-moss-500))] text-[var(--mc-moss-500)]'
            }`}
          >
            {t.isError ? <XCircle size={16} /> : <CheckCircle2 size={16} />}
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast() must be used inside <ToastProvider>');
  return ctx;
}
