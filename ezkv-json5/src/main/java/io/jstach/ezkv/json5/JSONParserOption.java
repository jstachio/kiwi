package io.jstach.ezkv.json5;
/**
 * Contains string constants for JSON parser flags.
 */
public class JSONParserOption {

//    /**
//     * Whether binary literals ({@code 0b10101...}) should be allowed.
//     * <p>
//     * Default: {@code false}
//     */
//    public static final String FLAG_BINARY_LITERALS = "binary_literals";
//
//    /**
//     * Whether octal literals ({@code 0o567...}) should be allowed.
//     * <p>
//     * Default: {@code false}
//     */
//    public static final String FLAG_OCTAL_LITERALS = "octal_literals";

    /**
     * Whether hexadecimal floating-point literals (e.g. {@code 0xA.BCp+12}) should be
     * allowed.
     * <p>
     * Default: {@code false}
     */
    public static final String FLAG_HEX_FLOATING_LITERALS = "hex_floating_literals";

//    /**
//     * Whether Java-style digit separators ({@code 123_456}) should be allowed.
//     * <p>
//     * Default: {@code false}
//     */
//    public static final String FLAG_JAVA_DIGIT_SEPARATORS = "java_digit_separators";
//
//    /**
//     * Whether C-style digit separators ({@code 123'456}) should be allowed.
//     * <p>
//     * Default: {@code false}
//     */
//    public static final String FLAG_C_DIGIT_SEPARATORS = "c_digit_separators";

    /**
     * Whether 32-bit Unicode escape sequences ({@code \U00123456}) should be allowed.
     * <p>
     * Default: {@code false}
     */
    public static final String FLAG_LONG_UNICODE_ESCAPES = "long_unicode_escapes";
}
