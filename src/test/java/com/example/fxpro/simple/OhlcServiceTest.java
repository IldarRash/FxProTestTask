package com.example.fxpro.simple;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcPeriod;
import com.example.fxpro.common.TestQuote;
import com.example.fxpro.simple.service.OhlcService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.example.fxpro.common.OhlcPeriod.H1;
import static com.example.fxpro.common.OhlcPeriod.M1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest
@Import(TestConfig.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class OhlcServiceTest {

    @Autowired
    private OhlcService ohlcService;

    /*
     * Tests get current method
     * single thread and all periods
     */
    @Test
    @DisplayName("Test ohlc with no quotes")
    void testCurrentOhlcNoQuotes() {
        var current = ohlcService.getCurrent(0L, M1);
        assertThat(current)
                .isNull();
    }

    @ParameterizedTest
    @DisplayName("Single test for all period")
    @EnumSource(OhlcPeriod.class)
    void testSingleTestForGetCurrent(OhlcPeriod ohlcPeriod) {
        var instrumentId = 666L;
        ohlcService.onQuote(new TestQuote(123.0, instrumentId, System.currentTimeMillis()));

        var ohlcM1 = ohlcService.getCurrent(instrumentId, ohlcPeriod);
        assertThat(ohlcM1)
                .isNotNull()
                .returns(123.0, Ohlc::getOpenPrice)
                .returns(123.0, Ohlc::getHighPrice)
                .returns(123.0, Ohlc::getLowPrice)
                .returns(123.0, Ohlc::getClosePrice);

    }

    @ParameterizedTest
    @DisplayName("Get last and open price test for all period")
    @EnumSource(OhlcPeriod.class)
    void testLastAndOpenPriceForGetCurrent(OhlcPeriod ohlcPeriod) {
        var instrumentId = 666L;
        ohlcService.onQuote(new TestQuote(123.0, instrumentId, System.currentTimeMillis()));
        ohlcService.onQuote(new TestQuote(126.0, instrumentId, System.currentTimeMillis()));
        ohlcService.onQuote(new TestQuote(129.0, instrumentId, System.currentTimeMillis()));

        var ohlc = ohlcService.getCurrent(instrumentId, ohlcPeriod);
        assertThat(ohlc)
                .isNotNull()
                .returns(123.0, Ohlc::getOpenPrice)
                .returns(129.0, Ohlc::getHighPrice)
                .returns(123.0, Ohlc::getLowPrice)
                .returns(129.0, Ohlc::getClosePrice);
    }


    @ParameterizedTest
    @DisplayName("Attemting to find a ohlc which isn't in the cache")
    @EnumSource(OhlcPeriod.class)
    void testOhlcThatHaventQuote(OhlcPeriod period) {
        ohlcService.onQuote(new TestQuote(123.0, 666L, System.currentTimeMillis()));

        var ohlc = ohlcService.getCurrent(777L, period);
        assertThat(ohlc)
                .isNull();
    }


    /*
     * Tests get historical method and currentAndHistorical
     * single thread and all periods
     */
    @Test
    @DisplayName("Test ohlc dont save while period is open")
    void testGetHistoricalBeforePeriodClose() throws InterruptedException {
        for (int price = 0; price < 5; price++) {
            ohlcService.onQuote(new TestQuote(price * 66.6, 666L, System.currentTimeMillis()));
        }

        // Save one ohlc because other quotes update current
        var historicalAndCurrent = ohlcService.getHistorical(666L, H1);

        assertThat(historicalAndCurrent).hasSize(0);
    }

    @Test
    @DisplayName("Test ohlc save after one minute period is close")
    void testGetHistorical() throws InterruptedException {
        for (int price = 0; price < 5; price++) {
            ohlcService.onQuote(new TestQuote(price * 66.6, 666L, System.currentTimeMillis()));
        }

        // Need to sleep before period is close
        Thread.sleep(60 * 1000);

        // Save one ohlc because other quotes update current
        var historicalAfterPeriodClose = ohlcService.getHistorical(666L, M1);

        // Then
        assertThat(historicalAfterPeriodClose).hasSize(1);

        assertThat(historicalAfterPeriodClose.get(0))
                .isNotNull()
                .returns(0.0, Ohlc::getOpenPrice)
                .returns(266.4, Ohlc::getHighPrice)
                .returns(0.0, Ohlc::getLowPrice)
                .returns(266.4, Ohlc::getClosePrice);

    }

    @Test
    @DisplayName("Test ohlc save after one minute period is close with current ohlc")
    void testGetHistoricalAndCurrent() throws InterruptedException {
        for (int price = 0; price < 5; price++) {
            ohlcService.onQuote(new TestQuote(price * 66.6, 666L, System.currentTimeMillis()));
        }

        // Need to sleep before period is close
        Thread.sleep(60 * 1000);

        ohlcService.onQuote(new TestQuote(666.6, 666L, System.currentTimeMillis()));

        // Save one ohlc because other quotes update current
        var historicalAndCurrent = ohlcService.getHistoricalAndCurrent(666L, M1);

        assertThat(historicalAndCurrent).hasSize(2);

        assertThat(historicalAndCurrent.get(0))
                .isNotNull()
                .returns(0.0, Ohlc::getOpenPrice)
                .returns(266.4, Ohlc::getHighPrice)
                .returns(0.0, Ohlc::getLowPrice)
                .returns(266.4, Ohlc::getClosePrice);

        assertThat(historicalAndCurrent.get(1))
                .isNotNull()
                .returns(666.6, Ohlc::getOpenPrice)
                .returns(666.6, Ohlc::getHighPrice)
                .returns(666.6, Ohlc::getLowPrice)
                .returns(666.6, Ohlc::getClosePrice);
    }

    @ParameterizedTest
    @DisplayName("Multi thread test for getCurrent method")
    @EnumSource(OhlcPeriod.class)
    void testMultiThreadGetCurrent(OhlcPeriod period) throws ExecutionException, InterruptedException {
        var excecutor = Executors.newFixedThreadPool(3);

        Consumer<Long> task = (instumenredId) -> {
            for (int price = 0; price < 5; price++) {
                ohlcService.onQuote(new TestQuote(price * 66.6, instumenredId, System.currentTimeMillis()));
            }
        };

        var listFuture = new ArrayList<Future<?>>();
        for (int thead = 0; thead < 5; thead++) {
            listFuture.add(excecutor.submit(() -> task.accept(666L)));

        }

        for (Future<?> future : listFuture) {
            future.get();
        }

        var ohlc = ohlcService.getCurrent(666L, period);

        assertThat(ohlc)
                .isNotNull()
                .returns(0.0, Ohlc::getOpenPrice)
                .returns(266.4, Ohlc::getHighPrice)
                .returns(0.0, Ohlc::getLowPrice)
                .returns(266.4, Ohlc::getClosePrice);

    }

    @Test
    @DisplayName("Multi thread test for getHistorical method")
    void testMultiThreadGetHistorical() throws ExecutionException, InterruptedException {
        var excecutor = Executors.newFixedThreadPool(3);

        Consumer<Long> task = (instumenredId) -> {
            for (int price = 0; price < 5; price++) {
                ohlcService.onQuote(new TestQuote(price * 66.6, instumenredId, System.currentTimeMillis()));
            }
        };

        var listFuture = new ArrayList<Future<?>>();
        for (int thead = 0; thead < 5; thead++) {
            listFuture.add(excecutor.submit(() -> task.accept(666L)));

        }

        for (Future<?> future : listFuture) {
            future.get();
        }


        // Need to sleep before period is close
        Thread.sleep(60 * 1000);

        // Save one ohlc because other quotes update current
        var historicalAfterPeriodClose = ohlcService.getHistorical(666L, M1);

        assertThat(historicalAfterPeriodClose).hasSize(1);
        assertThat(historicalAfterPeriodClose.get(0))
                .isNotNull()
                .returns(0.0, Ohlc::getOpenPrice)
                .returns(266.4, Ohlc::getHighPrice)
                .returns(0.0, Ohlc::getLowPrice)
                .returns(266.4, Ohlc::getClosePrice);
    }
}


