#!/bin/bash

# build.sh - Compila y ejecuta el analizador de fórmulas lógicas.
# Pasos: JFlex → CUP → javac → java
#
# Uso: ./build.sh "<formula>"
# Ejemplo: ./build.sh "((A AND B) OR (NOT C))"

BASE="$(cd "$(dirname "$0")" && pwd)"
LIB="$BASE/lib"
SRC="$BASE/src"
CLASSES="$BASE/classes"

JFLEX="$LIB/jflex.jar"
CUP="$LIB/java_cup.jar"
CUP_RT="$LIB/java_cup_runtime.jar"

# Limpiar y crear carpeta de clases
rm -rf "$CLASSES" && mkdir -p "$CLASSES"

echo "[ 1/4 ] Ejecutando JFlex..."
java -jar "$JFLEX" -d "$SRC/grammar" "$SRC/grammar/Lexer.jflex"
if [ $? -ne 0 ]; then echo "ERROR en JFlex"; exit 1; fi

echo "[ 2/4 ] Ejecutando CUP..."
java -cp "$CUP" java_cup.Main \
    -destdir "$SRC/grammar" \
    -parser parser \
    -symbols sym \
    "$SRC/grammar/parser.cup"
if [ $? -ne 0 ]; then echo "ERROR en CUP"; exit 1; fi

echo "[ 3/4 ] Compilando Java..."
javac -d "$CLASSES" \
    -classpath "$CUP_RT" \
    -sourcepath "$SRC" \
    "$BASE/Main.java" "$BASE/Server.java"
if [ $? -ne 0 ]; then echo "ERROR en javac"; exit 1; fi

echo "[ 4/4 ] Ejecutando..."
echo ""
java -classpath "$CLASSES:$CUP_RT" Main "$1"
