package eda.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class EventBus {

    private static final int MAX_TENTATIVAS = 3;

    private record Subscription(String subscriber, Consumer<Event> handler) {
    }

    private final Map<Class<? extends Event>, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final List<Envelope> eventStore = new CopyOnWriteArrayList<>();
    private final List<Envelope> deadLetterQueue = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;

    public EventBus() {
        AtomicInteger n = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "worker-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    // Consumer se inscreve em um tipo de evento (topico = classe do evento)
    public <T extends Event> void subscribe(String subscriber, Class<T> type, Consumer<T> handler) {
        subscriptions
                .computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(new Subscription(subscriber, e -> handler.accept(type.cast(e))));
    }

    // Producer publica e retorna na hora; a entrega acontece em outras threads
    public void publish(Event event) {
        Envelope envelope = new Envelope(UUID.randomUUID().toString().substring(0, 8), Instant.now(), event);
        eventStore.add(envelope);

        List<Subscription> destinos = new ArrayList<>();
        destinos.addAll(subscriptions.getOrDefault(event.getClass(), List.of()));
        destinos.addAll(subscriptions.getOrDefault(Event.class, List.of())); // quem escuta tudo (auditoria)

        for (Subscription s : destinos) {
            executor.submit(() -> deliver(envelope, s));
        }
    }

    private void deliver(Envelope envelope, Subscription s) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                s.handler().accept(envelope.event());
                return;
            } catch (Exception e) {
                Log.info("EventBus", s.subscriber() + " falhou em " + envelope.event().type()
                        + " (tentativa " + tentativa + "/" + MAX_TENTATIVAS + "): " + e.getMessage());
                Log.pausa(300);
            }
        }
        deadLetterQueue.add(envelope);
        Log.info("EventBus", "DLQ <- " + envelope.event().type() + " [" + envelope.event().pedidoId()
                + "] destino: " + s.subscriber());
    }

    public List<Envelope> getEventStore() {
        return List.copyOf(eventStore);
    }

    public List<Envelope> getDeadLetterQueue() {
        return List.copyOf(deadLetterQueue);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
