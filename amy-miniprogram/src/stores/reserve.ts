/**
 * 预约状态管理
 */

import { defineStore } from 'pinia'
import type { ReserveRecord } from '@/types'
import * as reserveApi from '@/api/reserve'

export const useReserveStore = defineStore('reserve', {
  state: () => ({
    records: [] as ReserveRecord[],
    loading: false,
  }),

  actions: {
    /** 拉取当前登录会员的预约记录（会员身份由后端根据 token 解析） */
    async fetchList() {
      this.loading = true
      try {
        this.records = await reserveApi.getRecords()
      } finally {
        this.loading = false
      }
      return this.records
    },

    /** 创建预约 */
    async create(payload: reserveApi.CreateReservePayload) {
      await reserveApi.createReserve(payload)
    },

    /** 按预约单号查询预约详情 */
    async detail(orderNum: string) {
      return reserveApi.getRecordDetail(orderNum)
    },
  },
})
