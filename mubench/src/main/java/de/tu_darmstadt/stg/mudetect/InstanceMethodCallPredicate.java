package de.tu_darmstadt.stg.mudetect;

import aug.model.Node;
import aug.model.actions.ConstructorCallNode;
import aug.model.actions.MethodCallNode;

import java.util.function.Predicate;

public class InstanceMethodCallPredicate implements Predicate<Node> {
    @Override
    public boolean test(Node node) {
        return node instanceof MethodCallNode && !(node instanceof ConstructorCallNode);
    }
}
