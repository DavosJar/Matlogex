package grammar.node;

/**
 * Convierte un árbol de nodos Node a formato JSON.
 * No usa librerías externas, construye el JSON manualmente.
 */
public class NodeSerializer {

    /**
     * Serializa un nodo y sus hijos recursivamente a JSON.
     *
     * @param node Nodo raíz del árbol o subárbol
     * @return String con representación JSON del árbol
     */
    public static String toJson(Node node) {
        if (node instanceof VariableNode) {
            VariableNode v = (VariableNode) node;
            return "{\"type\":\"variable\",\"name\":\"" + v.getName() + "\"}";
        }
        if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            return "{\"type\":\"unary\",\"operator\":\"" + u.getOperator()
                + "\",\"operand\":" + toJson(u.getOperand()) + "}";
        }
        if (node instanceof BinaryNode) {
            BinaryNode b = (BinaryNode) node;
            return "{\"type\":\"binary\",\"operator\":\"" + b.getOperator()
                + "\",\"left\":"  + toJson(b.getLeft())
                + ",\"right\":" + toJson(b.getRight()) + "}";
        }
        return "null";
    }
}
