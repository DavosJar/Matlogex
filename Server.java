import com.sun.net.httpserver.*;
import grammar.Lexer;
import grammar.parser;
import grammar.node.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java_cup.runtime.*;

/**
 * Servidor HTTP REST que expone el analizador léxico-sintáctico.
 * Escucha en el puerto 8080 y responde en JSON con CORS habilitado.
 *
 * Endpoints:
 *   POST /analizar  — body: { "formula": "((A AND B) OR (NOT C))" }
 *   GET  /health    — verifica que el servidor está activo
 */
public class Server {

    /**
     * Evalúa recursivamente el árbol con los valores asignados.
     *
     * @param node   Nodo a evaluar
     * @param values Mapa variable → booleano
     * @return Resultado booleano
     */
    private static boolean evaluate(Node node, Map<String, Boolean> values) {
        if (node instanceof VariableNode) {
            return values.get(((VariableNode) node).getName());
        }
        if (node instanceof UnaryNode) {
            return !evaluate(((UnaryNode) node).getOperand(), values);
        }
        if (node instanceof BinaryNode) {
            BinaryNode b = (BinaryNode) node;
            boolean left  = evaluate(b.getLeft(),  values);
            boolean right = evaluate(b.getRight(), values);
            return b.getOperator().equals("AND") ? left && right : left || right;
        }
        throw new RuntimeException("Nodo desconocido");
    }

    /**
     * Recorre el árbol y registra todas las variables únicas encontradas.
     *
     * @param node      Nodo a recorrer
     * @param variables Mapa donde se acumulan las variables
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

    /**
     * Agrega headers CORS a la respuesta para permitir
     * peticiones desde el frontend Angular en localhost:4200.
     *
     * @param exchange Objeto de intercambio HTTP
     */
    private static void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin",  "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json; charset=utf-8");
    }

    /**
     * Extrae el valor de una clave de un JSON simple de una sola línea.
     * Solo funciona para JSONs planos con strings, no anidados.
     *
     * @param json JSON en texto plano
     * @param key  Clave a buscar
     * @return Valor asociado a la clave o null si no existe
     */
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        int quote1 = json.indexOf("\"", colon + 1);
        int quote2 = json.indexOf("\"", quote1 + 1);
        return json.substring(quote1 + 1, quote2);
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ── Endpoint: GET /health ──
        server.createContext("/health", exchange -> {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String response = "{\"status\":\"ok\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        // ── Endpoint: POST /analizar ──
        server.createContext("/analizar", exchange -> {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            String formula = extractJsonString(body, "formula");
            String response;
            int statusCode = 200;

            try {
                Lexer  lexer  = new Lexer(new java.io.StringReader(formula));
                parser p      = new parser(lexer);
                Node   tree   = (Node) p.parse().value;

                Map<String, Boolean> values = new LinkedHashMap<>();
                collectVariables(tree, values);
                Random random = new Random();
                for (String var : values.keySet()) {
                    values.put(var, random.nextBoolean());
                }

                boolean result = evaluate(tree, values);

                // Construir JSON de variables
                StringBuilder varsJson = new StringBuilder("{");
                int i = 0;
                for (Map.Entry<String, Boolean> e : values.entrySet()) {
                    if (i++ > 0) varsJson.append(",");
                    varsJson.append("\"").append(e.getKey())
                            .append("\":").append(e.getValue());
                }
                varsJson.append("}");

                response = "{"
                    + "\"formula\":\""  + formula + "\","
                    + "\"arbol\":"      + NodeSerializer.toJson(tree) + ","
                    + "\"variables\":"  + varsJson + ","
                    + "\"resultado\":"  + result
                    + "}";

            } catch (Exception e) {
                statusCode = 400;
                response = "{\"error\":\"Formula invalida: " + e.getMessage() + "\"}";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        server.setExecutor(null);
        System.out.println("Servidor iniciado en http://localhost:8080");
        server.start();
    }
}
