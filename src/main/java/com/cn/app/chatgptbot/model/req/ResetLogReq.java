package com.cn.app.chatgptbot.model.req;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * ClassName:ResetLogReq
 * Package:com.cn.app.chatgptbot.model.req
 * Description:
 *
 * @Author: ShenShiPeng
 * @Create: 2023/3/24 - 22:39
 * @Version: v1.0
 */
@Data
public class ResetLogReq {

    @NotNull(message = "日志id不能为空")
    private Long logId;

    @NotNull(message = "消息数据不能为空")
    private String newMessages;

}
