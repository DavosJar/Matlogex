# Arquitectura y Flujo del Sistema MatLogEx

Analizador Léxico-Sintáctico de fórmulas lógicas con JFlex + CUP + Java + Angular.

---

## 1. Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND (Angular)                      │
│  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐   │
│  │analizador.html │  │  nodo.html   │  │  analizador.ts │   │
│  │   (template)   │  │ (template)   │  │ (component)    │   │
│  └───────┬────────┘  └──────┬───────┘  └───────┬────────┘   │
│          │                  │                  │            │
│  ┌───────┴──────────────────┴──────────────────┴────────┐   │
│  │              AnalizadorService (HTTP)                 │   │
│  └──────────────────────┬───────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────┘
                          │  POST /analizar {"formula":"..."}
                          │  HTTP 200 / 400
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     BACKEND (Java)                          │
│                                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │                  Server.java                       │     │
│  │  ┌──────────────┐  ┌──────────────┐               │     │
│  │  │  /analizar   │  │   /health    │               │     │
│  │  └──────┬───────┘  └──────────────┘               │     │
│  │         │                                          │     │
│  │         ▼                                          │     │
│  │  ┌──────────────┐  ┌──────────────┐               │     │
│  │  │ Lexer (JFlex)│─▶│parser (CUP)  │               │     │
│  │  │   tokens     │  │  Parse Tree  │               │     │
│  │  └──────────────┘  └──────┬───────┘               │     │
│  │                           │                        │     │
│  │                           ▼                        │     │
│  │  ┌────────────────────────────────────────┐       │     │
│  │  │            evaluate()                   │       │     │
│  │  │  collectVariables → random booleans →   │       │     │
│  │  │  post-order traversal → resultado       │       │     │
│  │  └────────────────────┬───────────────────┘       │     │
│  │                       │                            │     │
│  │                       ▼                            │     │
│  │  ┌────────────────────────────────────────┐       │     │
│  │  │         NodeSerializer.toJson()         │       │     │
│  │  │  ParseNode → {"label","value","children"}│      │     │
│  │  └────────────────────────────────────────┘       │     │
│  └────────────────────────────────────────────────────┘     │
│                                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │              ParseNode.java                        │     │
│  │  Nodo genérico: label + value + children           │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Componentes del Sistema

### 2.1 Frontend (Angular 17+)

| Archivo | Propósito |
|---------|-----------|
| `analizador.ts` | Componente principal. Orquesta la UI: input, validación local, tokenización, envío al backend, visualización de resultados. |
| `analizador.html` | Template con sidebar (input, ejemplos, tokens válidos, valores asignados, resultado final) y panel de tokens/árbol. |
| `analizador.scss` | Estilos del layout y componentes visuales (tema oscuro). |
| `nodo.ts` | Componente recursivo que renderiza un `NodoArbol` como círculo con color según su label. |
| `nodo.html` | Template recursivo: label → conector vertical → hijos en horizontal. |
| `nodo.scss` | Estilos de nodos (círculos con gradientes radiales) y conectores. |
| `analizador.ts` (service) | Servicio Angular con `analizar()` (HTTP POST) y `tokenizar()` (tokenización local). |

#### Interfaces del servicio

```typescript
NodoArbol {
  label:     string;       // "Exp", "Term", "Factor", "AND", "OR", "NOT", "VARIABLE", "LPAREN", "RPAREN"
  value?:    string;       // lexema del terminal (ej: "A", "AND", "(")
  children?: NodoArbol[];  // hijos en orden de producción
}

ResultadoAnalisis {
  formula:   string;
  arbol:     NodoArbol;
  variables: Record<string, boolean>;  // {"A": true, "B": false}
  resultado: boolean;
}

Token {
  lexema: string;    // texto del token
  tipo:   'AND' | 'OR' | 'NOT' | 'VARIABLE' | 'LPAREN' | 'RPAREN';
}
```

### 2.2 Backend (Java)

| Archivo | Propósito |
|---------|-----------|
| `Server.java` | Servidor HTTP en puerto 8080. Endpoints `/health` y `/analizar`. |
| `Main.java` | Interfaz CLI para pruebas sin servidor. |
| `Lexer.jflex` | Definición del analizador léxico (expresiones regulares → tokens). |
| `parser.cup` | Gramática libre de contexto (GLC) con acciones de construcción del árbol. |
| `ParseNode.java` | Clase genérica del árbol de derivación (Parse Tree). |
| `NodeSerializer.java` | Serializador recursivo ParseNode → JSON. |

#### Archivos generados automáticamente

| Archivo | Generado por | Propósito |
|---------|-------------|-----------|
| `Lexer.java` | JFlex | Scanner que implementa el autómata léxico. |
| `parser.java` | CUP | Parser LALR con tabla de transiciones y acciones. |
| `sym.java` | CUP | Constantes enteras para cada tipo de token. |

---

## 3. Gramática (GLC)

La gramática es **no ambigua** con **precedencia NOT > AND > OR** organizada en tres niveles:

```
Exp   → Exp OR Term   | Term        ← OR (menor precedencia)
Term  → Term AND Factor | Factor    ← AND (precedencia media)
Factor → NOT Factor               ← NOT (máxima precedencia)
       | ( Exp )
       | VARIABLE
```

Cada nivel produce un `ParseNode` que envuelve a sus hijos. La jerarquía de niveles asegura que NOT se evalúe antes que AND, y AND antes que OR, porque NOT está en el nivel más profundo del árbol.

### Derivación para `NOT A AND B`

```
      Term (AND)
      /          \
  Factor (NOT)   Factor
      |            |
  Factor        VARIABLE "B"
      |
  VARIABLE "A"
```

Notar que NOT solo puede absorber a `A` porque está en el nivel `Factor`. AND está en `Term`, un nivel arriba. Para que NOT abarque `A AND B` se necesitan paréntesis: `NOT (A AND B)`.

---

## 4. Árbol Sintáctico (ParseNode)

### Estructura de datos

```java
ParseNode {
    String label;                    // "Exp", "Term", "Factor", "AND", "OR", "NOT", "VARIABLE", "LPAREN", "RPAREN"
    String value;                    // lexema (null si es no terminal)
    List<ParseNode> children;        // hijos en orden de derivación
}
```

### Construcción por producción

Cada producción en `parser.cup` construye un `ParseNode` con su label y agrega los hijos correspondientes:

```
Exp   → Exp OR Term      → ParseNode("Exp") + [left, OR("OR"), right]
      | Term              → ParseNode("Exp") + [term]

Term  → Term AND Factor  → ParseNode("Term") + [left, AND("AND"), right]
      | Factor            → ParseNode("Term") + [factor]

Factor → NOT Factor      → ParseNode("Factor") + [NOT("NOT"), operand]
       | ( Exp )          → ParseNode("Factor") + [LPAREN("("), exp, RPAREN(")")]
       | VARIABLE         → ParseNode("Factor") + [VARIABLE("A")]
```

### Ejemplo concreto: `A AND B`

```
Exp
  Term
    Term
      Factor
        VARIABLE "A"
    AND "AND"
    Factor
      VARIABLE "B"
```

### Serialización JSON

`NodeSerializer.toJson()` recorre recursivamente el árbol:

```json
{
  "label": "Exp",
  "children": [{
    "label": "Term",
    "children": [{
      "label": "Term",
      "children": [{
        "label": "Factor",
        "children": [{"label": "VARIABLE", "value": "A"}]
      }]
    }, {
      "label": "AND",
      "value": "AND"
    }, {
      "label": "Factor",
      "children": [{"label": "VARIABLE", "value": "B"}]
    }]
  }]
}
```

---

## 5. Flujo Completo (Caso Exitoso)

### Paso 1: Usuario ingresa fórmula

El usuario escribe `(A AND B) OR (NOT C)` en el textarea del frontend y presiona "Analizar".

### Paso 2: Validación local (Frontend)

`AnalizadorComponent.validarFormula()` verifica:

1. **Balance de paréntesis** — recorre la cadena, incrementa/decrementa contador. Si queda negativo o positivo, retorna error.
2. **Caracteres permitidos** — regex `/[^A-Z\s()]/g`. Si encuentra caracteres no válidos, retorna error con la lista.

### Paso 3: Tokenización local (Frontend)

`AnalizadorService.tokenizar()` replica las reglas del Lexer.jflex:

1. Ignora espacios y tabs.
2. Busca palabras clave (`AND`, `OR`, `NOT`), paréntesis, variables (`A-Z`).
3. Genera un arreglo `Token[]` con lexema y tipo.

La tabla de tokens se muestra *inmediatamente* en la UI, incluso antes de que el backend responda.

### Paso 4: Petición HTTP

`AnalizadorService.analizar()` envía POST a `http://localhost:8080/analizar` con:

```json
{"formula": "(A AND B) OR (NOT C)"}
```

### Paso 5: Recepción en el backend

`Server.java` recibe la petición y extrae el campo `formula` del JSON mediante `extractJsonString()` (parser JSON casero que busca `"formula":"..."`).

### Paso 6: Análisis léxico (JFlex)

```java
Lexer lexer = new Lexer(new java.io.StringReader(formula));
```

JFlex escanea la cadena caracter por caracter aplicando las reglas definidas en `Lexer.jflex`:

| Regla | Patrón | Token generado |
|-------|--------|----------------|
| Palabras clave | `AND`, `OR`, `NOT` | `sym.AND`, `sym.OR`, `sym.NOT` |
| Paréntesis | `(`, `)` | `sym.LPAREN`, `sym.RPAREN` |
| Variables | `[A-Z]` | `sym.VARIABLE` |
| Espacios | `[ \t\r\n]+` | Ignorados |
| Otro | `.` | `sym.error` |

Regla crítica: las palabras clave se definen **antes** que `VARIABLE`, por lo que `AND` se reconoce como operador, no como tres variables.

### Paso 7: Análisis sintáctico (CUP)

```java
parser p = new parser(lexer);
ParseNode tree = (ParseNode) p.parse().value;
```

CUP aplica el autómata LALR generado a partir de `parser.cup`. Para cada producción que coincide, ejecuta la acción embebida que construye el `ParseNode` correspondiente.

Para `(A AND B) OR (NOT C)`, el árbol resultante es:

```
Exp
  Exp
    Term
      Factor
        LPAREN "("
        Exp
          Term
            Term
              Factor
                VARIABLE "A"
            AND "AND"
            Factor
              VARIABLE "B"
        RPAREN ")"
  OR "OR"
  Term
    Factor
      LPAREN "("
      Exp
        Term
          Factor
            NOT "NOT"
            Factor
              VARIABLE "C"
      RPAREN ")"
```

### Paso 8: Recolección de variables

```java
Map<String, Boolean> values = new LinkedHashMap<>();
collectVariables(tree, values);
```

Recorre el árbol en preorden buscando nodos con label `VARIABLE` y agrega cada una al mapa:

```
variables = {"A": false, "B": false, "C": false}
```

### Paso 9: Asignación aleatoria

```java
Random random = new Random();
for (String var : values.keySet()) {
    values.put(var, random.nextBoolean());
}
```

Cada variable recibe un valor booleano aleatorio, p.ej. `{"A": true, "B": false, "C": true}`.

### Paso 10: Evaluación (Post-order)

```java
boolean result = evaluate(tree, values);
```

`evaluate()` recorre el árbol en post-order interpretando los labels:

| Label | Hijos | Acción |
|-------|-------|--------|
| `Exp` | 3 (`Exp`, `OR`, `Term`) | OR lógico entre hijo[0] e hijo[2] |
| `Exp` | 1 (`Term`) | Propaga evaluación al hijo |
| `Term` | 3 (`Term`, `AND`, `Factor`) | AND lógico entre hijo[0] e hijo[2] |
| `Term` | 1 (`Factor`) | Propaga evaluación al hijo |
| `Factor` | 2 (`NOT`, `Factor`) | NOT del hijo[1] |
| `Factor` | 3 (`LPAREN`, `Exp`, `RPAREN`) | Evalúa hijo[1] (la expresión interna) |
| `Factor` | 1 (`VARIABLE`) | Lookup en `values` por el value del hijo |

Para `(A AND B) OR (NOT C)` con `A=true, B=false, C=true`:

1. `VARIABLE "A"` → `true`
2. `VARIABLE "B"` → `false`
3. Term `(A AND B)` → `true AND false` → `false`
4. `VARIABLE "C"` → `true`
5. Factor `(NOT C)` → `NOT true` → `false`
6. Exp `(A AND B) OR (NOT C)` → `false OR false` → `false`

### Paso 11: Serialización y respuesta

```java
response = "{\"formula\":\"...\",\"arbol\":" + NodeSerializer.toJson(tree)
         + ",\"variables\":{\"A\":true,\"B\":false,\"C\":true},\"resultado\":false}";
```

### Paso 12: Visualización (Frontend)

Angular recibe la respuesta y actualiza el estado:

1. **Pestaña "Tokens"**: Muestra la tabla tokenizada localmente.
2. **Pestaña "Árbol Sintáctico"**: Renderiza el `ParseNode` recursivamente con `NodoComponent`. Cada nodo se dibuja como un círculo coloreado según su label:
   - `AND` → púrpura
   - `OR` → azul
   - `NOT` → amarillo
   - `VARIABLE` → verde
   - `LPAREN`/`RPAREN` → violeta
   - `Exp`/`Term`/`Factor` → gris
3. **Sidebar**: Muestra los valores asignados a cada variable y el resultado TRUE/FALSE en un panel coloreado.

---

## 6. Flujo con Error Sintáctico (HTTP 400)

### Paso 1-4: Igual que el caso exitoso

### Paso 5: El backend recibe la fórmula

### Paso 6: CUP encuentra un error

El autómata LALR tiene una tabla de estados. Cuando el token actual no coincide con ningún token esperado en el estado actual, se invoca `parser.report_error()` y luego `unrecovered_syntax_error()` lanza una excepción.

Ejemplo: para `A AND` (test #17), después de consumir `A` y `AND`, el parser espera un `Factor` (variable, NOT o paréntesis) pero encuentra EOF. El estado actual no tiene transición para EOF, entonces reporta error.

### Paso 7: Captura de la excepción

```java
try {
    // lexer + parser + evaluación
} catch (Exception e) {
    statusCode = 400;
    response = "{\"error\":\"Formula invalida: ...\"}";
}
```

El servidor responde HTTP 400 con un mensaje de error genérico.

### Paso 8: Frontend maneja el error

```typescript
error: (err: HttpErrorResponse) => {
    if (err.status === 400) {
        this.errorTipo = 'sintaxis';
        this.error = 'La formula tiene un error de sintaxis...';
    } else if (err.status === 0) {
        this.errorTipo = 'conexion';
        this.error = 'El servidor no esta disponible...';
    }
}
```

La tabla de tokens **sigue visible** porque se tokenizó localmente en el Paso 3. Solo el panel de árbol y resultado se ocultan.

---

## 7. Flujo con Error de Conexión

Cuando el servidor Java no está corriendo:

1. El frontend envía POST a `localhost:8080`.
2. La petición falla con `err.status === 0` (error de red).
3. El frontend muestra un mensaje de error con instrucciones para iniciar el servidor.

---

## 8. Reglas Léxicas (JFlex)

Definidas en `Lexer.jflex`:

| Token | Patrón | Prioridad |
|-------|--------|-----------|
| `AND` | `"AND"` | 1 (más alta) |
| `OR` | `"OR"` | 1 |
| `NOT` | `"NOT"` | 1 |
| `LPAREN` | `"("` | 1 |
| `RPAREN` | `")"` | 1 |
| `VARIABLE` | `[A-Z]` | 2 |
| `WS` | `[ \t\r\n]+` | Ignorado |
| `error` | `.` | Captura cualquier otro carácter |

Las palabras clave tienen prioridad sobre `VARIABLE` porque aparecen primero en el archivo `.jflex`. JFlex aplica la regla de **emparejamiento más largo** (longest match) y, en caso de igual longitud, la primera regla definida.

**Comportamiento silencioso:** Una entrada como `AANDB` se tokeniza como `A`, `AND`, `B` porque:
1. Desde posición 0, `A` coincide con `VARIABLE` (un carácter).
2. Desde posición 1, `AND` coincide con la palabra clave (3 caracteres).
3. Desde posición 4, `B` coincide con `VARIABLE`.

El resultado `A AND B` es sintácticamente válido, pero el usuario puede no saber que su entrada se segmentó.

---

## 9. Evaluación (Post-order)

El método `evaluate()` en `Server.java` y `Main.java` interpreta el Parse Tree por labels y cantidad de hijos:

```
evaluate(Exp, values):
  si 3 hijos → evaluate(hijo[0]) OR evaluate(hijo[2])
  si 1 hijo → evaluate(hijo[0])

evaluate(Term, values):
  si 3 hijos → evaluate(hijo[0]) AND evaluate(hijo[2])
  si 1 hijo → evaluate(hijo[0])

evaluate(Factor, values):
  si 2 hijos (NOT) → NOT evaluate(hijo[1])
  si 3 hijos (parentesis) → evaluate(hijo[1])
  si 1 hijo (VARIABLE) → values.get(hijo[0].getValue())
```

No se usa `instanceof` ni herencia. Todo se resuelve por el label del nodo.

---

## 10. Límites del Sistema

| Aspecto | Límite | Causa |
|---------|--------|-------|
| Profundidad de NOTs | ~2,000-3,000 | StackOverflowError en `evaluate()` y `toJson()` |
| Paréntesis redundantes | >200,000 | Sin límite práctico (no crean niveles en el árbol) |
| Variables distintas | 26 (A-Z) | El lexer solo reconoce `[A-Z]` como variable |
| Largo de variable | 1 carácter | El patrón `VARIABLE = [A-Z]` empareja exactamente un carácter |
| Escape sequences en JSON | No soportado | `extractJsonString()` no procesa `\t`, `\n`, etc. |

---

## 11. Pipeline de Compilación

```
Lexer.jflex ──[JFlex]──→ Lexer.java
parser.cup  ──[CUP]───→ parser.java + sym.java

Lexer.java + parser.java + sym.java + ParseNode.java + NodeSerializer.java + Server.java
  ──[javac]──→ archivos .class
```

El `makefile` orquesta todo:

```makefile
# Limpiar, generar lexer/parser, compilar
make -f makefile clean all
```

Para producción, el servidor se inicia con:

```bash
java -classpath "../classes:../lib/java_cup_runtime.jar" Server
```
