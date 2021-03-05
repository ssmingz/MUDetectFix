package de.tu_darmstadt.stg.mudetect.matcher;

import aug.model.DataNode;
import aug.model.Node;

public class AllDataNodeMatcher implements NodeMatcher {
    @Override
    public boolean test(Node targetNode, Node patternNode) {
        return targetNode instanceof DataNode && patternNode instanceof DataNode;
    }
}
