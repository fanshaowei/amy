package com.amy.sunpalaceartspace.domain.req;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName ReservationOrderUpdateStatusReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-11-02 01:00
 */
@Data
public class ReservationOrderUpdateStatusReq {
    @NotBlank(message = "订单ID不能为空")
    private Long orderId;
    @NotBlank(message = "状态不能为空")
    private ReservationOrderStatus status;
}
