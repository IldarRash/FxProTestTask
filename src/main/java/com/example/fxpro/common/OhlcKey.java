package com.example.fxpro.common;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class OhlcKey {
    private final OhlcPeriod period;
    private final long instrumentId;

    public static OhlcKey of(OhlcPeriod period, long instrumentId) {
        return new OhlcKey(period, instrumentId);
    }
}
