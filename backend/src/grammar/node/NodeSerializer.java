package grammar.node;

import java.util.List;

/**
 * Serializa un arbol de ParseNode a JSON.
 *
 * Formato de salida:
 *   {"label":"Exp","children":[
 *     {"label":"Term","children":[
 *       {"label":"Factor","children":[
 *         {"label":"VARIABLE","value":"A"}]}]}]}
 */
public class NodeSerializer {

    /** Convierte el nodo y todo su subarbol a JSON. */
    public static String toJson(ParseNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"label\":\"").append(escape(node.getLabel())).append("\"");
        if (node.getValue() != null) {
            sb.append(",\"value\":\"").append(escape(node.getValue())).append("\"");
        }
        List<ParseNode> children = node.getChildren();
        if (!children.isEmpty()) {
            sb.append(",\"children\":[");
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(children.get(i)));
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
