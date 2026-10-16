import { Wrench } from 'lucide-react';
import { cn } from '../lib/utils';

export default function SplashScreen({ hiding }: { hiding: boolean }) {
  return (
    <div
      className={cn(
        'fixed inset-0 z-[100] flex items-center justify-center overflow-hidden bg-slate-950 transition-all duration-700 ease-out',
        hiding && 'pointer-events-none scale-105 opacity-0'
      )}
    >
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br from-blue-950 via-slate-900 to-slate-950" />
        <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-blue-600/20 blur-3xl animate-float" />
        <div className="absolute -bottom-24 -right-24 h-96 w-96 rounded-full bg-indigo-600/20 blur-3xl" />
        <div className="absolute right-1/3 top-1/4 h-64 w-64 rounded-full bg-sky-500/10 blur-3xl" />
      </div>

      <div className="relative flex flex-col items-center">
        <div className="relative flex h-24 w-24 items-center justify-center rounded-[1.75rem] bg-gradient-to-br from-blue-500 to-indigo-600 shadow-2xl shadow-blue-600/40 ring-1 ring-white/20">
          <span className="absolute -inset-2 animate-spin rounded-[2rem] border-2 border-t-transparent border-r-blue-400/70 border-b-blue-300/50 border-l-transparent" />
          <Wrench className="h-11 w-11 text-white" strokeWidth={2.2} />
        </div>

        <h1 className="mt-9 text-4xl font-extrabold tracking-tight text-white">
          SIGTM
        </h1>
        <p className="mt-1.5 text-[11px] font-semibold uppercase tracking-[0.35em] text-blue-200/70">
          Taller Mecánico
        </p>

        <div className="mt-9 flex items-center gap-1.5">
          {[0, 1, 2].map((i) => (
            <span
              key={i}
              className="h-2 w-2 rounded-full bg-blue-400 animate-bounce"
              style={{ animationDelay: `${i * 150}ms` }}
            />
          ))}
        </div>
        <p className="mt-4 text-xs text-slate-500">Iniciando sistema…</p>
      </div>
    </div>
  );
}