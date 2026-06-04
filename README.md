# MatLogEx — Guía de Instalación y Ejecución

## Requisitos previos

- Java JDK 11 o superior
- Node.js 18 o superior
- Angular CLI: `npm install -g @angular/cli`
- Git

## Estructura del proyecto

```
ExamenU2/
├── Matlogex/
│   ├── src/grammar/
│   │   ├── Lexer.jflex
│   │   ├── parser.cup
│   │   └── node/
│   ├── lib/
│   ├── Main.java
│   ├── Server.java
│   ├── build.sh
│   └── server.sh
└── matlogex-ui/
```

## Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/DavosJar/Matlogex.git
cd Matlogex
```

## Paso 2 — Descargar dependencias Java

```bash
cd lib

wget https://github.com/jflex-de/jflex/releases/download/v1.9.1/jflex-1.9.1.tar.gz -O jflex.tar.gz
tar -xzf jflex.tar.gz && cp jflex-1.9.1/lib/jflex-full-1.9.1.jar jflex.jar
rm -rf jflex.tar.gz jflex-1.9.1

wget "https://repo1.maven.org/maven2/com/github/vbmacher/java-cup/11b-20160615/java-cup-11b-20160615.jar" -O java_cup.jar
wget "https://repo1.maven.org/maven2/com/github/vbmacher/java-cup-runtime/11b-20160615/java-cup-runtime-11b-20160615.jar" -O java_cup_runtime.jar

cd ..
```

## Paso 3 — Compilar el backend

```bash
chmod +x build.sh server.sh
./build.sh "((A AND B) OR (NOT C))"
```

Salida esperada:

```
[ 1/4 ] Ejecutando JFlex...
[ 2/4 ] Ejecutando CUP...
[ 3/4 ] Compilando Java...
[ 4/4 ] Ejecutando...
Formula: ((A AND B) OR (NOT C))
Arbol sintáctico:
  OR
    AND
      A
      B
    NOT
      C
Resultado: true/false
```

## Paso 4 — Levantar el servidor REST

Abre una terminal y déjala corriendo:

```bash
./server.sh
```

Verás: `Servidor iniciado en http://localhost:8080`

Puedes verificar con:

```bash
curl -X POST http://localhost:8080/analizar \
  -H "Content-Type: application/json" \
  -d '{"formula":"((A AND B) OR (NOT C))"}'
```

## Paso 5 — Levantar el frontend Angular

Abre una segunda terminal:

```bash
cd ../matlogex-ui
npm install
ng serve
```

Abre el navegador en: **http://localhost:4200**

## Uso rápido

1. Escribe una fórmula en el campo de texto, por ejemplo: `((A AND B) OR (NOT C))`
2. Haz clic en **Analizar**
3. Revisa la pestaña **Tokens** para ver el análisis léxico
4. Revisa la pestaña **Árbol Sintáctico** para ver la estructura jerárquica
5. Observa los **Valores Asignados** y el **Resultado Final**

## Fórmulas de ejemplo válidas

```
((A AND B) OR (NOT C))
(A OR (B AND C))
(NOT (A AND B))
((A AND B) AND (C OR D))
(NOT A)
```

## Solución de problemas

| Problema | Solución |
|----------|----------|
| `ERROR en JFlex` | Verifica que `lib/jflex.jar` existe |
| `ERROR en CUP` | Verifica que `lib/java_cup.jar` existe |
| `ERROR en javac` | Verifica que tienes JDK 11+ instalado |
| Frontend no conecta | Verifica que `./server.sh` está corriendo |
| Puerto 8080 ocupado | Ejecuta `sudo fuser -k 8080/tcp` |