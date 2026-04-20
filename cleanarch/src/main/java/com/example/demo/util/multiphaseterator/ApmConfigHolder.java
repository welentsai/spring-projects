package com.example.demo.util.multiphaseterator;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ApmConfigHolder {

    private final AtomicReference<List<String>> apmPhaseList = new AtomicReference<>(List.of());

    public List<String> getApmPhaseList() {
        return apmPhaseList.get();
    }

    public void setApmPhaseList(List<String> phases) {
        apmPhaseList.set(List.copyOf(phases));
    }

    public boolean hasPhase(String phase) {
        return apmPhaseList.get().contains(phase);
    }
}
