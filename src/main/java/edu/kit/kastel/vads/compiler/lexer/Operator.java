package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {
        ASSIGN_MINUS("-="),
        MINUS("-"),
        ASSIGN_PLUS("+="),
        PLUS("+"),
        MUL("*"),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        DIV("/"),
        ASSIGN_MOD("%="),
        MOD("%"),
        ASSIGN("="),
        // L2
        EQ("=="),
        NE("!="),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
        ANDAND("&&"),
        OROR("||"),
        AND("&"),
        OR("|"),
        XOR("^"),
        NOT("!"),
        BITWISE_NOT("~"),
        LSHIFT("<<"),
        RSHIFT(">>"),
        ASSIGN_LSHIFT("<<="),
        ASSIGN_RSHIFT(">>="),
        ASSIGN_AND("&="),
        ASSIGN_OR("|="),
        ASSIGN_XOR("^="),
        QUESTION("?"),
        COLON(":"),
        SHL("<<"),
        SHR(">>")
        ;

        private final String value;

        OperatorType(String value) {
            this.value = value;
        }

        public String symbol() { // for test
            return value;
        }

        private static final Map<String, OperatorType> stringToTypeMap = new HashMap<>();

        static {
            for (OperatorType type : OperatorType.values()) {
                stringToTypeMap.put(type.symbol(), type);
            }
        }

        public static OperatorType fromString(String s) {
            return stringToTypeMap.get(s);
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
