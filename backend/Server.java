import com.sun.net.httpserver.*;
import grammar.Lexer;
import grammar.parser;
import grammar.node.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Servidor HTTP que expone el analizador sintactico via REST.
 * Endpoints:
 *   GET  /health  - health check
 *   POST /analizar - recibe {"formula":"..."}, devuelve arbol, variables y resultado
 */
public class Server {

    /**
     * Evalua el arbol sintactico en post-order interpretando los labels de la GLC:
     *
     *   Exp:     si tiene 3 hijos (Exp OR Term) -> OR, si tiene 1 (Exp -> Term) -> pasa al hijo
     *   Term:    si tiene 3 hijos (Term AND Factor) -> AND, si tiene 1 -> pasa al hijo
     *   Factor:  si tiene 2 hijos (NOT Factor) -> negacion
     *            si tiene 3 hijos (LPAREN Exp RPAREN) -> pasa al hijo del medio
     *            si tiene 1 hijo (VARIABLE) -> lookup en values
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

    /** Recolecta los nombres de variables declaradas en el arbol. */
    private static void collectVariables(ParseNode node, Map<String, Boolean> variables) {
        if ("VARIABLE".equals(node.getLabel())) {
            variables.put(node.getValue(), false);
            return;
        }
        for (ParseNode child : node.getChildren()) {
            collectVariables(child, variables);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin",  "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json; charset=utf-8");
    }

    /**
     * Extraccion simple del valor de una clave en JSON.
     * Busca "key":"...", sin soporte para escape sequences (limitacion conocida).
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
                // 1. Lexear + parsear -> arbol sintactico
                Lexer  lexer  = new Lexer(new java.io.StringReader(formula));
                parser p      = new parser(lexer);
                ParseNode tree = (ParseNode) p.parse().value;

                // 2. Recolectar variables y asignar valores aleatorios
                Map<String, Boolean> values = new LinkedHashMap<>();
                collectVariables(tree, values);
                Random random = new Random();
                for (String var : values.keySet()) {
                    values.put(var, random.nextBoolean());
                }

                // 3. Evaluar
                boolean result = evaluate(tree, values);

                // 4. Construir respuesta
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
