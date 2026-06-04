# MatLogEx — Documentación Técnica

## ¿Qué problema resuelve?

El sistema valida y determina el orden de evaluación de fórmulas lógicas
complejas con paréntesis anidados, como `((A AND B) OR (NOT C))`.

Este problema no puede resolverse únicamente con expresiones regulares,
ya que los autómatas finitos carecen de memoria estructurada (pila) para
contar y emparejar paréntesis de profundidad arbitraria. Se requiere una
Gramática Libre de Contexto (GLC), que pertenece a la Jerarquía de Chomsky
de Tipo 2.

## Arquitectura del sistema

```
Fórmula de texto
      ↓
  JFlex (Lexer.jflex)
  Análisis Léxico → Tokens
      ↓
  CUP (parser.cup)
  Análisis Sintáctico LALR → Árbol Sintáctico
      ↓
  Server.java
  API REST HTTP (puerto 8080)
      ↓
  Angular (matlogex-ui)
  Visualización → Tabla de tokens + Árbol + Resultado
```

## Fase 1 — Análisis Léxico (JFlex)

JFlex aplica internamente el algoritmo de Thompson para convertir cada
expresión regular en un AFN, luego aplica la construcción de subconjuntos
para obtener el AFD optimizado que reconoce los tokens.

### Tokens definidos

| Token    | Expresión Regular | Categoría        |
|----------|-------------------|------------------|
| VARIABLE | `[A-Z]`           | Operando         |
| AND      | `"AND"`           | Operador binario |
| OR       | `"OR"`            | Operador binario |
| NOT      | `"NOT"`           | Operador unario  |
| LPAREN   | `"("`             | Agrupación       |
| RPAREN   | `")"`             | Agrupación       |
| WS       | `[ \t\r\n]+`      | Ignorado         |

Las palabras clave (AND, OR, NOT) se declaran antes que VARIABLE en
JFlex para que tengan prioridad en la resolución de conflictos.

## Fase 2 — Análisis Sintáctico (CUP)

CUP genera un parser LALR a partir de la GLC. La gramática está
estructurada en tres niveles para modelar la precedencia de operadores
sin ambigüedad.

### Gramática Libre de Contexto (GLC)

```
Exp    → Exp OR Term       (OR: menor precedencia)
Exp    → Term
Term   → Term AND Factor   (AND: precedencia media)
Term   → Factor
Factor → NOT Factor        (NOT: mayor precedencia, unario)
Factor → ( Exp )           (Agrupación explícita)
Factor → VARIABLE          (Token base / hoja del árbol)
```

Esta estructura garantiza:
- `NOT` se evalúa primero (nivel Factor)
- `AND` se evalúa segundo (nivel Term)
- `OR` se evalúa último (nivel Exp)
- Los paréntesis fuerzan el orden explícitamente

### Árbol sintáctico para `((A AND B) OR (NOT C))`

```
        OR
       /  \
     AND   NOT
    /  \    \
   A    B    C
```

## Fase 3 — Evaluación Booleana

El sistema recorre el árbol de forma recursiva (post-orden):

1. Asigna valores booleanos aleatorios a cada variable única
2. Evalúa las hojas (variables) con su valor asignado
3. Evalúa los nodos internos de abajo hacia arriba:
   - `NOT`  → negación del hijo
   - `AND`  → conjunción de hijo izquierdo y derecho
   - `OR`   → disyunción de hijo izquierdo y derecho

## Fase 4 — API REST

El servidor HTTP expone dos endpoints:

### POST /analizar

**Request:**
```json
{ "formula": "((A AND B) OR (NOT C))" }
```

**Response:**
```json
{
  "formula": "((A AND B) OR (NOT C))",
  "arbol": {
    "type": "binary",
    "operator": "OR",
    "left": {
      "type": "binary",
      "operator": "AND",
      "left":  { "type": "variable", "name": "A" },
      "right": { "type": "variable", "name": "B" }
    },
    "right": {
      "type": "unary",
      "operator": "NOT",
      "operand": { "type": "variable", "name": "C" }
    }
  },
  "variables": { "A": true, "B": false, "C": true },
  "resultado": false
}
```

### GET /health

Verifica que el servidor está activo.

```json
{ "status": "ok" }
```

## Fase 5 — Frontend Angular

El frontend visualiza los resultados en tres secciones:

- **Tabla de Tokens**: muestra cada token identificado por JFlex con
  su lexema, tipo y categoría
- **Árbol Sintáctico**: visualización jerárquica del árbol generado
  por CUP con código de colores por tipo de nodo
- **Resultado**: valores booleanos asignados a cada variable y el
  resultado final de la evaluación

## Tecnologías utilizadas

| Componente | Tecnología | Versión |
|------------|-----------|---------|
| Análisis léxico | JFlex | 1.9.1 |
| Análisis sintáctico | CUP | 11b-20160615 |
| Backend | Java | 11+ |
| Servidor HTTP | com.sun.net.httpserver | JDK built-in |
| Frontend | Angular | 19+ |
| Estilos | SCSS | — |

## Integrantes

- César Daniel Ramos Merchán
- Alexis Jara
- Anthony Gutierrez
- Ivan Fernandez
- César López

**Universidad Nacional de Loja — FEIRNNR**  
**Carrera de Ingeniería en Computación**  
**Teoría de Autómatas y Computabilidad Avanzada**