package grammar.node;

/**
 * Nodo que representa una operación unaria: NOT.
 * Tiene un único hijo (el operando negado).
 */
public class UnaryNode extends Node {

    private final String operator;
    private final Node   operand;

    /**
     * @param operator Operador lógico unario ("NOT")
     * @param operand  Subárbol del operando
     */
    public UnaryNode(String operator, Node operand) {
        this.operator = operator;
        this.operand  = operand;
    }

    public String getOperator() { return operator; }
    public Node   getOperand()  { return operand;  }

    @Override
    public String toTree(String indent) {
        return indent + operator + "\n"
            + operand.toTree(indent + "  ");
    }
}
