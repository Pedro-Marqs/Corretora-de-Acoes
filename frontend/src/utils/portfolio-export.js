const encoder = new TextEncoder()

const CONTENT_TYPES = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>`

const ROOT_RELATIONSHIPS = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>`

const WORKBOOK = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Carteira" sheetId="1" r:id="rId1"/></sheets>
</workbook>`

const WORKBOOK_RELATIONSHIPS = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>`

function escapeXml(value) {
  return String(value ?? '').replace(/[<>&'"]/g, (character) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;' }[character]))
}

function columnName(index) {
  let name = ''
  let current = index + 1
  while (current > 0) {
    const remainder = (current - 1) % 26
    name = String.fromCharCode(65 + remainder) + name
    current = Math.floor((current - 1) / 26)
  }
  return name
}

function cell(value, column, row) {
  const reference = `${columnName(column)}${row}`
  const number = typeof value === 'number' && Number.isFinite(value)
  return number
    ? `<c r="${reference}"><v>${value}</v></c>`
    : `<c r="${reference}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>`
}

function worksheet(rows) {
  const body = rows.map((row, rowIndex) => `<row r="${rowIndex + 1}">${row.map((value, columnIndex) => cell(value, columnIndex, rowIndex + 1)).join('')}</row>`).join('')
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${body}</sheetData></worksheet>`
}

function crc32(bytes) {
  let crc = 0xffffffff
  for (const byte of bytes) {
    crc ^= byte
    for (let bit = 0; bit < 8; bit += 1) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0)
  }
  return (crc ^ 0xffffffff) >>> 0
}

function numberBytes(value, size) {
  const bytes = []
  for (let index = 0; index < size; index += 1) bytes.push((value >>> (index * 8)) & 0xff)
  return bytes
}

function zip(files) {
  const local = []
  const central = []
  let offset = 0
  for (const file of files) {
    const name = encoder.encode(file.name)
    const data = encoder.encode(file.content)
    const checksum = crc32(data)
    const header = [0x50, 0x4b, 0x03, 0x04, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, ...numberBytes(checksum, 4), ...numberBytes(data.length, 4), ...numberBytes(data.length, 4), ...numberBytes(name.length, 2), 0, 0]
    local.push(Uint8Array.from([...header, ...name, ...data]))
    const directory = [0x50, 0x4b, 0x01, 0x02, 20, 0, 20, 0, 0, 0, 0, 0, 0, 0, ...numberBytes(checksum, 4), ...numberBytes(data.length, 4), ...numberBytes(data.length, 4), ...numberBytes(name.length, 2), 0, 0, 0, 0, 0, 0, ...numberBytes(offset, 4)]
    central.push(Uint8Array.from([...directory, ...name]))
    offset += header.length + name.length + data.length
  }
  const centralSize = central.reduce((total, item) => total + item.length, 0)
  const centralOffset = offset
  const end = Uint8Array.from([0x50, 0x4b, 0x05, 0x06, 0, 0, 0, 0, ...numberBytes(files.length, 2), ...numberBytes(files.length, 2), ...numberBytes(centralSize, 4), ...numberBytes(centralOffset, 4), 0, 0])
  return new Blob([...local, ...central, end], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
}

function rowsFromPositions(positions) {
  return [
    ['Ticker', 'Ativo', 'Mercado', 'Corretora', 'Quantidade', 'Preço médio (BRL)', 'Preço atual (BRL)', 'Saldo / posição (BRL)', 'Valorização (BRL)'],
    ...(positions ?? []).map((position) => [
      position.ticker,
      position.name,
      position.market,
      position.brokerageName,
      Number(position.quantity),
      Number(position.averagePriceBrl),
      position.quotePriceBrl == null ? '' : Number(position.quotePriceBrl),
      position.marketValueBrl == null ? '' : Number(position.marketValueBrl),
      position.unrealizedResultBrl == null ? '' : Number(position.unrealizedResultBrl),
    ]),
  ]
}

export function exportPortfolioXlsx(positions, filename = `carteira-${new Date().toISOString().slice(0, 10)}.xlsx`) {
  if (typeof document === 'undefined' || typeof window === 'undefined' || !window.URL?.createObjectURL) return false
  const file = zip([
    { name: '[Content_Types].xml', content: CONTENT_TYPES },
    { name: '_rels/.rels', content: ROOT_RELATIONSHIPS },
    { name: 'xl/workbook.xml', content: WORKBOOK },
    { name: 'xl/_rels/workbook.xml.rels', content: WORKBOOK_RELATIONSHIPS },
    { name: 'xl/worksheets/sheet1.xml', content: worksheet(rowsFromPositions(positions)) },
  ])
  const url = window.URL.createObjectURL(file)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  window.setTimeout(() => window.URL.revokeObjectURL(url), 0)
  return true
}
