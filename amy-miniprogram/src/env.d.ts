/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'qrcode-generator' {
  interface QRCode {
    addData(data: string, mode?: string): void
    make(): void
    getModuleCount(): number
    isDark(row: number, col: number): boolean
    createDataURL(cellSize?: number, margin?: number): string
    createSvgTag(cellSize?: number, margin?: number, alt?: string): string
  }
  function qrcode(typeNumber: number, errorCorrectionLevel: string): QRCode
  export = qrcode
}
