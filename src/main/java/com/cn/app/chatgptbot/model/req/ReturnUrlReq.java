package com.cn.app.chatgptbot.model.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * ClassName:CreateOrderReq
 * Package:com.cn.app.chatgptbot.model.req
 * Description:
 *
 * @Author: ShenShiPeng
 * @Create: 2023/3/22 - 09:03
 * @Version: v1.0
 */
@Data
public class ReturnUrlReq {



    @ApiModelProperty(value = "订单id")
    @NotNull
    private Long orderId;

}
