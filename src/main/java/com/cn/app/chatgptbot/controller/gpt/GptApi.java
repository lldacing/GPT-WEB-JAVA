package com.cn.app.chatgptbot.controller.gpt;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.cn.app.chatgptbot.service.AsyncLogService;
import com.cn.app.chatgptbot.constant.CommonError;
import com.cn.app.chatgptbot.model.gptdto.GptAlphaDto;
import com.cn.app.chatgptbot.model.gptdto.GptBingDto;
import com.cn.app.chatgptbot.model.gptdto.GptCreditGrantsDto;
import com.cn.app.chatgptbot.model.gptdto.GptTurboDto;
import com.cn.app.chatgptbot.model.*;
import com.cn.app.chatgptbot.model.billing.CreditGrantsResponse;
import com.cn.app.chatgptbot.model.billing.OpenAiResponse;
import com.cn.app.chatgptbot.base.Result;
import com.cn.app.chatgptbot.service.IGptKeyService;
import com.cn.app.chatgptbot.service.IRefuelingKitService;
import com.cn.app.chatgptbot.service.IUseLogService;
import com.cn.app.chatgptbot.service.IUserService;
import com.cn.app.chatgptbot.uitls.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


/**
 * The type Gpt api controller.
 *
 * @author bdth
 * @email  2074055628@qq.com
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public final class GptApi {

    /**
     * broker tool
     */
    private final ProxyUtil proxyUtil;

    /**
     * weChat tool
     */
    private final WeChatDetectUtils weChatDetectUtils;

    @Resource
    IGptKeyService gptKeyService;
    @Resource
    IUserService userService;
    @Resource
    IRefuelingKitService refuelingKitService;
    @Resource
    IUseLogService useLogService;
    @Resource
    AsyncLogService asyncLogService;




    /**
     * Gets open id.
     *
     * @param code the code
     * @return the open id
     */
    @GetMapping(value = "/auth/{code}", name = "WeChat-OpenId", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getOpenId(@PathVariable final String code) {
        // Returns  WeChat user OpenId
        return Result.data(weChatDetectUtils.getUserOpenId(code));
    }

    /**
     * Gpt turbo result.
     *
     * @param dto the dto
     * @return the result
     */
    @PostMapping(value = "/chat/turbo", name = "GPT-Turbo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result gptTurbo(@Validated @RequestBody final GptTurboDto dto) {
//        final List<GptTurboModel.Messages> messages = dto.getMessages();
        /*
         * Obtain user-sent data for word interception
         * last one is always the data sent by  user
         */
//        weChatDetectUtils.filtration(messages.get(messages.size() - 1).getContent(), dto.getOpenId());
        // switch to the OpenAPI model
        final GptTurboModel gptTurboModel = GptTurboDto.convertToGptTurboModel(dto);
        // select the key randomly
        final String mainKey = GptUtil.getMainKey();
        Result result = checkUser(dto.getType(), mainKey, JSONObject.toJSONString(gptTurboModel.getMessages()),dto.getLogId());
        if(result.getCode() != 20000){
            return Result.error(result.getMsg());
        }
        // execute the request
        return Result.data( WebClientUtil.build(proxyUtil.getProxy(), "chat/completions", gptTurboModel, mainKey,(Long)result.getData()));

    }


    /**
     * Gpt alpha result.
     *
     * @param dto the dto
     * @return the result
     */
    @PostMapping(value = "/chat/official", name = "GPT-official", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result gptAlpha(@Validated @RequestBody final GptAlphaDto dto) {
        //filter sensitive words ( WeChat )
//        weChatDetectUtils.filtration(dto.getPrompt(), dto.getOpenId());
        // to DTO
        final GptAlphaModel gptAlphaModel = GptAlphaDto.convertToGptAlphaModel(dto);
        // get  key at random
        final String mainKey = GptUtil.getMainKey();
        Result result = checkUser(dto.getType(), mainKey, gptAlphaModel.getPrompt(),dto.getLogId());
        if(result.getCode() != 20000){
            return Result.error(result.getMsg());
        }
        return Result.data(
                WebClientUtil.build(proxyUtil.getProxy(), "images/generations", gptAlphaModel, mainKey,(Long)result.getData())
        );
    }

    /**
     * Gpt bing result.
     *
     * @param dto the dto
     * @return the result
     */
    @PostMapping(value = "/chat/bing", name = "微软Bing机器人", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result gptBing(@Validated @RequestBody final GptBingDto dto) {
        //filter sensitive words ( WeChat )
//        weChatDetectUtils.filtration(dto.getParam(), dto.getOpenId());
        // send  request
        return Result.data(WebClientUtil.intranet(dto.getParam()));

    }

    @PostMapping(value = "/creditGrants",produces = MediaType.APPLICATION_JSON_VALUE)
    public Result creditGrants(@Validated @RequestBody final GptCreditGrantsDto dto) throws JsonProcessingException {
        HttpResponse response = HttpRequest
                .get("https://api.openai.com/dashboard/billing/credit_grants")
                .header("Authorization", "Bearer " + dto.getKey())
                .execute();
        String body = response.body();
        if (!response.isOk()) {
            if (response.getStatus() == CommonError.OPENAI_AUTHENTICATION_ERROR.code()
                    || response.getStatus() == CommonError.OPENAI_LIMIT_ERROR.code()
                    || response.getStatus() == CommonError.OPENAI_SERVER_ERROR.code()) {
                OpenAiResponse openAiResponse = JSONUtil.toBean(response.body(), OpenAiResponse.class);
                log.error(openAiResponse.getError().getMessage());
                return Result.error(openAiResponse.getError().getMessage());
            }
            String errorMsg = response.body();
            log.error("询余额请求异常：{}", errorMsg);
            OpenAiResponse openAiResponse = JSONUtil.toBean(errorMsg, OpenAiResponse.class);
            if (Objects.nonNull(openAiResponse.getError())) {
                log.error(openAiResponse.getError().getMessage());
                return Result.error(openAiResponse.getError().getMessage());
            }
            return Result.error(CommonError.RETRY_ERROR.msg());
        }
        ObjectMapper mapper = new ObjectMapper();
        // 读取Json 返回值
        CreditGrantsResponse completionResponse = mapper.readValue(body, CreditGrantsResponse.class);
        return Result.data(completionResponse);

    }

    public Result checkUser(Integer type, String mainKey,String message,Long logId){
        List<GptKey> gptKeyList = gptKeyService.lambdaQuery().eq(GptKey::getKey, mainKey).last("limit 1").list();
        if(null == gptKeyList || gptKeyList.size() == 0){
            return Result.error("Key 异常 请稍后重试");
        }
        //查询当前用户信息
        UseLog useLog = new UseLog();
        useLog.setGptKey(mainKey);
        useLog.setUseValue(message);
        useLog.setUserId(JwtUtil.getUserId());
        User user = userService.getById(JwtUtil.getUserId());
        if(type != 2){
            if(type == 0){
                //判断剩余次数
                if(user.getRemainingTimes() < 1){
                    return Result.error("剩余次数不足请充值");
                }
                useLog.setUseType(1);
                user.setRemainingTimes(user.getRemainingTimes() - 1);
            }
            if(type == 1){
                //月卡用户 先查询是否有可用的加油包
                Long userKitId = refuelingKitService.getUserKitId();
                if(userKitId > 0){
                    useLog.setKitId(userKitId);
                    useLog.setUseType(3);
                }else {
                    //判断月卡是否到期
                    if(user.getExpirationTime().compareTo(LocalDateTime.now()) < 0){
                        //次数用户 查询用户次数
                        if(user.getRemainingTimes() < 1){
                            return Result.error("月卡过期或当日已超过最大访问次数");
                        }
                        useLog.setUseType(1);
                        user.setRemainingTimes(user.getRemainingTimes() - 1);
                    }else {
                        //是否已达今日已达上线
                        Integer dayUseNumber = useLogService.getDayUseNumber();
                        if((dayUseNumber + 1) > user.getCardDayMaxNumber()){
                            //判断剩余次数
                            if(user.getRemainingTimes() < 1){
                                return Result.error("月卡过期或当日已超过最大访问次数");
                            }
                            useLog.setUseType(1);
                            user.setRemainingTimes(user.getRemainingTimes() - 1);
                        }else {
                            useLog.setUseType(2);
                        }
                    }
                }
            }
        }
        GptKey gptKey = gptKeyList.get(0);
        gptKey.setUseNumber(gptKey.getUseNumber()+1);
        gptKey.setOperateTime(LocalDateTime.now());
        asyncLogService.saveKeyLog(gptKey,user);
        useLogService.save(useLog);
        if(null != logId){
            useLogService.removeById(logId);
        }
        return Result.data(useLog.getId());
    }


}
