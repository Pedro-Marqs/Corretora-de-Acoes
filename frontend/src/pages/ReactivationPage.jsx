import { cloneElement, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { checkReactivation, reactivateAccount } from '../api/accounts.js'
import { ErrorState } from '../components/common/AsyncStates.jsx'

function formatCpf(value) {
  return value.replace(/\D/g, '').slice(0, 11).replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3').replace(/\.(\d{3})(\d)/, '.$1-$2')
}

export default function ReactivationPage() {
  const navigate = useNavigate()
  const locks = useRef({ check: false, reactivate: false })
  const [cpf, setCpf] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [technical, setTechnical] = useState(null)
  const [result, setResult] = useState(null)
  const [pending, setPending] = useState({ check: false, reactivate: false })

  function clearFeedback() {
    setFieldErrors({})
    setMessage('')
    setTechnical(null)
  }

  function showError(error, retry) {
    if (!error.status || error.status >= 500) {
      setTechnical({ message: error.message, retry })
    } else {
      setFieldErrors(error.fieldErrors ?? {})
      setMessage(error.message)
    }
  }

  async function performCheck() {
    if (locks.current.check) return
    if (!cpf.trim()) {
      setFieldErrors({ cpf: ['CPF é obrigatório.'] })
      setMessage('Informe o CPF para consultar a reativação.')
      return
    }

    locks.current.check = true
    setPending((current) => ({ ...current, check: true }))
    clearFeedback()
    try {
      setResult(await checkReactivation(cpf))
    } catch (error) {
      showError(error, performCheck)
    } finally {
      locks.current.check = false
      setPending((current) => ({ ...current, check: false }))
    }
  }

  async function submitCheck(event) {
    event.preventDefault()
    await performCheck()
  }

  async function performReactivation() {
    if (locks.current.reactivate || !result?.reactivationAvailable) return
    locks.current.reactivate = true
    setPending((current) => ({ ...current, reactivate: true }))
    clearFeedback()
    try {
      await reactivateAccount(cpf)
      navigate('/login', { replace: true, state: { message: 'Conta reativada. Entre com seu e-mail e senha.' } })
    } catch (error) {
      showError(error, performReactivation)
    } finally {
      locks.current.reactivate = false
      setPending((current) => ({ ...current, reactivate: false }))
    }
  }

  function updateCpf(event) {
    setCpf(formatCpf(event.target.value))
    setResult(null)
    clearFeedback()
  }

  function createIndependentAccount() {
    navigate('/cadastro', { state: { fromReactivation: true, cpf } })
  }

  return (
    <main className="auth-page reactivation-page">
      <section className="reactivation-card" aria-labelledby="reactivation-title">
        <a className="brand brand-dark" href="/" aria-label="Carteira Clara — início"><span className="brand-mark" aria-hidden="true">C</span><span>Carteira Clara</span></a>
        <header className="form-header reactivation-header">
          <p className="eyebrow">Ciclo da conta</p>
          <h1 id="reactivation-title">Reative sua conta.</h1>
          <p>Consulte um CPF para recuperar uma conta inativa ou criar uma nova conta independente.</p>
        </header>
        <div className="risk-notice">
          <strong>Limitação desta versão acadêmica</strong>
          <p>A reativação não solicita comprovação adicional de identidade. Ela não cria uma sessão: após concluir, será necessário entrar normalmente.</p>
        </div>
        {technical && <ErrorState message={technical.message} onRetry={technical.retry} />}
        {message && <div className="error-banner" role="alert">{message}</div>}

        {!result && (
          <form onSubmit={submitCheck} noValidate>
            <ReactivationField label="CPF" id="reactivationCpf" errors={fieldErrors.cpf}>
              <input id="reactivationCpf" type="text" inputMode="numeric" autoComplete="off" required value={cpf} onChange={updateCpf} placeholder="000.000.000-00" />
            </ReactivationField>
            <button className="primary-button" type="submit" disabled={pending.check}>{pending.check ? 'Consultando…' : 'Consultar possibilidade'}</button>
          </form>
        )}

        {result?.reactivationAvailable && (
          <div className="reactivation-result">
            <div>
              <p className="eyebrow">Conta encontrada</p>
              <h2>Escolha como continuar</h2>
              <p role="status">Você pode reativar a conta preservada ou começar novamente com uma conta distinta.</p>
            </div>
            <div className="reactivation-options">
              <article className="option-card">
                <h3>Reativar conta existente</h3>
                <p>Restaura o acesso aos dados preservados. Depois, entre normalmente com suas credenciais.</p>
                <button className="primary-button" type="button" onClick={performReactivation} disabled={pending.reactivate}>{pending.reactivate ? 'Reativando…' : 'Reativar conta'}</button>
              </article>
              <article className="option-card option-card-secondary">
                <h3>Criar uma nova conta</h3>
                <p>A nova conta será independente, começará com R$ 10.000,00 e a conta anterior permanecerá inacessível.</p>
                <button className="secondary-button" type="button" onClick={createIndependentAccount} disabled={pending.reactivate}>Criar nova conta</button>
              </article>
            </div>
            <button className="text-button" type="button" onClick={() => setResult(null)} disabled={pending.reactivate}>Consultar outro CPF</button>
          </div>
        )}

        {result && !result.reactivationAvailable && (
          <div className="reactivation-result">
            <h2>Reativação indisponível</h2>
            <p role="status">Não foi encontrada uma conta elegível para este CPF.</p>
            <button className="secondary-button" type="button" onClick={() => setResult(null)}>Consultar outro CPF</button>
          </div>
        )}

        <button className="text-button" type="button" onClick={() => navigate('/login')}>Voltar para o login</button>
      </section>
    </main>
  )
}

function ReactivationField({ label, id, errors, children }) {
  const errorId = `${id}-error`
  return <div className="form-field"><label htmlFor={id}>{label} <span aria-hidden="true">*</span><span className="sr-only"> obrigatório</span></label>{cloneElement(children, { 'aria-invalid': Boolean(errors), 'aria-describedby': errors ? errorId : undefined })}{errors && <span className="field-error" id={errorId}>{errors.map((item) => <span key={item}>{item}</span>)}</span>}</div>
}
