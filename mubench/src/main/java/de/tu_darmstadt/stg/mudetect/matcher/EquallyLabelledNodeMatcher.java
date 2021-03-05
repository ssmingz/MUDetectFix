package de.tu_darmstadt.stg.mudetect.matcher;

import aug.model.Node;
import aug.visitors.AUGLabelProvider;
import aug.visitors.BaseAUGLabelProvider;

import java.util.function.Function;

public class EquallyLabelledNodeMatcher implements NodeMatcher {
    private final Function<Node, String> getLabel;

    public EquallyLabelledNodeMatcher(AUGLabelProvider labelProvider) {
        this.getLabel = labelProvider::getLabel;
    }

    @Override
    public boolean test(Node node1, Node node2) {
        return getLabel.apply(node1).equals(getLabel.apply(node2));
    }
}
