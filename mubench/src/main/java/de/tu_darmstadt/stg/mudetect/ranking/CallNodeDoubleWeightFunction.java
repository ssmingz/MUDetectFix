package de.tu_darmstadt.stg.mudetect.ranking;

import aug.model.actions.MethodCallNode;
import aug.model.Node;

public class CallNodeDoubleWeightFunction implements NodeWeightFunction {
    @Override
    public double getWeight(Node node) {
        return node instanceof MethodCallNode ? 2 : 1;
    }
}
