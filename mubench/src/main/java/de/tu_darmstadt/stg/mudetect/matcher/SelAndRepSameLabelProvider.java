package de.tu_darmstadt.stg.mudetect.matcher;

import aug.model.controlflow.*;
import aug.visitors.AUGElementVisitor;
import aug.visitors.AUGLabelProvider;
import aug.visitors.DelegateAUGVisitor;

public class SelAndRepSameLabelProvider extends DelegateAUGVisitor<String> implements AUGLabelProvider {
    private static final String SEL_AND_REP_LABEL = "ctrl";

    public SelAndRepSameLabelProvider(AUGElementVisitor<String> fallbackLabelProvider) {
        super(fallbackLabelProvider);
    }

    @Override
    public String visit(SelectionEdge edge) {
        return SEL_AND_REP_LABEL;
    }

    @Override
    public String visit(RepetitionEdge edge) { return SEL_AND_REP_LABEL; }

    @Override
    public String visit(SelectionTrueEdge edge) {
        return SEL_AND_REP_LABEL;
    }

    @Override
    public String visit(RepetitionTrueEdge edge) { return SEL_AND_REP_LABEL; }

    @Override
    public String visit(SelectionFalseEdge edge) {
        return SEL_AND_REP_LABEL;
    }

    @Override
    public String visit(RepetitionFalseEdge edge) { return SEL_AND_REP_LABEL; }
}
