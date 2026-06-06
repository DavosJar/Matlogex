import { Component, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnalizadorService, ResultadoAnalisis, Token } from '../../services/analizador';
import { NodoComponent } from '../nodo/nodo';

/**
 * Componente principal de la pagina de analisis.
 *
 * Orquesta la interaccion: input de formula -> validacion local ->
 * tokenizacion -> envio al backend -> muestra tokens/arbol/resultado.
 *
 * La tabla de tokens se muestra siempre que haya tokens (incluso en error).
 * La pestana "Arbol Sintactico" solo aparece si el backend retorno 200.
 */
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
  zoomLevel = 1;

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

    // Tokeniza ANTES de enviar para que la tabla aparezca incluso si hay error
    this.tokens = this.analizadorService.tokenizar(this.formula);

    this.analizadorService.analizar(this.formula).subscribe({
      next: (res) => {
        this.resultado = res;
        this.cargando  = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 400) {
          this.errorTipo = 'sintaxis';
          this.error = 'La formula tiene un error de sintaxis. Verifica que cada operador tenga sus operandos y que los parentesis sean correctos. Ejemplo valido: ((A AND B) OR (NOT C))';
        } else if (err.status === 0) {
          this.errorTipo = 'conexion';
          this.error = 'El servidor Java no esta disponible. Ejecuta ./server.sh en la carpeta backend/ y vuelve a intentarlo.';
        } else {
          this.errorTipo = 'sintaxis';
          this.error = 'Ocurrio un error inesperado. Revisa que la formula este bien escrita.';
        }
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Validacion local previa: balance de parentesis y caracteres permitidos.
   * No reemplaza el analisis del backend, solo filtra errores obvios.
   */
  private validarFormula(formula: string): string | null {
    let balance = 0;
    for (const c of formula) {
      if (c === '(') balance++;
      if (c === ')') balance--;
      if (balance < 0) return 'Parentesis desbalanceados: hay un ) sin su ( correspondiente.';
    }
    if (balance > 0) return 'Parentesis desbalanceados: falta cerrar ' + balance + ' parentesis.';

    const invalidos = formula.match(/[^A-Z\s()]/g);
    if (invalidos) {
      const unicos = [...new Set(invalidos)].join(', ');
      return `Caracteres no permitidos: "${unicos}". Solo se aceptan letras mayusculas A-Z, parentesis y los operadores AND, OR, NOT.`;
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
      VARIABLE: 'Operando', LPAREN: 'Agrupacion', RPAREN: 'Agrupacion'
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

  @HostListener('wheel', ['$event'])
  onWheel(event: WheelEvent): void {
    if (event.ctrlKey) {
      event.preventDefault();
      const delta = event.deltaY > 0 ? -0.1 : 0.1;
      this.zoomLevel = Math.max(0.25, Math.min(3, +(this.zoomLevel + delta).toFixed(2)));
    }
  }
}
