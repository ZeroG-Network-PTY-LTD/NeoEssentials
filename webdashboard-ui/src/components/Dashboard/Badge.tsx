import { PropsWithChildren } from 'react';

const VARIANTS = {
    cyan: 'text-[var(--mc-cyan-400)] bg-[var(--mc-cyan-50)]',
    purple: 'text-[var(--mc-purple-400)] bg-[var(--mc-purple-50)]',
    moss: 'text-[var(--mc-moss-400)] bg-[var(--mc-moss-50)]',
    ember: 'text-[var(--mc-ember-400)] bg-[var(--mc-ember-50)]',
    neutral: 'text-[var(--mc-text-secondary)] bg-[var(--mc-bg-surface-raised)]',
} as const;

export default function Badge({
    variant = 'neutral',
    dot = false,
    className = '',
    children,
}: PropsWithChildren<{
    variant?: keyof typeof VARIANTS;
    dot?: boolean;
    className?: string;
}>) {
    return (
        <span
            className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-[11px] font-medium ${VARIANTS[variant]} ${className}`}
        >
            {dot && (
                <span className="pulse-dot relative h-1.5 w-1.5 shrink-0 rounded-full bg-current" />
            )}
            {children}
        </span>
    );
}
