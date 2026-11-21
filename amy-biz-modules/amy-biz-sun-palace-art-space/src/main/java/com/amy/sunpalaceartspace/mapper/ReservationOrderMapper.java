package com.amy.sunpalaceartspace.mapper;

import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStockScope;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 预约订单Mapper接口
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface ReservationOrderMapper extends BaseMapper<ReservationOrder> {

    /**
     * 分页查询预约订单列表（含项目名称、会员姓名）。
     * <p>
     * 第一个参数必须是 MP 的 Page 对象：PaginationInnerInterceptor 只有在方法入参里发现 IPage
     * 才会改写 SQL 完成 count + limit。若改回「全量查 + page.setRecords(...)」，插件不生效：
     * 既没有 limit（大表全表扫描），total 也恒为 0（前端永远显示 0 条）。
     *
     * @param page             分页对象（返回时会被填充 total/records）
     * @param reservationOrder 查询条件
     * @return 分页结果
     */
    IPage<ReservationOrderVO> selectReservationOrderList(@Param("page") Page<ReservationOrderVO> page,
                                                         @Param("entity") ReservationOrder reservationOrder);

    /**
     * 不分页查询预约订单列表（导出用，与分页方法分开，避免复用导致拿不到 limit）。
     *
     * @param reservationOrder 查询条件
     * @return 预约订单视图列表
     */
    List<ReservationOrderVO> selectReservationOrderExportList(@Param("entity") ReservationOrder reservationOrder);

    /**
     * 根据ID查询预约订单（含项目名称）
     *
     * @param reservationOrderId 预约订单ID
     * @return 预约订单视图对象
     */
    ReservationOrderVO selectReservationOrderById(Long reservationOrderId);

    /**
     * 根据预约单号查询预约订单（含项目名称，关联 biz_spas_projects / biz_spas_reservation_user）
     *
     * @param orderNum 预约单号
     * @return 预约订单视图对象；未命中返回 null
     */
    ReservationOrderVO selectReservationOrderVoByNum(@Param("orderNum") String orderNum);

    /**
     * 按预约时间（日期）统计各时间段的预约订单数
     *
     * @param req 查询条件：项目ID（可选）、预约时间起止
     * @return 各时间段（天）的预约订单数
     */
    List<ReservationOrderStatisticVO> selectReservationOrderStatisticByDate(ReservationOrderStatisticCriteria req);

    /**
     * 按预约时间（日期+时间段）统计各时间段的预约订单数
     *
     * @param criteria 查询条件：项目ID（可选）、预约时间起止
     * @return 各日期+时间段的预约订单数，按日期、时间升序
     */
    List<ReservationOrderStatisticByTimeVO> selectReservationOrderStatisticByTime(ReservationOrderStatisticCriteria criteria);

    /**
     * 按项目分组统计预约订单总数（按日期时间范围）
     *
     * @param criteria 查询条件：projectId（可选）、reservationTimeStart、reservationTimeEnd
     * @return 各项目的预约订单总数，按项目ID升序
     */
    List<ReservationOrderStatisticByProjectVO> selectReservationOrderStatisticByProject(ReservationOrderStatisticCriteria criteria);

    /**
     * 按「生效库存放号日期」查询同项目、同日期的预约订单。
     * <p>每个 scope 是 {@code [当天 00:00:00, 次日 00:00:00)} 的<b>裸列半开区间</b>，
     *
     * @param scopes 由 status=1 的库存行推导出的「项目 + 日期」范围，<b>必须非空</b>
     *               （调用方需在空集合时直接返回，否则 &lt;foreach&gt; 会拼出非法 SQL）
     * @return 订单列表，按预约时间、项目ID升序
     */
    List<ReservationOrderVO> selectReservationOrdersByStockScopes(
            @Param("scopes") List<ReservationOrderStockScope> scopes);

    void batchUpdateUnVerifyOrderToExpire(@Param("now") Date now);
}
