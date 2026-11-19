package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.sunpalaceartspace.annotation.MinaApiAuth;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序端滑动拼图验证码接口。
 *
 * <p>交互流程（aj-captcha 标准两步验证）：</p>
 * <ol>
 *   <li>前端 POST /captcha/get 获取底图 + 滑块图 + token + secretKey；</li>
 *   <li>用户拖动滑块，前端对坐标 AES 加密后 POST /captcha/check，通过后返回 captchaVerification；</li>
 *   <li>前端提交预约时携带 captchaVerification，由后端 {@code CaptchaService#verification} 二次校验（token 一次性）。</li>
 * </ol>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mina/app/captcha")
public class MinaCaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取滑动拼图验证码（底图/滑块图/token/secretKey）
     */
    @MinaApiAuth
    @PostMapping("/get")
    public R<Object> get(@RequestBody CaptchaVO captchaVO) {
        ResponseModel model = captchaService.get(captchaVO);
        if (!model.isSuccess()) {
            return R.fail(model.getRepMsg());
        }
        return R.ok(model.getRepData());
    }

    /**
     * 校验滑动结果，成功返回 repData.captchaVerification（提交预约时携带）
     */
    @MinaApiAuth
    @PostMapping("/check")
    public R<Object> check(@RequestBody CaptchaVO captchaVO) {
        ResponseModel model = captchaService.check(captchaVO);
        if (!model.isSuccess()) {
            return R.fail(model.getRepMsg());
        }
        return R.ok(model.getRepData());
    }
}
