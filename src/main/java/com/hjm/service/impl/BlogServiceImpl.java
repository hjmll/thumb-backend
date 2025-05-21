package com.hjm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.hash.BloomFilter;
import com.hjm.constant.ThumbConstant;
import com.hjm.model.entity.Blog;
import com.hjm.model.entity.Thumb;
import com.hjm.model.entity.User;
import com.hjm.model.vo.BlogVO;
import com.hjm.service.BlogService;
import com.hjm.mapper.BlogMapper;
import com.hjm.service.ThumbService;
import com.hjm.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author hjm
* @description 针对表【blog】的数据库操作Service实现
* @createDate 2025-05-20 11:14:13
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private ThumbService thumbService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private BloomFilter<String> bloomFilter;




    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();

        if (ObjUtil.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());
            // 获取点赞
            List<Object> thumbList = redisTemplate.opsForHash().multiGet(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogIdList);
            for (int i = 0; i < thumbList.size(); i++) {
                if (thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }

        return blogList.stream()
                .map(blog -> {
                    BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                    blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }

    ///**
    // * 布隆过滤器+redis优化后
    // * @param blogList
    // * @param request
    // * @return
    // */
    //@Override
    //public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
    //    User loginUser = userService.getLoginUser(request);
    //    Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();
    //
    //    if (ObjUtil.isNotEmpty(loginUser)) {
    //        for (Blog blog : blogList) {
    //            String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId() + ":" + blog.getId();
    //            if (bloomFilter.mightContain(bloomKey)) {
    //                String key = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId() + ":" + blog.getId();
    //                Boolean hasThumb = redisTemplate.hasKey(key);
    //                if (hasThumb == null) {
    //                    // Redis 中不存在，检查是否超过过期时间
    //                    if (blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().plusMonths(1).isBefore(java.time.LocalDateTime.now())) {
    //                        // 超过过期时间，从数据库查询
    //                        hasThumb = thumbService.hasThumb(blog.getId(), loginUser.getId());
    //                    } else {
    //                        hasThumb = false;
    //                    }
    //                }
    //                blogIdHasThumbMap.put(blog.getId(), hasThumb);
    //            } else {
    //                blogIdHasThumbMap.put(blog.getId(), false);
    //            }
    //        }
    //    }
    //
    //    return blogList.stream()
    //            .map(blog -> {
    //                BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
    //                blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
    //                return blogVO;
    //            })
    //            .collect(Collectors.toList());
    //}

    private BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        if (loginUser == null) {
            return blogVO;
        }

        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        blogVO.setHasThumb(exist);


        return blogVO;
    }



}




