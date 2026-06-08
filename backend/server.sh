#!/bin/bash

# server.sh - Arranca el servidor REST en puerto 8080.

BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB="$BASE/../lib"
CLASSES="$BASE/../classes"
SRC="$BASE/src"
CUP_RT="$LIB/java_cup_runtime.jar"

if [[ ! -f "$CLASSES/Server.class" || ! -f "$CLASSES/Main.class" ]]; then
	echo "Compilando backend..."
	javac -d "$CLASSES" \
		-classpath "$CUP_RT" \
		-sourcepath "$SRC" \
		"$BASE/Main.java" "$BASE/Server.java"
fi

echo "Iniciando servidor en http://localhost:8080"
java -classpath "$CLASSES:$CUP_RT" Server
