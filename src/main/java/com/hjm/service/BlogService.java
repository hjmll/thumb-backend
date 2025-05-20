package com.hjm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjm.model.entity.Blog;
import com.hjm.model.vo.BlogVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author hjm
* @description 针对表【blog】的数据库操作Service
* @createDate 2025-05-20 11:14:13
*/
public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId, HttpServletRequest request);

    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);



}
