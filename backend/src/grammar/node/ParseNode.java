package grammar.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo generico del Arbol de Sintaxis (Parse Tree).
 *
 * Cada nodo representa un paso de la derivacion gramatical:
 * - No terminales (Exp, Term, Factor): tienen label, value=null, y children con
 *   los simbolos que componen la produccion aplicada.
 * - Terminales (VARIABLE, AND, OR, NOT, LPAREN, RPAREN): tienen label, value
 *   con el lexema reconocido, y children vacio.
 *
 * Ejemplo para "A AND B":
 *   Exp
 *     Term
 *       Term
 *         Factor
 *           VARIABLE "A"
 *       AND "AND"
 *       Factor
 *         VARIABLE "B"
 */
public class ParseNode {
    private final String label;
    private final String value;
    private final List<ParseNode> children;

    /**
     * @param label Nombre del simbolo (Exp, Term, Factor, AND, VARIABLE, ...)
     * @param value Lexema del terminal (ej: "A", "AND", "("), null si es no terminal
     */
    public ParseNode(String label, String value) {
        this.label = label;
        this.value = value;
        this.children = new ArrayList<>();
    }

    /** Crea un nodo sin valor (para no terminales o terminales sin lexema relevante). */
    public ParseNode(String label) {
        this(label, null);
    }

    /** Agrega un hijo en orden de derivacion. */
    public void add(ParseNode child) {
        children.add(child);
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
    public List<ParseNode> getChildren() { return children; }

    /**
     * Retorna representacion textual identada del subarbol.
     * Cada nivel se indentan dos espacios.
     */
    public String toTree(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(label);
        if (value != null) {
            sb.append(" \"").append(value).append("\"");
        }
        for (ParseNode child : children) {
            sb.append("\n").append(child.toTree(indent + "  "));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toTree("");
    }
}
