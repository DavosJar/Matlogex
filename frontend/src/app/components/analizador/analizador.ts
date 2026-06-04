import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  tabActiva: 'tokens' | 'arbol' = 'tokens';
  error:     string | null = null;
  cargando  = false;

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
    this.cargando  = true;
    this.resultado = null;
    this.error     = null;
    this.tokens    = [];
    this.cdr.detectChanges();

    this.tokens = this.analizadorService.tokenizar(this.formula);

    this.analizadorService.analizar(this.formula).subscribe({
      next: (res) => {
        this.resultado = res;
        this.cargando  = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error    = 'No se pudo conectar con el servidor Java. ¿Está corriendo ./server.sh?';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarEjemplo(ejemplo: string): void {
    this.formula = ejemplo;
    this.analizar();
  }

  variablesEntries(variables: Record<string, boolean>): { key: string; value: boolean }[] {
    return Object.entries(variables).map(([key, value]) => ({ key, value }));
  }

  /** Retorna la clase CSS del badge según el tipo de token */
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
