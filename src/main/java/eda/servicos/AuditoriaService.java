package eda.servicos;

import eda.core.Event;
import eda.core.EventBus;
import eda.core.Log;

public class AuditoriaService {

    public AuditoriaService(EventBus bus) {
        bus.subscribe("AuditoriaService", Event.class,
                e -> Log.info("AuditoriaService", "[AUDIT] " + e.type() + " | " + e.pedidoId()));
    }
}
