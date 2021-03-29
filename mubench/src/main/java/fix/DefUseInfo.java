package fix;

import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefUseInfo {
    private MethodDeclaration method;
    private Statement statement;
    public Set<String> def = new HashSet<>();
    public Set<String> use = new HashSet<>();

    public DefUseInfo(MethodDeclaration md, Statement stmt) {
        this.method = md;
        this.statement = stmt;
        computeDefUse(this.statement);
    }

    public DefUseInfo(MethodDeclaration md, Statement stmt, Set<String> preUse) {
        this.method = md;
        this.statement = stmt;
        this.use.addAll(preUse);
        computeDefUse(this.statement);
    }

    public void computeDefUse(ASTNode stmt) {
        if(stmt instanceof ExpressionStatement) {
            computeDefUse((ExpressionStatement) stmt);
        }
        else if(stmt instanceof IfStatement) {
            Expression cond = ((IfStatement) stmt).getExpression();
            addUse(cond);
        }
        else if(stmt instanceof WhileStatement) {
            Expression cond = ((WhileStatement) stmt).getExpression();
            addUse(cond);
        }
        else if(stmt instanceof DoStatement) {
            Expression cond = ((DoStatement) stmt).getExpression();
            addUse(cond);
        }
        else if(stmt instanceof ForStatement) {
            computeDefUse((ForStatement) stmt);
        }
        else if(stmt instanceof EnhancedForStatement) {
            computeDefUse((EnhancedForStatement) stmt);
        }
        else if(stmt instanceof VariableDeclarationStatement) {
            computeDefUse((VariableDeclarationStatement) stmt);
        }
        else if(stmt instanceof SwitchStatement) {
            Expression cond = ((SwitchStatement) stmt).getExpression();
            addUse(cond);
        }
        else if(stmt instanceof ReturnStatement) {
            Expression returnValue = ((ReturnStatement) stmt).getExpression();
            addUse(returnValue);
        }
        else if(stmt instanceof Block) {
            for(Object s : ((Block) stmt).statements()) {
                computeDefUse((Statement) s);
            }
        }
    }

    private void computeDefUse(VariableDeclarationStatement stmt) {
        for(Object vdf : stmt.fragments()) {
            Expression name = ((VariableDeclarationFragment) vdf).getName();
            addDef(name);
            Expression init = ((VariableDeclarationFragment) vdf).getInitializer();
            addUse(init);
        }
    }

    private void computeDefUse(EnhancedForStatement stmt) {
        // iterator
        SimpleName itr = stmt.getParameter().getName();
        addDef(itr);
        // collection
        Expression ct = stmt.getExpression();
        addUse(ct);
    }

    private void computeDefUse(ForStatement stmt) {
        // condition
        Expression cond = stmt.getExpression();
        addUse(cond);
        // initializers
        for(Object initializer : stmt.initializers()) {
            computeDefUse((Expression) initializer);
        }
        // updaters
        for(Object updater : stmt.updaters()) {
            computeDefUse((Expression) updater);
        }
    }

    private void computeDefUse(ExpressionStatement stmt) {
        Expression exp = stmt.getExpression();
        computeDefUse(exp);
    }

    private void computeDefUse(Expression expression) {
        if(expression instanceof MethodInvocation) {
            computeDefUse((MethodInvocation) expression);
        }
        else if(expression instanceof Assignment) {
            computeDefUse((Assignment) expression);
        }
        else if(expression instanceof VariableDeclarationExpression) {
            computeDefUse((VariableDeclarationExpression) expression);
        }
        else {
            addUse(expression);
        }
    }

    private void computeDefUse(Assignment assignment) {
        // use can be from right operand
        addUse(assignment.getRightHandSide());
        // def can be from left operand
        addDef(assignment.getLeftHandSide());
    }

    private void computeDefUse(MethodInvocation methodInvocation) {
        // use can be from receiver and argus
        Expression receiver = methodInvocation.getExpression();
        // def can be from method whose name is like "set...()", otherwise no def
        String methodName = methodInvocation.getName().getIdentifier();
        if(methodName.startsWith("set"))
            addDef(receiver);
        else
            addUse(receiver);
        List<ASTNode> args = methodInvocation.arguments();
        for(Object arg : args) {
            addUse((Expression) arg);
        }
    }

    private void computeDefUse(VariableDeclarationExpression varDeclExp) {
        for(Object vdf : varDeclExp.fragments()) {
            Expression name = ((VariableDeclarationFragment) vdf).getName();
            addDef(name);
            Expression init = ((VariableDeclarationFragment) vdf).getInitializer();
            addUse(init);
        }
    }

    private void addUse(Expression expression) {
        if(expression instanceof SimpleName) {
            this.use.add(((SimpleName) expression).getIdentifier());
        }
        else if(expression instanceof QualifiedName) {
            this.use.add(((QualifiedName) expression).getFullyQualifiedName());
            addUse(((QualifiedName) expression).getQualifier());
        }
        else if(expression instanceof ParenthesizedExpression) {
            addUse(((ParenthesizedExpression) expression).getExpression());
        }
        else if(expression instanceof InfixExpression) {
            addUse(((InfixExpression) expression).getLeftOperand());
            addUse(((InfixExpression) expression).getRightOperand());
            for(Object extend : ((InfixExpression) expression).extendedOperands()) {
                addUse((Expression) extend);
            }
        }
        else if(expression instanceof PrefixExpression) {
            addUse(((PrefixExpression) expression).getOperand());
        }
        else if(expression instanceof PostfixExpression) {
            addUse(((PostfixExpression) expression).getOperand());
        }
        else if(expression instanceof MethodInvocation) {
            computeDefUse((MethodInvocation) expression);
        }
        else if(expression instanceof ArrayCreation) {
            for(Object dimension : ((ArrayCreation) expression).dimensions()) {
                addUse((Expression) dimension);
            }
        }
        else if(expression instanceof InstanceofExpression) {
            addUse(((InstanceofExpression) expression).getLeftOperand());
        }
    }

    private void addDef(Expression exp) {
        if(exp instanceof SimpleName) {
            this.def.add(((SimpleName) exp).getIdentifier());
        }
        else if(exp instanceof QualifiedName) {
            this.def.add(((QualifiedName) exp).getFullyQualifiedName());
            addDef(((QualifiedName) exp).getQualifier());
        }
    }
}
