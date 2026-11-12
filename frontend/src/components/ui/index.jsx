import { useEffect, useId, useRef } from 'react'

function joinClasses(...values) {
  return values.filter(Boolean).join(' ')
}

function messagesFor(value) {
  if (!value) return []
  return Array.isArray(value) ? value : [value]
}

export function PageShell({ as: Component = 'main', className, children, ...props }) {
  return <Component className={joinClasses('ui-page-shell', className)} {...props}>{children}</Component>
}

export function SectionHeader({ eyebrow, title, titleId, description, actions, className, ...props }) {
  return <header className={joinClasses('ui-section-header', className)} {...props}>
    <div className="ui-section-header__copy">
      {eyebrow && <p className="ui-section-header__eyebrow">{eyebrow}</p>}
      <h1 className="ui-section-header__title" id={titleId}>{title}</h1>
      {description && <p className="ui-section-header__description">{description}</p>}
    </div>
    {actions && <div className="ui-section-header__actions">{actions}</div>}
  </header>
}

export function SurfaceCard({ as: Component = 'section', tone = 'default', className, children, ...props }) {
  return <Component className={joinClasses('ui-surface', `ui-surface--${tone}`, className)} {...props}>{children}</Component>
}

export function MetricCard({ label, value, detail, trend, trendTone = 'neutral', icon, className, ...props }) {
  return <article className={joinClasses('ui-surface', 'ui-metric-card', className)} {...props}>
    <div className="ui-metric-card__top"><span className="ui-metric-card__label">{label}</span>{icon && <span className="ui-metric-card__icon" aria-hidden="true">{icon}</span>}</div>
    <strong className="ui-metric-card__value">{value}</strong>
    {detail && <p className="ui-metric-card__detail">{detail}</p>}
    {trend && <p className={`ui-metric-card__trend ui-metric-card__trend--${trendTone}`}>{trend}</p>}
  </article>
}

export function Badge({ tone = 'neutral', children, className, ...props }) {
  return <span className={joinClasses('ui-badge', `ui-badge--${tone}`, className)} {...props}>{children}</span>
}

export function Button({ as: Component = 'button', variant = 'primary', size = 'md', loading = false, className, children, disabled, ...props }) {
  const stateProps = Component === 'button' ? { disabled: disabled || loading } : { 'aria-disabled': disabled || loading || undefined }
  return <Component className={joinClasses('ui-button', `ui-button--${variant}`, size !== 'md' && `ui-button--${size}`, className)} {...stateProps} aria-busy={loading || undefined} {...props}>
    {loading && <span className="ui-button__spinner" aria-hidden="true" />}
    {children}
  </Component>
}

export function TextField({ label, id, hint, error, required = false, className, ...props }) {
  const generatedId = useId()
  const fieldId = id || `field-${generatedId}`
  const messages = messagesFor(error)
  const hintId = hint ? `${fieldId}-hint` : undefined
  const errorId = messages.length > 0 ? `${fieldId}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return <div className={joinClasses('ui-field', className)}>
    <label className="ui-field__label" htmlFor={fieldId}>{label}{required && <span className="ui-field__required" aria-hidden="true"> *</span>}</label>
    <input className="ui-field__control" id={fieldId} required={required} aria-label={props['aria-label'] || label} aria-invalid={messages.length > 0} aria-describedby={describedBy} {...props} />
    {hint && messages.length === 0 && <span className="ui-field__hint" id={hintId}>{hint}</span>}
    {messages.length > 0 && <span className="ui-field__error" id={errorId}>{messages.map((message, index) => <span key={`${message}-${index}`}>{message}</span>)}</span>}
  </div>
}

export function SelectField({ label, id, hint, error, required = false, className, children, ...props }) {
  const generatedId = useId()
  const fieldId = id || `select-${generatedId}`
  const messages = messagesFor(error)
  const hintId = hint ? `${fieldId}-hint` : undefined
  const errorId = messages.length > 0 ? `${fieldId}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return <div className={joinClasses('ui-field', className)}>
    <label className="ui-field__label" htmlFor={fieldId}>{label}{required && <span className="ui-field__required" aria-hidden="true"> *</span>}</label>
    <select className="ui-field__control" id={fieldId} required={required} aria-label={props['aria-label'] || label} aria-invalid={messages.length > 0} aria-describedby={describedBy} {...props}>{children}</select>
    {hint && messages.length === 0 && <span className="ui-field__hint" id={hintId}>{hint}</span>}
    {messages.length > 0 && <span className="ui-field__error" id={errorId}>{messages.map((message, index) => <span key={`${message}-${index}`}>{message}</span>)}</span>}
  </div>
}

export function FilterBar({ children, className, ...props }) {
  return <form className={joinClasses('ui-surface', 'ui-filter-bar', className)} {...props}>{children}</form>
}

export function DataTable({ caption, columns, rows = [], rowKey, empty = 'Nenhum registro disponível.', className, ...props }) {
  return <div className="ui-table-wrap">
    <table className={joinClasses('ui-table', className)} {...props}>
      {caption && <caption>{caption}</caption>}
      <thead><tr>{columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}</tr></thead>
      <tbody>{rows.length === 0 ? <tr><td className="ui-table__empty" colSpan={columns.length}>{empty}</td></tr> : rows.map((row, index) => <tr key={rowKey ? rowKey(row, index) : index}>{columns.map((column) => <td key={column.key}>{column.render ? column.render(row, index) : row[column.key]}</td>)}</tr>)}</tbody>
    </table>
  </div>
}

export function Pagination({ page, totalPages, onPageChange, className }) {
  const currentPage = page + 1
  const safeTotalPages = Math.max(1, totalPages)
  return <nav className={joinClasses('ui-pagination', className)} aria-label="Paginação">
    <span>Página {currentPage} de {safeTotalPages}</span>
    <div className="ui-pagination__actions">
      <button className="ui-pagination__button" type="button" onClick={() => onPageChange(page - 1)} disabled={page <= 0}>Anterior</button>
      <button className="ui-pagination__button" type="button" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= safeTotalPages}>Próxima</button>
    </div>
  </nav>
}

export function StatusMessage({ kind = 'info', title, children, actionLabel, onAction, className, ...props }) {
  return <div className={joinClasses('ui-status-message', `ui-status-message--${kind}`, className)} role={kind === 'error' || kind === 'conflict' ? 'alert' : 'status'} {...props}>
    {title && <strong className="ui-status-message__title">{title}</strong>}
    <div className="ui-status-message__body">{children}</div>
    {onAction && <Button className="ui-status-message__action" variant="secondary" size="sm" type="button" onClick={onAction}>{actionLabel || 'Tentar novamente'}</Button>}
  </div>
}

export function AsyncState({ status, children, loadingMessage = 'Carregando…', emptyTitle = 'Nenhum dado disponível', emptyDescription, errorMessage = 'Tente novamente.', onRetry }) {
  if (status === 'loading') return <div className="ui-async-state" role="status" aria-live="polite"><span className="ui-async-state__spinner" aria-hidden="true" /><span>{loadingMessage}</span></div>
  if (status === 'empty') return <div className="ui-async-state" role="status"><strong className="ui-async-state__title">{emptyTitle}</strong>{emptyDescription && <p className="ui-async-state__description">{emptyDescription}</p>}</div>
  if (status === 'error') return <StatusMessage kind="error" title="Não foi possível carregar." actionLabel="Tentar novamente" onAction={onRetry}>{errorMessage}</StatusMessage>
  return children
}

export function MutationFeedback({ status = 'idle', successMessage = 'Alteração concluída.', errorMessage = 'Não foi possível concluir a alteração.', conflictMessage = 'O estado mudou no servidor. Atualize os dados antes de tentar novamente.', onRetry }) {
  if (status === 'pending') return <StatusMessage kind="info" title="Atualizando…" aria-live="polite">Aguarde a resposta oficial.</StatusMessage>
  if (status === 'success') return <StatusMessage kind="success" title="Alteração concluída." aria-live="polite">{successMessage}</StatusMessage>
  if (status === 'conflict') return <StatusMessage kind="conflict" title="Conflito na atualização." onAction={onRetry}>{conflictMessage}</StatusMessage>
  if (status === 'error') return <StatusMessage kind="error" title="Alteração não concluída." onAction={onRetry}>{errorMessage}</StatusMessage>
  return null
}

export function ConfirmDialog({ open, title, description, confirmLabel = 'Confirmar', cancelLabel = 'Cancelar', onConfirm, onClose, tone = 'primary', pending = false }) {
  const dialogRef = useRef(null)
  const previousFocus = useRef(null)
  const onCloseRef = useRef(onClose)
  const pendingRef = useRef(pending)
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => { onCloseRef.current = onClose; pendingRef.current = pending }, [onClose, pending])

  useEffect(() => {
    if (!open) return undefined
    previousFocus.current = document.activeElement
    dialogRef.current?.focus()
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !pendingRef.current) {
        event.preventDefault()
        onCloseRef.current?.()
        return
      }
      if (event.key !== 'Tab') return
      const focusable = [...dialogRef.current.querySelectorAll('button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])')]
      if (focusable.length === 0) {
        event.preventDefault()
        return
      }
      const first = focusable[0]
      const last = focusable.at(-1)
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      previousFocus.current?.focus?.()
    }
  }, [open])

  if (!open) return null
  return <div className="ui-confirm-dialog__backdrop" role="presentation">
    <section className="ui-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={description ? descriptionId : undefined} tabIndex="-1" ref={dialogRef}>
      <div className="ui-confirm-dialog__header"><h2 className="ui-confirm-dialog__title" id={titleId}>{title}</h2><button className="ui-confirm-dialog__close" type="button" aria-label="Fechar" onClick={onClose} disabled={pending}>×</button></div>
      {description && <p className="ui-confirm-dialog__description" id={descriptionId}>{description}</p>}
      <div className="ui-confirm-dialog__actions"><Button variant="secondary" type="button" onClick={onClose} disabled={pending}>{cancelLabel}</Button><Button variant={tone} type="button" onClick={onConfirm} loading={pending}>{confirmLabel}</Button></div>
    </section>
  </div>
}
