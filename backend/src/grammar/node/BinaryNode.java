package grammar.node;

/**
 * Nodo que representa una operación binaria: AND u OR.
 * Tiene un hijo izquierdo y un hijo derecho.
 */
public class BinaryNode extends Node {

    private final String operator;
    private final Node   left;
    private final Node   right;

    /**
     * @param operator Operador lógico binario ("AND" u "OR")
     * @param left     Subárbol izquierdo
     * @param right    Subárbol derecho
     */
    public BinaryNode(String operator, Node left, Node right) {
        this.operator = operator;
        this.left     = left;
        this.right    = right;
    }

    public String getOperator() { return operator; }
    public Node   getLeft()     { return left;     }
    public Node   getRight()    { return right;    }

    @Override
    public String toTree(String indent) {
        return indent + operator + "\n"
            + left.toTree(indent + "  ") + "\n"
            + right.toTree(indent + "  ");
    }
}
