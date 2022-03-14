package com.example.fxpro.reactive.dao;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcKey;
import com.example.fxpro.common.OhlcPeriod;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SimpleReactiveDao {
    private Map<OhlcKey, List<Ohlc>> data = new HashMap<>();

    public void store(Ohlc ohlc) {
        data.computeIfPresent( OhlcKey.of(ohlc.getOhlcPeriod(), ohlc.getInstrumentId()),
                (key, ohlcs) -> {
                    ohlcs.add(ohlc);
                    return ohlcs;
                });
    };

    public Flux<Ohlc> getHistorical (long instrumentId, OhlcPeriod period){
        return Flux.fromIterable(data.getOrDefault(OhlcKey.of(period, instrumentId), List.of()));
    };
}
