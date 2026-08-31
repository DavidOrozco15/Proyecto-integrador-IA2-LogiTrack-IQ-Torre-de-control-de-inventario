package com.logitrack.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class FechasBogota {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private FechasBogota() {
    }

    public static LocalDate hoyBogota() {
        return LocalDate.now(BOGOTA);
    }

    public static LocalDateTime inicioDeHoyBogota() {
        return hoyBogota().atStartOfDay();
    }

    public static LocalDateTime finDeHoyBogota() {
        return hoyBogota().atTime(LocalTime.MAX);
    }

    public static LocalDateTime inicioDeAyerBogota() {
        return hoyBogota().minusDays(1).atStartOfDay();
    }

    public static LocalDateTime finDeAyerBogota() {
        return hoyBogota().minusDays(1).atTime(LocalTime.MAX);
    }

    public static LocalDateTime inicioUltimos30DiasBogota() {
        return hoyBogota().minusDays(30).atStartOfDay();
    }
}