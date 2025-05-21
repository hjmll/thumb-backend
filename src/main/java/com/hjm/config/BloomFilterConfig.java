package com.hjm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;

@Configuration
public class BloomFilterConfig {

    @Bean
    public BloomFilter<String> bloomFilter() {
        // 预计插入元素数量
        int expectedInsertions = 1000000;
        // 误判率
        double fpp = 0.01;
        return BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, fpp);
    }
}
