package com.example.fxpro.simple;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcKey;
import com.example.fxpro.common.OhlcPeriod;
import com.example.fxpro.simple.dao.OhlcDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class TestConfig {

    @Bean
    OhlcDao createOhlcDao(){
        return new OhlcDao() {
            private final Map<OhlcKey, List<Ohlc>> storage = new HashMap<>();

            @Override
            public void store(Ohlc ohlc) {
                storage.compute(
                        OhlcKey.of(ohlc.getOhlcPeriod(), ohlc.getInstrumentId()),
                        (key, ohlcs) -> {
                            if (ohlcs == null) {
                                ohlcs = new ArrayList<>();
                            }
                            ohlcs.add(ohlc);
                            return ohlcs;
                        }
                );
            }

            @Override
            public List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
                return storage.getOrDefault(OhlcKey.of(period, instrumentId), Collections.emptyList());
            }
        };
    }
}
