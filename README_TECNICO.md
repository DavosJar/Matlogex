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

## ¿Qué demuestra el proyecto?

El proyecto demuestra tres cosas concretas:

El sistema **valida** cualquier fórmula léxica y sintácticamente. JFlex
verifica que cada token sea reconocible y CUP verifica que la estructura
gramatical sea correcta. Si la fórmula está mal escrita, el sistema la
rechaza con un mensaje de error descriptivo.

El sistema **calcula el orden de evaluación**. CUP construye el árbol
sintáctico respetando la precedencia NOT > AND > OR. Los paréntesis
fuerzan el orden explícitamente. Ese árbol es el orden de evaluación
— los nodos más profundos se evalúan primero.

El sistema **evalúa la fórmula**. El árbol se recorre en post-orden
asignando valores booleanos aleatorios a las variables (tal como lo
especifica el enunciado) y produciendo el resultado final. Los valores
aleatorios demuestran que el sistema evalúa correctamente cualquier
combinación posible.

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
  Angular (frontend/)
  Tabla de tokens + Árbol visual + Resultado booleano
```

## Fase 1 — Análisis Léxico (JFlex)

JFlex aplica internamente el algoritmo de Thompson para convertir cada
expresión regular en un AFN, luego aplica la construcción de subconjuntos
para obtener el AFD optimizado que reconoce los tokens.

Las palabras clave AND, OR, NOT se declaran antes que VARIABLE en JFlex
para que tengan prioridad en la resolución de conflictos por longitud
máxima de coincidencia. Esto garantiza que "AND" se reconozca como
operador y no como tres variables A, N, D.

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

## Fase 2 — Análisis Sintáctico (CUP)

CUP genera un parser LALR a partir de la GLC. La gramática está
estructurada en tres niveles jerárquicos para modelar la precedencia
de operadores sin ambigüedad. Si el programador no define correctamente
estos niveles, el sistema produce múltiples árboles (ambigüedad) o
errores shift/reduce.

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

Esta estructura garantiza que NOT se evalúa primero (nivel Factor),
AND segundo (nivel Term) y OR último (nivel Exp). Los paréntesis
fuerzan cualquier orden explícitamente al envolver una Exp completa.

### Árbol sintáctico para `((A AND B) OR (NOT C))`

```
        OR              ← se evalúa último
       /  \
     AND   NOT          ← se evalúan segundo y primero
    /  \    \
   A    B    C          ← hojas: valores de las variables
```

El árbol es la representación visual del orden de evaluación. Los
nodos más profundos (hojas) se evalúan primero y el resultado sube
hacia la raíz.

## Fase 3 — Evaluación Booleana

El sistema recorre el árbol en post-orden:

1. Asigna valores booleanos aleatorios a cada variable única encontrada
2. Evalúa las hojas con su valor asignado
3. Evalúa los nodos internos de abajo hacia arriba:
   - `NOT`  → niega el valor del hijo
   - `AND`  → conjunción del hijo izquierdo y derecho
   - `OR`   → disyunción del hijo izquierdo y derecho

Los valores aleatorios son parte del requisito del escenario. Su
propósito es demostrar que el sistema evalúa correctamente cualquier
combinación posible de valores, no solo un caso específico.

## Fase 4 — API REST

El servidor HTTP expone dos endpoints sin dependencias externas,
usando únicamente `com.sun.net.httpserver` incluido en el JDK.

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

## Fase 5 — Frontend Angular

El frontend visualiza el proceso completo en tres secciones:

**Tabla de Tokens** — muestra cada token identificado por JFlex con
su lexema, tipo y categoría. Permite ver el resultado del análisis
léxico token por token.

**Árbol Sintáctico** — visualización jerárquica del árbol generado
por CUP con código de colores por tipo de nodo. OR en azul, AND en
índigo, NOT en naranja, variables en verde. La profundidad del árbol
refleja el orden de evaluación.

**Resultado** — valores booleanos asignados aleatoriamente a cada
variable y el resultado final de evaluar la fórmula completa.

El frontend también valida localmente antes de enviar al servidor:
paréntesis balanceados, caracteres válidos y estructura básica. Los
errores se muestran con mensajes descriptivos en lugar de mensajes
técnicos del servidor.

## Tecnologías utilizadas

| Componente          | Tecnología             | Versión       |
|---------------------|------------------------|---------------|
| Análisis léxico     | JFlex                  | 1.9.1         |
| Análisis sintáctico | CUP                    | 11b-20160615  |
| Backend             | Java                   | 11+           |
| Servidor HTTP       | com.sun.net.httpserver | JDK built-in  |
| Frontend            | Angular                | 19+           |
| Estilos             | SCSS                   | —             |

## Integrantes

- César Daniel Ramos Merchán
- Alexis Jara
- Anthony Gutierrez
- Ivan Fernandez
- César López

**Universidad Nacional de Loja — FEIRNNR**
**Carrera de Ingeniería en Computación**
**Teoría de Autómatas y Computabilidad Avanzada**
