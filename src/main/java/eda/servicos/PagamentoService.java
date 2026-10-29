package eda.servicos;

import eda.core.EventBus;
import eda.core.Log;
import eda.eventos.Eventos.EstoqueInsuficiente;
import eda.eventos.Eventos.PagamentoAprovado;
import eda.eventos.Eventos.PagamentoRecusado;
import eda.eventos.Eventos.PedidoCriado;

public class PagamentoService {

    private static final double LIMITE = 5000.0;

    private final EventBus bus;

    public PagamentoService(EventBus bus) {
        this.bus = bus;
        bus.subscribe("PagamentoService", PedidoCriado.class, this::processar);
        bus.subscribe("PagamentoService", EstoqueInsuficiente.class, this::estornar);
    }

    private void processar(PedidoCriado e) {
        Log.pausa(500); // simula o tempo do gateway de pagamento
        if (e.valor() <= LIMITE) {
            Log.info("PagamentoService", e.pedidoId() + " pagamento APROVADO");
            bus.publish(new PagamentoAprovado(e.pedidoId(), e.cliente(), e.produto(), e.quantidade(), e.valor()));
        } else {
            Log.info("PagamentoService", e.pedidoId() + " pagamento RECUSADO (acima do limite)");
            bus.publish(new PagamentoRecusado(e.pedidoId(), e.cliente(), "valor acima do limite"));
        }
    }

    // Transacao de compensacao: desfaz o pagamento quando o estoque falha
    private void estornar(EstoqueInsuficiente e) {
        Log.pausa(300);
        Log.info("PagamentoService", e.pedidoId() + " ESTORNO de " + String.format("R$ %.2f", e.valor()));
    }
}
