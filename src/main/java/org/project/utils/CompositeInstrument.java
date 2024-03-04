package org.project.utils;

import org.project.exceptions.OrderException;

import java.util.List;


public class CompositeInstrument extends Instrument {

//    private static final Logger LOGGER = Logger.getLogger(CompositeInstrument.class.getName());

    private final List<InstrumentComponent> components;

    public CompositeInstrument(String id, String symbol, List<InstrumentComponent> components) throws OrderException {
        super(id, symbol);
        if (!components.isEmpty() && components.size() <= 3) {
            this.components = components;
        } else {
//            LOGGER.log(Level.SEVERE, "Basket orders must contain 1 to 3 instruments.");
            throw new OrderException("Basket orders must contain 1 to 3 instruments.");
        }
    }

    public List<InstrumentComponent> getComponents() {
        return components;
    }
}
