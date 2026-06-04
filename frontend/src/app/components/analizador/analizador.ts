import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnalizadorService, ResultadoAnalisis, Token } from '../../services/analizador';
import { NodoComponent } from '../nodo/nodo';

@Component({
  selector: 'app-analizador',
  standalone: true,
  imports: [CommonModule, FormsModule, NodoComponent],
  templateUrl: './analizador.html',
  styleUrl: './analizador.scss'
})
export class AnalizadorComponent {

  formula   = '((A AND B) OR (NOT C))';
  resultado: ResultadoAnalisis | null = null;
  tokens:    Token[] = [];
  error:     string | null = null;
  errorTipo: 'sintaxis' | 'conexion' | null = null;
  cargando  = false;
  tabActiva: 'tokens' | 'arbol' = 'tokens';

  ejemplos = [
    '((A AND B) OR (NOT C))',
    '(A OR (B AND C))',
    '(NOT (A AND B))',
    '((A AND B) AND (C OR D))',
    '(NOT A)',
  ];

  constructor(
    private analizadorService: AnalizadorService,
    private cdr: ChangeDetectorRef
  ) {}

  analizar(): void {
    if (!this.formula.trim()) return;

    const validacion = this.validarFormula(this.formula);
    if (validacion) {
      this.error     = validacion;
      this.errorTipo = 'sintaxis';
      this.resultado = null;
      this.tokens    = [];
      this.cdr.detectChanges();
      return;
    }

    this.cargando  = true;
    this.resultado = null;
    this.error     = null;
    this.errorTipo = null;
    this.tokens    = [];
    this.cdr.detectChanges();

    this.tokens = this.analizadorService.tokenizar(this.formula);

    this.analizadorService.analizar(this.formula).subscribe({
      next: (res) => {
        this.resultado = res;
        this.cargando  = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        // HTTP 400 = el servidor procesó la fórmula pero tiene error sintáctico
        if (err.status === 400) {
          this.errorTipo = 'sintaxis';
          this.error = 'La fórmula tiene un error de sintaxis. Verifica que cada operador tenga sus operandos y que los paréntesis sean correctos. Ejemplo válido: ((A AND B) OR (NOT C))';
        } else if (err.status === 0) {
          // Error de red: servidor no disponible
          this.errorTipo = 'conexion';
          this.error = 'El servidor Java no está disponible. Ejecuta ./server.sh en la carpeta backend/ y vuelve a intentarlo.';
        } else {
          this.errorTipo = 'sintaxis';
          this.error = 'Ocurrió un error inesperado. Revisa que la fórmula esté bien escrita.';
        }
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private validarFormula(formula: string): string | null {
    let balance = 0;
    for (const c of formula) {
      if (c === '(') balance++;
      if (c === ')') balance--;
      if (balance < 0) return 'Paréntesis desbalanceados: hay un ) sin su ( correspondiente.';
    }
    if (balance > 0) return 'Paréntesis desbalanceados: falta cerrar ' + balance + ' paréntesis.';

    const invalidos = formula.match(/[^A-Z\s()]/g);
    if (invalidos) {
      const unicos = [...new Set(invalidos)].join(', ');
      return `Caracteres no permitidos: "${unicos}". Solo se aceptan letras mayúsculas A–Z, paréntesis y los operadores AND, OR, NOT.`;
    }
    return null;
  }

  cargarEjemplo(ejemplo: string): void {
    this.formula = ejemplo;
    this.analizar();
  }

  variablesEntries(variables: Record<string, boolean>): { key: string; value: boolean }[] {
    return Object.entries(variables).map(([key, value]) => ({ key, value }));
  }

  categoriaToken(tipo: string): string {
    const mapa: Record<string, string> = {
      AND: 'Operador binario', OR: 'Operador binario', NOT: 'Operador unario',
      VARIABLE: 'Operando', LPAREN: 'Agrupación', RPAREN: 'Agrupación'
    };
    return mapa[tipo] ?? '';
  }

  claseBadge(tipo: string): string {
    const mapa: Record<string, string> = {
      AND: 'token-and', OR: 'token-or', NOT: 'token-not',
      VARIABLE: 'token-var', LPAREN: 'token-paren', RPAREN: 'token-paren'
    };
    return mapa[tipo] ?? '';
  }
}
