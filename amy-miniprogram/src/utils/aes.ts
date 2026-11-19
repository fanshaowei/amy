/**
 * AES-ECB 加密（滑动拼图验证码坐标加密用，算法与 aj-captcha 后端一致）
 *
 * @param word    待加密内容（坐标 JSON 或 token---坐标 JSON）
 * @param keyWord 后端下发的 16 位 secretKey
 */
import CryptoJS from 'crypto-js'

export function aesEncrypt(word: string, keyWord = 'XwKsGlMcdPMEhR1B'): string {
  const key = CryptoJS.enc.Utf8.parse(keyWord)
  const srcs = CryptoJS.enc.Utf8.parse(word)
  const encrypted = CryptoJS.AES.encrypt(srcs, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  })
  return encrypted.toString()
}
