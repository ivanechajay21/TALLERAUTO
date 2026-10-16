import type { ReactNode } from 'react';
import { Inbox } from 'lucide-react';
import { cn } from '../../lib/utils';
import usePagination from '../../hooks/usePagination';
import Pagination from './Pagination';

export interface ResponsiveColumn<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
  hideOnMobile?: boolean;
  hideOnDesktop?: boolean;
}

interface ResponsiveTableProps<T> {
  columns: ResponsiveColumn<T>[];
  rows: T[];
  keyField: (row: T) => string | number;
  emptyMessage?: string;
  cardTitle?: (row: T) => ReactNode;
  cardFooter?: (row: T) => ReactNode;
  mobileClassName?: string;
  /** Si es > 0, activa la paginación en el pie de la tabla. */
  pageSize?: number;
  /** Etiqueta del contador de paginación (p. ej. «productos»). */
  itemLabel?: string;
}

export default function ResponsiveTable<T>({
  columns,
  rows,
  keyField,
  emptyMessage = 'No hay registros.',
  cardTitle,
  cardFooter,
  mobileClassName,
  pageSize = 0,
  itemLabel,
}: ResponsiveTableProps<T>) {
  const desktopColumns = columns.filter((c) => !c.hideOnDesktop);
  const cardColumns = columns.filter((c) => !c.hideOnMobile);

  const paginacion = usePagination(rows, pageSize);
  const visibles = pageSize > 0 ? paginacion.pageItems : rows;

  const renderTitle = (row: T) => (cardTitle ? cardTitle(row) : cardColumns[0]?.render(row));

  return (
    <>
      {/* Tabla escritorio */}
      <div className="hidden overflow-x-auto md:block">
        <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700">
          <thead className="bg-slate-50/50 dark:bg-slate-800/50">
            <tr>
              {desktopColumns.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  className={cn(
                    'px-4 py-3.5 text-left text-sm font-semibold text-slate-900 sm:px-6 dark:text-slate-200',
                    col.className
                  )}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-700 dark:bg-slate-900">
            {visibles.map((row) => (
              <tr
                key={keyField(row)}
                className="transition-colors hover:bg-slate-50/70 dark:hover:bg-slate-800/40"
              >
                {desktopColumns.map((col) => (
                  <td
                    key={col.key}
                    className={cn(
                      'whitespace-nowrap px-4 py-4 text-sm sm:px-6',
                      col.className,
                      col.hideOnDesktop && 'hidden'
                    )}
                  >
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Tarjetas móvil */}
      <div className={cn('grid gap-3 p-3 sm:p-4 md:hidden', mobileClassName)}>
        {visibles.map((row) => (
          <div
            key={keyField(row)}
            className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900"
          >
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">{renderTitle(row)}</div>
              {cardFooter && <div className="shrink-0">{cardFooter(row)}</div>}
            </div>
            <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 border-t border-slate-100 pt-3 dark:border-slate-800">
              {cardColumns.slice(1).map((col) => (
                <div key={col.key}>
                  <dt className="text-[11px] font-medium uppercase tracking-wide text-slate-400 dark:text-slate-500">
                    {col.header}
                  </dt>
                  <dd className="mt-0.5 text-sm text-slate-700 dark:text-slate-300">{col.render(row)}</dd>
                </div>
              ))}
            </dl>
          </div>
        ))}
      </div>

      {rows.length === 0 && (
        <div className="flex flex-col items-center justify-center px-4 py-14 text-center md:hidden">
          <Inbox size={28} className="mb-2 text-slate-300 dark:text-slate-600" />
          <p className="text-sm text-slate-400 dark:text-slate-500">{emptyMessage}</p>
        </div>
      )}
      {rows.length === 0 && (
        <div className="hidden px-4 py-14 text-center md:block">
          <Inbox size={28} className="mx-auto mb-2 text-slate-300 dark:text-slate-600" />
          <p className="text-sm text-slate-400 dark:text-slate-500">{emptyMessage}</p>
        </div>
      )}

      {pageSize > 0 && rows.length > 0 && (
        <Pagination
          page={paginacion.page}
          pageCount={paginacion.pageCount}
          onChange={paginacion.setPage}
          total={paginacion.total}
          from={paginacion.from}
          to={paginacion.to}
          itemLabel={itemLabel ?? 'registros'}
        />
      )}
    </>
  );
}
