/**
 * 全局登录状态：token + 「已打开登录页等待授权」标记。
 *
 * 单独成零依赖模块，供 utils/request.ts、utils/authGuard.ts、stores/user.ts 共用，
 * 避免 authGuard(依赖 stores/user) 与 request(被 stores/user 依赖) 形成循环引用。
 *
 * ★ 为什么 token 必须是响应式的：
 *   Pinia 的 getters 本质是 Vue computed，只有读取了响应式依赖才会在变化时重算。
 *   早期 getToken() 直接读 uni.getStorageSync()（非响应式），loggedIn getter
 *   没有建立任何依赖，首次求值后被永久缓存 —— 清缓存冷启动时求值为 false，
 *   之后即使登录成功写入 token，所有页面读到的仍是 false（子页面反复跳登录页）。
 *   这里用 ref 保存 token，所有读写都经此模块，getter 才能实时感知登录/退出。
 */
import { ref } from 'vue'

export const TOKEN_KEY = 'spas_token'

function readTokenFromStorage(): string {
  try {
    return (typeof uni !== 'undefined' && uni.getStorageSync(TOKEN_KEY)) || ''
  } catch (e) {
    return ''
  }
}

/** 响应式 token：登录、退出、登录过期都必须经此读写 */
const tokenRef = ref<string>(readTokenFromStorage())

export function getToken(): string {
  return tokenRef.value
}

export function setToken(token: string) {
  tokenRef.value = token || ''
  try {
    uni.setStorageSync(TOKEN_KEY, tokenRef.value)
  } catch (e) {
    // storage 写入失败时至少保证本次会话内存态正确
  }
}

export function clearToken() {
  tokenRef.value = ''
  try {
    uni.removeStorageSync(TOKEN_KEY)
  } catch (e) {
    // ignore
  }
}

/**
 * 外部改动了 storage 时重新同步内存态。
 * 例如「清除本地缓存」按钮调 uni.clearStorageSync()，或开发者工具手动清数据缓存，
 * 这些路径不会经过 clearToken()，需要显式同步，否则内存里仍认为已登录。
 */
export function syncTokenFromStorage() {
  tokenRef.value = readTokenFromStorage()
}

/** 全局「已打开登录页、等待用户授权」标记 */
let loginPending = false

export function isLoginPending(): boolean {
  return loginPending
}

export function setLoginPending(value: boolean) {
  loginPending = value
}
