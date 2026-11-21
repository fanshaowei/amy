/**
 * 登录守卫：首页游客可见，进入需要登录的子页面统一跳授权登录页。
 *
 * 交互约定（与 pages/login/index.vue「登录成功统一回首页」配套）：
 *  - 未登录进入需登录页面 → navigateTo 登录页，并记下「待授权」状态；
 *  - 用户在登录页放弃授权（原生返回 / iOS 右滑返回）→ 来源页 onShow 再次触发守卫，
 *    发现仍处于「待授权」且未登录 → switchTab 回首页，不停留在空白的业务页；
 *  - 登录成功 → 登录页 switchTab 回首页，业务页随页面栈一起出栈。
 *
 * 为什么不用登录页 onUnload 里跳转：小程序在页面销毁回调里发起页面跳转并不可靠，
 * 且与来源页 onShow 的守卫会互相抢状态。这里用纯 JS 状态机，来源页自己决定去向。
 *
 * 注意：守卫在已登录时是纯判断、无副作用，不会重置业务页状态。
 * 真机 scanCode 等原生窗体会额外触发 onShow，因此调用方也不要在 onShow 里重置视图。
 */
import { useUserStore } from '@/stores/user'
import { isLoginPending, setLoginPending } from './loginState'

export const HOME_URL = '/pages/home/index'
export const LOGIN_URL = '/pages/login/index'

/** 登录成功 / 已回到首页时清除待授权标记 */
export function clearLoginPending() {
  setLoginPending(false)
}

/**
 * 需要登录的页面在 onShow 中调用。
 * @returns true 表示已登录，可继续加载数据；false 表示已发生跳转，调用方应直接 return
 */
export function requireLogin(): boolean {
  const store = useUserStore()
  if (store.loggedIn) {
    clearLoginPending()
    return true
  }
  if (isLoginPending()) {
    // 从登录页放弃授权返回：统一回首页
    clearLoginPending()
    uni.switchTab({ url: HOME_URL })
    return false
  }
  setLoginPending(true)
  uni.navigateTo({ url: LOGIN_URL })
  return false
}

/**
 * 公开页面（如「我的」）的主动登录入口：
 * 记下待授权状态，放弃授权返回时由 backHomeIfLoginAbandoned 统一回首页。
 */
export function openLogin() {
  setLoginPending(true)
  uni.navigateTo({ url: LOGIN_URL })
}

/** 公开页面 onShow 调用：若刚从登录页放弃授权返回，则回首页 */
export function backHomeIfLoginAbandoned(): boolean {
  if (!isLoginPending()) return false
  clearLoginPending()
  uni.switchTab({ url: HOME_URL })
  return true
}
