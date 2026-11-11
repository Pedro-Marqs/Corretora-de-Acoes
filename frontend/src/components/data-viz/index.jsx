import { useId } from 'react'

function valueFrom(point, valueKey, getValue) {
  const value = getValue ? getValue(point) : point?.[valueKey]
  if (value === null || value === undefined || value === '') return null
  return Number.isFinite(Number(value)) ? Number(value) : null
}

function labelFrom(point, labelKey, formatLabel) {
  const raw = point?.[labelKey]
  return formatLabel ? formatLabel(raw, point) : String(raw ?? '')
}

export function ResponsiveLineChart({ data = [], label = 'Série', description, period, currency, valueKey = 'value', labelKey = 'label', getValue, formatValue = (value) => String(value), formatLabel, className }) {
  const titleId = useId()
  const descriptionId = useId()
  const points = data.map((point) => ({ point, value: valueFrom(point, valueKey, getValue), label: labelFrom(point, labelKey, formatLabel) })).filter((item) => item.value !== null)
  if (points.length === 0) return <div className={className} role="status">Dados indisponíveis para esta visualização.</div>

  const values = points.map((point) => point.value)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  const chartWidth = 640
  const chartHeight = 240
  const padding = 28
  const plotted = points.map((point, index) => ({ ...point, x: points.length === 1 ? chartWidth / 2 : padding + (index / (points.length - 1)) * (chartWidth - padding * 2), y: chartHeight - padding - ((point.value - min) / range) * (chartHeight - padding * 2) }))
  const path = plotted.map((point) => `${point.x},${point.y}`).join(' ')
  const chartDescription = [description, period && `Período: ${period}`, currency && `Moeda: ${currency}`].filter(Boolean).join('. ')

  return <div className={className}>
    <div className="ui-viz" role="img" aria-labelledby={titleId} aria-describedby={descriptionId}>
      <svg className="ui-viz__svg" viewBox={`0 0 ${chartWidth} ${chartHeight}`} aria-hidden="true">
        <title id={titleId}>{label}</title>
        <desc id={descriptionId}>{chartDescription || 'Visualização de dados oficiais.'}</desc>
        {[.25, .5, .75].map((ratio) => <line className="ui-viz__grid" key={ratio} x1={padding} x2={chartWidth - padding} y1={chartHeight * ratio} y2={chartHeight * ratio} />)}
        <polyline className="ui-viz__line" points={path} />
        {plotted.map((point) => <circle className="ui-viz__point" key={`${point.label}-${point.x}`} cx={point.x} cy={point.y} r="4" />)}
      </svg>
    </div>
    <details className="ui-viz__details"><summary>Consultar pontos em tabela</summary><div className="ui-table-wrap"><table className="ui-table"><caption>{label}{chartDescription ? ` — ${chartDescription}` : ''}</caption><thead><tr><th scope="col">Ponto</th><th scope="col">Valor</th></tr></thead><tbody>{points.map((point) => <tr key={`${point.label}-${point.value}`}><th scope="row">{point.label}</th><td>{formatValue(point.value, point.point)}</td></tr>)}</tbody></table></div></details>
  </div>
}

export function BarList({ data = [], label = 'Distribuição', labelKey = 'label', valueKey = 'value', getValue, formatValue = (value) => String(value), emptyMessage = 'Dados indisponíveis para esta visualização.', className }) {
  const rows = data.map((item) => ({ item, label: String(item?.[labelKey] ?? ''), value: valueFrom(item, valueKey, getValue) })).filter((item) => item.value !== null)
  if (rows.length === 0) return <div className={className} role="status">{emptyMessage}</div>
  const maximum = Math.max(...rows.map((row) => row.value), 1)
  return <div className={className} aria-label={label}><ul className="ui-viz__bar-list">{rows.map((row) => <li className="ui-viz__bar-row" key={`${row.label}-${row.value}`}><span className="ui-viz__bar-label">{row.label}</span><span className="ui-viz__bar-track" aria-hidden="true"><span className="ui-viz__bar-fill" style={{ '--ui-bar-size': `${(row.value / maximum) * 100}%` }} /></span><span className="ui-viz__bar-value">{formatValue(row.value, row.item)}</span></li>)}</ul></div>
}
