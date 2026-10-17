package com.soundmarketplace.observability;

public final class QueryCountHolder {

    private static final ThreadLocal<QueryStats> STATS = ThreadLocal.withInitial(QueryStats::new);

    private QueryCountHolder() {}

    public static void reset() {
        STATS.get().reset();
    }

    public static void record(long elapsedMs) {
        STATS.get().increment(elapsedMs);
    }

    public static int getCount() {
        return STATS.get().count;
    }

    public static long getTimeMs() {
        return STATS.get().totalTimeMs;
    }

    public static void clear() {
        STATS.remove();
    }

    private static class QueryStats {
        int count = 0;
        long totalTimeMs = 0;

        void reset() {
            count = 0;
            totalTimeMs = 0;
        }

        void increment(long timeMs) {
            count++;
            totalTimeMs += timeMs;
        }
    }
}
