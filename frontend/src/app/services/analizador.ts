import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

/** Nodo del arbol sintactico devuelto por el backend. */
export interface NodoArbol {
  label:     string;
  value?:    string;
  children?: NodoArbol[];
}

/** Respuesta completa del endpoint /analizar. */
export interface ResultadoAnalisis {
  formula:   string;
  arbol:     NodoArbol;
  variables: Record<string, boolean>;
  resultado: boolean;
  error?:    string;
}

/** Token individual reconocido por el tokenizador local. */
export interface Token {
  lexema: string;
  tipo:   'AND' | 'OR' | 'NOT' | 'VARIABLE' | 'LPAREN' | 'RPAREN';
}

/** Error de analisis categorizado para mostrar en la UI. */
export interface ErrorAnalisis {
  tipo: 'sintaxis' | 'conexion';
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AnalizadorService {

  private readonly API = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  /**
   * Envia la formula al backend via POST /analizar.
   * HTTP 400 = error de sintaxis, error de red = servidor caido.
   */
  analizar(formula: string): Observable<ResultadoAnalisis> {
    return this.http.post<ResultadoAnalisis>(
      `${this.API}/analizar`,
      { formula }
    ).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => err))
    );
  }

  /**
   * Tokenizador local que replica las reglas del Lexer.jflex.
   * Solo reconoce AND, OR, NOT, parentesis y variables A-Z,
   * ignorando espacios y tabs.
   */
  tokenizar(formula: string): Token[] {
    const tokens: Token[] = [];
    let i = 0;
    while (i < formula.length) {
      if (formula[i] === ' ' || formula[i] === '\t') { i++; continue; }
      if (formula.startsWith('AND', i)) { tokens.push({ lexema: 'AND', tipo: 'AND' }); i += 3; continue; }
      if (formula.startsWith('OR',  i)) { tokens.push({ lexema: 'OR',  tipo: 'OR'  }); i += 2; continue; }
      if (formula.startsWith('NOT', i)) { tokens.push({ lexema: 'NOT', tipo: 'NOT' }); i += 3; continue; }
      if (formula[i] === '(') { tokens.push({ lexema: '(', tipo: 'LPAREN' }); i++; continue; }
      if (formula[i] === ')') { tokens.push({ lexema: ')', tipo: 'RPAREN' }); i++; continue; }
      if (formula[i] >= 'A' && formula[i] <= 'Z') {
        tokens.push({ lexema: formula[i], tipo: 'VARIABLE' }); i++; continue;
      }
      i++;
    }
    return tokens;
  }
}
