package com.hjm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjm.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author hjm
* @description 针对表【user】的数据库操作Service
* @createDate 2025-05-20 11:14:24
*/
public interface UserService extends IService<User> {

    User getLoginUser(HttpServletRequest request);
}
