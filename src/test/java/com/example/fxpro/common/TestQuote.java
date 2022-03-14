package com.example.fxpro.common;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TestQuote implements Quote {
    private final double price;
    private final long instrumentedId;
    private final long time;

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public long getInstrumentId() {
        return instrumentedId;
    }

    @Override
    public long getUtcTimestamp() {
        return time;
    }
}
