package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.ucenter.feignClient.CheckCodeFeignClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service(value = "password_auth_service")
public class PasswordAuthServiceImpl implements AuthService {

    @Resource
    private XcUserMapper xcUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private CheckCodeFeignClient checkCodeFeignClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        // 账号
        String username = authParamsDto.getUsername();
        // 校验验证码
        String inputCode = authParamsDto.getCheckcode();
        // 验证码的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        if (StringUtils.isBlank(checkcodekey) || StringUtils.isBlank(checkcodekey)) {
            throw new RuntimeException("请输入验证码");
        }
        // 调用远程服务
        Boolean verify = checkCodeFeignClient.verify(checkcodekey, inputCode);
        if (verify == null || !verify) {
            throw new RuntimeException("验证码输入错误");
        }

        // 根据username查询数据库
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        // 用户不存在 返回null spring security框架抛出异常用户不存在
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        // 验证密码
        String passwordDb = user.getPassword();
        String passwordForm = authParamsDto.getPassword();
        boolean isMatches = passwordEncoder.matches(passwordForm, passwordDb);
        if (!isMatches) {
            throw new RuntimeException("账号或密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }
}
