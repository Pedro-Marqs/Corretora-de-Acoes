import { createContext, useContext } from 'react'

export const AdminAuthContext = createContext(null)
export function useAdminAuth() { return useContext(AdminAuthContext) }
