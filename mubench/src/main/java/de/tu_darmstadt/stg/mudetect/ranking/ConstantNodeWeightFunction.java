package de.tu_darmstadt.stg.mudetect.ranking;

import aug.model.Node;

public class ConstantNodeWeightFunction implements NodeWeightFunction {
    @Override
    public double getWeight(Node node) {
        return 1;
    }
}
