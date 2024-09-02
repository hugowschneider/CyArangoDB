package com.github.hugowschneider.cyarangodb.internal.flex;

import java.io.*;
import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.*;


%%

%public
%class AqlTokenMaker
%extends AbstractJFlexTokenMaker
%unicode
%type org.fife.ui.rsyntaxtextarea.Token

%{
   private Segment s;
   private int offsetShift;
   /** the textposition at the last state to be included in yytext */
   private int zzPushbackPos;

   public AqlTokenMaker() {
   }

   public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
      resetTokenList();
      this.offsetShift = -text.offset + startOffset;

      // Start off in the proper state.
      int state = Token.NULL;
      switch (initialTokenType) {
         case Token.COMMENT_MULTILINE:
            state = COMMENT;
            break;
         default:
            state = Token.NULL;
      }

      s = text;
      try {
         yyreset(zzReader);
         yybegin(state);
         return yylex();
      } catch (IOException ioe) {
         ioe.printStackTrace();
         return new TokenImpl();
      }
   }

   private void addToken(int tokenType) {
      addToken(zzStartRead, zzMarkedPos - 1, tokenType);
   }

   private void addToken(int start, int end, int tokenType) {
      int so = start + offsetShift;
      addToken(zzBuffer, start, end, tokenType, so);
   }

   public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
      super.addToken(array, start, end, tokenType, startOffset);
      zzStartRead = zzMarkedPos;
   }

      /**   
    * Refills the input buffer.   
    *   
    * @return      <code>true</code> if EOF was reached, otherwise   
    *              <code>false</code>.   
    */   
   private boolean zzRefill() {   
      return zzCurrentPos>=s.offset+s.count;   
   }   
   
   /**   
    * Resets the scanner to read from a new input stream.   
    * Does not close the old reader.   
    *   
    * All internal variables are reset, the old input stream    
    * <b>cannot</b> be reused (internal buffer is discarded and lost).   
    * Lexical state is set to <tt>YY_INITIAL</tt>.   
    *   
    * @param reader   the new input stream    
    */   
   public final void yyreset(Reader reader) {   
      // 's' has been updated.   
      zzBuffer = s.array;   
      /*   
       * We replaced the line below with the two below it because zzRefill   
       * no longer "refills" the buffer (since the way we do it, it's always   
       * "full" the first time through, since it points to the segment's   
       * array).  So, we assign zzEndRead here.   
       */   
      //zzStartRead = zzEndRead = s.offset;   
      zzStartRead = s.offset;   
      zzEndRead = zzStartRead + s.count - 1;   
      zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;   
      zzLexicalState = YYINITIAL;   
      zzReader = reader;   
      zzAtBOL  = true;   
      zzAtEOF  = false;   
   }   
%}

%state STRING, COMMENT, DOUBLE_QUOTED_STRING, SINGLE_QUOTED_STRING

%%

<YYINITIAL> {
   /* Keywords */
   "FOR" | "RETURN" | "FILTER" | "LIMIT" | "SORT" | "COLLECT" | "INSERT" |
   "REMOVE" | "UPSERT" | "REPLACE" | "LET" | "DISTINCT" | "IN" | "PRUNE" | "INTO" |
   "INBOUND" | "OUTBOUND" | "ANY" | "ALL" | "NONE" | "AND" | "OR" | "NOT" |
   "WITH" | "COUNT" | "AGGREGATE" | "KEEP" { addToken(Token.RESERVED_WORD); }

   /* Functions */
   "ABS" | "ACOS" | "ASIN" | "ATAN" | "ATAN2" | "AVERAGE" | "AVG" | "CEIL" |
   "COS" | "DEGREES" | "EXP" | "EXP2" | "FLOOR" | "LOG" | "LOG2" | "LOG10" |
   "MAX" | "MIN" | "PI" | "POW" | "RADIANS" | "RAND" | "ROUND" | "SIN" |
   "SQRT" | "SUM" | "TAN" | "APPEND" | "COUNT" | "FIRST" | "FLATTEN" |
   "INTERSECTION" | "LAST" | "LENGTH" | "MINUS" | "OUTERSECTION" | "POP" |
   "POSITION" | "PUSH" | "REMOVE_NTH" | "REMOVE_VALUE" | "REMOVE_VALUES" |
   "REVERSE" | "SHIFT" | "SLICE" | "UNION" | "UNION_DISTINCT" | "UNSHIFT" |
   "DATE_ADD" | "DATE_COMPARE" | "DATE_DAY" | "DATE_DAYS_IN_MONTH" |
   "DATE_DIFF" | "DATE_FORMAT" | "DATE_HOUR" | "DATE_ISO8601" |
   "DATE_LEAP_YEAR" | "DATE_MILLISECOND" | "DATE_MINUTE" | "DATE_MONTH" |
   "DATE_NOW" | "DATE_QUARTER" | "DATE_SECOND" | "DATE_SUBTRACT" |
   "DATE_TIMESTAMP" | "DATE_YEAR" | "CONCAT" | "CONCAT_SEPARATOR" | "CONTAINS" |
   "COUNT_DISTINCT" | "ENCODE_URI_COMPONENT" | "FIND_FIRST" | "FIND_LAST" |
   "JSON_STRINGIFY" | "LEFT" | "LENGTH" | "LIKE" | "LOWER" | "LTRIM" | "MD5" |
   "RANDOM_TOKEN" | "EX_MATCHES" | "REGEX_REPLACE" | "REGEX_SPLIT" | "REGEX_TEST" |
   "REPLACE" | "REVERSE" | "RIGHT" | "RTRIM" | "SHA1" | "SHA512" |
   "SPLIT" | "SUBSTITUTE" | "TRIM" | "UPPER" | "UUID" | "DOCUMENT" | "MATCHES" |
   "MERGE" | "MERGE_RECURSIVE" | "PARSE_IDENTIFIER" | "UNSET" | "UNSET_RECURSIVE" |
   "UPDATE" | "KEEP" | "ZIP" { addToken(Token.FUNCTION); }

   /* Variable Definitions */
   "LET" { addToken(Token.RESERVED_WORD); }
   "COLLECT" { addToken(Token.RESERVED_WORD); }
   "FOR" { addToken(Token.RESERVED_WORD); }

   /* Identifiers */
   [^\W\d]([\w-]*[\w])? { addToken(Token.IDENTIFIER); }

   /* Numbers */
   [0-9]+(\.[0-9]*)?([eE][+-]?[0-9]+)? { addToken(Token.LITERAL_NUMBER_FLOAT); }

   /* Strings and Comments */
   "\"" { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); yybegin(DOUBLE_QUOTED_STRING); }
   "'" { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); yybegin(SINGLE_QUOTED_STRING); }

   /* Comments */
   "//" .* { addToken(Token.COMMENT_EOL); }
   "/*" { yybegin(COMMENT); }

   /* Separators and operators */
   [.,;(){}[]] { addToken(Token.SEPARATOR); }

   /* Whitespace */
   [ \t\n\r]+ { addToken(Token.WHITESPACE); }
}

<DOUBLE_QUOTED_STRING> {
   "\\". { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); } // Handle escapes
   "\"" { yybegin(YYINITIAL); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
   [^\\\"\n]+ { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); } // Consume and display characters
   \n { addToken(Token.ERROR_STRING_DOUBLE); yybegin(YYINITIAL); } // Handle unterminated string
   <<EOF>> { addToken(Token.ERROR_STRING_DOUBLE); yybegin(YYINITIAL); } // Handle EOF
}

<SINGLE_QUOTED_STRING> {
   "\\". { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); } // Handle escapes
   "'" { yybegin(YYINITIAL); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
   [^\\'\n]+ { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); } // Consume and display characters
   \n { addToken(Token.ERROR_STRING_DOUBLE); yybegin(YYINITIAL); } // Handle unterminated string
   <<EOF>> { addToken(Token.ERROR_STRING_DOUBLE); yybegin(YYINITIAL); } // Handle EOF
}

<COMMENT> {
   "*/" { yybegin(YYINITIAL); addToken(zzStartRead, zzMarkedPos - 1, Token.COMMENT_MULTILINE); }
   . { } // consume any character
   \n { } // consume newline
}

/* EOF Handling */
<<EOF>> { addNullToken(); return firstToken; }