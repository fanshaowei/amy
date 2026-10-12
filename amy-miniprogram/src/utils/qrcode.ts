/**
 * 二维码矩阵生成（跨端，纯 JS 无 DOM 依赖）
 * 基于 qrcode-generator 计算矩阵，返回布尔二维数组，供 view/canvas 渲染
 */

import qrcode from 'qrcode-generator'

export interface QRMatrix {
  size: number
  /** data[row][col] === true 表示黑色模块 */
  data: boolean[][]
}

/**
 * 生成二维码模块矩阵
 * @param text 编码内容（预约编号）
 * @param typeNumber 版本号，0 表示自动
 */
export function generateQRMatrix(text: string, typeNumber = 0): QRMatrix {
  const qr = qrcode(typeNumber, 'M')
  qr.addData(text)
  qr.make()
  const size = qr.getModuleCount()
  const data: boolean[][] = []
  for (let row = 0; row < size; row++) {
    const line: boolean[] = []
    for (let col = 0; col < size; col++) {
      line.push(qr.isDark(row, col))
    }
    data.push(line)
  }
  return { size, data }
}
