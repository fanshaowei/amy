/**
 * 核销接口
 *
 * 后端：MinaAppController
 *  - GET  /mina/app/reservation/{orderNum}          按预约单号查询预约（扫码后展示）
 *  - POST /mina/app/reservation/verify/{orderNum}   确认核销（需登录且为核销员）
 */

import type { ReserveRecord } from '@/types'
import { getRecordDetail } from './reserve'
import { request } from '@/utils/request'

/** 扫码后按预约单号查询预约信息 */
export async function queryOrder(orderNum: string): Promise<ReserveRecord> {
  return getRecordDetail(orderNum)
}

/**
 * 确认核销
 * POST /mina/app/reservation/verify/{orderNum}
 */
export async function confirm(orderNum: string): Promise<boolean> {
  return request<boolean>({
    url: `/mina/app/reservation/verify/${encodeURIComponent(orderNum)}`,
    method: 'POST',
  })
}
