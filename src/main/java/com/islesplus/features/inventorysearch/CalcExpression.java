package com.islesplus.features.inventorysearch;

import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

/**
 * Calculator expression state, manipulation, and evaluation.
 * Uses exp4j with custom degree-based trig functions.
 */
final class CalcExpression {

    // ---- State ----
    static String expression = "";
    static String displayText = "";
    static String lastEntry = "";
    static boolean showingResult = false;
    static double lastResult = 0;

    // ---- Custom exp4j functions ----
    private static final Function[] FUNCTIONS = {
        new Function("sin", 1) {
            @Override public double apply(double... args) {
                return Math.sin(InventoryCalculator.radiansMode ? args[0] : Math.toRadians(args[0]));
            }
        },
        new Function("cos", 1) {
            @Override public double apply(double... args) {
                return Math.cos(InventoryCalculator.radiansMode ? args[0] : Math.toRadians(args[0]));
            }
        },
        new Function("tan", 1) {
            @Override public double apply(double... args) {
                return Math.tan(InventoryCalculator.radiansMode ? args[0] : Math.toRadians(args[0]));
            }
        },
        new Function("sqrt", 1) {
            @Override public double apply(double... args) { return Math.sqrt(args[0]); }
        },
        new Function("log", 1) {
            @Override public double apply(double... args) { return Math.log10(args[0]); }
        }
    };

    private static final String[] FUNCTION_NAMES = {"sin(", "cos(", "tan(", "sqrt(", "log("};

    private CalcExpression() {}

    static boolean isFunction(String token) {
        for (String f : FUNCTION_NAMES) if (f.equals(token)) return true;
        return false;
    }

    static void useAns() {
        String ans = formatResult(lastResult);
        if (showingResult) {
            expression = ans;
            showingResult = false;
        } else {
            expression += ans;
        }
        lastEntry = ans;
        displayText = expression;
    }

    static void clear() {
        expression = "";
        displayText = "";
        lastEntry = "";
        showingResult = false;
        lastResult = 0;
    }

    static void clearEntry() {
        if (lastEntry.isEmpty() || expression.isEmpty()) return;
        if (expression.endsWith(lastEntry)) {
            expression = expression.substring(0, expression.length() - lastEntry.length()).stripTrailing();
            lastEntry = "";
            displayText = expression;
        }
    }

    static void appendToExpression(String token) {
        // % is postfix percent: appends /100 rather than acting as a binary operator
        if (token.equals("%")) {
            if (showingResult) {
                expression = formatResult(lastResult) + " /100";
                showingResult = false;
            } else {
                expression = expression.stripTrailing() + " /100";
            }
            lastEntry = " /100";
            displayText = expression;
            return;
        }

        boolean isOperator = token.equals("+") || token.equals("-") || token.equals("*")
                || token.equals("/") || token.equals("^");

        // If appending a function and expression ends with a number, wrap it
        if (isFunction(token)) {
            if (showingResult) {
                expression = token + formatResult(lastResult) + ")";
                showingResult = false;
                lastEntry = token;
                displayText = expression;
                return;
            }
            int i = expression.length();
            while (i > 0 && (Character.isDigit(expression.charAt(i - 1)) || expression.charAt(i - 1) == '.')) {
                i--;
            }
            if (i < expression.length()) {
                String number = expression.substring(i);
                String inserted = token + number + ")";
                expression = expression.substring(0, i) + inserted;
                lastEntry = inserted;
                displayText = expression;
                return;
            }
            expression += token;
            lastEntry = token;
            displayText = expression;
            return;
        }

        if (showingResult) {
            if (isOperator) {
                expression = formatResult(lastResult) + " " + token + " ";
                lastEntry = " " + token + " ";
            } else {
                expression = token;
                lastEntry = token;
            }
            showingResult = false;
        } else {
            if (isOperator) {
                expression = expression.stripTrailing() + " " + token + " ";
                lastEntry = " " + token + " ";
            } else {
                expression += token;
                lastEntry = token;
            }
        }
        displayText = expression;
    }

    static void backspace() {
        if (expression.isEmpty()) return;
        if (showingResult) {
            clear();
            return;
        }
        for (String f : FUNCTION_NAMES) {
            if (expression.endsWith(f)) {
                expression = expression.substring(0, expression.length() - f.length());
                displayText = expression;
                return;
            }
        }
        String trimmed = expression.stripTrailing();
        if (trimmed.isEmpty()) {
            expression = "";
        } else {
            expression = trimmed.substring(0, trimmed.length() - 1);
        }
        if (expression.endsWith(" ")) {
            expression = expression.stripTrailing();
        }
        displayText = expression;
    }

    static void evaluate() {
        if (expression.isEmpty()) return;
        String expr = expression.stripTrailing();
        int openParens = 0;
        for (char ch : expr.toCharArray()) {
            if (ch == '(') openParens++;
            else if (ch == ')') openParens--;
        }
        expr += ")".repeat(Math.max(0, openParens));
        expr = expr.replace("%", "/100");
        try {
            double result = new ExpressionBuilder(expr)
                    .functions(FUNCTIONS)
                    .build()
                    .evaluate();
            lastResult = result;
            displayText = expression.stripTrailing() + " = " + formatResult(result);
            showingResult = true;
        } catch (Exception e) {
            displayText = "Error";
            showingResult = true;
        }
    }

    static void toggleSign() {
        if (showingResult) {
            lastResult = -lastResult;
            expression = formatResult(lastResult);
            displayText = expression;
            showingResult = false;
            return;
        }
        if (expression.isEmpty()) {
            expression = "-";
            displayText = expression;
            return;
        }
        int i = expression.length() - 1;
        while (i >= 0 && Character.isWhitespace(expression.charAt(i))) i--;
        int end = i;
        while (i >= 0 && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) i--;

        if (end != i) {
            // Trailing number found — toggle sign before it
            if (i >= 0 && expression.charAt(i) == '-') {
                expression = expression.substring(0, i) + expression.substring(i + 1);
            } else {
                expression = expression.substring(0, i + 1) + "-" + expression.substring(i + 1);
            }
        } else {
            // No trailing number (e.g. ends with ')') — wrap whole expression
            String trimmed = expression.stripTrailing();
            if (trimmed.startsWith("-(") && trimmed.endsWith(")")) {
                expression = trimmed.substring(2, trimmed.length() - 1);
            } else {
                expression = "-(" + trimmed + ")";
            }
        }
        displayText = expression;
    }

    static String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        String s = String.format("%.10f", value);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s;
    }

    static String getGhostParens() {
        if (showingResult || expression.isEmpty()) return "";
        int unmatched = 0;
        for (char ch : expression.toCharArray()) {
            if (ch == '(') unmatched++;
            else if (ch == ')') unmatched--;
        }
        return ")".repeat(Math.max(0, unmatched));
    }
}
