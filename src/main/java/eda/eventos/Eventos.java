package eda.eventos;

import eda.core.Event;

public final class Eventos {

    private Eventos() {
    }

    public record PedidoCriado(String pedidoId, String cliente, String produto, int quantidade, double valor)
            implements Event {
    }

    public record PagamentoAprovado(String pedidoId, String cliente, String produto, int quantidade, double valor)
            implements Event {
    }

    public record PagamentoRecusado(String pedidoId, String cliente, String motivo) implements Event {
    }

    public record EstoqueReservado(String pedidoId, String cliente, String produto, int quantidade)
            implements Event {
    }

    public record EstoqueInsuficiente(String pedidoId, String cliente, String produto, double valor)
            implements Event {
    }
}
