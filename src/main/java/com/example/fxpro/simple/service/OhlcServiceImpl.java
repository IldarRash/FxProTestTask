package com.example.fxpro.simple.service;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcKey;
import com.example.fxpro.common.OhlcPeriod;
import com.example.fxpro.common.Quote;
import com.example.fxpro.simple.dao.OhlcDao;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.fxpro.common.OhlcPeriod.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OhlcServiceImpl implements OhlcService {
    private final Cache<OhlcKey, Ohlc> ohlcCache;
    private final Map<OhlcPeriod, ReentrantLock> locsForPeriod = Map.of(
            M1, new ReentrantLock(),
            H1, new ReentrantLock(),
            D1, new ReentrantLock()
    );
    private final OhlcDao ohlcDao;

    @Override
    public Ohlc getCurrent(long instrumentId, OhlcPeriod period) {
        return ohlcCache.getIfPresent(new OhlcKey(period, instrumentId));
    }

    @Override
    public List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
        return ohlcDao.getHistorical(instrumentId, period);
    }

    @Override
    public List<Ohlc> getHistoricalAndCurrent(long instrumentId, OhlcPeriod period) {
        var historical = getHistorical(instrumentId, period);
        historical.add(getCurrent(instrumentId, period));
        return historical;
    }

    @Override
    public void onQuote(Quote quote) {
        log.debug("Quote is listened = {}", quote);
        for (var period : OhlcPeriod.values()) {
            var key = OhlcKey.of(period, quote.getInstrumentId());

            ohlcCache.asMap()
                    .merge(key,
                            Ohlc.create(quote.getPrice(), quote.getUtcTimestamp(), period, quote.getInstrumentId()),
                            (old, curr) -> old.update(quote.getPrice(), quote.getUtcTimestamp(), period, quote.getInstrumentId())
                    );
        }
    }
}
