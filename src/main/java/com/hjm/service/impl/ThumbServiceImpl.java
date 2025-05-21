//package com.hjm.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.hash.BloomFilter;
//import com.hjm.constant.ThumbConstant;
//import com.hjm.model.dto.thumb.DoThumbRequest;
//import com.hjm.model.entity.Thumb;
//import com.hjm.model.entity.User;
//import com.hjm.model.entity.Blog;
//import com.hjm.service.BlogService;
//import com.hjm.service.ThumbService;
//import com.hjm.mapper.ThumbMapper;
//import com.hjm.service.UserService;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//
///**
//* @author hjm
//* @description 针对表【thumb】的数据库操作Service实现
//* @createDate 2025-05-20 11:14:20
//*/
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
//
//    private final UserService userService;
//
//    private final BlogService blogService;
//
//    private final TransactionTemplate transactionTemplate;
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private final BloomFilter<String> bloomFilter;
//
//    private final ObjectMapper objectMapper; // JSON处理工具
//
//    // 博客创建时间缓存的Key前缀
//    private static final String BLOG_CREATE_TIME_KEY = "blog:create_time:";
//
//    @Override
//    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
//        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
//            throw new RuntimeException("参数错误");
//        }
//        User loginUser = userService.getLoginUser(request);
//        Long blogId = doThumbRequest.getBlogId();
//        Blog blog = blogService.getById(blogId);
//
//        // 缓存博客创建时间到Redis
//        cacheBlogCreateTime(blogId, blog.getCreateTime());
//
//        // 加锁
//        synchronized (loginUser.getId().toString().intern()) {
//            return transactionTemplate.execute(status -> {
//                Boolean exists = this.hasThumb(blogId, loginUser.getId());
//                if (exists) {
//                    throw new RuntimeException("用户已点赞");
//                }
//
//                boolean update = blogService.lambdaUpdate()
//                        .eq(Blog::getId, blogId)
//                        .setSql("thumbCount = thumbCount + 1")
//                        .update();
//
//                Thumb thumb = new Thumb();
//                thumb.setUserId(loginUser.getId());
//                thumb.setBlogId(blogId);
//                boolean success = update && this.save(thumb);
//
//                // 点赞记录存入 Redis
//                if (success) {
//                    String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
//                    ThumbValue thumbValue = new ThumbValue(thumb.getId(),
//                            ThumbConstant.getExpirationTime(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()));
//
//                    try {
//                        // 将点赞信息转换为JSON存储
//                        String jsonValue = objectMapper.writeValueAsString(thumbValue);
//                        redisTemplate.opsForHash().put(userKey, blogId.toString(), jsonValue);
//                    } catch (Exception e) {
//                        log.error("转换点赞记录为JSON失败", e);
//                        return false;
//                    }
//
//                    // 将点赞记录加入布隆过滤器
//                    String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId() + ":" + blogId;
//                    bloomFilter.put(bloomKey);
//                }
//                return success;
//            });
//        }
//    }
//
//    //@Override
//    //public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
//    //    if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
//    //        throw new RuntimeException("参数错误");
//    //    }
//    //    User loginUser = userService.getLoginUser(request);
//    //    Long blogId = doThumbRequest.getBlogId();
//    //    Blog blog = blogService.getById(blogId);
//    //
//    //    //// 先通过布隆过滤器判断
//    //    //String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString()  + ":" + blogId.toString();
//    //    //if (!bloomFilter.mightContain(bloomKey)) {
//    //    //    return false;
//    //    //}
//    //
//    //    // 加锁
//    //    synchronized (loginUser.getId().toString().intern()) {
//    //
//    //        // 编程式事务
//    //        return transactionTemplate.execute(status -> {
//    //            //Long blogId = doThumbRequest.getBlogId();
//    //            Boolean exists = this.hasThumb(blogId, loginUser.getId());
//    //            if (exists) {
//    //                throw new RuntimeException("用户已点赞");
//    //            }
//    //
//    //            boolean update = blogService.lambdaUpdate()
//    //                    .eq(Blog::getId, blogId)
//    //                    .setSql("thumbCount = thumbCount + 1")
//    //                    .update();
//    //
//    //            Thumb thumb = new Thumb();
//    //            thumb.setUserId(loginUser.getId());
//    //            thumb.setBlogId(blogId);
//    //            // 更新成功才执行
//    //            boolean success = update && this.save(thumb);
//    //
//    //            // 点赞记录存入 Redis
//    //            //if (success) {
//    //            //    redisTemplate.opsForHash().put(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString(), thumb.getId());
//    //            //}
//    //            if (success) {
//    //                String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
//    //                redisTemplate.opsForHash().put(userKey, blogId.toString(), thumb.getId());
//    //                //String key = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString() + ":" + blogId.toString();
//    //                //long expirationTime = ThumbConstant.getExpirationTime(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime());
//    //                //redisTemplate.opsForValue().set(key, thumb.getId());
//    //                // 设置整个用户点赞集合的过期时间
//    //                long expirationTime = ThumbConstant.getExpirationTime(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime());
//    //                redisTemplate.expireAt(userKey, new Date(expirationTime * 1000));
//    //                // 将点赞记录加入布隆过滤器
//    //                String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId() + ":" + blogId;
//    //                bloomFilter.put(bloomKey);
//    //            }
//    //            // 更新成功才执行
//    //            return success;
//    //
//    //        });
//    //    }
//    //
//    //}
//    //@Override
//    //public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
//    //    if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
//    //        throw new RuntimeException("参数错误");
//    //    }
//    //    User loginUser = userService.getLoginUser(request);
//    //    // 加锁
//    //    synchronized (loginUser.getId().toString().intern()) {
//    //
//    //        // 编程式事务
//    //        return transactionTemplate.execute(status -> {
//    //            Long blogId = doThumbRequest.getBlogId();
//    //            Object thumbIdObj = redisTemplate.opsForHash().get(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogId);
//    //            if (thumbIdObj == null) {
//    //                throw new RuntimeException("用户未点赞");
//    //            }
//    //            Long thumbId = Long.valueOf(thumbIdObj.toString());
//    //
//    //            boolean update = blogService.lambdaUpdate()
//    //                    .eq(Blog::getId, blogId)
//    //                    .setSql("thumbCount = thumbCount - 1")
//    //                    .update();
//    //
//    //            boolean success = update && this.removeById(thumbId);
//    //
//    //            // 点赞记录从 Redis 删除
//    //            if (success) {
//    //                redisTemplate.opsForHash().delete(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogId);
//    //            }
//    //            return success;
//    //
//    //        });
//    //    }
//    //}
//
//    @Override
//    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
//        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
//            throw new RuntimeException("参数错误");
//        }
//        User loginUser = userService.getLoginUser(request);
//        Long blogId = doThumbRequest.getBlogId();
//        Blog blog = blogService.getById(blogId);
//
//        // 从Redis获取博客创建时间
//        LocalDateTime createTime = getBlogCreateTime(blogId);
//        if (createTime == null) {
//            // 如果Redis中没有，从数据库获取并缓存
//
//            createTime = blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
//            cacheBlogCreateTime(blogId, blog.getCreateTime());
//        }
//
//        // 先通过布隆过滤器判断
//        String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId() + ":" + blogId;
//        if (!bloomFilter.mightContain(bloomKey)) {
//            return false;
//        }
//        //
//        //// 加锁
//        //synchronized (loginUser.getId().toString().intern()) {
//        //    return transactionTemplate.execute(status -> {
//        //        String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
//        //        Object thumbIdObj = redisTemplate.opsForHash().get(userKey, blogId.toString());
//        //
//        //        if (thumbIdObj == null) {
//        //            // Redis 中不存在，检查是否超过过期时间
//        //            if (blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().plusMonths(1).isBefore(LocalDateTime.now())) {
//        //                // 超过过期时间，从数据库查询
//        //                thumbIdObj = this.lambdaQuery()
//        //                        .eq(Thumb::getUserId, loginUser.getId())
//        //                        .eq(Thumb::getBlogId, blogId)
//        //                        .one();
//        //            } else {
//        //                throw new RuntimeException("用户未点赞");
//        //            }
//        //        }
//        //        if (thumbIdObj == null) {
//        //            throw new RuntimeException("用户未点赞");
//        //        }
//        //        Long thumbId = Long.valueOf(thumbIdObj.toString());
//        //
//        //        boolean update = blogService.lambdaUpdate()
//        //                .eq(Blog::getId, blogId)
//        //                .setSql("thumbCount = thumbCount - 1")
//        //                .update();
//        //
//        //        boolean success = update && this.removeById(thumbId);
//        //
//        //        // 点赞记录从 Redis 删除
//        //        if (success) {
//        //            redisTemplate.opsForHash().delete(userKey, blogId.toString());
//        //            // 从布隆过滤器中移除（布隆过滤器不支持移除，可考虑定期重建）
//        //        }
//        //        return success;
//        //    });
//        //}
//
//        // 加锁
//        synchronized (loginUser.getId().toString().intern()) {
//            LocalDateTime finalCreateTime = createTime;
//            return transactionTemplate.execute(status -> {
//                String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
//                Object jsonValueObj = redisTemplate.opsForHash().get(userKey, blogId.toString());
//
//                if (jsonValueObj == null) {
//                    // Redis 中不存在，检查是否超过过期时间
//                    if (finalCreateTime.plusMonths(1).isBefore(LocalDateTime.now())) {
//                        // 超过过期时间，从数据库查询
//                        Thumb thumb = this.lambdaQuery()
//                                .eq(Thumb::getUserId, loginUser.getId())
//                                .eq(Thumb::getBlogId, blogId)
//                                .one();
//                        if (thumb == null) {
//                            throw new RuntimeException("用户未点赞");
//                        }
//                    } else {
//                        throw new RuntimeException("用户未点赞");
//                    }
//                } else {
//                    try {
//                        // 解析JSON获取点赞信息
//                        ThumbValue thumbValue = objectMapper.readValue(jsonValueObj.toString(), ThumbValue.class);
//                        long currentTime = System.currentTimeMillis() / 1000;
//
//                        if (thumbValue.getExpireTime() < currentTime) {
//                            // 数据已过期，从数据库查询并处理
//                            Thumb thumb = this.lambdaQuery()
//                                    .eq(Thumb::getUserId, loginUser.getId())
//                                    .eq(Thumb::getBlogId, blogId)
//                                    .one();
//                            if (thumb == null) {
//                                throw new RuntimeException("用户未点赞");
//                            }
//                            redisTemplate.opsForHash().delete(userKey, blogId.toString());
//                        } else {
//                            // 未过期，正常处理取消点赞
//                            boolean update = blogService.lambdaUpdate()
//                                    .eq(Blog::getId, blogId)
//                                    .setSql("thumbCount = thumbCount - 1")
//                                    .update();
//
//                            boolean success = update && this.removeById(thumbValue.getThumbId());
//
//                            // 从 Redis 删除
//                            if (success) {
//                                redisTemplate.opsForHash().delete(userKey, blogId.toString());
//                            }
//                            return success;
//                        }
//                    } catch (Exception e) {
//                        log.error("解析点赞记录JSON失败", e);
//                        return false;
//                    }
//                }
//                return false;
//            });
//        }
//    }
//
//
//
//
//    //@Override
//    //public Boolean hasThumb(Long blogId, Long userId) {
//    //    Blog blog = blogService.getById(blogId);
//    //    // 先通过布隆过滤器判断
//    //    String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId + ":" + blogId;
//    //    if (!bloomFilter.mightContain(bloomKey)) {
//    //        return false;
//    //    }
//    //
//    //    if (blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().plusMonths(1).isAfter(LocalDateTime.now())) {
//    //        // 热数据，查询 Redis
//    //        String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
//    //        return redisTemplate.opsForHash().hasKey(userKey, blogId.toString());
//    //    } else {
//    //        // 冷数据，查询数据库
//    //        return this.lambdaQuery()
//    //                .eq(Thumb::getUserId, userId)
//    //                .eq(Thumb::getBlogId, blogId)
//    //                .one() != null;
//    //    }
//    //    //return redisTemplate.opsForHash().hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
//    //}
//
//    @Override
//    public Boolean hasThumb(Long blogId, Long userId) {
//        // 从Redis获取博客创建时间
//        LocalDateTime createTime = getBlogCreateTime(blogId);
//        if (createTime == null) {
//            // 如果Redis中没有，从数据库获取并缓存
//            Blog blog = blogService.getById(blogId);
//            createTime = blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
//            cacheBlogCreateTime(blogId, blog.getCreateTime());
//        }
//
//        // 先通过布隆过滤器判断
//        String bloomKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId + ":" + blogId;
//        if (!bloomFilter.mightContain(bloomKey)) {
//            return false;
//        }
//
//        String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
//        Object jsonValueObj = redisTemplate.opsForHash().get(userKey, blogId.toString());
//
//        if (jsonValueObj != null) {
//            try {
//                // 解析JSON获取点赞信息
//                ThumbValue thumbValue = objectMapper.readValue(jsonValueObj.toString(), ThumbValue.class);
//                long currentTime = System.currentTimeMillis() / 1000;
//
//                if (thumbValue.getExpireTime() > currentTime) {
//                    return true;
//                } else {
//                    // 数据已过期，从数据库查询
//                    return this.lambdaQuery()
//                            .eq(Thumb::getUserId, userId)
//                            .eq(Thumb::getBlogId, blogId)
//                            .one() != null;
//                }
//            } catch (Exception e) {
//                log.error("解析点赞记录JSON失败", e);
//                return false;
//            }
//        } else {
//            // Redis 无记录，从数据库查询
//            return this.lambdaQuery()
//                    .eq(Thumb::getUserId, userId)
//                    .eq(Thumb::getBlogId, blogId)
//                    .one() != null;
//        }
//    }
//
//
//    // 缓存博客创建时间到Redis
//    private void cacheBlogCreateTime(Long blogId, Date createTime) {
//        String key = BLOG_CREATE_TIME_KEY + blogId;
//        long timestamp = createTime.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
//        redisTemplate.opsForValue().set(key, timestamp);
//
//        // 设置较长的过期时间，确保在博客存在期间都有效
//        redisTemplate.expire(key, 30, TimeUnit.DAYS);
//    }
//
//    // 从Redis获取博客创建时间
//    private LocalDateTime getBlogCreateTime(Long blogId) {
//        String key = BLOG_CREATE_TIME_KEY + blogId;
//        Object timestampObj = redisTemplate.opsForValue().get(key);
//
//        if (timestampObj != null) {
//            long timestamp = Long.parseLong(timestampObj.toString());
//            return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
//        }
//
//        return null;
//    }
//
//    // 点赞记录的内部类，用于JSON序列化/反序列化
//    private static class ThumbValue {
//        private Long thumbId;
//        private long expireTime;
//
//        public ThumbValue(Long thumbId, long expireTime) {
//            this.thumbId = thumbId;
//            this.expireTime = expireTime;
//        }
//
//        public Long getThumbId() {
//            return thumbId;
//        }
//
//        public long getExpireTime() {
//            return expireTime;
//        }
//    }
//
//}
//
//
//
//


package com.hjm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.hjm.constant.ThumbConstant;
import com.hjm.model.dto.thumb.DoThumbRequest;
import com.hjm.model.entity.Thumb;
import com.hjm.model.entity.User;
import com.hjm.model.entity.Blog;
import com.hjm.service.BlogService;
import com.hjm.service.ThumbService;
import com.hjm.mapper.ThumbMapper;
import com.hjm.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;
    private final BlogService blogService;
    private final TransactionTemplate transactionTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BloomFilter<String> bloomFilter;
    private final ObjectMapper objectMapper;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        Blog blog = blogService.getById(blogId);

        // 检查布隆过滤器
        String bloomKey = generateBloomKey(loginUser.getId(), blogId);
        if (!bloomFilter.mightContain(bloomKey)) {
            // 布隆过滤器认为不存在，直接处理点赞逻辑
            return processThumb(loginUser.getId(), blogId, blog);
        }

        // 布隆过滤器认为可能存在，进一步检查
        if (ThumbConstant.isHotData(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
            // 热数据：检查Redis
            String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
            Object jsonValue = redisTemplate.opsForHash().get(userKey, blogId.toString());

            if (jsonValue != null) {
                // Redis中存在，检查是否过期
                try {
                    ThumbValue thumbValue = objectMapper.readValue(jsonValue.toString(), ThumbValue.class);
                    if (thumbValue.getExpireTime() > System.currentTimeMillis() / 1000) {
                        throw new RuntimeException("用户已点赞");
                    }
                } catch (Exception e) {
                    log.error("解析点赞记录失败", e);
                    throw new RuntimeException("系统错误");
                }
            }
        } else {
            // 冷数据：检查数据库
            if (this.lambdaQuery()
                    .eq(Thumb::getUserId, loginUser.getId())
                    .eq(Thumb::getBlogId, blogId)
                    .count() > 0) {
                throw new RuntimeException("用户已点赞");
            }
        }

        // 继续处理点赞逻辑
        return processThumb(loginUser.getId(), blogId, blog);
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        Blog blog = blogService.getById(blogId);

        // 检查布隆过滤器
        String bloomKey = generateBloomKey(loginUser.getId(), blogId);
        if (!bloomFilter.mightContain(bloomKey)) {
            throw new RuntimeException("用户未点赞");
        }

        // 加锁
        synchronized (loginUser.getId().toString().intern()) {
            return transactionTemplate.execute(status -> {
                Thumb thumb = null;

                if (ThumbConstant.isHotData(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
                    // 热数据：检查Redis
                    String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
                    Object jsonValue = redisTemplate.opsForHash().get(userKey, blogId.toString());

                    if (jsonValue != null) {
                        try {
                            ThumbValue thumbValue = objectMapper.readValue(jsonValue.toString(), ThumbValue.class);
                            thumb = this.getById(thumbValue.getThumbId());
                        } catch (Exception e) {
                            log.error("解析点赞记录失败", e);
                            throw new RuntimeException("系统错误");
                        }
                    }
                } else {
                    // 冷数据：检查数据库
                    thumb = this.lambdaQuery()
                            .eq(Thumb::getUserId, loginUser.getId())
                            .eq(Thumb::getBlogId, blogId)
                            .one();
                }

                if (thumb == null) {
                    throw new RuntimeException("用户未点赞");
                }

                // 更新博客点赞数
                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount - 1")
                        .update();

                // 删除点赞记录
                boolean success = update && this.removeById(thumb.getId());

                // 更新Redis和布隆过滤器
                if (success) {
                    String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
                    redisTemplate.opsForHash().delete(userKey, blogId.toString());
                    // 布隆过滤器不支持删除，可考虑定期重建
                }

                return success;
            });
        }
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        Blog blog = blogService.getById(blogId);

        // 检查布隆过滤器
        String bloomKey = generateBloomKey(userId, blogId);
        if (!bloomFilter.mightContain(bloomKey)) {
            return false;
        }

        if (ThumbConstant.isHotData(blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
            // 热数据：检查Redis
            String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
            Object jsonValue = redisTemplate.opsForHash().get(userKey, blogId.toString());

            if (jsonValue != null) {
                try {
                    ThumbValue thumbValue = objectMapper.readValue(jsonValue.toString(), ThumbValue.class);
                    return thumbValue.getExpireTime() > System.currentTimeMillis() / 1000;
                } catch (Exception e) {
                    log.error("解析点赞记录失败", e);
                    return false;
                }
            }
        } else {
            // 冷数据：检查数据库
            return this.lambdaQuery()
                    .eq(Thumb::getUserId, userId)
                    .eq(Thumb::getBlogId, blogId)
                    .count() > 0;
        }

        return false;
    }

    // 处理点赞逻辑
    private Boolean processThumb(Long userId, Long blogId, Blog blog) {
        // 加锁
        synchronized (userId.toString().intern()) {
            return transactionTemplate.execute(status -> {
                // 更新博客点赞数
                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount + 1")
                        .update();

                // 保存点赞记录
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                boolean success = update && this.save(thumb);

                // 更新Redis和布隆过滤器
                if (success) {
                    String userKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
                    LocalDateTime publishTime = blog.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();

                    // 存储点赞记录到Redis
                    ThumbValue thumbValue = new ThumbValue(
                            thumb.getId(),
                            ThumbConstant.getExpirationTime(publishTime)
                    );

                    try {
                        String jsonValue = objectMapper.writeValueAsString(thumbValue);
                        redisTemplate.opsForHash().put(userKey, blogId.toString(), jsonValue);

                        // 存储博客创建时间到Redis
                        cacheBlogCreateTime(blogId, blog.getCreateTime());

                        // 添加到布隆过滤器
                        String bloomKey = generateBloomKey(userId, blogId);
                        bloomFilter.put(bloomKey);
                    } catch (Exception e) {
                        log.error("存储点赞记录失败", e);
                        throw new RuntimeException("系统错误");
                    }
                }

                return success;
            });
        }
    }

    // 缓存博客创建时间
    private void cacheBlogCreateTime(Long blogId, Date createTime) {
        String key = ThumbConstant.BLOG_CREATE_TIME_PREFIX + blogId;
        long timestamp = createTime.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
        redisTemplate.opsForValue().set(key, timestamp);

        // 设置较长的过期时间
        redisTemplate.expire(key, 365, TimeUnit.DAYS);
    }

    // 生成布隆过滤器的键
    private String generateBloomKey(Long userId, Long blogId) {
        return userId + ":" + blogId;
    }

    // 点赞记录的内部类
    private static class ThumbValue {
        private Long thumbId;
        private long expireTime;

        public ThumbValue(Long thumbId, long expireTime) {
            this.thumbId = thumbId;
            this.expireTime = expireTime;
        }

        public Long getThumbId() {
            return thumbId;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}
