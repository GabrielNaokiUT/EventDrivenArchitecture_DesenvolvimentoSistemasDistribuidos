package eda.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Log {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private Log() {
    }

    public static void info(String origem, String mensagem) {
        System.out.printf("%s [%-8s] %-20s %s%n",
                hora(Instant.now()), Thread.currentThread().getName(), origem, mensagem);
    }

    public static String hora(Instant instante) {
        return FMT.format(instante);
    }

    public static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
