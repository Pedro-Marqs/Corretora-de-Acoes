const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 2 })
const brasilia = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' })

export function formatCurrency(value) {
  const number = Number(value)
  return Number.isFinite(number) ? brl.format(number) : 'Valor indisponível'
}

export function formatCurrencyInput(value) {
  const digits = String(value ?? '').replace(/\D/g, '').slice(0, 15)
  if (!digits) return ''
  const padded = digits.padStart(3, '0')
  const integer = padded.slice(0, -2).replace(/^0+(?=\d)/, '')
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `R$ ${integer},${padded.slice(-2)}`
}

export function currencyInputToDecimal(value) {
  const digits = String(value ?? '').replace(/\D/g, '')
  if (!digits) return ''
  const padded = digits.padStart(3, '0')
  return `${padded.slice(0, -2).replace(/^0+(?=\d)/, '')}.${padded.slice(-2)}`
}

export function formatBrasiliaDateTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Data indisponível' : brasilia.format(date)
}
