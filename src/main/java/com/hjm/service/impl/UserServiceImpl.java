package com.hjm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjm.constant.UserConstant;
import com.hjm.model.entity.User;
import com.hjm.service.UserService;
import com.hjm.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
* @author hjm
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-05-20 11:14:24
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
    }


}




