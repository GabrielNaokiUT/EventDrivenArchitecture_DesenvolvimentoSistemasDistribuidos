package eda.core;

public interface Event {

    String pedidoId();

    default String type() {
        return getClass().getSimpleName();
    }
}
