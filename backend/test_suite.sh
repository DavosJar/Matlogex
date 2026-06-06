#!/bin/bash
# Batería de pruebas para MatLogEx
# Uso: ./test_suite.sh
# Requiere: servidor corriendo en localhost:8080

BASE="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0
RESULTS_FILE="/tmp/test_results.txt"
> "$RESULTS_FILE"

green() { echo -e "\033[32m$1\033[0m"; }
red()   { echo -e "\033[31m$1\033[0m"; }
bold()  { echo -e "\033[1m$1\033[0m"; }

run_test() {
    local id="$1"
    local desc="$2"
    local formula="$3"
    local esperado="$4"
    local check="$5"

    TOTAL=$((TOTAL + 1))

    # Build JSON payload con escapado básico
    local json="{\"formula\":\"$formula\"}"

    local tmpf="/tmp/test_resp_$$.txt"
    curl -s -o "$tmpf" -w "%{http_code}" -X POST "$BASE/analizar" \
        -H "Content-Type: application/json" \
        -d "$json" 2>&1 > /tmp/test_http_$$.txt
    local http_code
    http_code=$(cat /tmp/test_http_$$.txt)
    local body
    body=$(cat "$tmpf")
    rm -f "$tmpf" /tmp/test_http_$$.txt

    local status="PASS"
    local detalle=""

    if [ "$http_code" != "$esperado" ]; then
        status="FAIL"
        detalle="HTTP $http_code (esperaba $esperado)"
        FAIL=$((FAIL + 1))
    elif [ "$check" != "IGNORE" ] && ! echo "$body" | grep -q "$check"; then
        status="FAIL"
        detalle="No contiene '$check' en body"
        FAIL=$((FAIL + 1))
    else
        PASS=$((PASS + 1))
    fi

    # Escape pipes for storage
    local desc_esc=$(echo "$desc" | sed 's/|/\\|/g')
    local formula_esc=$(echo "$formula" | sed 's/|/\\|/g')
    local body_esc=$(echo "$body" | tr -d '\n\r' | sed 's/|/\\|/g')
    echo "id:${id}|desc:${desc_esc}|formula:${formula_esc}|http:${http_code}|status:${status}|body:${body_esc}" >> "$RESULTS_FILE"

    if [ "$status" = "PASS" ]; then
        green "[PASS] #$id: $desc"
    else
        red "[FAIL] #$id: $desc"
        echo "       Formula: $formula"
        echo "       HTTP: $http_code | Esperado: $esperado"
        echo "       Body: $body"
        [ -n "$detalle" ] && echo "       Detalle: $detalle"
    fi
}

echo "============================================"
echo "  MatLogEx - Batería de Pruebas"
echo "  $(date)"
echo "============================================"
echo ""

# ============================================================================
# CATEGORÍA 1: VÁLIDOS BÁSICOS
# ============================================================================
bold "═══ CATEGORÍA 1: VÁLIDOS BÁSICOS ═══"

run_test "01" "Variable única"                             "A"               "200" "resultado"
run_test "02" "NOT simple"                                 "NOT A"           "200" "resultado"
run_test "03" "AND simple"                                 "A AND B"         "200" "resultado"
run_test "04" "OR simple"                                  "A OR B"          "200" "resultado"
run_test "05" "AND con paréntesis"                         "(A AND B)"       "200" "resultado"

# ============================================================================
# CATEGORÍA 2: VÁLIDOS COMPLEJOS
# ============================================================================
bold "═══ CATEGORÍA 2: VÁLIDOS COMPLEJOS ═══"

run_test "06" "AND y OR anidados"                          "((A AND B) OR (NOT C))" "200" "resultado"
run_test "07" "OR con AND anidado"                         "(A OR (B AND C))" "200" "resultado"
run_test "08" "NOT sobre AND"                              "(NOT (A AND B))"  "200" "resultado"
run_test "09" "Triple AND"                                 "((A AND B) AND C)" "200" "resultado"
run_test "10" "Doble negación"                             "NOT NOT A"        "200" "resultado"
run_test "11" "Paréntesis redundantes"                     "((A))"            "200" "resultado"
run_test "12" "Misma variable repetida"                    "A AND A"          "200" "resultado"

# ============================================================================
# CATEGORÍA 3: PRECEDENCIA DE OPERADORES
# ============================================================================
bold "═══ CATEGORÍA 3: PRECEDENCIA DE OPERADORES ═══"

run_test "13" "NOT antes que AND (precedencia)"            "NOT A AND B"      "200" "resultado"
run_test "14" "AND antes que OR (precedencia)"             "A AND B OR C"     "200" "resultado"
run_test "15" "NOT sobre AND explícito"                    "NOT (A AND B)"    "200" "resultado"

# ============================================================================
# CATEGORÍA 4: INVÁLIDOS SINTÁCTICOS (pasan front, fallan backend)
# ============================================================================
bold "═══ CATEGORÍA 4: INVÁLIDOS SINTÁCTICOS ═══"

run_test "16" "Variables seguidas sin operador"            "A B"              "400" "error"
run_test "17" "AND sin operando derecho"                   "A AND"            "400" "error"
run_test "18" "AND sin operando izquierdo"                 "AND A"            "400" "error"
run_test "19" "Operador repetido AND AND"                  "A AND AND B"      "400" "error"
run_test "20" "OR seguido de AND"                          "A OR AND B"       "400" "error"
run_test "21" "Paréntesis vacío"                           "()"               "400" "error"
run_test "22" "NOT en medio de binarios"                   "A NOT B"          "400" "error"
run_test "23" "AND con paréntesis desbalanceados"          "(A AND B"         "400" "error"
run_test "24" "Cierre de paréntesis sin apertura"          "A AND B)"         "400" "error"

# ============================================================================
# CATEGORÍA 5: EDGE CASES DEL LEXER (palabras sin espacios)
# ============================================================================
bold "═══ CATEGORÍA 5: EDGE CASES DEL LEXER ═══"

run_test "25" "AANDB tokeniza como A AND B"                "AANDB"            "200" "resultado"
run_test "26" "AORB tokeniza como A OR B"                  "AORB"             "200" "resultado"
run_test "27" "NOTA tokeniza como NOT A"                   "NOTA"             "200" "resultado"
run_test "28" "ANDB tokeniza como AND B (invalido)"        "ANDB"             "400" "error"
run_test "29" "ORAND tokeniza como OR AND"                 "ORAND"            "400" "error"

# ============================================================================
# CATEGORÍA 6: CASOS FRONTERA
# ============================================================================
bold "═══ CATEGORÍA 6: CASOS FRONTERA ═══"

run_test "30" "26 variables diferentes (A-Z)"              "A AND B AND C AND D AND E AND F AND G AND H AND I AND J AND K AND L AND M AND N AND O AND P AND Q AND R AND S AND T AND U AND V AND W AND X AND Y AND Z" "200" "resultado"
run_test "31" "Espacios extra al inicio"                   "   A AND B"       "200" "resultado"
run_test "32" "Espacios extra al final"                    "A AND B   "       "200" "resultado"
run_test "33" "Tabulacion como separacion"                 "A	AND	B"          "200" "resultado"
run_test "34" "Formula vacia"                              ""                 "400" "error"

# ============================================================================
# CATEGORÍA 7: ANIDAMIENTO MODERADO
# ============================================================================
bold "═══ CATEGORÍA 7: ANIDAMIENTO MODERADO ═══"

DEEP50=$(python3 -c "print('('*50 + 'A AND B' + ')'*50)")
run_test "35" "Anidamiento 50 parentesis"                  "$DEEP50"          "200" "resultado"

DEEP200=$(python3 -c "print('('*200 + 'A AND B' + ')'*200)")
run_test "36" "Anidamiento 200 parentesis"                 "$DEEP200"         "200" "resultado"

# ============================================================================
# RESUMEN
# ============================================================================
echo ""
echo "============================================"
echo "  RESUMEN"
echo "  Total: $TOTAL | PASS: $PASS | FAIL: $FAIL"
echo "============================================"
