import { LucideIcon } from 'lucide-react';
import { ReactNode } from 'react';

export default function PageHeading({
    title,
    icon: Icon,
    count,
    subtitle,
    action,
}: {
    title: string;
    icon?: LucideIcon;
    count?: number;
    subtitle?: string;
    action?: ReactNode;
}) {
    return (
        <div className="mb-6 flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
                {Icon && (
                    <span className="flex h-9 w-9 items-center justify-center rounded-[10px] bg-gradient-to-br from-[var(--mc-cyan-50)] to-[var(--mc-purple-50)] text-[var(--mc-cyan-400)]">
                        <Icon size={17} strokeWidth={2} />
                    </span>
                )}
                <div>
                    <h1 className="font-display text-[20px] font-semibold leading-tight">
                        {title}
                        {count !== undefined && (
                            <span className="ml-2 font-data text-[15px] font-normal text-[var(--mc-text-muted)]">
                                ({count})
                            </span>
                        )}
                    </h1>
                    {subtitle && (
                        <p className="mt-0.5 text-[12.5px] text-[var(--mc-text-secondary)]">
                            {subtitle}
                        </p>
                    )}
                </div>
            </div>
            {action}
        </div>
    );
}
