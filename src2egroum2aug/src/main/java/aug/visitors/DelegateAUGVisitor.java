package aug.visitors;

import aug.model.actions.*;
import aug.model.controlflow.*;
import aug.model.data.*;
import aug.model.dataflow.DefinitionEdge;
import aug.model.dataflow.ParameterEdge;
import aug.model.dataflow.QualifierEdge;
import aug.model.dataflow.ReceiverEdge;

public class DelegateAUGVisitor<R> implements AUGElementVisitor<R> {
    private final AUGElementVisitor<R> delegate;

    public DelegateAUGVisitor(AUGElementVisitor<R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public R visit(ContainsEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(ExceptionHandlingEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(FinallyEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(OrderEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(RepetitionEdge edge) { return delegate.visit(edge); }

    @Override
    public R visit(SelectionEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(RepetitionTrueEdge edge) { return delegate.visit(edge); } // added by jaz

    @Override
    public R visit(RepetitionFalseEdge edge) { return delegate.visit(edge); } // added by jaz

    @Override
    public R visit(SelectionTrueEdge edge) {
        return delegate.visit(edge);
    } // added by jaz

    @Override
    public R visit(SelectionFalseEdge edge) {
        return delegate.visit(edge);
    } // added by jaz

    @Override
    public R visit(SynchronizationEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(ThrowEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(DefinitionEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(ParameterEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(QualifierEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(ReceiverEdge edge) {
        return delegate.visit(edge);
    }

    @Override
    public R visit(ArrayAccessNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ArrayAssignmentNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ArrayCreationNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(AssignmentNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(BreakNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(CastNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(CatchNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ConstructorCallNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ContinueNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(InfixOperatorNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(MethodCallNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(NullCheckNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ReturnNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(SuperConstructorCallNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ThrowNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(TypeCheckNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(UnaryOperatorNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(AnonymousClassMethodNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(AnonymousObjectNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ConstantNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(ExceptionNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(LiteralNode node) {
        return delegate.visit(node);
    }

    @Override
    public R visit(VariableNode node) {
        return delegate.visit(node);
    }
}
