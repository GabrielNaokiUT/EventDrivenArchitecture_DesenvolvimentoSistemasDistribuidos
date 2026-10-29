package eda.servicos;

import eda.core.EventBus;
import eda.core.Log;
import eda.eventos.Eventos.EstoqueInsuficiente;
import eda.eventos.Eventos.EstoqueReservado;
import eda.eventos.Eventos.PagamentoAprovado;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EstoqueService {

    private final EventBus bus;
    private final Map<String, Integer> estoque = new HashMap<>(Map.of("Notebook", 5, "Mouse", 50, "Monitor", 1));
    private final Set<String> processados = new HashSet<>();

    public EstoqueService(EventBus bus) {
        this.bus = bus;
        bus.subscribe("EstoqueService", PagamentoAprovado.class, this::reservar);
    }

    // synchronized: varias threads podem tentar reservar o mesmo produto ao mesmo tempo
    private synchronized void reservar(PagamentoAprovado e) {
        if (!processados.add(e.pedidoId())) { // consumer idempotente
            Log.info("EstoqueService", e.pedidoId() + " evento DUPLICADO ignorado (idempotencia)");
            return;
        }
        Log.pausa(400);
        int disponivel = estoque.getOrDefault(e.produto(), 0);
        if (disponivel >= e.quantidade()) {
            estoque.put(e.produto(), disponivel - e.quantidade());
            Log.info("EstoqueService", e.pedidoId() + " reservado " + e.quantidade() + "x " + e.produto()
                    + " (restam " + (disponivel - e.quantidade()) + ")");
            bus.publish(new EstoqueReservado(e.pedidoId(), e.cliente(), e.produto(), e.quantidade()));
        } else {
            Log.info("EstoqueService", e.pedidoId() + " SEM ESTOQUE de " + e.produto()
                    + " (pedido " + e.quantidade() + ", tem " + disponivel + ")");
            bus.publish(new EstoqueInsuficiente(e.pedidoId(), e.cliente(), e.produto(), e.valor()));
        }
    }
}
