package aug.visitors;

import aug.model.actions.*;
import aug.model.controlflow.*;
import aug.model.data.*;
import aug.model.dataflow.DefinitionEdge;
import aug.model.dataflow.ParameterEdge;
import aug.model.dataflow.QualifierEdge;
import aug.model.dataflow.ReceiverEdge;

public class BaseAUGLabelProvider implements AUGLabelProvider {
    @Override
    public String visit(ContainsEdge edge) {
        return "contains";
    }

    @Override
    public String visit(ExceptionHandlingEdge edge) {
        return "hdl";
    }

    @Override
    public String visit(FinallyEdge edge) {
        return "finally";
    }

    @Override
    public String visit(OrderEdge edge) {
        return "order";
    }

    @Override
    public String visit(RepetitionEdge edge) {
        return "rep";
    }

    @Override
    public String visit(SelectionEdge edge) {
        return "sel";
    }

    @Override
    public String visit(RepetitionTrueEdge edge) {
        return "rep_T";
    } // added by jaz

    @Override
    public String visit(RepetitionFalseEdge edge) {
        return "rep_F";
    } // added by jaz

    @Override
    public String visit(SelectionTrueEdge edge) {
        return "sel_T";
    } // added by jaz

    @Override
    public String visit(SelectionFalseEdge edge) {
        return "sel_F";
    } // added by jaz

    @Override
    public String visit(SynchronizationEdge edge) {
        return "sync";
    }

    @Override
    public String visit(ThrowEdge edge) {
        return "throw";
    }

    @Override
    public String visit(DefinitionEdge edge) {
        return "def";
    }

    @Override
    public String visit(ParameterEdge edge) {
        return "para";
    }

    @Override
    public String visit(QualifierEdge edge) {
        return "qual";
    }

    @Override
    public String visit(ReceiverEdge edge) {
        return "recv";
    }

    @Override
    public String visit(ArrayAccessNode node) {
        return visit((MethodCallNode) node);
    }

    @Override
    public String visit(ArrayAssignmentNode node) {
        return visit((MethodCallNode) node);
    }

    @Override
    public String visit(ArrayCreationNode node) {
        return "{" + node.getDeclaringTypeName() + "}";
    }

    @Override
    public String visit(AssignmentNode node) {
        return "=";
    }

    @Override
    public String visit(BreakNode node) {
        return "<break>";
    }

    @Override
    public String visit(CastNode node) {
        return node.getTargetType() + ".<cast>";
    }

    @Override
    public String visit(CatchNode node) {
        return "<catch>";
    }

    @Override
    public String visit(ConstructorCallNode node) {
        return visit((MethodCallNode) node);
    }

    @Override
    public String visit(ContinueNode node) {
        return "<continue>";
    }

    @Override
    public String visit(InfixOperatorNode node) {
        return visit((OperatorNode) node);
    }

    @Override
    public String visit(MethodCallNode node) {
        return node.getDeclaringTypeName() + "." + node.getMethodSignature();
    }

    @Override
    public String visit(NullCheckNode node) {
        return visit((InfixOperatorNode) node);
    }

    private String visit(OperatorNode node) {
        return node.getOperator();
    }

    @Override
    public String visit(ReturnNode node) {
        return "<return>";
    }

    @Override
    public String visit(SuperConstructorCallNode node) {
        return node.getDeclaringTypeName() + "()";
    }

    @Override
    public String visit(ThrowNode node) {
        return "<throw>";
    }

    @Override
    public String visit(TypeCheckNode node) {
        return node.getTargetTypeName() + ".<instanceof>";
    }

    @Override
    public String visit(UnaryOperatorNode node) {
        return visit((OperatorNode) node);
    }

    @Override
    public String visit(AnonymousClassMethodNode node) {
        return node.getName();
    }

    @Override
    public String visit(AnonymousObjectNode node) {
        return node.getType();
    }

    @Override
    public String visit(ConstantNode node) {
        return node.getType();
    }

    @Override
    public String visit(ExceptionNode node) {
        return node.getType();
    }

    @Override
    public String visit(LiteralNode node) {
        return node.getType();
    }

    @Override
    public String visit(VariableNode node) {
        return node.getType();
    }
}
