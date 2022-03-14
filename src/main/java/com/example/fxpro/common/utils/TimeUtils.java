package com.example.fxpro.common.utils;


import com.example.fxpro.common.OhlcPeriod;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static long nanosBeforeExpire(OhlcPeriod period) {
        return getRoundTimeToPeriod(period) - getNanos(LocalDateTime.now());
    }


    private static long getRoundTimeToPeriod(OhlcPeriod period) {
        switch (period) {
            case M1:
                return getNanos(LocalDateTime
                        .now()
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plus(Duration.ofMinutes(1)));

            case H1:
                return getNanos(LocalDateTime
                        .now()
                        .truncatedTo(ChronoUnit.HOURS)
                        .plus(Duration.ofHours(1)));
            case D1:
                return getNanos(LocalDateTime
                        .now()
                        .truncatedTo(ChronoUnit.DAYS)
                        .plus(Duration.ofDays(1)));
        }

        return 0L;
    }

    private static long getNanos(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        long epochNanos = TimeUnit.NANOSECONDS.convert(instant.getEpochSecond(), TimeUnit.SECONDS);
        epochNanos += instant.getNano();
        return epochNanos;
    }
}
