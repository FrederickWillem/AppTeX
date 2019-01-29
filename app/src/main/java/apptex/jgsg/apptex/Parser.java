package apptex.jgsg.apptex;

import java.util.ArrayList;
import java.util.function.ToDoubleBiFunction;

public class Parser {

    public static String doubleEscapeTeX(String s) {
        String t = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\'') t += '\\';
            if (s.charAt(i) != '\n') t += s.charAt(i);
            if (s.charAt(i) == '\\') t += "\\";
        }
        return t;
    }

    int pos = -1, ch = -1;
    String expr;

    public Parser(String s) {
        expr = s;
    }

    public String normalToTeX() {
        nextChar();
        return processExpression();
    }

    // ################ Parse expression into a tree ################

    /**
     * Parses the following expression as a vector or matrix object. Vectors have
     * these forms:
     * <ul>
     * <li>[1,2,3] -> vector with elements 1,2,3</li>
     * <li>[[1,2,3],[4,5,6]] vector of two vectors with elements 1,2,3 and 4,5,6
     * respectively</li>
     * <li>[1,2,3;4,5,6] 2D matrix with elements 1,2,3 in row 1 and elements 4,5,6
     * in row 2</li>
     * </ul>
     */
    protected String getVector() {
        int count = 1;
        int position = pos;
        while (count != 0 && ch > 0) {
            if (ch == '[') count++;
            if (ch == ']') count--;
            nextChar();
        }
        return new VectorParser("[" + expr.substring(position, pos)).normalToTeX();
    }

    /**
     * Sets the character to the next character in the expression string (or -1 if
     * there is no next character) and increases <code>position</code> by 1.
     */
    protected int nextChar() {
        if (++pos < expr.length())
            ch = expr.charAt(pos);
        else
            ch = -1;
        return ch;
    }

    /**
     * Compares the given character with the current character. The current
     * character is consumed if it's the same.
     *
     * @param c The character to compare the current character to.
     * @return <code>true</code> if the characters match, otherwise
     * <code>false</code>.
     */
    protected boolean consume(int c) {
        while (ch == ' ')
            nextChar();
        if (ch == c) {
            nextChar(); // Consume character by moving the position pointer on to the next one.
            return true;
        } else
            return false;
    }

    private String processExpression() {
        String s = processTerm();
        while (true) {
            if (consume('+'))
                s += "+" + processTerm();
            else if (consume('-'))
                s += "-" + processTerm();
            else
                return s;
        }
    }

    // term = factor, or term '*' factor, or term '/' factor.
    private String processTerm() {
        String s = processFactor();
        while (true) {
            if (consume('*'))
                s += "\\cdot" + processFactor();
            else if (consume('/'))
                s = "\\frac{" + s + "}{" + processFactor() + "}";
            else if (pos < expr.length() && Character.isLetter(ch))
                s += processFactor();
            else
                return s;
        }
    }

    // factor = '+' factor (positive) or '-' factor (negative), or '('
    // expression ')', or number (double), or factor^factor, or function(factor)
    // (e.g. sine, cosine, etc.)
    private String processFactor() {
        if (consume('+'))
            return processFactor();
        if (consume('-'))
            return "-" + processFactor();

        String s = "";
        int p = pos;

        if (consume('(')) { // new expression within the parentheses
            String s2 = processExpression();
            if(s2.contains("\\frac{") || s2.contains("pmatrix"))
                s = "\\left(" + processExpression() +"\\right)";
            else
                s = "(" + processExpression() +")";
            consume(')');
        } else if (consume('[')) {
            s = getVector();
        } else if (consume('|')) {
            String s2 = processExpression();
            if(s2.contains("\\frac{") || s2.contains("pmatrix"))
                s = "\\left|" + processExpression() +"\\right|";
            else
                s = "|" + processExpression() +"|";
            consume('|');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {// number
            while ((ch >= '0' && ch <= '9') || ch == '.')
                nextChar();
            s = expr.substring(p, pos);
            if (consume('E'))
                s += "e" + processFactor();
            // Letter, which is part of a variable or function
        } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') {
            while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch >= '0' && ch <= '9')
                nextChar();
            String letters = expr.substring(p, pos);
            switch (letters) {
                case "sin":
                case "cos":
                case "tan":
                case "asin":
                case "acos":
                case "atan":
                    s = "\\"  + letters + "{" + processFactor() + "}";
                    break;
                case "sqrt":
                    s = "\\sqrt{" + letters + "}";
                    break;
                case "int":
                    //TODO
                    break;
                case "prod":
                case "sum":
                    //TODO
                    break;
                default:
                    s = letters;
                    break;
            }
        } else {
            s += (char) nextChar();
        }

        if (consume('^'))
            s += "^{" + processFactor() + "}";
        if (consume('%'))
            s = "\\Mod{" + processFactor() + "}";
        return s;
    }

    private class VectorParser extends Parser {

        public VectorParser(String s) {
            super(s);
            expr = expr.substring(1, expr.length() - 1);
            expr = expr.replace(" ", "");
        }

        @Override
        public String normalToTeX () {
            nextChar();
            String v = "\\begin{pmatrix}";
            int p = 0;
            while(ch > 0) {
                if (consume(',')) {
                    v += (new Parser(expr.substring(p, pos))).normalToTeX() + "&";
                    p = pos + 1;
                } else if (consume(';')) {
                    v += (new Parser(expr.substring(p, pos))).normalToTeX() + "\\\\";
                    p = pos+1;
                }
                nextChar();
            }
            return v + "\\end{pmatrix}";
        }
    }
}