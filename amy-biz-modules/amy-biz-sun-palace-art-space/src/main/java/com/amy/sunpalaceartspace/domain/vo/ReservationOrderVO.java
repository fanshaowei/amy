package com.amy.sunpalaceartspace.domain.vo;

import java.util.Date;
import java.util.List;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.sunpalaceartspace.enums.OrderSource;
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

    /** 会员真实姓名（来自 biz_spas_reservation_user.name） */
    //@Excel(name = "会员姓名")
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
     * 状态码（0/1/2/3，对应数据库列 {@code reservation_status}）。
     * <p>SQL/XML 把 DB 列直接灌到这里；中文名见 {@link #reservationStatusName}。</p>
     */
    @Excel(name = "状态", readConverterExp = "-1=预约失败,0=待核销,1=已完成,2=已过期,3=已取消,4=预约中")
    private Integer reservationStatus;

    /**
     * 状态中文名（{@link ReservationOrderStatus#name}），优先返回此字段给前端展示。
     */
    @Excel(name = "状态名称")
    private String reservationStatusName;

    /**
     * 预约状态说明（数据库 {@code status_msg} 列）。
     */
    private String statusMsg;

    /** 核销人员 */
    @Excel(name = "核销人员")
    private String verifyBy;

    /** 核销时间 */
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 取消时间 */
    @Excel(name = "取消时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 订单来源码（1小程序预约 2后台手动添加，见 {@link OrderSource}） */
    @Excel(name = "订单来源", readConverterExp = "1=小程序预约,2=后台手动添加")
    private Integer orderSource;

    /** 订单来源中文名（由 {@link #fillOrderSourceName()} 装配，不参与 Excel 导出） */
    private String orderSourceName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 预约随行人身份信息（数组列表）。
     * <p>数据库 {@code comp_users} 列存的是 JSON 字符串，本 VO 字段由
     * {@code ReservationOrderMapper.xml} 的 resultMap 通过
     * {@link com.amy.sunpalaceartspace.config.StringListJsonTypeHandler} 反序列化为
     * {@code List<String>}（每个元素为一条随行人信息的 JSON 对象字符串）。</p>
     * <p><b>注意：</b>该字段为 {@code List<String>}，元素不是带 {@code @Excel} 字段的 POJO，
     * 不能走 ExcelUtil 的 sub-list 机制（否则会被误判为子列表、导致导出出现两行表头且本列无法落值）。
     * 若需导出随行人信息，请改用 {@link #compUsersText} 这种已扁平化的字符串字段。</p>
     */
    private List<String> compUsers;

    /**
     * 随行人信息导出文本（扁平化）。由装配 VO 时把 {@link #compUsers} 用分隔符拼接而成。
     * 仅用于 Excel 导出，不参与列表 JSON 返回。
     */
    @Excel(name = "随行人信息", width = 40)
    private String compUsersText;

    /**
     * 查询订单时才会生成的核销码
     */
    private String verifyCode;

    /**
     * 通过状态码把 {@link #reservationStatus} 解析成枚举并写入 {@link #reservationStatusName}。
     * <p>供 Service 在装配 VO 时调用；XML 完成后由调用方负责。</p>
     */
    public void fillReservationStatusName() {
        ReservationOrderStatus s = ReservationOrderStatus.fromCode(this.reservationStatus);
        this.reservationStatusName = s == null ? null : s.getName();
    }

    /**
     * 通过来源码把 {@link #orderSource} 解析成枚举并写入 {@link #orderSourceName}。
     * <p>与状态中文名一样，供 Service 在装配 VO 时调用。</p>
     */
    public void fillOrderSourceName() {
        OrderSource source = OrderSource.fromCode(this.orderSource);
        this.orderSourceName = source == null ? null : source.getName();
    }

    /**
     * 把 {@link #compUsers}（JSON 字符串列表）扁平化为 {@link #compUsersText}，供导出使用。
     * <p>每个元素为一条随行人信息的 JSON 对象字符串，这里直接用分隔符拼接，避免触发 ExcelUtil 的
     * sub-list 机制（见 {@link #compUsers} 上的说明）。若需更友好的展示，可在 service 层自行解析后写入。</p>
     */
    public void fillCompUsersText() {
        if (this.compUsers == null || this.compUsers.isEmpty()) {
            this.compUsersText = "";
        } else {
            this.compUsersText = String.join("；", this.compUsers);
        }
    }
}
