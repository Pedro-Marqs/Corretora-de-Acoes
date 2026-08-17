const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 2 })
const brasilia = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' })

export function formatCurrency(value) {
  const number = Number(value)
  return Number.isFinite(number) ? brl.format(number) : 'Valor indisponível'
}

export function formatBrasiliaDateTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Data indisponível' : brasilia.format(date)
}
