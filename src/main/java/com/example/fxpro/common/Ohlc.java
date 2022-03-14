package com.example.fxpro.common;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;


@Builder
@Getter
@Setter
public class Ohlc {
    private final long instrumentId;
    private final double openPrice;
    private final double highPrice;
    private final double lowPrice;
    private final double closePrice;
    private final OhlcPeriod ohlcPeriod;
    private final long periodStartUtcTimestamp;


    public Ohlc update(double price, long timestamp, OhlcPeriod period, long instrumentId) {
        return Ohlc.builder()
                .openPrice(openPrice)
                .closePrice(price)
                .lowPrice(Math.min(lowPrice, price))
                .highPrice(Math.max(highPrice, price))
                .periodStartUtcTimestamp(timestamp)
                .ohlcPeriod(period)
                .instrumentId(instrumentId)
                .build();
    }

    public static Ohlc create(double price, long timestamp, OhlcPeriod period, long instrumentId) {
        return Ohlc.builder()
                .openPrice(price)
                .closePrice(price)
                .lowPrice(price)
                .highPrice(price)
                .periodStartUtcTimestamp(timestamp)
                .ohlcPeriod(period)
                .instrumentId(instrumentId)
                .build();
    }
}
