import { cloneElement, useEffect, useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { changeEmail, changePassword, deleteAccount, getCurrentAccount } from '../api/accounts.js'
import { useAuth } from '../context/auth-context.js'
import { ErrorState } from '../components/common/AsyncStates.jsx'

const emptyEmail = { newEmail: '', currentPassword: '' }
const emptyPassword = { currentPassword: '', newPassword: '' }
const emptyDeletion = { email: '', password: '', confirmation: '' }

export default function AccountPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const locks = useRef({ email: false, password: false, deletion: false })
  const deleteTriggerRef = useRef(null)
  const [email, setEmail] = useState(emptyEmail)
  const [password, setPassword] = useState(emptyPassword)
  const [deletion, setDeletion] = useState(emptyDeletion)
  const [emailErrors, setEmailErrors] = useState({})
  const [passwordErrors, setPasswordErrors] = useState({})
  const [deletionErrors, setDeletionErrors] = useState({})
  const [emailMessage, setEmailMessage] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [deletionMessage, setDeletionMessage] = useState('')
  const [pending, setPending] = useState({ email: false, password: false, deletion: false })
  const [technical, setTechnical] = useState(null)
  const [expandedCredential, setExpandedCredential] = useState(null)
  const [deletionOpen, setDeletionOpen] = useState(false)

  async function handle401(error, kind) {
    setTechnical(null)
    try {
      await getCurrentAccount()
      showFunctional(error, kind)
    } catch (sessionError) {
      if (sessionError.status === 401) {
        auth.clear()
        navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } })
      } else {
        setTechnical({ message: sessionError.message, retry: () => handle401(error, kind) })
      }
    }
  }

  function showFunctional(error, kind) {
    if (kind === 'email') {
      setEmailErrors(error.fieldErrors ?? {})
      setEmailMessage(error.message)
    } else if (kind === 'password') {
      setPasswordErrors(error.fieldErrors ?? {})
      setPasswordMessage(error.message)
    } else {
      setDeletionErrors(error.fieldErrors ?? {})
      setDeletionMessage(error.message)
    }
  }

  function clearFeedback(kind) {
    setTechnical(null)
    if (kind === 'email') {
      setEmailErrors({})
      setEmailMessage('')
    } else if (kind === 'password') {
      setPasswordErrors({})
      setPasswordMessage('')
    } else {
      setDeletionErrors({})
      setDeletionMessage('')
    }
  }

  async function submitCredential(kind, event) {
    event.preventDefault()
    if (locks.current[kind]) return
    locks.current[kind] = true
    setPending((current) => ({ ...current, [kind]: true }))
    clearFeedback(kind)
    try {
      await (kind === 'email' ? changeEmail(email) : changePassword(password))
      setEmail(emptyEmail)
      setPassword(emptyPassword)
      auth.clear()
      navigate('/login', { replace: true, state: { message: 'Dados alterados. Entre novamente.' } })
    } catch (error) {
      if (error.status === 401) await handle401(error, kind)
      else showFunctional(error, kind)
    } finally {
      locks.current[kind] = false
      setPending((current) => ({ ...current, [kind]: false }))
    }
  }

  function validateDeletion() {
    const errors = {}
    if (!deletion.email.trim()) errors.email = ['E-mail atual é obrigatório.']
    if (!deletion.password) errors.password = ['Senha atual é obrigatória.']
    if (deletion.confirmation !== 'Excluir') {
      errors.confirmation = [deletion.confirmation ? 'A confirmação deve ser exatamente "Excluir".' : 'Digite "Excluir" para confirmar.']
    }
    return errors
  }

  async function submitDeletion(event) {
    event.preventDefault()
    if (locks.current.deletion) return
    clearFeedback('deletion')
    const errors = validateDeletion()
    if (Object.keys(errors).length) {
      setDeletionErrors(errors)
      setDeletionMessage('Revise os campos necessários para excluir a conta.')
      return
    }

    locks.current.deletion = true
    setPending((current) => ({ ...current, deletion: true }))
    try {
      await deleteAccount(deletion)
      setDeletion(emptyDeletion)
      flushSync(() => auth.clear())
      navigate('/login', { replace: true, state: { message: 'Conta excluída. Seus dados permanecem preservados para uma possível reativação.' } })
    } catch (error) {
      if (error.status === 401) await handle401(error, 'deletion')
      else showFunctional(error, 'deletion')
    } finally {
      locks.current.deletion = false
      setPending((current) => ({ ...current, deletion: false }))
    }
  }

  function updateDeletion(event) {
    const { name, value } = event.target
    setDeletion((current) => ({ ...current, [name]: value }))
    setDeletionErrors((current) => ({ ...current, [name]: undefined }))
    setDeletionMessage('')
  }

  return (
    <main className="account-page">
      <header>
        <p className="eyebrow">Configurações</p>
        <h1>Minha conta</h1>
        <p>Nome e CPF não podem ser alterados.</p>
      </header>
      <section className="profile-card">
        <dl>
          <div><dt>Nome</dt><dd>{auth.account.name}</dd></div>
          <div><dt>CPF</dt><dd>{auth.account.cpf}</dd></div>
          <div><dt>E-mail</dt><dd>{auth.account.email}</dd></div>
        </dl>
      </section>
      {technical && <ErrorState message={technical.message} onRetry={technical.retry} />}
      <div className="settings-list">
        <CredentialForm id="email-settings" title="Alterar e-mail" expanded={expandedCredential === 'email'} onToggle={() => setExpandedCredential((current) => current === 'email' ? null : 'email')} message={emailMessage} onSubmit={(event) => submitCredential('email', event)} pending={pending.email}>
          <Field label="Novo e-mail" id="newEmail" errors={emailErrors.newEmail}>
            <input id="newEmail" type="email" required value={email.newEmail} onChange={(event) => setEmail({ ...email, newEmail: event.target.value })} />
          </Field>
          <Field label="Senha atual" id="emailPassword" errors={emailErrors.currentPassword}>
            <input id="emailPassword" type="password" required value={email.currentPassword} onChange={(event) => setEmail({ ...email, currentPassword: event.target.value })} />
          </Field>
        </CredentialForm>
        <CredentialForm id="password-settings" title="Alterar senha" expanded={expandedCredential === 'password'} onToggle={() => setExpandedCredential((current) => current === 'password' ? null : 'password')} message={passwordMessage} onSubmit={(event) => submitCredential('password', event)} pending={pending.password}>
          <Field label="Senha atual" id="passwordCurrent" errors={passwordErrors.currentPassword}>
            <input id="passwordCurrent" type="password" required value={password.currentPassword} onChange={(event) => setPassword({ ...password, currentPassword: event.target.value })} />
          </Field>
          <Field label="Nova senha" id="newPassword" errors={passwordErrors.newPassword}>
            <input id="newPassword" type="password" required value={password.newPassword} onChange={(event) => setPassword({ ...password, newPassword: event.target.value })} />
          </Field>
          <p className="field-hint">Mínimo de 8 caracteres, com maiúscula, minúscula, número e símbolo.</p>
        </CredentialForm>
      </div>
      <section className="danger-zone" aria-labelledby="delete-account-title">
        <div className="danger-zone-copy">
          <p className="eyebrow">Zona de atenção</p>
          <h2 id="delete-account-title">Excluir minha conta</h2>
          <p>A exclusão é lógica: sua sessão será encerrada e seus dados permanecerão preservados caso você decida reativar a conta.</p>
        </div>
        <button ref={deleteTriggerRef} className="danger-button danger-trigger" type="button" onClick={() => setDeletionOpen(true)}>Excluir minha conta</button>
      </section>
      {deletionOpen && <DeletionModal deletion={deletion} errors={deletionErrors} message={deletionMessage} pending={pending.deletion} onChange={updateDeletion} onClose={() => setDeletionOpen(false)} onSubmit={submitDeletion} returnFocusRef={deleteTriggerRef} />}
    </main>
  )
}

function CredentialForm({ id, title, expanded, onToggle, message, onSubmit, pending, children }) {
  return <section className="settings-card collapsible-settings">
    <button className="settings-toggle" type="button" aria-expanded={expanded} aria-controls={id} onClick={onToggle}>
      <span>{title}</span><span aria-hidden="true">{expanded ? '−' : '+'}</span>
    </button>
    {expanded && <form id={id} className="settings-content" onSubmit={onSubmit} noValidate>{message && <div className="error-banner" role="alert">{message}</div>}{children}<button className="primary-button" disabled={pending}>{pending ? 'Salvando…' : 'Salvar alteração'}</button></form>}
  </section>
}

function DeletionModal({ deletion, errors, message, pending, onChange, onClose, onSubmit, returnFocusRef }) {
  const cancelRef = useRef(null)
  const submitRef = useRef(null)

  useEffect(() => {
    const returnFocus = returnFocusRef.current
    cancelRef.current?.focus()
    return () => returnFocus?.focus()
  }, [returnFocusRef])

  function close() {
    if (pending) return
    onClose()
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      close()
    } else if (event.key === 'Tab' && !pending) {
      if (event.shiftKey && document.activeElement === cancelRef.current) {
        event.preventDefault()
        submitRef.current?.focus()
      } else if (!event.shiftKey && document.activeElement === submitRef.current) {
        event.preventDefault()
        cancelRef.current?.focus()
      }
    }
  }

  return <section className="account-modal" role="dialog" aria-modal="true" aria-labelledby="delete-modal-title" onKeyDown={handleKeyDown}><div className="account-modal-card"><h2 id="delete-modal-title">Excluir minha conta</h2><p>Confirme seus dados para excluir a conta.</p><form onSubmit={onSubmit} noValidate>
    {message && <div className="error-banner" role="alert">{message}</div>}
    <Field label="E-mail atual" id="deleteEmail" errors={errors.email}><input id="deleteEmail" name="email" type="email" autoComplete="email" required value={deletion.email} onChange={onChange} /></Field>
    <Field label="Senha atual" id="deletePassword" errors={errors.password}><input id="deletePassword" name="password" type="password" autoComplete="current-password" required value={deletion.password} onChange={onChange} /></Field>
    <Field label={'Digite "Excluir"'} id="deleteConfirmation" errors={errors.confirmation}><input id="deleteConfirmation" name="confirmation" type="text" autoComplete="off" required value={deletion.confirmation} onChange={onChange} /></Field>
    <div className="modal-actions"><button ref={cancelRef} className="secondary-button" type="button" onClick={close} disabled={pending}>Cancelar</button><button ref={submitRef} className="danger-button" type="submit" disabled={pending}>{pending ? 'Excluindo conta…' : 'Confirmar exclusão'}</button></div>
  </form></div></section>
}

function Field({ label, id, errors, children }) {
  const errorId = `${id}-error`
  return <div className="form-field"><label htmlFor={id}>{label} <span aria-hidden="true">*</span><span className="sr-only"> obrigatório</span></label>{cloneElement(children, { 'aria-invalid': Boolean(errors), 'aria-describedby': errors ? errorId : undefined })}{errors && <span className="field-error" id={errorId}>{errors.map((item) => <span key={item}>{item}</span>)}</span>}</div>
}
