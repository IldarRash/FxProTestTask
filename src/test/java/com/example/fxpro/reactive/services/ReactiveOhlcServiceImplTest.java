package com.example.fxpro.reactive.services;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcPeriod;
import com.example.fxpro.common.TestQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.example.fxpro.common.OhlcPeriod.M1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class ReactiveOhlcServiceImplTest {

    @Autowired
    private ReactiveOhlcServiceImpl ohlcService;

    @Test
    @DisplayName("Test ohlc with no quotes")
    void testCurrentOhlcNoQuotes() {
        var current = ohlcService.getCurrent(0L, M1);
        assertThat(current.blockFirst())
                .isNull();
    }

    @ParameterizedTest
    @DisplayName("Single test for all period")
    @EnumSource(OhlcPeriod.class)
    void testSingleTestForAllPeriod(OhlcPeriod ohlcPeriod) {
        var instrumentId = 666L;
        ohlcService.onQuote(new TestQuote(123.0, instrumentId, System.currentTimeMillis()));

        var ohlcM1 = ohlcService.getCurrent(instrumentId, ohlcPeriod).blockFirst();
        assertThat(ohlcM1)
                .isNotNull()
                .returns(123.0, Ohlc::getOpenPrice)
                .returns(123.0, Ohlc::getHighPrice)
                .returns(123.0, Ohlc::getLowPrice)
                .returns(123.0, Ohlc::getClosePrice);

    }

    @ParameterizedTest
    @DisplayName("Attemting to find a ohlc which isn't in the cache")
    @EnumSource(OhlcPeriod.class)
    void testOhlcThatHaventQuote(OhlcPeriod period) {
        ohlcService.onQuote(new TestQuote(123.0, 666L, System.currentTimeMillis()));

        var ohlc = ohlcService.getCurrent(777L, period).blockFirst();
        assertThat(ohlc)
                .isNull();
    }

    @ParameterizedTest
    @DisplayName("Get connect for many clients")
    @EnumSource(OhlcPeriod.class)
    void testLastAndOpenPriceForAllPeriod(OhlcPeriod ohlcPeriod) {
        var instrumentId = 666L;

        var clientOne = ohlcService.getCurrent(instrumentId, ohlcPeriod);
        var clientTwo = ohlcService.getCurrent(instrumentId, ohlcPeriod);


        ohlcService.onQuote(new TestQuote(129.0, instrumentId, System.currentTimeMillis()));
        
        clientOne.doOnNext(ohlc -> {
            assertThat(ohlc)
                    .isNotNull()
                    .returns(129.0, Ohlc::getOpenPrice)
                    .returns(129.0, Ohlc::getHighPrice)
                    .returns(129.0, Ohlc::getLowPrice)
                    .returns(129.0, Ohlc::getClosePrice);
        }).subscribe();

        clientTwo.doOnNext(ohlc -> {
            assertThat(ohlc)
                    .isNotNull()
                    .returns(129.0, Ohlc::getOpenPrice)
                    .returns(129.0, Ohlc::getHighPrice)
                    .returns(129.0, Ohlc::getLowPrice)
                    .returns(129.0, Ohlc::getClosePrice);
        }).subscribe();
    }
}
