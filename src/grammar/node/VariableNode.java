package grammar.node;

/**
 * Nodo hoja que representa una variable booleana (A, B, C, ..., Z).
 * No tiene hijos; es el elemento terminal del árbol.
 */
public class VariableNode extends Node {

    private final String name;

    /**
     * @param name Nombre de la variable (una letra mayúscula A-Z)
     */
    public VariableNode(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public String toTree(String indent) {
        return indent + name;
    }
}
