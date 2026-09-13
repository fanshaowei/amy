package com.amy.sunpalaceartspace.domain.entity;

import java.util.Date;
import java.util.List;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.common.core.web.domain.BaseEntity;
import com.amy.sunpalaceartspace.config.StringListJsonTypeHandler;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约订单对象 biz_spas_reservation_order
 *
 * @Description 管理每个预约订单（单号、预约项目、预约人信息、预约时间、状态等）
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_reservation_order")
public class ReservationOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 预约订单ID */
    @Excel(name = "预约订单ID", cellType = ColumnType.NUMERIC)
    @TableId(type = IdType.AUTO)
    private Long reservationOrderId;

    /** 预约单号（根据预约时间生成，如 202609131210095112） */
    @Excel(name = "预约单号")
    @NotBlank(message = "预约单号不能为空")
    @Size(min = 0, max = 32, message = "预约单号长度不能超过32个字符")
    private String reservationNum;

    /** 预约项目ID */
    @Excel(name = "项目ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "预约项目不能为空")
    private Long projectId;

    /** 预约会员ID（关联 biz_spas_reservation_user.reservation_user_id；可空表示匿名预约） */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long reservationUserId;

    /** 预约人姓名 */
    @Excel(name = "姓名")
    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    /** 联系电话 */
    @Excel(name = "电话")
    @NotBlank(message = "联系电话不能为空")
    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    /** {@link com.amy.sunpalaceartspace.enums.IdType }证件类型（身份证、护照等） */
    @Excel(name = "证件类型")
    @NotBlank(message = "证件类型不能为空")
    @Size(min = 0, max = 20, message = "证件类型长度不能超过20个字符")
    private String idType;

    /** 证件号码 */
    @Excel(name = "证件号码")
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    private String idNum;

    /** 预约人数（含预约人本人） */
    @Excel(name = "人数", cellType = ColumnType.NUMERIC)
    private Integer guestsNum;

    /** 预约时间 */
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

    /**
     * 预约状态码（业务值用 {@link ReservationOrderStatus}）
     * <p>0待核销 1已完成 2已过期 3已取消</p>
     */
    @Excel(name = "状态", readConverterExp = "0=待核销,1=已完成,2=已过期,3=已取消")
    @TableField("reservation_status")
    private Integer reservationStatus;

    /** 核销人员 */
    @Excel(name = "核销人员")
    private String verifyBy;

    /** 核销时间 */
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /**
     * 预约随行人身份信息（数组列表字符串）。
     *
     * <p>类型 {@code List<String>}，每个元素的内容为 {@code com.amy.sunpalaceartspace.domain.entity.UserIdentity}
     * 字段序列化后的 JSON 字符串（如 {@code "{\"name\":\"张三\",\"idNum\":\"110101199001011234\"}"}）。</p>
     *
     * <p>通过字段级 typeHandler {@link StringListJsonTypeHandler} 把 {@code List<String>}
     * 以 JSON 数组字符串形式存入 {@code comp_users} VARCHAR(2000) 列；读取时自动反序列化。</p>
     *
     * <p>仅保存随行人"快照信息"，不与 {@code biz_spas_user_identity} 表建立 FK，
     * 避免随行人后续修改/删除时影响历史预约单的可追溯性。</p>
     */
    @Excel(name = "随行人信息")
    @TableField(value = "comp_users", typeHandler = StringListJsonTypeHandler.class)
    private List<String> compUsers;
}
