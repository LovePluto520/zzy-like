package com.example.zzylike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zzylike.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author pine
 */
public interface UserService extends IService<User> {

    User getLoginUser(HttpServletRequest request);
}
