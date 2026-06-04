import grammar.Lexer;
import grammar.parser;
import grammar.node.*;
import java_cup.runtime.*;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Clase principal del analizador de fórmulas lógicas.
 * Recibe una fórmula por argumento, ejecuta el análisis léxico
 * y sintáctico, imprime el árbol sintáctico y evalúa la fórmula
 * asignando valores booleanos aleatorios a cada variable.
 */
public class Main {

    /**
     * Evalúa recursivamente el árbol sintáctico con los valores
     * asignados a cada variable.
     *
     * @param node   Nodo raíz o subárbol a evaluar
     * @param values Mapa de variable → valor booleano
     * @return Resultado booleano de la fórmula
     */
    private static boolean evaluate(Node node, Map<String, Boolean> values) {
        if (node instanceof VariableNode) {
            String name = ((VariableNode) node).getName();
            return values.get(name);
        }
        if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            boolean operand = evaluate(u.getOperand(), values);
            return !operand;
        }
        if (node instanceof BinaryNode) {
            BinaryNode b = (BinaryNode) node;
            boolean left  = evaluate(b.getLeft(),  values);
            boolean right = evaluate(b.getRight(), values);
            if (b.getOperator().equals("AND")) return left && right;
            if (b.getOperator().equals("OR"))  return left || right;
        }
        throw new RuntimeException("Nodo desconocido: " + node.getClass());
    }

    /**
     * Recorre el árbol y recolecta todos los nombres de variables únicas.
     *
     * @param node      Nodo a recorrer
     * @param variables Mapa donde se registran las variables encontradas
     */
    private static void collectVariables(Node node, Map<String, Boolean> variables) {
        if (node instanceof VariableNode) {
            variables.put(((VariableNode) node).getName(), false);
        } else if (node instanceof UnaryNode) {
            collectVariables(((UnaryNode) node).getOperand(), variables);
        } else if (node instanceof BinaryNode) {
            collectVariables(((BinaryNode) node).getLeft(),  variables);
            collectVariables(((BinaryNode) node).getRight(), variables);
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
        System.out.println("─────────────────────────────");

        // Análisis léxico y sintáctico
        Lexer  lexer  = new Lexer(new StringReader(formula));
        parser parser = new parser(lexer);
        Node   tree   = (Node) parser.parse().value;

        // Árbol sintáctico
        System.out.println("Arbol sintáctico:");
        System.out.println(tree.toTree("  "));
        System.out.println("─────────────────────────────");

        // Asignar valores aleatorios a las variables
        Map<String, Boolean> values = new HashMap<>();
        collectVariables(tree, values);
        Random random = new Random();
        for (String var : values.keySet()) {
            values.put(var, random.nextBoolean());
        }

        // Mostrar valores asignados
        System.out.println("Valores asignados:");
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("─────────────────────────────");

        // Resultado final
        boolean result = evaluate(tree, values);
        System.out.println("Resultado: " + result);
    }
}
