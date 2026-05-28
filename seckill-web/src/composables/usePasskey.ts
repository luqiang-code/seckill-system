import { startRegistration, startAuthentication } from '@simplewebauthn/browser'
import {
  registerOptions,
  registerVerify,
  loginOptions,
  loginVerify,
  discoverOptions,
  discoverVerify,
  type AuthData,
} from '../api'

export function usePasskey() {
  async function registerPasskey(username: string): Promise<AuthData> {
    const opts = await registerOptions(username)
    const regResp = await startRegistration({ optionsJSON: opts })
    return registerVerify(username, regResp)
  }

  async function authenticateWithPasskey(username: string): Promise<AuthData> {
    const opts = await loginOptions(username)
    const authResp = await startAuthentication({ optionsJSON: opts })
    return loginVerify(username, authResp)
  }

  async function authenticateDiscoverable(): Promise<AuthData> {
    const opts = await discoverOptions()
    const authResp = await startAuthentication({ optionsJSON: opts })
    return discoverVerify(authResp)
  }

  return { registerPasskey, authenticateWithPasskey, authenticateDiscoverable }
}
