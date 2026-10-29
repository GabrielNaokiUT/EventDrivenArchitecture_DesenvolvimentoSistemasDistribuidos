package eda;

import eda.core.Envelope;
import eda.core.EventBus;
import eda.core.Log;
import eda.eventos.Eventos.PagamentoAprovado;
import eda.servicos.AuditoriaService;
import eda.servicos.EstoqueService;
import eda.servicos.NotificacaoService;
import eda.servicos.PagamentoService;
import eda.servicos.PedidoService;

import java.util.Scanner;

public class App {

    private static final EventBus bus = new EventBus();
    private static final PedidoService pedidos = new PedidoService(bus);
    private static final NotificacaoService notificacao = new NotificacaoService(bus);

    public static void main(String[] args) {
        new PagamentoService(bus);
        new EstoqueService(bus);
        new AuditoriaService(bus);

        Scanner in = new Scanner(System.in);
        while (true) {
            menu();
            if (!in.hasNextLine()) {
                break;
            }
            String opcao = in.nextLine().trim();
            System.out.println();
            switch (opcao) {
                case "1" -> cenarioSucesso();
                case "2" -> cenarioPagamentoRecusado();
                case "3" -> cenarioSemEstoque();
                case "4" -> cenarioAssincrono();
                case "5" -> cenarioRetry();
                case "6" -> cenarioDlq();
                case "7" -> cenarioDuplicado();
                case "8" -> mostrarEventStore();
                case "0" -> {
                    bus.shutdown();
                    return;
                }
                default -> System.out.println("Opcao invalida");
            }
        }
        bus.shutdown();
    }

    private static void menu() {
        System.out.println();
        System.out.println("===== EDA - Sistema de Pedidos =====");
        System.out.println("1) Pedido com sucesso");
        System.out.println("2) Pagamento recusado (cancelamento)");
        System.out.println("3) Sem estoque (compensacao / estorno)");
        System.out.println("4) 3 pedidos ao mesmo tempo (assincrono + consistencia eventual)");
        System.out.println("5) Falha temporaria na notificacao (retry)");
        System.out.println("6) Falha permanente na notificacao (Dead Letter Queue)");
        System.out.println("7) Evento duplicado (idempotencia)");
        System.out.println("8) Ver Event Store e DLQ");
        System.out.println("0) Sair");
        System.out.print("Opcao: ");
    }

    private static void cenarioSucesso() {
        String id = pedidos.criarPedido("Ana", "Mouse", 2, 150.0);
        aguardar(3000);
        System.out.println(">> Status final de " + id + ": " + pedidos.status(id));
    }

    private static void cenarioPagamentoRecusado() {
        String id = pedidos.criarPedido("Bruno", "Notebook", 1, 9000.0);
        aguardar(3000);
        System.out.println(">> Status final de " + id + ": " + pedidos.status(id));
    }

    private static void cenarioSemEstoque() {
        String id = pedidos.criarPedido("Carla", "Monitor", 3, 3000.0);
        aguardar(3000);
        System.out.println(">> Status final de " + id + ": " + pedidos.status(id));
    }

    private static void cenarioAssincrono() {
        String a = pedidos.criarPedido("Ana", "Mouse", 1, 80.0);
        String b = pedidos.criarPedido("Bia", "Mouse", 1, 90.0);
        String c = pedidos.criarPedido("Caio", "Mouse", 3, 200.0);
        System.out.println(">> Os 3 publish() ja retornaram. Status agora: "
                + pedidos.status(a) + " / " + pedidos.status(b) + " / " + pedidos.status(c)
                + "  (consistencia eventual)");
        aguardar(4000);
        System.out.println(">> Status final: " + pedidos.status(a) + " / " + pedidos.status(b)
                + " / " + pedidos.status(c));
    }

    private static void cenarioRetry() {
        notificacao.simularFalhas(2);
        String id = pedidos.criarPedido("Ana", "Mouse", 1, 50.0);
        aguardar(4000);
        System.out.println(">> Status final de " + id + ": " + pedidos.status(id)
                + " (a notificacao falhou 2x e deu certo na 3a tentativa)");
    }

    private static void cenarioDlq() {
        notificacao.simularFalhas(3);
        String id = pedidos.criarPedido("Davi", "Mouse", 1, 60.0);
        aguardar(4000);
        System.out.println(">> Status final de " + id + ": " + pedidos.status(id)
                + " (pedido nao foi afetado pela falha da notificacao)");
        System.out.println(">> Mensagens na DLQ: " + bus.getDeadLetterQueue().size());
    }

    private static void cenarioDuplicado() {
        String id = pedidos.criarPedido("Eva", "Mouse", 2, 120.0);
        aguardar(3000);
        System.out.println(">> Reenviando PagamentoAprovado de " + id + " (simula entrega duplicada)");
        bus.publish(new PagamentoAprovado(id, "Eva", "Mouse", 2, 120.0));
        aguardar(1500);
    }

    private static void mostrarEventStore() {
        System.out.println("--- EVENT STORE (" + bus.getEventStore().size() + " eventos) ---");
        for (Envelope env : bus.getEventStore()) {
            System.out.println(env.id() + " | " + Log.hora(env.occurredAt()) + " | "
                    + env.event().type() + " | " + env.event().pedidoId());
        }
        System.out.println("--- DEAD LETTER QUEUE (" + bus.getDeadLetterQueue().size() + ") ---");
        for (Envelope env : bus.getDeadLetterQueue()) {
            System.out.println(env.id() + " | " + env.event().type() + " | " + env.event().pedidoId());
        }
    }

    private static void aguardar(long ms) {
        Log.pausa(ms);
    }
}
