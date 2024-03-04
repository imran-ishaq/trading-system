package org.project.utils;

// InstrumentComponent (holds information about components within a composite instrument)
public class InstrumentComponent {

    private final Instrument instrument;
    private final double weight;

    public InstrumentComponent(Instrument instrument, double weight) {
        this.instrument = instrument;
        this.weight = weight;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public double getWeight() {
        return weight;
    }
}