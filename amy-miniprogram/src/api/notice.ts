/**
 * 通知公告接口
 *
 * 后端：MinaAppController（公开信息，游客可见）
 *  - GET /spas/mina/app/notice/list?limit=2   公告列表（不带正文，按发布时间倒序）
 *  - GET /spas/mina/app/notice/{noticeId}     公告详情（正文为 HTML 富文本）
 *
 * 数据源为后台管理的 sys_notice 表（amy-system 模块 SysNoticeController 维护），
 * 小程序端只读展示。
 */

import type { NoticeDetail, NoticeItem } from '@/types'
import { request } from '@/utils/request'

/**
 * 通知公告列表（公开接口，无需登录；静默失败由页面决定提示）
 * GET /spas/mina/app/notice/list?limit=N
 */
export async function getNotices(limit = 2): Promise<NoticeItem[]> {
  const list = await request<NoticeItem[]>({
    url: '/spas/mina/app/notice/list',
    data: { limit },
    auth: false,
    silent: true,
    skipAuthRedirect: true,
  })
  return list || []
}

/**
 * 通知公告详情（公开接口，无需登录）
 * GET /spas/mina/app/notice/{noticeId}
 */
export async function getNoticeDetail(noticeId: number): Promise<NoticeDetail> {
  const detail = await request<NoticeDetail>({
    url: `/spas/mina/app/notice/${noticeId}`,
    auth: false,
    skipAuthRedirect: true,
  })
  return detail
}
