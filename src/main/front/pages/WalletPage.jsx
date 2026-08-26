import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deposit, getWalletBalance } from '../api/wallet.js'
import { ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatCurrency } from '../utils/formatters.js'

export default function WalletPage() {
  const { clear: clearAuth } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState({ status: 'loading', balance: null, message: '' })
  const [amount, setAmount] = useState('')
  const [amountError, setAmountError] = useState('')
  const [confirmationAmount, setConfirmationAmount] = useState(null)
  const depositLock = useRef(false)
  const [depositState, setDepositState] = useState({ pending: false, message: '', error: '' })

  const loadBalance = useCallback(async () => {
    setState({ status: 'loading', balance: null, message: '' })
    try {
      const wallet = await getWalletBalance()
      setState({ status: 'ready', balance: wallet.balance, message: '' })
    } catch (error) {
      if (error.status === 401) {
        setState({ status: 'unauthenticated', balance: null, message: '' })
        clearAuth()
        navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } })
      } else {
        setState({ status: 'error', balance: null, message: error.message })
      }
    }
  }, [clearAuth, navigate])

  useEffect(() => {
    let active = true
    async function initialLoad() {
      try {
        const wallet = await getWalletBalance()
        if (active) setState({ status: 'ready', balance: wallet.balance, message: '' })
      } catch (error) {
        if (!active) return
        if (error.status === 401) {
          setState({ status: 'unauthenticated', balance: null, message: '' })
          clearAuth()
          navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } })
        } else {
          setState({ status: 'error', balance: null, message: error.message })
        }
      }
    }
    initialLoad()
    return () => { active = false }
  }, [clearAuth, navigate])

  function requestDeposit(event) {
    event.preventDefault()
    if (depositLock.current) return
    const normalized = amount.trim().replace(',', '.')
    const numericAmount = Number(normalized)
    let message = ''
    if (!normalized) message = 'Informe o valor do aporte.'
    else if (!Number.isFinite(numericAmount)) message = 'Informe um valor numérico válido.'
    else if (numericAmount <= 0) message = 'O valor do aporte deve ser positivo.'
    else if (numericAmount < 10) message = 'O aporte mínimo é R$ 10,00.'
    if (message) {
      setAmountError(message)
      setConfirmationAmount(null)
      return
    }
    setAmountError('')
    setDepositState({ pending: false, message: '', error: '' })
    setConfirmationAmount(normalized)
  }

  async function confirmDeposit() {
    if (depositLock.current || !confirmationAmount) return
    depositLock.current = true
    setDepositState({ pending: true, message: '', error: '' })
    try {
      const wallet = await deposit(confirmationAmount)
      setState({ status: 'ready', balance: wallet.balance, message: '' })
      setAmount('')
      setConfirmationAmount(null)
      setDepositState({ pending: false, message: 'Aporte realizado. O saldo foi atualizado.', error: '' })
    } catch (error) {
      if (error.status === 401) {
        setState({ status: 'unauthenticated', balance: null, message: '' })
        clearAuth()
        navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } })
      } else {
        setAmountError(error.fieldErrors?.amount?.[0] ?? '')
        setDepositState({ pending: false, message: '', error: error.message })
      }
    } finally {
      depositLock.current = false
    }
  }

  if (state.status === 'loading') return <main className="wallet-page"><LoadingState message="Carregando saldo…" /></main>
  if (state.status === 'error') return <main className="wallet-page"><ErrorState message={state.message} onRetry={loadBalance} /></main>
  if (state.status !== 'ready') return null

  return (
    <main className="wallet-page">
      <header className="wallet-heading">
        <p className="eyebrow">Área financeira</p>
        <h1>Minha carteira</h1>
        <p>Consulte o saldo compartilhado entre todas as suas corretoras.</p>
      </header>
      <section className="wallet-balance" aria-labelledby="wallet-balance-title">
        <div>
          <p id="wallet-balance-title">Saldo disponível</p>
          <strong>{formatCurrency(state.balance)}</strong>
        </div>
        <span aria-hidden="true">R$</span>
      </section>
      <section className="deposit-card" aria-labelledby="deposit-title">
        <div>
          <p className="eyebrow">Adicionar saldo</p>
          <h2 id="deposit-title">Realizar aporte</h2>
          <p>O aporte é fictício e não representa lucro ou rendimento.</p>
        </div>
        <form onSubmit={requestDeposit} noValidate>
          {depositState.message && <Message kind="success">{depositState.message}</Message>}
          <div className="form-field">
            <label htmlFor="depositAmount">Valor do aporte <span aria-hidden="true">*</span></label>
            <input id="depositAmount" name="amount" type="text" inputMode="decimal" autoComplete="off" placeholder="0,00" value={amount} onChange={(event) => { setAmount(event.target.value); setAmountError('') }} aria-invalid={Boolean(amountError)} aria-describedby={amountError ? 'depositAmount-error' : 'depositAmount-hint'} />
            {amountError ? <span className="field-error" id="depositAmount-error">{amountError}</span> : <span className="field-hint" id="depositAmount-hint">Valor mínimo de R$ 10,00.</span>}
          </div>
          <button className="primary-button" type="submit" disabled={depositState.pending}>Continuar</button>
        </form>
      </section>
      {confirmationAmount && (
        <DepositConfirmation
          amount={confirmationAmount}
          error={depositState.error}
          pending={depositState.pending}
          onCancel={() => { setConfirmationAmount(null); setDepositState({ pending: false, message: '', error: '' }) }}
          onConfirm={confirmDeposit}
        />
      )}
    </main>
  )
}

function DepositConfirmation({ amount, error, pending, onCancel, onConfirm }) {
  const cancelRef = useRef(null)
  const confirmRef = useRef(null)

  useEffect(() => {
    const previousFocus = document.activeElement
    cancelRef.current?.focus()
    return () => previousFocus?.focus()
  }, [])

  function handleKeyDown(event) {
    if (event.key === 'Escape' && !pending) {
      event.preventDefault()
      onCancel()
      return
    }
    if (event.key !== 'Tab' || pending) return
    if (event.shiftKey && document.activeElement === cancelRef.current) {
      event.preventDefault()
      confirmRef.current?.focus()
    } else if (!event.shiftKey && document.activeElement === confirmRef.current) {
      event.preventDefault()
      cancelRef.current?.focus()
    }
  }

  return (
    <section className="deposit-confirmation" role="dialog" aria-modal="true" aria-labelledby="deposit-confirmation-title" onKeyDown={handleKeyDown}>
      <div>
        <h2 id="deposit-confirmation-title">Confirmar aporte</h2>
        <p>Deseja adicionar <strong>{formatCurrency(amount)}</strong> ao saldo da carteira?</p>
        {error && <Message kind="error">{error}</Message>}
        <div className="confirmation-actions">
          <button ref={cancelRef} className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button>
          <button ref={confirmRef} className="primary-button" type="button" onClick={onConfirm} disabled={pending}>{pending ? 'Enviando aporte…' : 'Confirmar aporte'}</button>
        </div>
      </div>
    </section>
  )
}
