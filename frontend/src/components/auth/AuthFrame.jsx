import { Badge } from '../ui/index.jsx'

export default function AuthFrame({ title, description, admin = false, size = 'default', children }) {
  return <main className={`auth-page ui-page-shell auth-frame-page${admin ? ' auth-frame-page--admin' : ''}${size === 'wide' ? ' auth-frame-page--wide' : ''}`}>
    <div className="auth-frame">
      <section className="auth-brand-panel" aria-labelledby="auth-brand-title">
        <a className="auth-brand" href="/" aria-label="Carteira Clara — início">
          <span className="auth-brand__mark" aria-hidden="true">C</span>
          <span>Carteira Clara</span>
        </a>
        <div className="auth-brand__copy">
          <p className="ui-section-header__eyebrow">{admin ? 'Área restrita' : 'Simulação de investimentos'}</p>
          <h1 id="auth-brand-title">{title}</h1>
          <p>{description}</p>
        </div>
        <div className="auth-brand__footer">
          <Badge tone={admin ? 'info' : 'success'}>{admin ? 'Contexto administrativo' : 'Ambiente acadêmico'}</Badge>
          {!admin && <span className="auth-brand__balance"><strong>R$ 10.000,00</strong> de saldo inicial para sua simulação</span>}
          <span>{admin ? 'Sessão independente e protegida.' : 'Nenhuma operação envolve dinheiro real.'}</span>
        </div>
      </section>
      {children}
    </div>
  </main>
}
