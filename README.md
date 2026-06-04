# MatLogEx — Guía de Instalación y Ejecución

## Integrantes

- César Daniel Ramos Merchán
- Alexis Jara
- Anthony Gutierrez
- Ivan Fernandez
- César López

**Universidad Nacional de Loja — FEIRNNR**  
**Carrera de Ingeniería en Computación**  
**Teoría de Autómatas y Computabilidad Avanzada**

---

## Requisitos previos

Antes de clonar el proyecto verifica que tienes instalado:

| Herramienta | Versión mínima | Verificar con |
|-------------|----------------|---------------|
| Java JDK | 11 | `java -version` |
| Node.js | 18 | `node -v` |
| Angular CLI | 17+ | `ng version` |
| Git | cualquiera | `git --version` |
| wget | cualquiera | `wget --version` |

Instalar Angular CLI si no lo tienes:
```bash
npm install -g @angular/cli
```

---

## Estructura del proyecto

```
Matlogex/
├── backend/
│   ├── src/
│   │   └── grammar/
│   │       ├── Lexer.jflex        # Analizador léxico (JFlex)
│   │       ├── parser.cup         # Analizador sintáctico (CUP)
│   │       └── node/              # Nodos del árbol sintáctico
│   ├── Main.java                  # Ejecución por consola
│   ├── Server.java                # Servidor REST HTTP
│   ├── build.sh                   # Compila todo el backend
│   └── server.sh                  # Arranca el servidor REST
├── frontend/
│   └── src/
│       └── app/                   # Aplicación Angular
├── README.md                      # Esta guía
└── README_TECNICO.md              # Documentación técnica
```

> **Nota:** Las carpetas `lib/` y `classes/` no están en el repositorio.
> Se generan localmente siguiendo los pasos de esta guía.

---

## Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/DavosJar/Matlogex.git
cd Matlogex
git checkout develop
```

---

## Paso 2 — Descargar dependencias Java

Las dependencias no están en el repo. Descárgalas así:

```bash
mkdir -p lib && cd lib

# JFlex
wget https://github.com/jflex-de/jflex/releases/download/v1.9.1/jflex-1.9.1.tar.gz -O jflex.tar.gz
tar -xzf jflex.tar.gz
cp jflex-1.9.1/lib/jflex-full-1.9.1.jar jflex.jar
rm -rf jflex.tar.gz jflex-1.9.1

# CUP
wget "https://repo1.maven.org/maven2/com/github/vbmacher/java-cup/11b-20160615/java-cup-11b-20160615.jar" -O java_cup.jar
wget "https://repo1.maven.org/maven2/com/github/vbmacher/java-cup-runtime/11b-20160615/java-cup-runtime-11b-20160615.jar" -O java_cup_runtime.jar

cd ..
```

Verifica que quedaron los tres archivos:
```bash
ls lib/
# jflex.jar  java_cup.jar  java_cup_runtime.jar
```

---

## Paso 3 — Compilar el backend

```bash
cd backend
chmod +x build.sh server.sh
./build.sh "((A AND B) OR (NOT C))"
```

Salida esperada:
```
[ 1/4 ] Ejecutando JFlex...
[ 2/4 ] Ejecutando CUP...
  0 errors and 0 warnings
[ 3/4 ] Compilando Java...
[ 4/4 ] Ejecutando...

Formula: ((A AND B) OR (NOT C))
─────────────────────────────
Arbol sintáctico:
  OR
    AND
      A
      B
    NOT
      C
─────────────────────────────
Valores asignados:
  A = true
  B = false
  C = false
─────────────────────────────
Resultado: true
```

---

## Paso 4 — Levantar el servidor REST

Abre una terminal y déjala corriendo durante toda la sesión:

```bash
cd backend
./server.sh
```

Verás:
```
Iniciando servidor en http://localhost:8080
Servidor iniciado en http://localhost:8080
```

Para verificar que funciona, en otra terminal ejecuta:
```bash
curl -X POST http://localhost:8080/analizar \
  -H "Content-Type: application/json" \
  -d '{"formula":"((A AND B) OR (NOT C))"}'
```

---

## Paso 5 — Levantar el frontend Angular

Abre una segunda terminal:

```bash
cd frontend
npm install
ng serve
```

Abre el navegador en: **http://localhost:4200**

> **Importante:** El servidor REST (Paso 4) debe estar corriendo antes
> de usar el frontend.

---

## Uso de la aplicación

1. Escribe una fórmula lógica en el campo de texto
2. Haz clic en **Analizar** o presiona Enter
3. Pestaña **Tokens** → tabla con el análisis léxico de JFlex
4. Pestaña **Árbol Sintáctico** → árbol jerárquico generado por CUP
5. Panel **Valores Asignados** → variables con valores booleanos aleatorios
6. Panel **Resultado** → evaluación final de la fórmula

---

## Fórmulas de ejemplo válidas

```
((A AND B) OR (NOT C))
(A OR (B AND C))
(NOT (A AND B))
((A AND B) AND (C OR D))
(NOT A)
(A AND B)
(NOT (NOT A))
```

---

## Solución de problemas

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| `ERROR en JFlex` | Falta `lib/jflex.jar` | Repite el Paso 2 |
| `ERROR en CUP` | Falta `lib/java_cup.jar` | Repite el Paso 2 |
| `ERROR en javac` | JDK menor a 11 | Instala JDK 11+ |
| Frontend no conecta | Servidor no está corriendo | Ejecuta `./server.sh` en `backend/` |
| Puerto 8080 ocupado | Otro proceso usa ese puerto | Ejecuta `sudo fuser -k 8080/tcp` |
| Puerto 4200 ocupado | Otra instancia de Angular | Usa `ng serve --port 4201` |
| `ng: command not found` | Angular CLI no instalado | Ejecuta `npm install -g @angular/cli` |

---

## Flujo de ramas

```
main     → versión estable final (no tocar hasta terminar)
develop  → versión de desarrollo activa (trabajar aquí)
```

Para subir cambios siempre trabajar en `develop`:
```bash
git checkout develop
git add .
git commit -m "descripción del cambio"
git push origin develop
```
