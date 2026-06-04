#!/bin/bash

# server.sh - Arranca el servidor REST en puerto 8080.

BASE="$(cd "$(dirname "$0")" && pwd)"
LIB="$BASE/../lib"
CLASSES="$BASE/../classes"
CUP_RT="$LIB/java_cup_runtime.jar"

echo "Iniciando servidor en http://localhost:8080"
java -classpath "$CLASSES:$CUP_RT" Server
