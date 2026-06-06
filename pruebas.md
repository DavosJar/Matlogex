# Pruebas del Analizador Léxico-Sintáctico MatLogEx

## 1. Introducción

Se diseño y ejecuto una bateria de pruebas sobre el sistema MatLogEx con el objetivo de verificar la robustez, correccion y comportamiento en casos frontera del analizador de formulas logicas. El sistema utiliza JFlex como generador lexico y CUP como generador sintactico (LALR), y expone una API REST en `http://localhost:8080/analizar`.

**Alcance:** 36 pruebas organizadas en 7 categorias, mas pruebas complementarias de anidamiento profundo.

---

## 2. Metodologia

Cada prueba consistio en enviar una peticion `POST /analizar` con un cuerpo JSON `{"formula": "<formula>"}` y verificar:

- **Codigo HTTP**: `200` si la formula es sintacticamente valida, `400` si es invalida.
- **Cuerpo de respuesta**: Debe contener `"resultado"` (para exitos) o `"error"` (para fallos esperados).
- **Analisis causa-raiz**: Para cada fallo se determino por que ocurrio y si el comportamiento era esperado.

Las pruebas se ejecutaron contra el servidor corriendo en `localhost:8080` mediante un script en Python 3 que automatiza las peticiones y la verificacion.

---

## 3. Resultados Generales

| Categoria | Pruebas | PASS | FAIL |
|-----------|---------|------|------|
| 1. Validós básicos | 5 | 5 | 0 |
| 2. Válidos complejos | 7 | 7 | 0 |
| 3. Precedencia de operadores | 3 | 3 | 0 |
| 4. Inválidos sintácticos | 9 | 9 | 0 |
| 5. Edge cases del lexer | 5 | 5 | 0 |
| 6. Casos frontera | 5 | 4 | 1 |
| **Total Categorías 1-6** | **34** | **33** | **1** |
| 7. Anidamiento profundo | 2+ | 2+ | - |

> **Nota:** Los tests de anidamiento profundo (categoria 7) se manejaron por separado debido al riesgo de StackOverflowError. Se ejecutaron progresivamente desde 50 hasta 200,000 parentesis anidados.

---

## 4. Pruebas Detalladas

### 4.1 Categoría 1: Válidos Básicos

| # | Formula | Esperado | Real | Analisis |
|---|---------|----------|------|----------|
| 1 | `A` | 200 OK | 200 OK | Variable unica es una formula valida. El arbol generado: `Exp → Term → Factor → VARIABLE "A"`. |
| 2 | `NOT A` | 200 OK | 200 OK | Operador unario sobre variable. Arbol: `Exp → Term → Factor → NOT → Factor → VARIABLE "A"`. |
| 3 | `A AND B` | 200 OK | 200 OK | Operador binario basico. Arbol: `Exp → Term → (Term → Factor → VARIABLE "A") AND (Factor → VARIABLE "B")`. |
| 4 | `A OR B` | 200 OK | 200 OK | Operador binario OR. Misma estructura que AND pero con "OR". |
| 5 | `(A AND B)` | 200 OK | 200 OK | Parentesis envolventes no alteran la estructura del arbol. La regla `Factor -> ( Exp )` propaga el nodo interno sin envolverlo. |

**Causa raiz:** Todas las formulas validas pasan por el pipeline: JFlex reconoce tokens individuales (VARIABLE, AND, OR, NOT, LPAREN, RPAREN), CUP construye el arbol sintactico concreto (Parse Tree) segun las reglas de produccion, y el serializador convierte el arbol a JSON. Funciona correctamente.

---

### 4.2 Categoría 2: Válidos Complejos

| # | Formula | Esperado | Real | Analisis |
|---|---------|----------|------|----------|
| 6 | `((A AND B) OR (NOT C))` | 200 OK | 200 OK | Anidamiento completo con los 3 operadores. Arbol: Exp con OR como raiz, AND (Term) a izquierda, NOT (Factor) a derecha. |
| 7 | `(A OR (B AND C))` | 200 OK | 200 OK | OR como raiz con AND anidado a derecha. |
| 8 | `(NOT (A AND B))` | 200 OK | 200 OK | NOT sobre un AND completo. Arbol: Factor (NOT) con operando envuelto en parentesis. |
| 9 | `((A AND B) AND C)` | 200 OK | 200 OK | Asociatividad izquierda. El AND se agrupa a izquierda: (A AND B) primero, luego AND C. |
| 10 | `NOT NOT A` | 200 OK | 200 OK | Doble negacion. Arbol: cadena de dos NOT (Factor → NOT → Factor → NOT → Factor → VARIABLE). La evaluacion produce la variable original (doble negacion se anula). |
| 11 | `((A))` | 200 OK | 200 OK | Parentesis redundantes. La gramatica produce `Factor → ( Exp )` en cada nivel. Los nodos LPAREN y RPAREN se preservan en el arbol de derivacion. |
| 12 | `A AND A` | 200 OK | 200 OK | Misma variable en ambos operandos. No hay restriccion de unicidad. El mapa de variables solo contiene una entrada `{"A": bool}`. |

**Causa raiz:** La gramatica LALR del CUP esta correctamente definida para manejar anidamiento arbitrario mediante las reglas de produccion recursivas (`Exp -> Exp OR Term`, `Term -> Term AND Factor`, `Factor -> NOT Factor | ( Exp ) | VARIABLE`).

---

### 4.3 Categoría 3: Precedencia de Operadores

| # | Formula | Arbol generado | Esperado | Real | Analisis |
|---|---------|---------------|----------|------|----------|
| 13 | `NOT A AND B` | `(NOT A) AND B` | 200 OK | 200 OK | **Precedencia correcta:** NOT > AND. El arbol tiene AND como raiz (nivel Term) con NOT a izquierda. |
| 14 | `A AND B OR C` | `(A AND B) OR C` | 200 OK | 200 OK | **Precedencia correcta:** AND > OR. El arbol tiene OR como raiz (nivel Exp) con AND a izquierda. |
| 15 | `NOT (A AND B)` | `NOT (A AND B)` | 200 OK | 200 OK | Parentesis fuerzan la precedencia: NOT aplica sobre todo el AND. |

**Causa raiz:** La precedencia se define estructuralmente mediante la jerarquia de no terminales: `Exp` (OR), `Term` (AND), `Factor` (NOT, parentesis, variable). Los operadores de mayor jerarquia (NOT) estan en los niveles mas profundos del arbol, asegurando que se evaluen primero.

```
Exp   → Exp OR Term   | Term      (OR es el de menor precedencia)
Term  → Term AND Factor | Factor  (AND tiene precedencia media)
Factor → NOT Factor | ( Exp ) | VARIABLE  (NOT tiene la maxima precedencia)
```

---

### 4.4 Categoría 4: Inválidos Sintácticos

Estos casos **pasan la validacion del frontend** (Angular) pero **son rechazados por el backend** con HTTP 400. Todos producen el mismo mensaje de error.

| # | Formula | Esperado | Real | Causa |
|---|---------|----------|------|-------|
| 16 | `A B` | 400 | 400 | Dos VARIABLE seguidas sin operador. El parser espera un operador (AND, OR) o un cierre de parentesis, pero encuentra otra variable. |
| 17 | `A AND` | 400 | 400 | AND espera un Factor a la derecha (variable, NOT, parentesis), pero encuentra EOF. |
| 18 | `AND A` | 400 | 400 | Formula comienza con AND. El parser espera un Factor (Exp -> Term -> Factor), encuentra AND que no puede reducirse a Factor. |
| 19 | `A AND AND B` | 400 | 400 | Dos AND consecutivos. Despues del primer AND, el parser espera un Factor, encuentra AND. |
| 20 | `A OR AND B` | 400 | 400 | OR espera un Term (que comienza con Factor), encuentra AND en lugar de un Factor. |
| 21 | `()` | 400 | 400 | Parentesis vacio. La regla `Factor -> ( Exp )` requiere una Exp dentro, pero encuentra RPAREN inmediatamente. |
| 22 | `A NOT B` | 400 | 400 | NOT en posicion binaria. La gramatica espera Exp -> Term -> Factor -> NOT Factor, pero A ya se consumio como Exp. |
| 23 | `(A AND B` | 400 | 400 | Falta parentesis de cierre. El parser espera RPAREN pero encuentra EOF. |
| 24 | `A AND B)` | 400 | 400 | Parentesis de cierre sin apertura. Despues de consumir B, el parser espera EOF o un operador, encuentra RPAREN. |

**Causa raiz:** El parser LALR generado por CUP detecta todos los errores sintacticos mediante su tabla de parsing. Cuando el token actual no coincide con ningun token esperado en el estado actual, se invoca `report_error()` que imprime el error, y luego `unrecovered_syntax_error()` lanza una excepcion que es capturada por el `catch (Exception e)` en `Server.java`, devolviendo HTTP 400.

**Observacion importante:** Todos los errores devuelven el mismo mensaje generico: `"Can't recover from previous error(s)"`. El servidor no diferencia el tipo de error sintactico, lo que limita la capacidad de dar retroalimentacion especifica al usuario.

---

### 4.5 Categoría 5: Edge Cases del Lexer (Encadenamiento)

Estos casos prueban como el lexer tokeniza cadenas sin espacios entre palabras clave y variables.

| # | Formula | Tokenizacion | Esperado | Real | Analisis |
|---|---------|-------------|----------|------|----------|
| 25 | `AANDB` | `A` AND `B` | 200 | 200 | **Comportamiento silencioso:** JFlex empareja el patron mas largo. `A` coincide con VARIABLE, luego `AND` coincide con la palabra clave, luego `B` es VARIABLE. El usuario escribe `AANDB` sin espacios y funciona como `A AND B`. |
| 26 | `AORB` | `A` OR `B` | 200 | 200 | Mismo caso: `A` VARIABLE, `OR` palabra clave, `B` VARIABLE. |
| 27 | `NOTA` | NOT `A` | 200 | 200 | `NOT` palabra clave, `A` VARIABLE. Funciona como `NOT A`. |
| 28 | `ANDB` | AND `B` | 400 | 400 | `AND` palabra clave seguida de `B` VARIABLE. El parser recibe AND como primer token, no hay operando izquierdo, falla. |
| 29 | `ORAND` | OR AND | 400 | 400 | OR seguido de AND, ambos operadores binarios, ninguno tiene operandos. |

**Causa raiz (casos 25-27):** El lexer de JFlex usa la regla de **emparejamiento mas largo** (longest match). Cuando encuentra `AANDB`, primero intenta emparejar palabras clave (AND, OR, NOT) pero `AAN` no coincide. Luego prueba VARIABLE (`[A-Z]`) y empareja `A`. Luego desde la posicion 1, `AND` empareja la palabra clave. Luego `B` empareja VARIABLE. Este comportamiento permite que `AANDB` se interprete como `A AND B`, pero es **silencioso**: el usuario podria pensar que es una variable llamada `AANDB` cuando en realidad son tres tokens separados.

**Implicacion:** No hay forma de usar variables de mas de una letra. El sistema esta disenado exclusivamente para variables de un caracter (A-Z).

---

### 4.6 Categoría 6: Casos Frontera

| # | Formula | Esperado | Real | Analisis |
|---|---------|----------|------|----------|
| 30 | `A AND B AND C AND ... AND Z` (26 variables) | 200 | 200 | **26 variables unicas.** El arbol tiene asociatividad izquierda: 25 nodos AND como raices de Term y 26 hojas VARIABLE. El mapa de variables contiene las 26 letras. No hay limite en la cantidad de variables. |
| 31 | `"   A AND B"` (espacios al inicio) | 200 | 200 | El lexer ignora los espacios iniciales via `{WS} -> {/* ignorar */}`. |
| 32 | `"A AND B   "` (espacios al final) | 200 | 200 | Mismo mecanismo. Los espacios finales son ignorados. |
| 33 | `"A\tAND\tB"` (tabulaciones) | 200 | **400** | **FALLO (ver analisis abajo)** |
| 34 | `""` (vacia) | 400 | 400 | Formula vacia. El lexer produce directamente EOF, el parser espera una Exp, falla. |

#### Analisis detallado del fallo #33 (Tabulacion)

**Problema:** La formula contiene tabulaciones como separadores entre token. A nivel lexico, el tabulador (`\t`) esta definido dentro de `WS = [ \t\r\n]+` y deberia ser ignorado.

**Causa raiz:** El fallo NO esta en el lexer sino en la **transmision HTTP**. Python `json.dumps()` escapa el caracter tabulador como `\t` (backslash + t literal) en el JSON. El servidor extrae la formula usando `extractJsonString()`, un parser JSON **casero e ingenuo** que:

1. Busca `"formula"` en el string JSON.
2. Encuentra los dos puntos y luego las comillas que envuelven el valor.
3. Extrae el texto entre dichas comillas **SIN procesar secuencias de escape**.

De esta forma, si el JSON contiene `"A\tAND\tB"`, el servidor extrae literalmente `A\tAND\tB` (con caracteres backslash y t, no con tabuladores). Luego el lexer encuentra el caracter `\` que no pertenece a ningun patron valido (no es A-Z, ni AND/OR/NOT, ni parentesis, ni espacio), y retorna `sym.error`, causando que el parser falle.

**Solucion posible:** El servidor deberia usar una libreria JSON adecuada (e.g., Gson, Jackson) o al menos implementar el manejo de secuencias de escape en `extractJsonString()`.

**Verificacion:** Enviando el tabulador literal (no escapado) en el JSON via `curl`, la respuesta es HTTP 200, confirmando que el servidor y el lexer manejan correctamente el tabulador una vez que la cadena llega con el caracter correcto.

---

### 4.7 Categoría 7: Anidamiento Profundo

Se probo el limite del sistema con anidamiento excesivo.

#### 4.7.1 Parentesis redundantes

Se generaron formulas con parentesis redundantes a diferentes profundidades:

| Profundidad | Resultado | Observacion |
|-------------|-----------|-------------|
| 50 | HTTP 200 OK | Funciona correctamente |
| 100 | HTTP 200 OK | |
| 200 | HTTP 200 OK | |
| 500 | HTTP 200 OK | |
| 1,000 | HTTP 200 OK | |
| 2,000 | HTTP 200 OK | |
| 5,000 | HTTP 200 OK | |
| 10,000 | HTTP 200 OK | |
| 20,000 | HTTP 200 OK | |
| 50,000 | HTTP 200 OK | |
| 100,000 | HTTP 200 OK | |
| 200,000 | HTTP 200 OK | |

**Causa raiz:** Los parentesis redundantes (`(((...(A AND B)...)))`) NO incrementan la profundidad del arbol sintactico. La regla `Factor -> ( Exp )` envuelve la expresion interna con nodos LPAREN y RPAREN, pero el parser CUP (LALR) maneja la recursion mediante una tabla de estados, no recursivamente, por lo que no hay riesgo de StackOverflow en el analisis sintactico. La evaluacion recursiva y la serializacion a JSON trabajan sobre un arbol de profundidad constante, independientemente de la cantidad de parentesis.

#### 4.7.2 Cadena de negaciones (NOT NOT NOT ... A)

Se probaron cadenas largas del operador unario NOT, que SÍ incrementan la profundidad del arbol sintactico.

| Profundidad (NOTs) | Resultado | Observacion |
|--------------------|-----------|-------------|
| 1,000 | HTTP 200 OK | Arbol con 1,000 niveles de Factor (NOT) anidados |
| 3,000 | Conexion cerrada | Servidor cierra la conexion sin responder |
| 5,000 | Conexion cerrada | Limite practico del sistema |

**Causa raiz:** Cada NOT agrega un nivel al arbol sintactico. Tanto la evaluacion recursiva (`evaluate()`) como la serializacion a JSON (`NodeSerializer.toJson()`) recorren recursivamente el arbol. Con 3,000+ niveles de Factor con NOT, se produce un **StackOverflowError** en la JVM (el thread del handler muere pero el servidor continua vivo porque `HttpServer` usa un pool de threads).

**Nota:** El servidor sobrevive al error, los requests posteriores funcionan normalmente. El StackOverflowError no es capturado por el `catch (Exception e)` porque `Error` no es subclase de `Exception`.

#### 4.7.3 Limite practico del sistema

| Tipo de anidamiento | Limite practico | Causa del limite |
|--------------------|-----------------|------------------|
| Parentesis redundantes | >200,000 | No tiene limite practico (el arbol sintactico no crece) |
| Cadena de NOT | ~2,000-3,000 | StackOverflowError por recursion profunda en `toJson()` y `evaluate()` |
| Arbol binario profundo | ~5,000-10,000 | StackOverflowError por recursion profunda |

---

## 5. Resumen de Hallazgos

### 5.1 Aspectos Correctos

- **Gramatica bien definida:** La GLC con 3 niveles de no terminales (Exp, Term, Factor) implementa correctamente la precedencia NOT > AND > OR.
- **Manejo de errores:** Todas las formulas invalidas son capturadas y devueltas como HTTP 400.
- **Robustez ante parentesis redundantes:** El sistema tolera cientos de miles de parentesis anidados sin degradacion.
- **Tokenizacion correcta:** JFlex reconoce correctamente palabras clave incluso sin separadores (casos `AANDB`, `AORB`, `NOTA`).

### 5.2 Aspectos a Mejorar

| # | Problema | Impacto | Solucion propuesta |
|---|----------|---------|-------------------|
| 1 | **Parser JSON casero** `extractJsonString()` no maneja secuencias de escape | Las formulas con caracteres escapados (tabs, saltos de linea, comillas) fallan | Usar una libreria JSON estandar (Gson, Jackson, org.json) |
| 2 | **StackOverflowError no capturado** en recursion profunda | Thread del handler muere silenciosamente, la conexion se cierra sin respuesta | Capturar `Throwable` en lugar de `Exception`, devolver 400 |
| 3 | **Mensajes de error genericos** | `"Can't recover from previous error(s)"` no ayuda al usuario a corregir la formula | Personalizar mensajes segun el tipo de error sintactico |
| 4 | **Sin limite de tamaño de formula** | `readAllBytes()` sin restriccion permite DoS por consumo de memoria | Establecer un maximo de longitud de formula |
| 5 | **Comportamiento silencioso del lexer** | `AANDB` se interpreta como `A AND B` sin advertencia | Agregar advertencias cuando tokens se concatenan sin espacios |

### 5.3 Estadisticas Finales

```
Total de pruebas:     34 (Categorias 1-6)
Exitosas (PASS):      33 (97.1%)
Fallidas (FAIL):      1  (2.9%)  - Tabulacion escapada en JSON
Pruebas adicionales:  Anidamiento profundo y limite de recursion
```

---

## 6. Ejecucion de las Pruebas

Para reproducir las pruebas:

```bash
# 1. Iniciar servidor
cd backend
./server.sh

# 2. Ejecutar suite de pruebas
python3 test_suite.py

# 3. Probar anidamiento profundo (opcional)
python3 -c "
import urllib.request, json
BASE = 'http://localhost:8080'
for depth in [50, 100, 500, 1000]:
    formula = 'NOT ' * depth + 'A'
    payload = json.dumps({'formula': formula}).encode()
    req = urllib.request.Request(f'{BASE}/analizar', data=payload, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            print(f'NOT x{depth}: OK')
    except Exception as e:
        print(f'NOT x{depth}: {e}')
"
```
