package apptex.jgsg.apptex;

import java.util.ArrayList;

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
    boolean inHashTag = false, inColon = false, inBar = false;
    private static final String[] symbols = {"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu", "xi", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega",
            "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega",
            "varrho", "varepsilon", "vartheta", "varphi", "nabla", "Box", "Re", "Im", "emptyset", "cdots", "equiv", "approx", "infty", "partial", "hbar", "times"};


    public Parser(String s) {
        expr = s;
    }

    public String toLatex() {
        nextChar();
        String s = "";
        while(pos < expr.length())
            s += processExpression();
        return s;
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
        return new VectorParser("[" + expr.substring(position, pos)).toLatex();
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
            else if(ch == '=')
                s += processExpression();
            else
                return s;
        }
    }

    // term = factor, or term '*' factor, or term '/' factor.
    private String processTerm() {
        String s = processFactor();
        while (true) {
            if (consume('*'))
                s += "\\cdot " + processFactor();
            else if (consume('/')) {
                String s2 = processFactor();
                //Remove brackets from s and s2
                if(s.startsWith("(") && s.endsWith(")"))
                    s = s.substring(1, s.length()-1).trim();
                if(s2.startsWith("(") && s2.endsWith(")"))
                    s2 = s2.substring(1, s2.length()-1).trim();

                if(s.startsWith("del") && s2.startsWith("del")) {
                    s = "\\partial " + s.substring(3);
                    s2 = "\\partial " + s2.substring(3);
                } else if(s.charAt(0)=='d' && s2.charAt(0)=='d') {
                    s = "\\mathrm{d}" + s.substring(1);
                    s2 = "\\mathrm{d}" + s2.substring(1);
                }
                s = "\\frac{" + s + "}{" + s2 + "}";
            } else if (pos < expr.length() && Character.isLetter(ch))
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
            //String s2 = processExpression();
            String s2 = new Parser(getEndOfBrackets()).toLatex();
            if (s2.contains("frac{") || s2.contains("pmatrix"))
                s = "\\left(" + s2 + "\\right)";
            else
                s = "(" + s2 + ")";
        } if(consume('{')) {
            s += "{" + new Parser(getEndOfBrackets()).toLatex() + "}";
        } else if (consume('[')) {
            s = getVector();
        } else if (consume('|')) {
            String s2 = new Parser(substringTo('|')).toLatex();
            if (s2.contains("frac{") || s2.contains("pmatrix"))
                s = "\\left|" + s2 + "\\right|";
            else
                s = "|" + s2 + "|";
        } else if(consume('#')) {
            int i = expr.indexOf('#', pos);
            if(i==-1) {
                s = "\\mathrm{" + expr.substring(p + 1, p + 2) + "}";
                nextChar();
            } else {
                s = "\\mathrm{" + expr.substring(p+1, i) + "}";
                pos = i;
                nextChar();
            }
        }else if(consume(':')) {
            int i = expr.indexOf(':', pos);
            if(i==-1) {
                s = "\\mathbf{" + expr.substring(p + 1, p + 2) + "}";
                nextChar();
            } else {
                s = "\\mathbf{" + expr.substring(p+1, i) + "}";
                pos = i;
                nextChar();
            }
        } else if(consume('$')) {
            s = "\\mathbb{"+(char)ch+"}";
            nextChar();
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {// number
            while ((ch >= '0' && ch <= '9') || ch == '.')
                nextChar();
            s = expr.substring(p, pos);
            if (consume('E'))
                s += "e" + processFactor();
            // Letter, which is part of a variable or function
        } else if(consume('=')) {
            if (consume('>'))
                s += "\\geq ";
            else if (consume('<'))
                s += "\\leq ";
            else if (consume('='))
                s += "\\equiv ";
            else {
                String s2 = (pos+2 > expr.length() ? "" : expr.substring(pos, pos + 2));
                if (s2.equals("~=")) {
                    s += "\\approx ";
                    pos += 1;
                    nextChar();
                } else if (s2.equals("/=")) {
                    s += "\\neq ";
                    pos += 1;
                    nextChar();
                } else
                    s += "=";
            }
            s += processExpression();
        } else if(consume('>')) {
            if (consume('>'))
                s += "\\gg ";
            else
                s += ">";
            s += processExpression();
        } else if(consume('<')) {
            if(consume('<'))
                s += "\\ll ";
            else
                s += "<";
            s += processExpression();
        } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch >= '0' && ch <= '9' || ch=='\'')
                nextChar();
            String letters = expr.substring(p, pos);
            switch (letters) {
                case "cross":
                    s = "\\times ";
                    break;
                case "dot":
                    s += "\\cdot ";
                    break;
                case "div":
                case "grad":
                case "curl":
                    s = "\\mathrm{" + letters + "}";
                    if(ch!='(')
                        s += "\\,";
                    break;
                case "sin":
                case "cos":
                case "tan":
                case "asin":
                case "acos":
                case "atan":
                    s = "\\"  + letters + "{" + processFactor() + "}";
                    break;
                case "sqrt":
                    consume('(');
                    String s2 = new Parser(getEndOfBrackets()).toLatex();
                    s = "\\sqrt{" + s2 + "}";
                    break;
                case "int":
                    if(!consume('('))
                        s="\\int ";
                    else {
                        String args[] = getArguments();
                        s = "\\int ";
                        if(args.length >= 3)
                            s += "\\limits_{" + new Parser(args[0]).toLatex() + "}";
                        if(args.length >= 4)
                            s += "^{" + new Parser(args[1]).toLatex() + "}";
                        if(args.length>=2)
                            s += new Parser(args[args.length-2]).toLatex() + "\\," + (new Parser(args[args.length-1]).toLatex()).replaceFirst("d", "\\\\mathrm{d}");
                        else
                            s += new Parser(args[0]).toLatex();
                        consume(')');
                    }
                    break;
                case "prod":
                case "sum":
                    if(!consume('('))
                        s="\\" + letters + " ";
                    else {
                        String args[] = getArguments();
                        s = "\\" + letters + " ";
                        if(args.length >= 2)
                            s += "\\limits_{" + new Parser(args[0]).toLatex() + "}";
                        if(args.length >= 3)
                            s += "^{" + new Parser(args[1]).toLatex() + "}";
                        s += new Parser(args[args.length-1]).toLatex();
                    }
                    break;
                default:
                    for(String symb : symbols) {
                        if (letters.equals(symb)) {
                            s += "\\" + symb + " ";
                            break;
                        }
                    }
                    if(s=="")
                        s += letters;
                    while(ch==' ') {
                        s += ' ';
                        nextChar();
                    }
                    break;
            }
        } else {
            s += (char) ch;
            nextChar();
        }

        if(consume('_'))
            s += "_{" + processFactor() + "}";
        if (consume('^'))
            s += "^{" + processFactor() + "}";
        if (consume('%'))
            s = "\\Mod{" + processFactor() + "}";
        return s;
    }

    private String substringTo(char c) {
        int count = 0, position = pos;
        while(count >= 0) {
            nextChar();
            if(ch == -1)
                return expr.substring(position);
            if(count==0 && ch==(int) c)
                break;
            count += (ch == '(' || ch == '{' || ch == '[' ? 1 : (ch == ')' || ch == '}' || ch == ']' ? -1 : 0));
        }
        nextChar(); //move on to the next character after the found character.
        return expr.substring(position, pos-1); //-1 because of nextChar() the line above
    }

    private String getEndOfBrackets() {
        int count = 1, position = pos;
        while(count > 0) {
            nextChar();
            if(ch == -1)
                return expr.substring(position);
            count += (ch == '(' || ch == '{' || ch == '[' ? 1 : (ch == ')' || ch == '}' || ch == ']' ? -1 : 0));
        }
        nextChar(); //move on to the next character after the closing bracket.
        return expr.substring(position, pos-1); //-1 because of nextChar() the line above
    }

    private String[] getArguments() {
        int brCount = 0;
        int lastPos = pos;
        ArrayList<String> arguments = new ArrayList<>();
        while(brCount>= 0 && ch != -1) {
            nextChar();
            if(ch == ',' && brCount == 0) {
                arguments.add(expr.substring(lastPos, pos).trim());
                //increment i because s.charAt(i)==',' which shouldn't be included in the next argument
                lastPos = pos+1;
            } else if(ch == '(' || ch=='[' || ch == '{')
                brCount++;
            else if(ch==')' || ch==']' || ch=='}')
                brCount--;
        }
        arguments.add(expr.substring(lastPos, pos).trim());
        String[] args = new String[arguments.size()];
        return arguments.toArray(args);
    }

    public static String[] getArguments(String s) {
        int brCount = 0;
        int lastPos = 0;
        ArrayList<String> arguments = new ArrayList<>();
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c==' ') continue;
            if(c == ',' && brCount == 0) {
                arguments.add(s.substring(lastPos, i).trim());
                //increment i because s.charAt(i)==',' which shouldn't be included in the next argument
                lastPos = i+1;
            } else if(c == '(' || c=='[' || c == '{')
                brCount++;
            else if(c==')' || c==']' || c=='}')
                brCount--;
            if(brCount<0)
                break;
        }
        arguments.add(s.substring(lastPos).trim());
        String[] args = new String[arguments.size()];
        return arguments.toArray(args);
    }

    private class VectorParser extends Parser {

        public VectorParser(String s) {
            super(s);
            expr = expr.substring(1, expr.length() - 1);
            expr = expr.replace(" ", "");
        }

        private boolean exprContainsInVector(int c) {
            int index = -1;
            while((index = expr.indexOf((char) c, index+1))!=-1) {
                int indexleft, indexright;
                indexright = indexleft = index;
                while(indexleft >= 0 && !"[]".contains("" + expr.charAt(indexleft))) indexleft--;
                while(indexright < expr.length() && !"[]".contains("" + expr.charAt(indexright))) indexright++;
                if((indexleft >= 0 && expr.charAt(indexleft) == '[') && (indexright < expr.length() && expr.charAt(indexright) == ']'))
                    return true;
            }
            return false;
        }

        private boolean exprContainsNotInVector(int c) {
            return !exprContainsInVector(c) && expr.contains("" + (char) c);
        }

        @Override
        public String toLatex () {
            boolean vector = !exprContainsNotInVector(';');
            nextChar();
            String v = "\\begin{pmatrix}";
            int p = 0;
            int brCount = 0;

            while(ch > 0) {
                if(ch == '(' || ch == '[' || ch=='{')
                    brCount++;
                else if(ch==')' || ch==']' || ch=='}')
                    brCount--;
                else if(brCount==0) {
                    if (ch == ',') {
                        v += (new Parser(expr.substring(p, pos))).toLatex() + (vector ? "\\\\" : "&");
                        p = pos + 1;
                    } else if (ch == ';') {
                        v += (new Parser(expr.substring(p, pos))).toLatex() + "\\\\";
                        p = pos + 1;
                    }
                }
                nextChar();
            }
            v += new Parser(expr.substring(p, pos)).toLatex();
            return v + "\\end{pmatrix}";
        }
    }
}