#!/usr/bin/env python3
"""Batería de pruebas para MatLogEx - Categorías 1 a 6 (sin anidamiento profundo)."""
import json
import urllib.request
import urllib.error
import sys

BASE = "http://localhost:8080"
PASS = 0
FAIL = 0
TOTAL = 0
results = []

def test(id, desc, formula, esperado, check_substring):
    global PASS, FAIL, TOTAL
    TOTAL += 1
    payload = json.dumps({"formula": formula}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE}/analizar",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            http_code = resp.status
            body = resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        http_code = e.code
        body = e.read().decode("utf-8")
    except Exception as e:
        http_code = 0
        body = str(e)

    status = "PASS"
    detalle = ""
    if http_code != esperado:
        status = "FAIL"
        detalle = f"HTTP {http_code} (esperaba {esperado})"
    elif check_substring and check_substring not in body:
        status = "FAIL"
        detalle = f"No contiene '{check_substring}' en body"

    if status == "PASS":
        PASS += 1
        print(f"[PASS] #{id:02d}: {desc}")
    else:
        FAIL += 1
        print(f"[FAIL] #{id:02d}: {desc}")
        print(f"       Formula: {formula[:80]}{'...' if len(formula)>80 else ''}")
        print(f"       HTTP: {http_code} | Esperado: {esperado}")
        print(f"       Body: {body[:200]}")
        if detalle:
            print(f"       Detalle: {detalle}")

    results.append({
        "id": id, "desc": desc, "formula": formula,
        "http": http_code, "status": status,
        "body": body, "detalle": detalle
    })

def header(title):
    print(f"\n{'='*50}")
    print(f"  {title}")
    print(f"{'='*50}")

# ============================================================================
# CATEGORÍA 1: VÁLIDOS BÁSICOS
# ============================================================================
header("CATEGORIA 1: VALIDOS BASICOS")

test(1,  "Variable unica",                           "A",                200, "resultado")
test(2,  "NOT simple",                               "NOT A",            200, "resultado")
test(3,  "AND simple",                               "A AND B",          200, "resultado")
test(4,  "OR simple",                                "A OR B",           200, "resultado")
test(5,  "AND con parentesis",                       "(A AND B)",        200, "resultado")

# ============================================================================
# CATEGORÍA 2: VÁLIDOS COMPLEJOS
# ============================================================================
header("CATEGORIA 2: VALIDOS COMPLEJOS")

test(6,  "AND y OR anidados",                "((A AND B) OR (NOT C))",  200, "resultado")
test(7,  "OR con AND anidado",               "(A OR (B AND C))",        200, "resultado")
test(8,  "NOT sobre AND",                    "(NOT (A AND B))",         200, "resultado")
test(9,  "Triple AND",                       "((A AND B) AND C)",       200, "resultado")
test(10, "Doble negacion",                   "NOT NOT A",               200, "resultado")
test(11, "Parentesis redundantes",           "((A))",                   200, "resultado")
test(12, "Misma variable repetida",          "A AND A",                 200, "resultado")

# ============================================================================
# CATEGORÍA 3: PRECEDENCIA DE OPERADORES
# ============================================================================
header("CATEGORIA 3: PRECEDENCIA DE OPERADORES")

test(13, "NOT antes que AND",                "NOT A AND B",             200, "resultado")
test(14, "AND antes que OR",                 "A AND B OR C",            200, "resultado")
test(15, "NOT sobre AND con parentesis",     "NOT (A AND B)",           200, "resultado")

# ============================================================================
# CATEGORÍA 4: INVÁLIDOS SINTÁCTICOS
# ============================================================================
header("CATEGORIA 4: INVALIDOS SINTACTICOS")

test(16, "Variables seguidas sin operador",  "A B",                     400, "error")
test(17, "AND sin operando derecho",         "A AND",                   400, "error")
test(18, "AND sin operando izquierdo",       "AND A",                   400, "error")
test(19, "Operador repetido AND AND",        "A AND AND B",             400, "error")
test(20, "OR seguido de AND",                "A OR AND B",              400, "error")
test(21, "Parentesis vacio",                 "()",                      400, "error")
test(22, "NOT en medio de binarios",         "A NOT B",                 400, "error")
test(23, "Parentesis desbalanceados",        "(A AND B",                400, "error")
test(24, "Cierre sin apertura",              "A AND B)",                400, "error")

# ============================================================================
# CATEGORÍA 5: EDGE CASES DEL LEXER (encadenamiento)
# ============================================================================
header("CATEGORIA 5: EDGE CASES DEL LEXER")

test(25, "AANDB -> A AND B (silencioso)",     "AANDB",                   200, "resultado")
test(26, "AORB -> A OR B (silencioso)",       "AORB",                    200, "resultado")
test(27, "NOTA -> NOT A (silencioso)",        "NOTA",                    200, "resultado")
test(28, "ANDB -> AND B (falta izquierdo)",   "ANDB",                    400, "error")
test(29, "ORAND -> OR AND (falla)",           "ORAND",                   400, "error")

# ============================================================================
# CATEGORÍA 6: CASOS FRONTERA
# ============================================================================
header("CATEGORIA 6: CASOS FRONTERA")

test(30, "26 variables (A..Z)",              "A AND B AND C AND D AND E AND F AND G AND H AND I AND J AND K AND L AND M AND N AND O AND P AND Q AND R AND S AND T AND U AND V AND W AND X AND Y AND Z", 200, "resultado")
test(31, "Espacios al inicio",               "   A AND B",              200, "resultado")
test(32, "Espacios al final",                "A AND B   ",              200, "resultado")
test(33, "Tabulacion como separador",        "A\tAND\tB",               200, "resultado")
test(34, "Formula vacia",                    "",                        400, "error")

# ============================================================================
# RESUMEN
# ============================================================================
print(f"\n{'='*50}")
print(f"  RESUMEN FINAL")
print(f"  Total: {TOTAL} | PASS: {PASS} | FAIL: {FAIL}")
print(f"{'='*50}")

# Save results
with open("/tmp/test_results.json", "w") as f:
    json.dump({"total": TOTAL, "pass": PASS, "fail": FAIL, "results": results}, f, indent=2)
