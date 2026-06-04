import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

/** Representa un token léxico extraído de la fórmula */
export interface Token {
  lexema: string;
  tipo:   'AND' | 'OR' | 'NOT' | 'VARIABLE' | 'LPAREN' | 'RPAREN';
}

@Injectable({ providedIn: 'root' })
export class AnalizadorService {

  private readonly API = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  /**
   * Envía una fórmula lógica al backend para su análisis léxico-sintáctico.
   */
  analizar(formula: string): Observable<ResultadoAnalisis> {
    return this.http.post<ResultadoAnalisis>(`${this.API}/analizar`, { formula });
  }

  /**
   * Tokeniza la fórmula en el frontend para mostrar la tabla de tokens.
   * Sigue el mismo orden de precedencia que JFlex: palabras clave antes que variables.
   *
   * @param formula Cadena con la fórmula lógica
   * @returns Array de tokens identificados
   */
  tokenizar(formula: string): Token[] {
    const tokens: Token[] = [];
    let i = 0;
    while (i < formula.length) {
      if (formula[i] === ' ' || formula[i] === '\t') { i++; continue; }
      if (formula.startsWith('AND', i)) { tokens.push({ lexema: 'AND', tipo: 'AND' }); i += 3; continue; }
      if (formula.startsWith('OR',  i)) { tokens.push({ lexema: 'OR',  tipo: 'OR'  }); i += 2; continue; }
      if (formula.startsWith('NOT', i)) { tokens.push({ lexema: 'NOT', tipo: 'NOT' }); i += 3; continue; }
      if (formula[i] === '(') { tokens.push({ lexema: '(', tipo: 'LPAREN'   }); i++; continue; }
      if (formula[i] === ')') { tokens.push({ lexema: ')', tipo: 'RPAREN'   }); i++; continue; }
      if (formula[i] >= 'A' && formula[i] <= 'Z') {
        tokens.push({ lexema: formula[i], tipo: 'VARIABLE' }); i++; continue;
      }
      i++;
    }
    return tokens;
  }
}
