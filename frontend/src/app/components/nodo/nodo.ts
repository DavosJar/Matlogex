import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NodoArbol } from '../../services/analizador';

/**
 * Componente recursivo para renderizar el arbol sintactico.
 *
 * Recibe un NodoArbol por @Input y genera HTML identado para
 * cada nivel. La clase CSS se determina segun el label:
 *   VARIABLE -> nodo-variable
 *   AND      -> nodo-and
 *   OR       -> nodo-or
 *   NOT      -> nodo-not
 *   LPAREN/RPAREN -> nodo-paren
 *   cualquier otro (Exp, Term, Factor) -> nodo-default
 */
@Component({
  selector: 'app-nodo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nodo.html',
  styleUrl: './nodo.scss'
})
export class NodoComponent {

  @Input() nodo!: NodoArbol;

  get claseNodo(): string {
    if (!this.nodo) return '';
    const label = this.nodo.label;
    if (label === 'VARIABLE') return 'nodo-variable';
    if (label === 'AND')      return 'nodo-and';
    if (label === 'OR')       return 'nodo-or';
    if (label === 'NOT')      return 'nodo-not';
    if (label === 'LPAREN' || label === 'RPAREN') return 'nodo-paren';
    return 'nodo-default';
  }

  /** Muestra el value del terminal, o el label si es no terminal. */
  get etiqueta(): string {
    if (!this.nodo) return '';
    return this.nodo.value ?? this.nodo.label;
  }

  get tieneHijos(): boolean {
    return !!this.nodo.children && this.nodo.children.length > 0;
  }
}
