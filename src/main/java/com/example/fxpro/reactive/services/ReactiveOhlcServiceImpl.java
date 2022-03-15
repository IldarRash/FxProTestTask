package com.example.fxpro.reactive.services;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcKey;
import com.example.fxpro.common.OhlcPeriod;
import com.example.fxpro.common.Quote;
import com.example.fxpro.reactive.dao.SimpleReactiveDao;
import com.example.fxpro.simple.service.QuoteListener;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveOhlcServiceImpl implements QuoteListener {
    private final Sinks.Many<Ohlc> liveOhlc =
            Sinks.many().multicast().directBestEffort();

    private final SimpleReactiveDao simpleReactiveDao;
    private final Cache<OhlcKey, Ohlc> cache;

    public Flux<Ohlc> getCurrent(long instrumentId, OhlcPeriod period) {
        log.info("Get connect with current Ohlc and channel with update quotes = {}", instrumentId);
        return Mono.justOrEmpty(cache.getIfPresent(OhlcKey.of(period, instrumentId)))
                .mergeWith(liveOhlc.asFlux());
    }

    public Flux<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
        log.info("Get history Ohlc = {}", instrumentId);
        return simpleReactiveDao.getHistorical(instrumentId, period);
    }

    public Flux<Ohlc> getHistoricalAndCurrent(long instrumentId, OhlcPeriod period) {
        log.info("Get history Ohlc connect with current Ohlc and channel with update quotes = {}", instrumentId);
        return getCurrent(instrumentId, period)
                .mergeWith(getHistorical(instrumentId, period))
                .publishOn(Schedulers.single());
    }

    @Override
    public void onQuote(Quote quote) {
        for (var period : OhlcPeriod.values()) {
            var key = OhlcKey.of(period, quote.getInstrumentId());
            liveOhlc.tryEmitNext(
                    cache.asMap().merge(
                            key,
                            Ohlc.create(quote.getPrice(), quote.getUtcTimestamp(),
                                    period,
                                    quote.getInstrumentId()),
                            (old, curr) -> old.update(quote.getPrice(), quote.getUtcTimestamp(), period, quote.getInstrumentId())
                    )
            );
        }
    }
}
