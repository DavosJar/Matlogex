import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface NodoArbol {
  type:     'binary' | 'unary' | 'variable';
  operator?: string;
  name?:     string;
  left?:     NodoArbol;
  right?:    NodoArbol;
  operand?:  NodoArbol;
}

export interface ResultadoAnalisis {
  formula:   string;
  arbol:     NodoArbol;
  variables: Record<string, boolean>;
  resultado: boolean;
  error?:    string;
}

export interface Token {
  lexema: string;
  tipo:   'AND' | 'OR' | 'NOT' | 'VARIABLE' | 'LPAREN' | 'RPAREN';
}

export interface ErrorAnalisis {
  tipo: 'sintaxis' | 'conexion';
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AnalizadorService {

  private readonly API = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  /**
   * Envía la fórmula al backend y maneja los errores correctamente.
   * HTTP 400 = fórmula inválida (error de sintaxis)
   * Error de red = servidor no disponible
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
   * Tokeniza la fórmula localmente para mostrar la tabla de tokens.
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
