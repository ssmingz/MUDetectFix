package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.ControlFlowEdge;
import aug.model.Node;

import static aug.model.Edge.Type.CONDITION;

public abstract class ConditionEdge extends BaseEdge implements ControlFlowEdge {
    public ConditionType conditionType;
    public String branch;

    public enum ConditionType {
        SELECTION("sel"),
        REPETITION("rep"),
        SELECTION_T("sel_T"),
        REPETITION_T("rep_T"),
        SELECTION_F("sel_F"),
        REPETITION_F("rep_F");

        private final String label;

        ConditionType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    protected ConditionEdge(Node source, Node target, ConditionType conditionType) {
        super(source, target, CONDITION);
        this.conditionType = conditionType;
    }

    /**
     * Use the edge's class type instead.
     */
    @Deprecated
    public ConditionType getConditionType() {
        return conditionType;
    }
}
