package grammar;

import java_cup.runtime.*;

%%

%public
%class Lexer
%type Symbol
%cup
%char
%line
%column
%unicode

%{
    /**
     * Construye y retorna un Symbol con el tipo de token,
     * la posición inicial y final en el texto fuente.
     *
     * @param tokenType Tipo de token definido en sym.java (generado por CUP)
     * @return Symbol con tipo, posición y lexema
     */
    private Symbol token(int tokenType) {
        return new Symbol(tokenType, (int)yychar, (int)(yychar + yytext().length()), yytext());
    }
%}

/* ── Definiciones de expresiones regulares ── */
VARIABLE   = [A-Z]
AND        = "AND"
OR         = "OR"
NOT        = "NOT"
LPAREN     = "("
RPAREN     = ")"
WS         = [ \t\r\n]+

%%

/* ── Reglas léxicas (palabras clave antes que VARIABLE) ── */
{AND}       { return token(sym.AND);      }
{OR}        { return token(sym.OR);       }
{NOT}       { return token(sym.NOT);      }
{LPAREN}    { return token(sym.LPAREN);   }
{RPAREN}    { return token(sym.RPAREN);   }
{VARIABLE}  { return token(sym.VARIABLE); }
{WS}        { /* ignorar espacios */      }

<<EOF>>     { return token(sym.EOF);      }
.           { return token(sym.error);    }
