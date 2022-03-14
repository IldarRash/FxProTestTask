package com.example.fxpro.simple.service;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcKey;
import com.example.fxpro.common.utils.TimeUtils;
import com.example.fxpro.simple.dao.OhlcDao;
import com.github.benmanes.caffeine.cache.*;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class CacheConfiguration {
    @Value("${cache.sheduler.coreSize}")
    private int coreSize;
    /*
     * If ohlc is in cache after time expire, listener save it into storage
     */
    @Bean
    RemovalListener<OhlcKey, Ohlc> removalListener(OhlcDao ohlcDao) {
        return (key, ohlc, cause) -> {
            if (RemovalCause.EXPIRED == cause) {
                ohlcDao.store(ohlc);
            }
        };
    }

    @Bean
    public Cache<OhlcKey, Ohlc> cache(OhlcDao ohlcDao) {
        return Caffeine.newBuilder()
                .expireAfter(expiry())
                .removalListener(removalListener(ohlcDao))
                .scheduler(scheduler())
                .softValues()
                .build();
    }

    @Bean
    Scheduler scheduler() {
        ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool(coreSize);
        return Scheduler.forScheduledExecutorService(newScheduledThreadPool);
    }

    /*
     * Give expiration time to near period
     */
    @Bean
    Expiry<OhlcKey, Ohlc> expiry() {
        return new Expiry<>() {

            // nanos time before near minute, hour or day
            @Override
            public long expireAfterCreate(OhlcKey key, Ohlc value, long currentTime) {
                return TimeUtils.nanosBeforeExpire(value.getOhlcPeriod());
            }

            // dont update time after rewrite
            @Override
            public long expireAfterUpdate(OhlcKey key, Ohlc value, long currentTime, @NonNegative long currentDuration) {
                return currentDuration;
            }

            @Override
            public long expireAfterRead(OhlcKey key, Ohlc value, long currentTime, @NonNegative long currentDuration) {
                return currentDuration;
            }
        };
    }
}
