# Cambios Realizados — MatLogEx

## Resumen

Se reemplazo el modelo de nodos semanticos (AST) por un modelo de nodos genericos (Parse Tree) que preserva la derivacion completa de la gramatica. Ademas se ajusto la expresion regular de `WS` en el lexer para coincidir exactamente con lo definido en la Tabla 2.

---

## 1. Nuevo modelo de nodos: `ParseNode.java`

**Archivo:** `backend/src/grammar/node/ParseNode.java` (nuevo)

### Estructura

```java
public class ParseNode {
    private String label;              // "Exp", "Term", "Factor", "AND", "VARIABLE", ...
    private String value;              // lexema si es terminal (ej: "A", "AND", "("), null si es no terminal
    private List<ParseNode> children;  // hijos en orden de derivacion
}
```

Cada nodo representa un paso de la derivacion. Los no terminales (Exp, Term, Factor) tienen `value = null` y `children` con los simbolos de la produccion. Los terminales (AND, OR, NOT, VARIABLE, LPAREN, RPAREN) tienen `value` con el lexema y `children` vacio.

### Metodos

- `ParseNode(label)` — constructor para no terminales y terminales sin valor
- `ParseNode(label, value)` — constructor para terminales con lexema
- `add(child)` — agrega un hijo en orden
- `getLabel()`, `getValue()`, `getChildren()` — getters
- `toTree(indent)` — representacion textual identada del arbol de derivacion

---

## 2. Archivos eliminados

| Archivo | Eliminado | Razon |
|---------|-----------|-------|
| `backend/src/grammar/node/Node.java` | Si | Clase base abstracta obsoleta, reemplazada por ParseNode |
| `backend/src/grammar/node/BinaryNode.java` | Si | Nodo semantico con left/right/operator, ya no existe |
| `backend/src/grammar/node/UnaryNode.java` | Si | Nodo semantico con operand/operator, ya no existe |
| `backend/src/grammar/node/VariableNode.java` | Si | Nodo semantico con name, ya no existe |

---

## 3. `Lexer.jflex` — Ajuste de `WS`

**Antes:**
```
WS = [ \t\r\n]+
```

**Despues:**
```
WS = [ \t]+
```

**Motivo:** La Tabla 2 del documento define `WS = [ \t]+` (espacio y tabulacion). Se elimino `\r\n` para coincidir exactamente con la especificacion. Los saltos de linea no estan contemplados como separadores en el alfabeto.

---

## 4. `parser.cup` — Reescritura completa de acciones

### Antes (AST semantico)

Cada produccion construia nodos semanticos concretos:

```cup
Exp ::= Exp:left OR Term:right
    {: RESULT = new BinaryNode("OR", left, right); :}
    ;
Factor ::= LPAREN Exp:e RPAREN
    {: RESULT = e; :}  // los parentesis se descartaban
    ;
```

### Despues (Parse Tree completo)

Cada produccion construye un `ParseNode` con su label y todos los hijos en orden:

```cup
Exp ::= Exp:left OR:op Term:right
    {:
        ParseNode n = new ParseNode("Exp");
        n.add(left);
        n.add(new ParseNode("OR", op));
        n.add(right);
        RESULT = n;
    :}
    ;

Factor ::= LPAREN:lp Exp:e RPAREN:rp
    {:
        ParseNode n = new ParseNode("Factor");
        n.add(new ParseNode("LPAREN", lp));
        n.add(e);
        n.add(new ParseNode("RPAREN", rp));
        RESULT = n;
    :}
    ;
```

### Cambios clave

| Aspecto | Antes (AST) | Despues (Parse Tree) |
|---------|-------------|---------------------|
| `Factor -> ( Exp )` | Los parentesis se descartaban, se propagaba `e` | Se preservan como hijos LPAREN y RPAREN |
| `Factor -> NOT Factor` | Se creaba `UnaryNode("NOT", operand)` | Se crea `ParseNode("Factor")` con hijos NOT y Factor |
| `Factor -> VARIABLE` | Se creaba `VariableNode(v)` | Se crea `ParseNode("Factor")` con hijo `ParseNode("VARIABLE", v)` |
| Terminales declarados | `terminal AND, OR, ...` (sin tipo) | `terminal String AND, OR, ...` (con tipo String para acceder al lexema) |

### Mapeo Tabla 7 -> parser.cup

| Produccion (Tabla 7) | Accion en parser.cup |
|---------------------|---------------------|
| `Exp -> Exp OR Term` | `new ParseNode("Exp")` con hijos: Exp, OR(lexema), Term |
| `Exp -> Term` | `new ParseNode("Exp")` con hijo: Term |
| `Term -> Term AND Factor` | `new ParseNode("Term")` con hijos: Term, AND(lexema), Factor |
| `Term -> Factor` | `new ParseNode("Term")` con hijo: Factor |
| `Factor -> NOT Factor` | `new ParseNode("Factor")` con hijos: NOT(lexema), Factor |
| `Factor -> ( Exp )` | `new ParseNode("Factor")` con hijos: LPAREN(, Exp, RPAREN) |
| `Factor -> VARIABLE` | `new ParseNode("Factor")` con hijo: VARIABLE(lexema) |

---

## 5. `NodeSerializer.java` — Reescritura para ParseNode

### Antes

Usaba `instanceof` para determinar el tipo de nodo y serializar campos semanticos:
```java
if (node instanceof BinaryNode) {
    return "{\"type\":\"binary\",\"operator\":\"...\",\"left\":...,\"right\":...}";
}
```

### Despues

Serializa recursivamente la estructura generica label/value/children:
```json
{
  "label": "Exp",
  "children": [
    {"label": "Term", "children": [...]},
    {"label": "OR", "value": "OR"},
    {"label": "Term", "children": [...]}
  ]
}
```

---

## 6. `Server.java` y `Main.java` — Reescritura de evaluate() y collectVariables()

### evaluate()

Ya no usa `instanceof`. Ahora interpreta la estructura del Parse Tree:

| Label | # hijos | Interpretacion |
|-------|---------|---------------|
| `Exp` | 3 | Exp OR Term → evaluar hijo[0] OR hijo[2] |
| `Exp` | 1 | Exp → Term → evaluar hijo[0] |
| `Term` | 3 | Term AND Factor → evaluar hijo[0] AND hijo[2] |
| `Term` | 1 | Term → Factor → evaluar hijo[0] |
| `Factor` | 2 | NOT Factor → NOT evaluar hijo[1] |
| `Factor` | 3 | ( Exp ) → evaluar hijo[1] |
| `Factor` | 1 | VARIABLE → devolver valor de variable |

### collectVariables()

Recorre recursivamente el arbol buscando nodos con `label == "VARIABLE"` y registra su `value` como nombre de variable.

**Antes:** Visitaba nodos especificos (VariableNode, UnaryNode, BinaryNode) con instanceof.

**Despues:** Itera sobre `children` de cada ParseNode sin importar el label del padre.

---

## 7. Frontend — Adaptacion al nuevo modelo

### `analizador.ts` — Interfaz NodoArbol

**Antes:**
```typescript
interface NodoArbol {
  type: 'binary' | 'unary' | 'variable';
  operator?: string;
  name?: string;
  left?: NodoArbol;
  right?: NodoArbol;
  operand?: NodoArbol;
}
```

**Despues:**
```typescript
interface NodoArbol {
  label: string;
  value?: string;
  children?: NodoArbol[];
}
```

### `nodo.ts` — Logica de clases CSS

Las clases CSS se asignan segun el `label` del nodo:

| Label | Clase CSS | Color |
|-------|-----------|-------|
| `AND` | `nodo-and` | Indigo |
| `OR` | `nodo-or` | Azul |
| `NOT` | `nodo-not` | Amarillo |
| `VARIABLE` | `nodo-variable` | Verde |
| `LPAREN`, `RPAREN` | `nodo-paren` | Violeta |
| otros (Exp, Term, Factor) | `nodo-default` | Gris |

La etiqueta visible es `value` (lexema) si existe, o `label` si es un no terminal.

### `nodo.html` — Render generico

**Antes:** Estructura condicional con `*ngIf` para binary (left/right) y unary (operand).

**Despues:** Iteracion generica con `*ngFor` sobre `children`:
```html
<div class="nodo-rama" *ngFor="let child of nodo.children!">
    <app-nodo [nodo]="child"></app-nodo>
</div>
```

Esto permite renderizar cualquier cantidad de hijos (1 para Exp → Term, 2 para Factor → NOT Factor, 3 para Exp → Exp OR Term, etc.)

---

## 8. Resultados de las pruebas

**33 PASS / 1 FAIL** (mismo resultado que antes del cambio).

| Categoria | Pruebas | PASS | FAIL |
|-----------|---------|------|------|
| 1. Validos basicos | 5 | 5 | 0 |
| 2. Validos complejos | 7 | 7 | 0 |
| 3. Precedencia de operadores | 3 | 3 | 0 |
| 4. Invalidos sintacticos | 9 | 9 | 0 |
| 5. Edge cases del lexer | 5 | 5 | 0 |
| 6. Casos frontera | 5 | 4 | 1 |
| **Total** | **34** | **33** | **1** |

El unico fallo (#33, tabulacion escapada en JSON) es el mismo de antes y no esta relacionado con los cambios en el parse tree.

---

## 9. Ejemplo antes/despues del parse tree

### Formula: `((A AND B) OR (NOT C))`

**Antes (AST):** Solo nodos semanticos, parentesis descartados, estructura minima:
```
OR
├── AND
│   ├── A
│   └── B
└── NOT
    └── C
```

**Despues (Parse Tree):** Derivacion completa, parentesis preservados, todos los niveles jerarquicos:
```
Exp
└── Term
    └── Factor
        ├── LPAREN "("
        ├── Exp
        │   ├── Exp
        │   │   └── Term
        │   │       └── Factor
        │   │           ├── LPAREN "("
        │   │           ├── Exp
        │   │           │   └── Term
        │   │           │       ├── Term
        │   │           │       │   └── Factor
        │   │           │       │       └── VARIABLE "A"
        │   │           │       ├── AND "AND"
        │   │           │       └── Factor
        │   │           │           └── VARIABLE "B"
        │   │           └── RPAREN ")"
        │   ├── OR "OR"
        │   └── Term
        │       └── Factor
        │           ├── LPAREN "("
        │           ├── Exp
        │           │   └── Term
        │           │       └── Factor
        │           │           ├── NOT "NOT"
        │           │           └── Factor
        │           │               └── VARIABLE "C"
        │           └── RPAREN ")"
        └── RPAREN ")"
```

### Formula: `A AND B` (sin parentesis)

**Antes (AST):**
```
AND
├── A
└── B
```

**Despues (Parse Tree):**
```
Exp
└── Term
    ├── Term
    │   └── Factor
    │       └── VARIABLE "A"
    ├── AND "AND"
    └── Factor
        └── VARIABLE "B"
```

---

## 10. Resumen de archivos modificados

| Archivo | Cambio |
|---------|--------|
| `backend/src/grammar/node/ParseNode.java` | **Creado** — nodo generico para parse tree |
| `backend/src/grammar/node/Node.java` | **Eliminado** — reemplazado por ParseNode |
| `backend/src/grammar/node/BinaryNode.java` | **Eliminado** — reemplazado por ParseNode |
| `backend/src/grammar/node/UnaryNode.java` | **Eliminado** — reemplazado por ParseNode |
| `backend/src/grammar/node/VariableNode.java` | **Eliminado** — reemplazado por ParseNode |
| `backend/src/grammar/node/NodeSerializer.java` | **Reescrito** — serializa ParseNode generico |
| `backend/src/grammar/Lexer.jflex` | **Modificado** — WS de `[ \t\r\n]+` a `[ \t]+` |
| `backend/src/grammar/parser.cup` | **Reescrito** — acciones construyen ParseNode |
| `backend/Server.java` | **Reescrito** — evaluate/collectVariables para ParseNode |
| `backend/Main.java` | **Reescrito** — evaluate/collectVariables para ParseNode |
| `frontend/src/app/services/analizador.ts` | **Modificado** — interfaz NodoArbol generica |
| `frontend/src/app/components/nodo/nodo.ts` | **Reescrito** — logica basada en label |
| `frontend/src/app/components/nodo/nodo.html` | **Reescrito** — render generico con *ngFor |
| `frontend/src/app/components/nodo/nodo.scss` | **Modificado** — estilos para nodo-default y nodo-paren |
| `test_suite.py` | Sin cambios — mismo resultado 33/34 |
