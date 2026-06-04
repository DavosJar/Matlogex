# MatLogEx — Documentación Técnica

## Objetivo del proyecto

El objetivo es demostrar que los lenguajes formales resuelven un problema
que parece simple pero no lo es: enseñarle a una computadora a "entender"
una fórmula lógica como `((A AND B) OR (NOT C))`.

Una computadora no puede interpretar esa expresión por sí sola. Necesita
un modelo matemático formal para saber qué significa cada símbolo y en
qué orden evaluarlos. Este proyecto construye ese modelo desde cero
aplicando teoría de autómatas y gramáticas formales.

## ¿Qué problema resuelve?

Validar y calcular el orden de evaluación de fórmulas lógicas complejas
con paréntesis anidados, como `((A AND B) OR (NOT C))`.

Este problema tiene dos restricciones matemáticas importantes:

**Las expresiones regulares no bastan.** Un autómata finito no puede
contar paréntesis anidados de profundidad arbitraria porque no tiene
memoria estructurada (pila). Intentar resolver el anidamiento solo con
regex es un error matemático — los AFD carecen de la capacidad de
emparejar paréntesis a profundidad infinita.

**Se requiere una Gramática Libre de Contexto.** El problema pertenece
a los Lenguajes de Tipo 2 de la Jerarquía de Chomsky. Solo una GLC con
un parser que use pila puede manejar el anidamiento arbitrario y la
precedencia de operadores sin ambigüedad.

---

## Estructura del backend

```
backend/
├── src/
│   └── grammar/
│       ├── Lexer.jflex          ← ESCRITO POR EL EQUIPO
│       ├── parser.cup           ← ESCRITO POR EL EQUIPO
│       ├── Lexer.java           ← GENERADO por JFlex
│       ├── parser.java          ← GENERADO por CUP
│       ├── sym.java             ← GENERADO por CUP
│       └── node/
│           ├── Node.java        ← ESCRITO POR EL EQUIPO
│           ├── BinaryNode.java  ← ESCRITO POR EL EQUIPO
│           ├── UnaryNode.java   ← ESCRITO POR EL EQUIPO
│           ├── VariableNode.java← ESCRITO POR EL EQUIPO
│           └── NodeSerializer.java ← ESCRITO POR EL EQUIPO
├── Main.java                    ← ESCRITO POR EL EQUIPO
├── Server.java                  ← ESCRITO POR EL EQUIPO
├── build.sh                     ← ESCRITO POR EL EQUIPO
└── server.sh                    ← ESCRITO POR EL EQUIPO
```

---

## Archivos escritos por el equipo

### Lexer.jflex

Es la especificación del analizador léxico. El equipo definió aquí las
expresiones regulares de cada token y el orden de prioridad de las
reglas. Las palabras clave AND, OR, NOT se declaran antes que VARIABLE
para que JFlex las reconozca con mayor prioridad que una variable simple,
evitando que "AND" sea tratado como tres variables A, N, D.

```
AND    = "AND"
OR     = "OR"
NOT    = "NOT"
VARIABLE = [A-Z]
LPAREN = "("
RPAREN = ")"
WS     = [ \t\r\n]+
```

JFlex toma este archivo y genera `Lexer.java` automáticamente.

### parser.cup

Es la especificación de la gramática libre de contexto y las acciones
que se ejecutan al reconocer cada regla. El equipo diseñó la GLC en
tres niveles jerárquicos para garantizar la precedencia de operadores
sin ambigüedad:

```
Exp    → Exp OR Term       (OR: menor precedencia)
Exp    → Term
Term   → Term AND Factor   (AND: precedencia media)
Term   → Factor
Factor → NOT Factor        (NOT: mayor precedencia)
Factor → ( Exp )           (Agrupación explícita)
Factor → VARIABLE          (Hoja del árbol)
```

Cada regla tiene una acción Java asociada que construye el nodo
correspondiente del árbol sintáctico:

```java
Exp ::= Exp:left OR Term:right
    {: RESULT = new BinaryNode("OR", left, right); :}
```

CUP toma este archivo y genera `parser.java` y `sym.java` automáticamente.

### Node.java, BinaryNode.java, UnaryNode.java, VariableNode.java

Son las clases que representan los nodos del árbol sintáctico. El equipo
las diseñó siguiendo una jerarquía:

- `Node` — clase base abstracta con el método `toTree(indent)`
- `BinaryNode` — nodo con dos hijos (AND, OR)
- `UnaryNode` — nodo con un hijo (NOT)
- `VariableNode` — nodo hoja sin hijos (A, B, C...)

### NodeSerializer.java

Convierte el árbol de nodos a formato JSON sin librerías externas para
que la API REST pueda enviarlo al frontend Angular.

### Main.java

Punto de entrada para ejecución por consola. Recibe la fórmula como
argumento, ejecuta el análisis léxico y sintáctico, imprime el árbol
y evalúa la fórmula con valores aleatorios.

### Server.java

Servidor HTTP REST usando `com.sun.net.httpserver` del JDK sin
dependencias externas. Expone dos endpoints:
- `POST /analizar` — recibe la fórmula y devuelve árbol + resultado
- `GET /health` — verifica que el servidor está activo

### build.sh

Script de compilación que ejecuta los cuatro pasos en orden:
1. JFlex sobre `Lexer.jflex` → genera `Lexer.java`
2. CUP sobre `parser.cup` → genera `parser.java` y `sym.java`
3. javac compila todo el proyecto
4. Ejecuta `Main.java` con la fórmula de prueba

### server.sh

Arranca el servidor REST. Si no existe la carpeta `classes`, ejecuta
`build.sh` automáticamente antes de iniciar.

---

## Archivos generados automáticamente

Estos archivos **no deben editarse** — se regeneran cada vez que se
ejecuta `build.sh`. Están en `.gitignore` y no se suben al repositorio.

### Lexer.java (generado por JFlex)

JFlex lee `Lexer.jflex` y aplica internamente el algoritmo de Thompson
para convertir cada expresión regular en un AFN. Luego aplica la
construcción de subconjuntos para obtener el AFD optimizado. El resultado
es `Lexer.java`, una clase Java con tablas de transición que implementa
el AFD y reconoce los tokens uno por uno.

El AFD generado por JFlex para este proyecto tiene:
- 25 estados en el AFN original
- 16 estados antes de la minimización
- 14 estados en el AFD minimizado final

### parser.java (generado por CUP)

CUP lee `parser.cup` y calcula las tablas del parser LALR a partir de
la GLC. El archivo generado contiene tres tablas:

**`_production_table`** — lista las 8 reglas de la GLC. CUP las numera
del 0 al 7 para referenciarlas en las otras tablas.

**`_action_table`** — tabla de decisiones del parser. Para cada estado
y token entrante, indica si debe hacer shift (desplazar el token a la
pila y avanzar) o reduce (aplicar una regla de la GLC). Esta tabla es
el resultado del algoritmo LALR que CUP calculó automáticamente.

**`_reduce_table`** — tabla de goto. Después de aplicar una reducción,
indica a qué estado ir según el no-terminal producido.

El método `CUP$parser$do_action` contiene los 8 casos que corresponden
a las 8 reglas de la GLC. Cuando el parser reduce por una regla, ejecuta
la acción Java definida en `parser.cup`, que construye el nodo del árbol.

### sym.java (generado por CUP)

Clase con constantes enteras para cada terminal y no-terminal de la
gramática. Por ejemplo `sym.AND = 3`, `sym.OR = 4`, etc. Tanto `Lexer.java`
como `parser.java` usan estas constantes para comunicarse.

---

## Flujo completo de ejecución

```
Fórmula: ((A AND B) OR (NOT C))
         ↓
[Lexer.java - AFD generado por JFlex]
Tokeniza la cadena:
LPAREN LPAREN VARIABLE AND VARIABLE RPAREN OR LPAREN NOT VARIABLE RPAREN RPAREN
         ↓
[parser.java - LALR generado por CUP]
Aplica la GLC y construye el árbol:
         OR
        /  \
      AND   NOT
     /  \    \
    A    B    C
         ↓
[Server.java]
Serializa el árbol a JSON y lo envía al frontend
         ↓
[Angular frontend]
Visualiza tokens + árbol + resultado booleano
```

---

## Fase léxica — Algoritmo de Thompson

JFlex aplica el algoritmo de Thompson internamente al procesar
`Lexer.jflex`. Thompson convierte cada expresión regular en un AFN
siguiendo reglas estructurales:

- Un símbolo `a` → dos estados con transición etiquetada `a`
- Concatenación `AB` → conectar el estado final de A con el inicial de B
- Alternancia `A|B` → nuevo estado inicial con ε-transiciones a A y B
- Clausura `A*` → ε-transiciones para el ciclo y el bypass

El AFN resultante se convierte a AFD mediante la construcción de
subconjuntos, eliminando las transiciones épsilon y el no-determinismo.

---

## Fase sintáctica — Parser LALR

CUP genera un parser LALR (Look-Ahead Left-to-Right Rightmost derivation).
El algoritmo LALR es una variante eficiente del análisis LR que usa una
tabla de estados compacta. Para cada estado, examina el token actual
(look-ahead) y decide entre dos acciones:

**Shift** — desplaza el token actual a la pila y avanza al siguiente
estado. Se usa cuando aún no hay suficiente información para aplicar
una regla.

**Reduce** — aplica una regla de la GLC. Saca de la pila los símbolos
del lado derecho de la regla, construye el nodo del árbol y empuja el
no-terminal del lado izquierdo.

La gramática diseñada produjo 0 conflictos shift/reduce, lo que confirma
que es no ambigua y apta para análisis LALR determinista.

---

## Evaluación booleana

El árbol se recorre en post-orden:

1. Asigna valores booleanos aleatorios a cada variable única
2. Evalúa las hojas (variables) con su valor asignado
3. Evalúa los nodos internos de abajo hacia arriba:
   - `NOT`  → niega el valor del hijo
   - `AND`  → conjunción del hijo izquierdo y derecho
   - `OR`   → disyunción del hijo izquierdo y derecho

Los valores aleatorios son parte del requisito del escenario. Su
propósito es demostrar que el sistema evalúa correctamente cualquier
combinación posible de valores, no solo un caso específico.

---

## Uso del operador NOT

Según la GLC `Factor → NOT Factor`, el NOT es recursivo y puede
aplicarse sin paréntesis cuando el operando es una variable o un NOT:

| Fórmula | Válida | Por qué |
|---------|--------|---------|
| `NOT A` | Sí | NOT aplicado a una variable |
| `NOT NOT A` | Sí | Doble negación, recursividad de la GLC |
| `NOT (A AND B)` | Sí | NOT aplicado a un grupo con paréntesis |
| `(NOT A) AND (NOT B)` | Sí | NOT en cada operando |

Los paréntesis son obligatorios solo cuando NOT se aplica a una
expresión compuesta con AND u OR.

---

## Casos válidos e inválidos

### Fórmulas válidas

```
A
NOT A
NOT NOT A
(A AND B)
(A OR B)
NOT (A AND B)
((A AND B) OR (NOT C))
((NOT A) AND (NOT B))
(A OR (NOT (B AND C)))
((A OR B) AND (C OR D))
```

### Fórmulas inválidas

| Fórmula | Motivo |
|---------|--------|
| `(A AND B` | Paréntesis sin cerrar |
| `A AND B)` | Paréntesis sin abrir |
| `AND A` | Operador binario sin operando izquierdo |
| `A OR` | Operador sin operando derecho |
| `()` | Paréntesis vacíos |
| `(AND)` | Operador dentro de paréntesis sin variables |
| `A AND AND B` | Operador duplicado |
| `a and b` | Minúsculas no son tokens válidos |
| `123` | Números no son tokens válidos |

---

## API REST

### POST /analizar

**Request:**
```json
{ "formula": "((A AND B) OR (NOT C))" }
```

**Response exitoso (200):**
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

**Response con error (400):**
```json
{ "error": "Formula invalida: ..." }
```

### GET /health

```json
{ "status": "ok" }
```

---

## Tecnologías utilizadas

| Componente          | Tecnología             | Versión       |
|---------------------|------------------------|---------------|
| Análisis léxico     | JFlex                  | 1.9.1         |
| Análisis sintáctico | CUP                    | 11b-20160615  |
| Backend             | Java                   | 11+           |
| Servidor HTTP       | com.sun.net.httpserver | JDK built-in  |
| Frontend            | Angular                | 19+           |
| Estilos             | SCSS                   | —             |

---

## Integrantes

- César Daniel Ramos Merchán
- Alexis Jara
- Anthony Gutierrez
- Ivan Fernandez
- César López

**Universidad Nacional de Loja — FEIRNNR**
**Carrera de Ingeniería en Computación**
**Teoría de Autómatas y Computabilidad Avanzada**

---

## Descripción de cada archivo del proyecto

### Archivos escritos por el equipo

**`Lexer.jflex`**
Especificación del analizador léxico. El equipo definió aquí las
expresiones regulares de cada token y el orden de prioridad de las
reglas. JFlex lo lee y genera `Lexer.java` automáticamente.

**`parser.cup`**
Especificación de la gramática libre de contexto y las acciones Java
que construyen el árbol sintáctico al reconocer cada regla. CUP lo lee
y genera `parser.java` y `sym.java` automáticamente.

**`Node.java`**
Clase base abstracta del árbol sintáctico. Define el método `toTree(indent)`
que todas las clases hijas implementan para visualizar el árbol.

**`BinaryNode.java`**
Nodo con dos hijos. Representa los operadores binarios AND y OR.
Almacena el operador, el hijo izquierdo y el hijo derecho.

**`UnaryNode.java`**
Nodo con un solo hijo. Representa el operador unario NOT.
Almacena el operador y el operando.

**`VariableNode.java`**
Nodo hoja sin hijos. Representa las variables booleanas A, B, C, etc.
Es el elemento terminal del árbol — donde termina la recursión.

**`NodeSerializer.java`**
Convierte el árbol de nodos a formato JSON sin librerías externas.
Lo usa `Server.java` para enviar el árbol al frontend Angular.

**`Main.java`**
Punto de entrada para ejecución por consola. Recibe la fórmula como
argumento, ejecuta el análisis, imprime el árbol y evalúa la fórmula
con valores aleatorios. Útil para probar el sistema sin levantar el servidor.

**`Server.java`**
Servidor HTTP REST usando `com.sun.net.httpserver` del JDK sin
dependencias externas. Expone `POST /analizar` y `GET /health`.

**`build.sh`**
Script que compila todo el proyecto en cuatro pasos en orden:
JFlex → CUP → javac → ejecución de prueba.

**`server.sh`**
Arranca el servidor REST en el puerto 8080. Si no existe la carpeta
`classes`, ejecuta `build.sh` automáticamente antes de iniciar.

---

### Archivos generados por JFlex

**`Lexer.java`**
El AFD compilado. JFlex leyó `Lexer.jflex`, aplicó el algoritmo de
Thompson para construir el AFN y luego la construcción de subconjuntos
para obtener el AFD minimizado. El resultado es este archivo Java con
tablas de transición listas para ejecutarse. No se edita manualmente.

**`Lexer.java~`**
Backup automático que JFlex guarda cada vez que regenera `Lexer.java`.
No tiene uso funcional — es solo un respaldo de la versión anterior.

---

### Archivos generados por CUP

**`parser.java`**
El parser LALR compilado. CUP leyó `parser.cup` y calculó
automáticamente las tablas de estados del algoritmo LALR. Contiene
tres tablas: `_production_table` con las 8 reglas de la GLC,
`_action_table` con las decisiones shift/reduce, y `_reduce_table`
con las transiciones después de cada reducción. No se edita manualmente.

**`sym.java`**
Clase con constantes enteras para cada terminal y no-terminal de la
gramática. Por ejemplo `sym.AND`, `sym.OR`, `sym.VARIABLE`. Tanto
`Lexer.java` como `parser.java` usan estas constantes para comunicarse
entre sí durante el análisis.

---

### Carpeta `classes/`

Contiene los archivos `.class` — el bytecode Java que genera javac al
compilar todos los `.java`. La JVM ejecuta estos archivos cuando se
corre `server.sh` o `Main.java`. Esta carpeta no se sube al repositorio
porque se regenera automáticamente con `build.sh`.
