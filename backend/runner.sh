#!/bin/bash
# Runner: inicia servidor, ejecuta pruebas, limpia
set -e

BASE="/home/cesar/SEXTO CICLO/AUTOMATAS/UNIDAD2/Matlogex/backend"

# Iniciar servidor
java -classpath "$BASE/../classes:$BASE/../lib/java_cup_runtime.jar" Server &
SERVER_PID=$!
echo "Servidor iniciado (PID=$SERVER_PID)"
sleep 2

# Verificar
if ! curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo "ERROR: servidor no responde"
    kill $SERVER_PID 2>/dev/null
    exit 1
fi

# Ejecutar pruebas (sin las de anidamiento profundo, se prueban aparte)
python3 "$BASE/test_suite.py"

# Limpiar
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
echo "Servidor detenido"
