package com.hjm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjm.model.dto.thumb.DoThumbRequest;
import com.hjm.model.entity.Thumb;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author hjm
* @description 针对表【thumb】的数据库操作Service
* @createDate 2025-05-20 11:14:20
*/
public interface ThumbService extends IService<Thumb> {

    /**
     * 点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);


}
