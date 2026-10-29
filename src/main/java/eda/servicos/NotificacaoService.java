package eda.servicos;

import eda.core.EventBus;
import eda.core.Log;
import eda.eventos.Eventos.EstoqueInsuficiente;
import eda.eventos.Eventos.EstoqueReservado;
import eda.eventos.Eventos.PagamentoRecusado;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificacaoService {

    private final AtomicInteger falhasSimuladas = new AtomicInteger(0);

    public NotificacaoService(EventBus bus) {
        bus.subscribe("NotificacaoService", EstoqueReservado.class,
                e -> enviar(e.cliente(), "pedido " + e.pedidoId() + " confirmado!"));
        bus.subscribe("NotificacaoService", PagamentoRecusado.class,
                e -> enviar(e.cliente(), "pedido " + e.pedidoId() + " cancelado: " + e.motivo()));
        bus.subscribe("NotificacaoService", EstoqueInsuficiente.class,
                e -> enviar(e.cliente(), "pedido " + e.pedidoId() + " cancelado: sem estoque de " + e.produto()));
    }

    // Usado na demo para forcar falhas e mostrar retry / DLQ
    public void simularFalhas(int quantidade) {
        falhasSimuladas.set(quantidade);
    }

    private void enviar(String cliente, String mensagem) {
        Log.pausa(200);
        if (falhasSimuladas.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            throw new RuntimeException("servidor de e-mail indisponivel");
        }
        Log.info("NotificacaoService", "e-mail para " + cliente + ": " + mensagem);
    }
}
