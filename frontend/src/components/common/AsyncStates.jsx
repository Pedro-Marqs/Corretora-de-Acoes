import { Button, ConfirmDialog, StatusMessage } from '../ui/index.jsx'

export function LoadingState({ message = 'Carregando…' }) { return <div className="common-state" role="status" aria-live="polite"><span className="state-spinner" aria-hidden="true" />{message}</div> }
export function EmptyState({ title = 'Nenhum dado disponível', description }) { return <div className="common-state empty-state"><strong>{title}</strong>{description && <p>{description}</p>}</div> }
export function ErrorState({ message, onRetry }) { return <div className="common-state error-state" role="alert"><strong>Não foi possível carregar.</strong><p>{message}</p>{onRetry && <Button className="secondary-button" variant="secondary" type="button" onClick={onRetry}>Tentar novamente</Button>}</div> }
export function Message({ children, kind = 'info' }) { return <StatusMessage className={`message message-${kind}`} kind={kind}>{children}</StatusMessage> }
export function ConflictState({ message, onRetry }) { return <StatusMessage kind="conflict" title="Conflito na atualização." onAction={onRetry}>{message}</StatusMessage> }
export { ConfirmDialog }
