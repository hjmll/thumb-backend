package com.hjm.constant;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author pine
 */
public interface ThumbConstant {

    /**
     * 用户点赞 hash key
     */
    String USER_THUMB_KEY_PREFIX = "thumb:";

    Long UN_THUMB_CONSTANT = 0L;

    /**
     * 临时 点赞记录 key
     */
    String TEMP_THUMB_KEY_PREFIX = "thumb:temp:%s";

    // 博客创建时间的Redis Key前缀
    String BLOG_CREATE_TIME_PREFIX = "blog:create_time:";


    /**
     * 计算点赞记录过期时间（帖子发布时间 + 1个月）
     * @param publishTime 帖子发布时间
     * @return 过期时间戳（秒）
     */
    static long getExpirationTime(LocalDateTime publishTime) {
        LocalDateTime expirationTime = publishTime.plusMonths(1);
        return expirationTime.toEpochSecond(ZoneOffset.UTC);
    }

    // 判断是否为热数据（发布时间在1个月内）
    static boolean isHotData(LocalDateTime publishTime) {
        return publishTime.plusMonths(1).isAfter(LocalDateTime.now());
    }
}


