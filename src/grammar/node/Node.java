package grammar.node;

/**
 * Clase base abstracta para todos los nodos del árbol sintáctico.
 * Cada nodo representa un elemento de la fórmula lógica parseada.
 */
public abstract class Node {

    /**
     * Retorna una representación en texto del nodo y sus hijos,
     * con indentación para visualizar la jerarquía del árbol.
     *
     * @param indent Cadena de indentación acumulada
     * @return Representación textual del subárbol
     */
    public abstract String toTree(String indent);

    @Override
    public String toString() {
        return toTree("");
    }
}
