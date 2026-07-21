import { LucideIcon } from 'lucide-react';
import { PropsWithChildren, ReactNode } from 'react';

const ACCENTS = {
    cyan: 'text-[var(--mc-cyan-400)] bg-[var(--mc-cyan-50)]',
    purple: 'text-[var(--mc-purple-400)] bg-[var(--mc-purple-50)]',
    moss: 'text-[var(--mc-moss-400)] bg-[var(--mc-moss-50)]',
    ember: 'text-[var(--mc-ember-400)] bg-[var(--mc-ember-50)]',
} as const;

export default function Card({
    title,
    icon: Icon,
    accent = 'cyan',
    action,
    padded = false,
    interactive = false,
    className = '',
    children,
}: PropsWithChildren<{
    title?: ReactNode;
    icon?: LucideIcon;
    accent?: keyof typeof ACCENTS;
    action?: ReactNode;
    padded?: boolean;
    interactive?: boolean;
    className?: string;
}>) {
    return (
        <div
            className={`dash-card ${interactive ? 'dash-card-interactive' : ''} overflow-hidden ${className}`}
        >
            {title && (
                <div className="flex items-center justify-between gap-3 border-b border-[var(--mc-border)] px-4 py-3">
                    <div className="flex items-center gap-2.5">
                        {Icon && (
                            <span
                                className={`flex h-6 w-6 items-center justify-center rounded-[7px] ${ACCENTS[accent]}`}
                            >
                                <Icon size={13} strokeWidth={2} />
                            </span>
                        )}
                        <span className="font-display text-[14px] font-semibold">
                            {title}
                        </span>
                    </div>
                    {action}
                </div>
            )}
            <div className={padded ? 'p-4' : ''}>{children}</div>
        </div>
    );
}
