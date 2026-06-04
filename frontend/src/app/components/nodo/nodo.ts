import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NodoArbol } from '../../services/analizador';

@Component({
  selector: 'app-nodo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nodo.html',
  styleUrl: './nodo.scss'
})
export class NodoComponent {

  /** Nodo del árbol sintáctico a renderizar */
  @Input() nodo!: NodoArbol;

  /**
   * Retorna la clase CSS según el tipo de nodo.
   * binary → operador AND/OR
   * unary  → operador NOT
   * variable → hoja del árbol
   */
  get claseNodo(): string {
    if (!this.nodo) return '';
    if (this.nodo.type === 'variable') return 'nodo-variable';
    if (this.nodo.type === 'unary')    return 'nodo-not';
    if (this.nodo.operator === 'AND')  return 'nodo-and';
    if (this.nodo.operator === 'OR')   return 'nodo-or';
    return 'nodo-default';
  }

  /**
   * Retorna la etiqueta visible del nodo.
   */
  get etiqueta(): string {
    if (!this.nodo) return '';
    if (this.nodo.type === 'variable') return this.nodo.name ?? '';
    return this.nodo.operator ?? '';
  }

  /**
   * Indica si el nodo tiene hijos para renderizar recursivamente.
   */
  get tieneHijos(): boolean {
    return this.nodo.type === 'binary' || this.nodo.type === 'unary';
  }
}
