import grammar.Lexer;
import grammar.parser;
import grammar.node.*;
import java_cup.runtime.*;
import java.io.StringReader;
import java.util.*;

/**
 * Interfaz de linea de comandos para probar el analizador sintactico.
 * Uso: java Main "<formula>"
 *
 * Ejecuta todo el pipeline: lexer -> parser -> arbol -> variables aleatorias -> evaluacion.
 */
public class Main {

    /**
     * Evalua el arbol sintactico en post-order interpretando los labels de la GLC:
     *
     *   Exp: si 3 hijos -> OR; si 1 -> pasa al hijo
     *   Term: si 3 hijos -> AND; si 1 -> pasa al hijo
     *   Factor: si 2 hijos -> NOT; si 3 -> (Exp) pasa al medio; si 1 -> variable
     */
    private static boolean evaluate(ParseNode node, Map<String, Boolean> values) {
        String label = node.getLabel();
        List<ParseNode> c = node.getChildren();

        if ("Exp".equals(label)) {
            if (c.size() == 3) {
                return evaluate(c.get(0), values) || evaluate(c.get(2), values);
            }
            return evaluate(c.get(0), values);
        }
        if ("Term".equals(label)) {
            if (c.size() == 3) {
                return evaluate(c.get(0), values) && evaluate(c.get(2), values);
            }
            return evaluate(c.get(0), values);
        }
        if ("Factor".equals(label)) {
            if (c.size() == 2) {
                return !evaluate(c.get(1), values);
            }
            if (c.size() == 3) {
                return evaluate(c.get(1), values);
            }
            return values.get(c.get(0).getValue());
        }
        throw new RuntimeException("Nodo desconocido: " + label);
    }

    /** Recolecta los nombres de las variables del arbol. */
    private static void collectVariables(ParseNode node, Map<String, Boolean> variables) {
        if ("VARIABLE".equals(node.getLabel())) {
            variables.put(node.getValue(), false);
            return;
        }
        for (ParseNode child : node.getChildren()) {
            collectVariables(child, variables);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java Main \"<formula>\"");
            System.out.println("Ejemplo: java Main \"((A AND B) OR (NOT C))\"");
            return;
        }

        String formula = args[0];
        System.out.println("Formula: " + formula);
        System.out.println("-----------------------------");

        Lexer  lexer  = new Lexer(new StringReader(formula));
        parser parser = new parser(lexer);
        ParseNode tree = (ParseNode) parser.parse().value;

        System.out.println("Arbol sintactico:");
        System.out.println(tree.toTree("  "));
        System.out.println("-----------------------------");

        Map<String, Boolean> values = new HashMap<>();
        collectVariables(tree, values);
        Random random = new Random();
        for (String var : values.keySet()) {
            values.put(var, random.nextBoolean());
        }

        System.out.println("Valores asignados:");
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("-----------------------------");

        boolean result = evaluate(tree, values);
        System.out.println("Resultado: " + result);
    }
}
