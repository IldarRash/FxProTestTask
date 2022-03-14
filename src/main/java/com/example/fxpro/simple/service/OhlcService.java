package com.example.fxpro.simple.service;

import com.example.fxpro.common.Ohlc;
import com.example.fxpro.common.OhlcPeriod;

import java.util.List;

public interface OhlcService extends QuoteListener {
    /** all OHLCs which are kept in a database */
    /** latest non persisted OHLC and OHLCs which are kept in a database */

    /** latest non persisted OHLC */
    Ohlc getCurrent (long instrumentId, OhlcPeriod period);

    /** all OHLCs which are kept in a database */
    List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period);

    /** latest non persisted OHLC and OHLCs which are kept in a database */
    List<Ohlc> getHistoricalAndCurrent (long instrumentId, OhlcPeriod period);
}
