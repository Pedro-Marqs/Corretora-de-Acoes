import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useRef, useState } from 'react'
import { logout } from '../api/auth.js'
import { useAuth } from '../context/auth-context.js'
export default function AppLayout() {
  const auth=useAuth(); const navigate=useNavigate(); const lock=useRef(false); const [pending,setPending]=useState(false); const [message,setMessage]=useState('')
  async function leave(){if(lock.current)return;lock.current=true;setPending(true);setMessage('');try{await logout();auth.clear();navigate('/login',{replace:true})}catch(error){if(error.status===401){auth.clear();navigate('/login',{replace:true})}else setMessage(error.message)}finally{lock.current=false;setPending(false)}}
  return <div className="private-layout"><header className="private-header"><span className="brand brand-dark"><span className="brand-mark" aria-hidden="true">C</span>Carteira Clara</span><nav aria-label="Navegação principal"><NavLink to="/app" end>Início</NavLink><NavLink to="/app/carteira">Carteira</NavLink><NavLink to="/app/corretoras">Corretoras</NavLink><NavLink to="/app/conta">Minha conta</NavLink></nav><button className="secondary-button" type="button" onClick={leave} disabled={pending}>{pending?'Saindo…':'Sair da conta'}</button></header>{message&&<div className="error-banner layout-error" role="alert">{message}</div>}<Outlet /></div>
}
