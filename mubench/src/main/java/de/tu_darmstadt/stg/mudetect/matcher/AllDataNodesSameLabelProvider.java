package de.tu_darmstadt.stg.mudetect.matcher;

import aug.model.data.ConstantNode;
import aug.model.data.ExceptionNode;
import aug.model.data.LiteralNode;
import aug.model.data.VariableNode;
import aug.visitors.AUGLabelProvider;
import aug.visitors.DelegateAUGVisitor;

public class AllDataNodesSameLabelProvider extends DelegateAUGVisitor<String> implements AUGLabelProvider {
    private static final String DATA_NODE_LABEL = "<Object>";

    public AllDataNodesSameLabelProvider(AUGLabelProvider fallbackLabelProvider) {
        super(fallbackLabelProvider);
    }

    @Override
    public String visit(LiteralNode node) {
        return DATA_NODE_LABEL;
    }

    @Override
    public String visit(ConstantNode node) {
        return DATA_NODE_LABEL;
    }

    @Override
    public String visit(VariableNode node) {
        return DATA_NODE_LABEL;
    }

    @Override
    public String visit(ExceptionNode node) {
        return DATA_NODE_LABEL;
    }
}
