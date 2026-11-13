import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { checkReactivation, reactivateAccount } from '../api/accounts.js'
import { ErrorState } from '../components/common/AsyncStates.jsx'
import AuthFrame from '../components/auth/AuthFrame.jsx'
import { Button, SectionHeader, StatusMessage, SurfaceCard, TextField } from '../components/ui/index.jsx'

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
    if (!error.status || error.status >= 500) setTechnical({ message: error.message, retry })
    else {
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
    try { setResult(await checkReactivation(cpf)) }
    catch (error) { showError(error, performCheck) }
    finally {
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
    } catch (error) { showError(error, performReactivation) }
    finally {
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

  return <AuthFrame title="Reative sua conta." description="Recupere uma conta inativa ou crie uma nova conta independente." size="wide">
    <SurfaceCard as="section" className="auth-form-surface reactivation-surface" aria-labelledby="reactivation-title">
      <SectionHeader titleId="reactivation-title" eyebrow="Ciclo da conta" title="Consulte sua conta" description="Informe o CPF para verificar as opções disponíveis." />
      <div className="risk-notice">
        <strong>Limitação desta versão acadêmica</strong>
        <p>A reativação não solicita comprovação adicional de identidade. Ela não cria uma sessão: após concluir, será necessário entrar normalmente.</p>
      </div>
      {technical && <ErrorState message={technical.message} onRetry={technical.retry} />}
      {message && <StatusMessage kind="error" title="Não foi possível continuar.">{message}</StatusMessage>}

      {!result && <form className="auth-form" onSubmit={submitCheck} noValidate>
        <TextField label="CPF" id="reactivationCpf" error={fieldErrors.cpf} type="text" inputMode="numeric" autoComplete="off" required value={cpf} onChange={updateCpf} placeholder="000.000.000-00" />
        <Button className="auth-submit" type="submit" disabled={pending.check}>{pending.check ? 'Consultando…' : 'Consultar possibilidade'}</Button>
      </form>}

      {result?.reactivationAvailable && <div className="reactivation-result">
        <div>
          <p className="ui-section-header__eyebrow">Conta encontrada</p>
          <h2>Escolha como continuar</h2>
          <p role="status">Você pode reativar a conta preservada ou começar novamente com uma conta distinta.</p>
        </div>
        <div className="reactivation-options">
          <article className="option-card">
            <h3>Reativar conta existente</h3>
            <p>Restaura o acesso aos dados preservados. Depois, entre normalmente com suas credenciais.</p>
            <Button className="auth-submit" type="button" onClick={performReactivation} disabled={pending.reactivate}>{pending.reactivate ? 'Reativando…' : 'Reativar conta'}</Button>
          </article>
          <article className="option-card option-card-secondary">
            <h3>Criar uma nova conta</h3>
            <p>A nova conta será independente, começará com R$ 10.000,00 e a conta anterior permanecerá inacessível.</p>
            <Button variant="secondary" type="button" onClick={createIndependentAccount} disabled={pending.reactivate}>Criar nova conta</Button>
          </article>
        </div>
        <Button className="auth-link" variant="ghost" type="button" onClick={() => setResult(null)} disabled={pending.reactivate}>Consultar outro CPF</Button>
      </div>}

      {result && !result.reactivationAvailable && <div className="reactivation-result">
        <h2>Reativação indisponível</h2>
        <p role="status">Não foi encontrada uma conta elegível para este CPF.</p>
        <Button variant="secondary" type="button" onClick={() => setResult(null)}>Consultar outro CPF</Button>
      </div>}

      <Button className="auth-link" variant="ghost" type="button" onClick={() => navigate('/login')}>Voltar para o login</Button>
    </SurfaceCard>
  </AuthFrame>
}
