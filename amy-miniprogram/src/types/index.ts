/**
 * 全局类型定义
 *
 * 字段命名与后端 amy-biz-modules/amy-biz-sun-palace-art-space 的小程序接口保持一致，
 * 接口原始结构见：
 *  - MinaHomePageProjectResp / MinaProjectReservationInfoResp / MinaReservationUserInfoResp
 *  - ReservationOrderReq / ReservationOrderVO
 */

/** 证件类型（与后端 IdType 枚举一致：1身份证 2护照 3港澳通行证 4台湾通行证 5军官证） */
export type CardType = 1 | 2 | 3 | 4 | 5

export interface CardTypeOption {
  id: CardType
  title: string
  enabled: boolean
}

/** 证件类型选项 */
export const CARD_TYPES: CardTypeOption[] = [
  { id: 1, title: '第二代居民身份证', enabled: true },
  { id: 2, title: '护照', enabled: true },
  { id: 3, title: '港澳通行证', enabled: true },
  { id: 4, title: '台湾通行证', enabled: true },
  { id: 5, title: '军官证', enabled: true },
]

/** 后端 ReservationOrderReq.idType 为字符串，取 IdType 枚举名称 */
const CARD_TYPE_ID_TYPE: Record<number, string> = {
  1: '身份证',
  2: '护照',
  3: '港澳通行证',
  4: '台湾通行证',
  5: '军官证',
}

/** 证件类型展示文案 */
export function cardTypeTitle(id: CardType | number): string {
  return CARD_TYPES.find((c) => c.id === id)?.title || '未知证件'
}

/** 证件类型数字 -> 后端订单 idType 字符串 */
export function cardTypeToIdType(id: CardType | number): string {
  return CARD_TYPE_ID_TYPE[id] || CARD_TYPE_ID_TYPE[1]
}

/**
 * 预约订单状态（与后端 ReservationOrderStatus 一致）
 * 0待核销 1已完成 2已过期 3已取消
 */
export type ReserveStatus = 0 | 1 | 2 | 3

export const RESERVE_STATUS: Record<number, { text: string; type: string }> = {
  0: { text: '待核销', type: 'pending' },
  1: { text: '已完成', type: 'done' },
  2: { text: '已过期', type: 'expired' },
  3: { text: '已取消', type: 'cancel' },
}

/**
 * 号源状态（与后端 ReservationStatusEnum 一致）
 * 1充足 2紧张 3已满
 */
export type RemainStatus = 1 | 2 | 3

export const REMAIN_STATUS: Record<number, { text: string; type: string }> = {
  1: { text: '充足号源', type: 'plenty' },
  2: { text: '号源紧张', type: 'tight' },
  3: { text: '已约满', type: 'soldout' },
}

/** 会员身份（与后端 UserTypeEnum 一致：1用户 2核销员） */
export type UserType = 1 | 2

/** 项目/活动（对应后端 MinaHomePageProjectResp） */
export interface Activity {
  projectId: number
  projectName: string
  /** 封面图 */
  coverArtUrl: string
  /** 可预约开始日期 yyyy-MM-dd */
  reservationStartDate: string
  /** 可预约结束日期 yyyy-MM-dd */
  reservationEndDate: string
  /** 每日预约开始时间 HH:mm:ss */
  reservationStartTime: string
  /** 每日预约结束时间 HH:mm:ss */
  reservationEndTime: string
  /** 剩余预约号数 */
  remainingReservationCount: number
  /** 总预约号数 */
  totalReservationCount: number
  /** 每日预约截止时间 HH:mm:ss */
  cutOffTime: string
  /** 号源状态 1充足 2紧张 3已满 */
  reservationStatus: number
  /** 可随行人数（对应 Projects.travelerCount） */
  travelerCount: number
  /** 预约界面停留秒数（下单倒计时） */
  reservationStaySecond: number
  /** 预约须知（详情接口返回） */
  reservationNotes?: string
  /** 可预约日期列表（详情接口返回） */
  reservationDates?: string[]
  /** 时段间隔（分钟），由号源明细推导，用于展示参观时长 */
  intervalMinutes?: number
  /** 概览文案（前端根据开放时间规则生成） */
  desc?: string
}

/** 时间段号源（对应后端 ReservationTimeDetails） */
export interface TimeSlot {
  /** 时段 HH:mm:ss */
  time: string
  /** 剩余号数 */
  remain: number
  /** 号源状态 1充足 2紧张 3已满 */
  status: number
  /** 是否已约满 */
  full: boolean
}

/** 随行人身份信息（对应后端 UserIdentity 快照） */
export interface ReservePerson {
  name: string
  /** 证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 */
  idType: number
  idNum: string
  phone?: string
  /** 性别 0女 1男 */
  gender?: number
}

/** 某日期的可预约时段（对应后端 ReservationDetails） */
export interface ReservationDateDetail {
  /** 预约日期 yyyy-MM-dd */
  reservationDate: string
  reservationTimeDetails: {
    /** 时段 HH:mm:ss */
    reservationTime: string
    remainingReservationCount: number
    /** 号源状态 1充足 2紧张 3已满 */
    reservationStatus: number
  }[]
}

/** 项目预约信息（对应后端 MinaProjectReservationInfoResp） */
export interface ReservationInfo {
  /** 预约须知 */
  reservationNotes: string
  reservationDetails: ReservationDateDetail[]
}

/** 预约记录（对应后端 ReservationOrderVO） */
export interface ReserveRecord {
  reservationOrderId: number
  /** 预约单号（核销/详情查询以此为准） */
  reservationNum: string
  projectId: number
  projectName: string
  reservationUserId: number
  /** 会员编号 */
  reservationUserNumber: string
  reservationUserName: string
  /** 预约人姓名 */
  name: string
  /** 预约人手机号 */
  phone: string
  /** 证件类型（后端为字符串，如「身份证」） */
  idType: string
  idNum: string
  /** 预约人数（含本人） */
  guestsNum: number
  /** 预约时间 yyyy-MM-dd HH:mm:ss */
  reservationTime: string
  /** 状态 0待核销 1已完成 2已过期 3已取消 */
  reservationStatus: number
  reservationStatusName: string
  /** 核销人 */
  verifyBy: string
  /** 核销时间 */
  verifyTime: string
  createTime: string
  /** 随行人员（后端为 JSON 字符串列表，已在 api 层解析） */
  compUsers: ReservePerson[]
}

/** 会员信息（对应后端 MinaReservationUserInfoResp） */
export interface User {
  reservationUserId: number
  nickname: string
  avatarImgUrl: string
  phone: string
  /** 1用户 2核销员 */
  type: UserType
}

/** 分页结果（对应后端 TableDataInfo） */
export interface TableDataInfo<T> {
  total: number
  rows: T[]
}

/**
 * API 统一返回结构（对应后端 R<T>）
 * 注意：成功码为 200（Constants.SUCCESS），不是 0
 */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}
