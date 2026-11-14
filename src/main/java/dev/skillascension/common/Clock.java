package dev.skillascension.common;

/**
 * Milliseconds since the game started. Bonus timers keep times in floats; a float cannot hold an epoch
 * time in milliseconds (it rounds to about 2 minutes), so timers use this small number instead.
 */
public final class Clock {
    private static final long START = System.nanoTime();

    private Clock() {
    }

    public static long now() {
        return (System.nanoTime() - START) / 1_000_000L;
    }
}
