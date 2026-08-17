import { cloneElement, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeEmail, changePassword, getCurrentAccount } from '../api/accounts.js'
import { useAuth } from '../context/auth-context.js'
import { ErrorState } from '../components/common/AsyncStates.jsx'

const emptyEmail={newEmail:'',currentPassword:''}, emptyPassword={currentPassword:'',newPassword:''}
export default function AccountPage() {
  const auth=useAuth(), navigate=useNavigate(), locks=useRef({email:false,password:false})
  const [email,setEmail]=useState(emptyEmail), [password,setPassword]=useState(emptyPassword)
  const [emailErrors,setEmailErrors]=useState({}), [passwordErrors,setPasswordErrors]=useState({})
  const [emailMessage,setEmailMessage]=useState(''), [passwordMessage,setPasswordMessage]=useState('')
  const [pending,setPending]=useState({email:false,password:false}), [technical,setTechnical]=useState(null)

  async function handle401(error,kind) {
    setTechnical(null)
    try { await getCurrentAccount(); showFunctional(error,kind) }
    catch (sessionError) {
      if(sessionError.status===401){auth.clear();navigate('/login',{replace:true})}
      else setTechnical({message:sessionError.message,retry:()=>handle401(error,kind)})
    }
  }
  function showFunctional(error,kind){
    if(kind==='email'){setEmailErrors(error.fieldErrors??{});setEmailMessage(error.message)}
    else{setPasswordErrors(error.fieldErrors??{});setPasswordMessage(error.message)}
  }
  async function submit(kind,event){
    event.preventDefault();if(locks.current[kind])return;locks.current[kind]=true;setPending(p=>({...p,[kind]:true}));setTechnical(null)
    if(kind==='email'){setEmailErrors({});setEmailMessage('')}else{setPasswordErrors({});setPasswordMessage('')}
    try{await(kind==='email'?changeEmail(email):changePassword(password));setEmail(emptyEmail);setPassword(emptyPassword);auth.clear();navigate('/login',{replace:true,state:{message:'Dados alterados. Entre novamente.'}})}
    catch(error){if(error.status===401)await handle401(error,kind);else showFunctional(error,kind)}
    finally{locks.current[kind]=false;setPending(p=>({...p,[kind]:false}))}
  }
  return <main className="account-page"><header><p className="eyebrow">Configurações</p><h1>Minha conta</h1><p>Nome e CPF não podem ser alterados.</p></header><section className="profile-card"><dl><div><dt>Nome</dt><dd>{auth.account.name}</dd></div><div><dt>CPF</dt><dd>{auth.account.cpf}</dd></div><div><dt>E-mail</dt><dd>{auth.account.email}</dd></div></dl></section>{technical&&<ErrorState message={technical.message} onRetry={technical.retry}/>}<div className="settings-grid"><CredentialForm title="Alterar e-mail" message={emailMessage} onSubmit={e=>submit('email',e)} pending={pending.email}><Field label="Novo e-mail" id="newEmail" errors={emailErrors.newEmail}><input id="newEmail" type="email" required value={email.newEmail} onChange={e=>setEmail({...email,newEmail:e.target.value})}/></Field><Field label="Senha atual" id="emailPassword" errors={emailErrors.currentPassword}><input id="emailPassword" type="password" required value={email.currentPassword} onChange={e=>setEmail({...email,currentPassword:e.target.value})}/></Field></CredentialForm><CredentialForm title="Alterar senha" message={passwordMessage} onSubmit={e=>submit('password',e)} pending={pending.password}><Field label="Senha atual" id="passwordCurrent" errors={passwordErrors.currentPassword}><input id="passwordCurrent" type="password" required value={password.currentPassword} onChange={e=>setPassword({...password,currentPassword:e.target.value})}/></Field><Field label="Nova senha" id="newPassword" errors={passwordErrors.newPassword}><input id="newPassword" type="password" required value={password.newPassword} onChange={e=>setPassword({...password,newPassword:e.target.value})}/></Field><p className="field-hint">Mínimo de 8 caracteres, com maiúscula, minúscula, número e símbolo.</p></CredentialForm></div></main>
}
function CredentialForm({title,message,onSubmit,pending,children}){return <form className="settings-card" onSubmit={onSubmit} noValidate><h2>{title}</h2>{message&&<div className="error-banner" role="alert">{message}</div>}{children}<button className="primary-button" disabled={pending}>{pending?'Salvando…':'Salvar alteração'}</button></form>}
function Field({label,id,errors,children}){const errorId=`${id}-error`;return <div className="form-field"><label htmlFor={id}>{label} <span aria-hidden="true">*</span><span className="sr-only"> obrigatório</span></label>{cloneElement(children,{'aria-invalid':Boolean(errors),'aria-describedby':errors?errorId:undefined})}{errors&&<span className="field-error" id={errorId}>{errors.map(item=><span key={item}>{item}</span>)}</span>}</div>}
