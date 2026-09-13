package com.amy.sunpalaceartspace.domain.vo;

import java.util.Date;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 预约订单视图对象 ReservationOrderVO
 *
 * @Description 预约订单列表/导出返回对象，含关联项目名称
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
public class ReservationOrderVO {

    /** 预约订单ID */
    @Excel(name = "预约订单ID", cellType = ColumnType.NUMERIC)
    private Long reservationOrderId;

    /** 预约单号 */
    @Excel(name = "预约单号")
    private String reservationNum;

    /** 预约项目ID */
    @Excel(name = "项目ID", cellType = ColumnType.NUMERIC)
    private Long projectId;

    /** 预约项目名称 */
    @Excel(name = "预约项目")
    private String projectName;

    /** 预约会员ID（关联 biz_spas_reservation_user.reservation_user_id） */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long reservationUserId;

    /** 会员编号（来自 biz_spas_reservation_user.user_number） */
    @Excel(name = "会员编号")
    private String reservationUserNumber;

    /** 会员真实姓名（来自 biz_spas_reservation_user.name） */
    @Excel(name = "会员姓名")
    private String reservationUserName;

    /** 预约人姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 联系电话 */
    @Excel(name = "电话")
    private String phone;

    /** 证件类型 */
    @Excel(name = "证件类型")
    private String idType;

    /** 证件号码 */
    @Excel(name = "证件号码")
    private String idNum;

    /** 预约人数 */
    @Excel(name = "人数", cellType = ColumnType.NUMERIC)
    private Integer guestsNum;

    /** 预约时间 */
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

    /**
     * 状态值（0/1/2/3，对应数据库列 {@code status}）。
     * <p>SQL/XML 把 DB 列直接灌到这里；中文名见 {@link #reservationStatusName}。</p>
     */
    @Excel(name = "状态", readConverterExp = "0=待核销,1=已完成,2=已过期,3=已取消")
    private String reservationStatus;

    /**
     * 状态中文名（{@link ReservationOrderStatus#name}），优先返回此字段给前端展示。
     */
    @Excel(name = "状态名称")
    private String reservationStatusName;

    /** 核销人员 */
    @Excel(name = "核销人员")
    private String verifyBy;

    /** 核销时间 */
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 通过状态码把 {@link #reservationStatus} 解析成枚举并写入 {@link #reservationStatusName}。
     * <p>供 Service 在装配 VO 时调用；XML 完成后由调用方负责。</p>
     */
    public void fillReservationStatusName() {
        ReservationOrderStatus s = ReservationOrderStatus.fromCodeString(this.reservationStatus);
        this.reservationStatusName = s == null ? null : s.getName();
    }
}
