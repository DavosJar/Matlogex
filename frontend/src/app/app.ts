import { Component } from '@angular/core';
import { AnalizadorComponent } from './components/analizador/analizador';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AnalizadorComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
