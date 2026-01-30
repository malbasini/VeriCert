package com.example.vericert.service;

import java.time.LocalDate;

final class UsageWindowCalculator {

    private UsageWindowCalculator() {}

    /**
     * Calcola la finestra mensile corrente ancorata a periodStart.
     * Esempio: periodo annuale che parte il 17/03 ->
     * finestre: 17/03-16/04, 17/04-16/05, ...
     *
     * Ritorna [from, to] inclusivi.
     */
    static LocalDate[] currentMonthlyWindow(LocalDate periodStart,
                                            LocalDate periodEnd,
                                            LocalDate today) {

        // Limitiamo la finestra al massimo a "oggi" e alla fine periodo
        LocalDate effectiveEnd = periodEnd.isBefore(today) ? periodEnd : today;

        if (today.isBefore(periodStart)) {
            return new LocalDate[]{periodStart, effectiveEnd};
        }

        LocalDate windowStart = periodStart;

        // Avanza a blocchi mensili fino ad arrivare alla finestra corrente
        // (max 12 iterazioni per annuale => costo minimo)
        while (!windowStart.plusMonths(1).isAfter(effectiveEnd.plusDays(1))) {
            windowStart = windowStart.plusMonths(1);
        }

        LocalDate windowEnd = windowStart.plusMonths(1).minusDays(1);

        //if (windowEnd.isAfter(effectiveEnd)) {
        //    windowEnd = effectiveEnd;
        //}

        return new LocalDate[]{windowStart, windowEnd};
    }
}
