package eda.servicos;

import eda.core.EventBus;
import eda.core.Log;
import eda.eventos.Eventos.EstoqueInsuficiente;
import eda.eventos.Eventos.EstoqueReservado;
import eda.eventos.Eventos.PagamentoRecusado;
import eda.eventos.Eventos.PedidoCriado;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PedidoService {

    private final EventBus bus;
    private final Map<String, String> pedidos = new ConcurrentHashMap<>();
    private final AtomicInteger sequencia = new AtomicInteger(1);

    public PedidoService(EventBus bus) {
        this.bus = bus;
        bus.subscribe("PedidoService", EstoqueReservado.class,
                e -> atualizar(e.pedidoId(), "CONFIRMADO"));
        bus.subscribe("PedidoService", PagamentoRecusado.class,
                e -> atualizar(e.pedidoId(), "CANCELADO (pagamento recusado)"));
        bus.subscribe("PedidoService", EstoqueInsuficiente.class,
                e -> atualizar(e.pedidoId(), "CANCELADO (sem estoque)"));
    }

    public String criarPedido(String cliente, String produto, int quantidade, double valor) {
        String id = "PED-" + sequencia.getAndIncrement();
        pedidos.put(id, "CRIADO");
        Log.info("PedidoService", id + " criado (" + cliente + ", " + quantidade + "x " + produto
                + ", " + String.format("R$ %.2f", valor) + ")");
        bus.publish(new PedidoCriado(id, cliente, produto, quantidade, valor));
        return id;
    }

    public String status(String pedidoId) {
        return pedidos.getOrDefault(pedidoId, "DESCONHECIDO");
    }

    private void atualizar(String pedidoId, String status) {
        pedidos.put(pedidoId, status);
        Log.info("PedidoService", pedidoId + " -> " + status);
    }
}
