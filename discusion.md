# Discusion

## Comparacion de Resultados con Trabajos Relacionados

A continuacion se comparan los resultados obtenidos en las pruebas del analizador MatLogEx con cinco trabajos relacionados, analizando similitudes, diferencias y causas subyacentes. La comparacion se fundamenta en los resultados concretos de las 34 pruebas ejecutadas (33 PASS, 1 FAIL) mas las pruebas complementarias de anidamiento profundo.

---

### Tabla Comparativa General

| Autor | Año | Metodo | Resultado Principal | Limitacion | Comparacion con MatLogEx |
|------|-----|--------|-------------------|------------|--------------------------|
| Tshering et al. [1] | 2022 | JFlex y CUP en Java | Calculadora con arboles sintacticos para expresiones aritmeticas | Solo operadores aritmeticos basicos (suma, multiplicacion) | Mismo stack tecnologico, pero MatLogEx implementa operadores logicos con jerarquia de precedencia |
| Afroozeh e Izmaylova [2] | 2016 | Parsing basado en precedencia de operadores | Resolucion efectiva de precedencia y asociatividad | Sobrecarga de rendimiento con reglas profundas | MatLogEx logra la misma precedencia sin sobrecarga gracias a la estructura LALR |
| Alamirew [3] | 2017 | GLC y arboles de expresion | Validacion estructural para parentesis anidados y jerarquias | Requiere reescritura manual si hay ambiguedad | La gramatica de MatLogEx es no ambigua de diseno, no requiere reescritura |
| Johansson [4] | 2020 | JFlex y CUP como generadores LALR | Validacion de efectividad de CUP para arboles bottom-up | Dificultad con lenguajes sin delimitadores claros | MatLogEx usa delimitadores claros (parentesis), evitando ese problema |
| Scells et al. [5] | 2020 | Arboles sintacticos para consultas booleanas | Derivacion de formulas booleanas desde parse trees | Arbol no regido por reglas de produccion deterministas | MatLogEx usa reglas de produccion estrictas y deterministas |

---

### 1. Tshering et al. (2022) — Calculadora con JFlex y CUP

**Metodo:** JFlex y CUP en Java para implementar una calculadora que evalua expresiones recursivamente mediante arboles sintacticos.

**Resultado:** Implementacion exitosa de una calculadora capaz de evaluar expresiones con operadores aritmeticos basicos (suma y multiplicacion), generando arboles de derivacion (Parse Trees).

**Limitacion:** Desarrollado exclusivamente para operadores aritmeticos basicos. Carece de la estructura gramatical necesaria para resolver la precedencia de operadores booleanos y soportar operadores logicos.

**Discusion:**

*Similitud:* Ambos trabajos utilizan el mismo stack tecnologico (JFlex + CUP + Java) y el mismo enfoque de generacion de arboles sintacticos a partir de una gramatica. Ambos demuestran la viabilidad de integrar JFlex como generador lexico y CUP como parser LALR.

*Diferencia:* MatLogEx implementa tres operadores logicos (AND, OR, NOT) con una jerarquia de precedencia explicita (NOT > AND > OR), mientras que Tshering et al. se limitan a operadores aritmeticos sin jerarquia entre suma y multiplicacion. MatLogEx ademas incorpora un servidor HTTP REST y una interfaz Angular.

*Causa:* La diferencia radica en la complejidad de la gramatica. Tshering et al. usan una GLC simple con 2 operadores al mismo nivel, mientras que MatLogEx define 3 niveles de no terminales (Exp, Term, Factor) para establecer la precedencia. Las pruebas de precedencia (tests #13, #14, #15) confirman que la estructura de 3 niveles resuelve correctamente la jerarquia: `NOT A AND B` se parsea como `(NOT A) AND B`, y `A AND B OR C` como `(A AND B) OR C`.

---

### 2. Afroozeh e Izmaylova (2016) — Parsing basado en Precedencia de Operadores

**Metodo:** Diseno de un analisis sintactico guiado por gramaticas dependientes de datos para manejar la precedencia de operadores, independientemente del algoritmo de parsing subyacente.

**Resultado:** Resolucion efectiva de la precedencia y asociatividad al estructurar el arbol, garantizando que los operadores de mayor jerarquia (NOT) se agrupen estructuralmente antes que los de menor jerarquia (AND, OR).

**Limitacion:** Posible sobrecarga de rendimiento en el parser en tiempo de ejecucion al tener que manejar reglas de produccion gramaticales muy profundas o indirectas para evitar la ambiguedad.

**Discusion:**

*Similitud:* Ambos trabajos resuelven la precedencia de operadores mediante la estructura jerarquica de la gramatica. En ambos casos, los operadores de mayor jerarquia estan en los niveles mas profundos del arbol sintactico.

*Diferencia:* Afroozeh e Izmaylova utilizan un enfoque generico basado en gramaticas dependientes de datos que puede adaptarse a diferentes algoritmos de parsing. MatLogEx, en cambio, implementa la precedencia de forma fija mediante la estructura especifica de su GLC de 3 niveles, disenada exclusivamente para el parser LALR de CUP.

*Causa:* Las pruebas de anidamiento profundo demuestran que la preocupacion por la sobrecarga de rendimiento no se materializa en MatLogEx. Incluso con 200,000 parentesis redundantes, el parser responde en tiempo aceptable. La razon es que CUP genera un parser LALR basado en tablas (no recursivo), donde la profundidad de las reglas de produccion no afecta el tiempo de ejecucion. Sin embargo, la recursion en la evaluacion del arbol via `evaluate()` y `toJson()` SÍ presenta un limite practico: ~2,000-3,000 niveles de anidamiento real (cadena de NOT) causan StackOverflowError, lo que constituye una forma de sobrecarga en tiempo de ejecucion que Afroozeh e Izmaylova anticipaban.

---

### 3. Alamirew (2017) — GLC y Arboles de Expresion

**Metodo:** Aplicacion de Gramaticas Libres de Contexto (GLC) en conjunto con el diseno de arboles de expresion (Parse Trees) para la validacion de jerarquias formales.

**Resultado:** Metodo de validacion estructural que garantiza el orden correcto de las expresiones sintacticas anidadas (parentesis), validando que cada cadena derive en un unico arbol.

**Limitacion:** Necesidad de reescribir y modificar manualmente las reglas de la gramatica si el parser detecta multiples arboles de analisis para una misma entrada, derivado de problemas de ambiguedad sintactica.

**Discusion:**

*Similitud:* Ambos trabajos utilizan GLC y arboles sintacticos como mecanismo central para validar la estructura de expresiones. MatLogEx, al igual que Alamirew, verifica que cada cadena produzca un unico arbol de analisis.

*Diferencia:* La gramatica de MatLogEx esta disenada para ser **no ambigua desde su origen**. Las pruebas #5 y #11 (parentesis redundantes) confirman que cadenas como `((A))` producen un unico arbol (la regla `Factor -> ( Exp )` simplemente propaga el nodo interno). Alamirew parte de gramaticas que pueden presentar ambiguedad y requiere correccion manual.

*Causa:* La no ambiguedad de MatLogEx se logra mediante la estructura de la GLC: las reglas de produccion estan organizadas en 3 niveles con recursion a izquierda para Exp y Term (`Exp -> Exp OR Term`, `Term -> Term AND Factor`), lo que fuerza una unica secuencia de derivacion para cualquier cadena. Las pruebas de invalidos sintacticos (#16-#24) confirman que no existe ninguna cadena para la cual el parser produzca multiples arboles — o bien se produce exactamente un arbol (valido), o bien el parser reporta error (invalido).

---

### 4. Johansson (2020) — JFlex y CUP como Generadores LALR

**Metodo:** Evaluacion y comparativa de generadores de codigo para parsers, utilizando JFlex como escaner y CUP como parser LALR.

**Resultado:** Validacion de la efectividad de usar CUP para estructurar los tokens en un Arbol de Sintaxis de forma ascendente (bottom-up), organizando el codigo exclusivamente segun las reglas de produccion gramaticales.

**Limitacion:** Dificultad netamente sintactica que sufre el analizador cuando intenta agrupar tokens si la gramatica carece de simbolos delimitadores claros, provocando fallos en la estructura del arbol.

**Discusion:**

*Similitud:* Ambos trabajos confirman la efectividad de CUP como parser LALR para construir arboles sintacticos ascendentes. MatLogEx utiliza exactamente la misma arquitectura: JFlex produce tokens, CUP los estructura en un arbol sintactico concreto (Parse Tree) segun las reglas de produccion.

*Diferencia:* Johansson senala la dificultad de CUP con lenguajes que carecen de delimitadores claros. MatLogEx utiliza parentesis como delimitadores explicitos, lo que elimina este problema. Sin embargo, las pruebas #25-#27 (lexer silencioso) revelan una dificultad relacionada: el analizador puede interpretar `AANDB` como `A AND B` sin delimitadores, lo que constituye un comportamiento ambiguo que Johansson anticipaba.

*Causa:* Los parentesis en MatLogEx actuan como delimitadores claros que permiten a CUP agrupar tokens sin ambiguedad. Las pruebas #6-#9 (anidamiento completo), #11 (parentesis redundantes) y #21 (parentesis vacio) demuestran que el parser maneja correctamente los parentesis en todos los casos. Sin embargo, la ausencia de delimitadores entre tokens (como en `AANDB`) aprovecha la regla de emparejamiento mas largo de JFlex, lo que puede considerarse una forma de "tokenizacion silenciosa" que Johansson identificaria como un riesgo potencial.

---

### 5. Scells et al. (2020) — Arboles Sintacticos para Consultas Booleanas

**Metodo:** Diseno de un marco computacional para formular automaticamente consultas booleanas complejas, utilizando analizadores sintacticos para derivar la jerarquia logica de los conceptos.

**Resultado:** Modelo estructural para derivar formulas booleanas directamente desde un parse tree, manteniendo jerarquicamente los nodos superiores con el operador AND y las ramas inferiores con el operador OR.

**Limitacion:** La construccion del arbol sintactico no se rige por reglas de produccion deterministas para operadores explicitos. El metodo asume la jerarquia de los conectores logicos basandose en niveles de agrupacion en lugar de exigir una estructura formal estricta.

**Discusion:**

*Similitud:* Ambos trabajos utilizan parse trees para representar formulas booleanas. En ambos casos, la estructura del arbol determina la jerarquia de evaluacion de los operadores logicos.

*Diferencia:* Scells et al. construyen el arbol basandose en niveles de agrupacion (asumiendo que AND esta en niveles superiores y OR en niveles inferiores), sin reglas de produccion deterministas. MatLogEx, en cambio, utiliza una GLC formal con reglas de produccion explicitas que determinan de manera unica la estructura del arbol. Las pruebas #13-#15 confirman que la precedencia esta determinada por la gramatica, no por supuestos de agrupacion.

*Causa:* La diferencia fundamental es que Scells et al. trabajan con un modelo conceptual de jerarquia (AND sobre OR), mientras que MatLogEx implementa la jerarquia mediante una GLC concreta. En MatLogEx, la precedencia NOT > AND > OR no es un supuesto sino una consecuencia directa de las reglas:
```
Exp   → Exp OR Term   | Term      (OR es raiz)
Term  → Term AND Factor | Factor  (AND en medio)
Factor → NOT Factor | ( Exp ) | VARIABLE  (NOT es profundo)
```
Las pruebas de arbol (tests #6, #13, #14) muestran que esta estructura produce arboles donde OR esta en la raiz, AND en niveles intermedios y NOT en niveles profundos, exactamente lo opuesto al modelo de Scells et al. (que colocaba AND en niveles superiores). Esto se debe a que Scells et al. modelan la **generacion** de consultas booleanas (donde AND agrupa conceptos generales), mientras que MatLogEx modela el **analisis** de formulas existentes (donde la precedencia determina la agrupacion natural).

---

## Conclusion de la Discusion

Los resultados de las pruebas de MatLogEx confirman que:

1. **El stack JFlex + CUP + Java** es adecuado para implementar analizadores logicos con precedencia de operadores, validando los hallazgos de Tshering et al. [1] y Johansson [4], y extendiendolos al dominio booleano.

2. **La precedencia de operadores** se resuelve eficazmente mediante la estructura jerarquica de la GLC, como proponen Afroozeh e Izmaylova [2], pero sin la sobrecarga de rendimiento que ellos anticipaban, gracias a la naturaleza tabular (no recursiva) del parser LALR de CUP.

3. **La no ambiguedad** es inherente al diseno de la gramatica, lo que evita la necesidad de reescritura manual que Alamirew [3] identifica como limitacion.

4. **El uso de delimitadores explicitos** (parentesis) evita las dificultades de agrupacion que Johansson [4] senala, aunque persiste el comportamiento silencioso del lexer ante ausencia de separadores.

5. **Las reglas de produccion deterministas** diferencian a MatLogEx del enfoque heuristico de Scells et al. [5], proporcionando una base formal solida para el analisis de expresiones booleanas.

La unica prueba fallida (tabulacion escapada en JSON, test #33) no corresponde a un problema del analizador lexico-sintactico sino a una limitacion del parser JSON casero del servidor, lo que refuerza la importancia de utilizar librerias estandar para el manejo de formatos de intercambio de datos.
