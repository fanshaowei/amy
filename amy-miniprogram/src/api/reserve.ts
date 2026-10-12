/**
 * 预约接口
 *
 * 后端：MinaAppController
 *  - POST /mina/app/reservation/submit                     提交预约（需登录）
 *  - GET  /mina/app/user/reservation/records               我的预约记录（需登录）
 *  - GET  /mina/app/reservation/{orderNum}                 预约详情（按预约单号）
 */

import type { ReservePerson, ReserveRecord } from '@/types'
import { cardTypeToIdType } from '@/types'
import { request, requestPage } from '@/utils/request'

export interface CreateReservePayload {
  projectId: number
  /** 会员ID（当前登录会员） */
  reservationUserId: number
  /** 预约日期 yyyy-MM-dd */
  date: string
  /** 预约时段 HH:mm:ss */
  time: string
  /** 预约人姓名 */
  name: string
  phone: string
  /** 证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 */
  cardType: number
  idNum: string
  /** 随行人员（不含本人） */
  companions: ReservePerson[]
}

/** 后端订单 VO：compUsers 为 JSON 字符串列表 */
interface RawReserveRecord extends Omit<ReserveRecord, 'compUsers'> {
  compUsers: string[] | null
}

/** 解析随行人员（后端 comp_users 元素为 UserIdentity 序列化后的 JSON 字符串） */
function parseCompUsers(list: string[] | null): ReservePerson[] {
  if (!list || !list.length) return []
  const result: ReservePerson[] = []
  list.forEach((item) => {
    try {
      const obj = JSON.parse(item)
      result.push({
        name: obj.name || '',
        idType: Number(obj.idType ?? 1),
        idNum: obj.idNum || '',
        phone: obj.phone || '',
        gender: obj.gender,
      })
    } catch (e) {
      // 兼容历史脏数据：忽略无法解析的元素
      console.warn('解析随行人员失败', item)
    }
  })
  return result
}

/** 统一时段格式为 HH:mm:ss，后端 @JsonFormat 要求 yyyy-MM-dd HH:mm:ss */
function toHms(time: string): string {
  const parts = (time || '').split(':')
  while (parts.length < 3) parts.push('00')
  return parts
    .slice(0, 3)
    .map((item) => (item || '00').padStart(2, '0'))
    .join(':')
}

function normalizeRecord(raw: RawReserveRecord): ReserveRecord {
  return {
    ...raw,
    reservationStatusName: raw.reservationStatusName || '',
    compUsers: parseCompUsers(raw.compUsers),
  }
}

/**
 * 提交预约
 * POST /mina/app/reservation/submit
 */
export async function createReserve(payload: CreateReservePayload): Promise<boolean> {
  return request<boolean>({
    url: '/mina/app/reservation/submit',
    method: 'POST',
    data: {
      projectId: payload.projectId,
      reservationUserId: payload.reservationUserId,
      name: payload.name,
      phone: payload.phone,
      idType: cardTypeToIdType(payload.cardType),
      idNum: payload.idNum,
      guestsNum: 1 + payload.companions.length,
      reservationTime: `${payload.date} ${toHms(payload.time)}`,
      compUsers: payload.companions.map((item) =>
        JSON.stringify({
          name: item.name,
          idType: item.idType,
          idNum: item.idNum,
          phone: item.phone || payload.phone,
        })
      ),
    },
  })
}

/**
 * 我的预约记录
 * GET /mina/app/user/reservation/records
 */
export async function getRecords(): Promise<ReserveRecord[]> {
  const page = await requestPage<RawReserveRecord>({
    url: '/mina/app/user/reservation/records',
    pageNum: 1,
    pageSize: 100,
  })
  return (page.rows || []).map(normalizeRecord)
}

/**
 * 预约详情（按预约单号查询）
 * GET /mina/app/reservation/{orderNum}
 */
export async function getRecordDetail(orderNum: string): Promise<ReserveRecord> {
  const raw = await request<RawReserveRecord>({
    url: `/mina/app/reservation/${encodeURIComponent(orderNum)}`,
  })
  if (!raw || !raw.reservationNum) {
    throw new Error('未找到该预约单')
  }
  return normalizeRecord(raw)
}
