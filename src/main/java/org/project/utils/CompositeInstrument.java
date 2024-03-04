package org.project.utils;

import java.util.List;

public class CompositeInstrument extends Instrument {

    private final List<InstrumentComponent> components;

    public CompositeInstrument(String id, String symbol, List<InstrumentComponent> components) {
        super(id, symbol);
        this.components = components;
    }

    public List<InstrumentComponent> getComponents() {
        return components;
    }
}
