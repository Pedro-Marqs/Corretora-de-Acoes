import { Link } from 'react-router-dom'
import { Button, SectionHeader, SurfaceCard } from '../components/ui/index.jsx'

export default function NotFoundPage() {
  return <main className="common-state ui-page-shell not-found-page">
    <SurfaceCard as="section" className="not-found-card" aria-labelledby="not-found-title">
      <SectionHeader titleId="not-found-title" eyebrow="Erro 404" title="Página não encontrada." description="O endereço informado não corresponde a uma tela disponível." />
      <Button as={Link} to="/cadastro">Voltar ao início</Button>
    </SurfaceCard>
  </main>
}
