package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * 微信扫码认证
 */
public interface WeChatAuthService {

    /**
     * 申请令牌，携带令牌查询用户信息，保存用户信息到数据库
     * @author Mr.M
     * @version 1.0
     * @description 微信认证接口
     * @date 2023/2/21 22:15
     */
    XcUser wxAuth(String code);
}
