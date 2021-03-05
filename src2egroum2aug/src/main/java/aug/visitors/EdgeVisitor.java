package aug.visitors;

import aug.model.controlflow.*;
import aug.model.dataflow.DefinitionEdge;
import aug.model.dataflow.ParameterEdge;
import aug.model.dataflow.QualifierEdge;
import aug.model.dataflow.ReceiverEdge;

public interface EdgeVisitor<R> {
    // Control Flow
    R visit(ContainsEdge edge);
    R visit(ExceptionHandlingEdge edge);
    R visit(FinallyEdge edge);
    R visit(OrderEdge edge);
    R visit(RepetitionEdge edge);
    R visit(SelectionEdge edge);

    R visit(RepetitionTrueEdge edge); // added by jaz
    R visit(RepetitionFalseEdge edge); // added by jaz
    R visit(SelectionTrueEdge edge); // added by jaz
    R visit(SelectionFalseEdge edge); // added by jaz

    R visit(SynchronizationEdge edge);
    R visit(ThrowEdge edge);
    // Data Flow
    R visit(DefinitionEdge edge);
    R visit(ParameterEdge edge);
    R visit(QualifierEdge edge);
    R visit(ReceiverEdge edge);
}
