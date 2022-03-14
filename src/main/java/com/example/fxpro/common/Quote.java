package com.example.fxpro.common;

 public interface Quote {
    double getPrice();
    long getInstrumentId();
    long getUtcTimestamp();
}
