package com.example.zzylike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zzylike.constant.UserConstant;
import com.example.zzylike.mapper.UserMapper;
import com.example.zzylike.model.entity.User;
import com.example.zzylike.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * @author
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    /**
     * 从 HttpServletRequest 对象中获取当前登录用户的信息。
     * 该方法通过 HttpServletRequest 对象获取与之关联的会话（Session），
     * 然后从会话中获取存储的登录用户信息。
     *
     * @param request HttpServletRequest 对象，代表客户端的 HTTP 请求。
     * @return 如果会话中存在登录用户信息，则返回存储的 User 对象；
     *         如果会话中不存在登录用户信息，或者存储的对象类型不是 User 类型，
     *         则返回 null（由于是强制类型转换，若类型不匹配会抛出 ClassCastException，此处假设调用者会处理异常）。
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
    }

}




