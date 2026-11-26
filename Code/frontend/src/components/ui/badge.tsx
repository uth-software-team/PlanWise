import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors',
  {
    variants: {
      variant: {
        proposed: 'bg-amber-100 text-amber-800 border border-amber-300',
        confirmed: 'bg-emerald-100 text-emerald-800 border border-emerald-300',
        danger: 'bg-rose-100 text-rose-800 border border-rose-300',
        neutral: 'bg-slate-100 text-slate-700 border border-slate-200',
      },
    },
    defaultVariants: {
      variant: 'neutral',
    },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}
