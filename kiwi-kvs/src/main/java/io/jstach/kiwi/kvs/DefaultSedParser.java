package io.jstach.kiwi.kvs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

class DefaultSedParser {

	static Command parseCommand(String input) {
		Tokenizer tokenizer = new Tokenizer(input);
		SedParser parser = new SedParser(tokenizer.iterator());
		return parser.parse();
	}

	enum TokenType {

		ADDRESS, COMMAND, DELIMITER, PATTERN, REPLACEMENT, FLAG, EOF

	}

	static class Token {

		private final TokenType type;

		private final String value;

		public Token(TokenType type, String value) {
			this.type = type;
			this.value = value;
		}

		public TokenType getType() {
			return type;
		}

		public String getValue() {
			return value;
		}

	}

	record Address(String regex) {
		public boolean matches(String key) {
			return key.matches(regex);
		}
	}

	static class Tokenizer implements Iterable<Token> {

		private final String input;

		private int position = 0;

		public Tokenizer(String input) {
			this.input = input;
		}

		public List<Token> tokenize() {
			List<Token> tokens = new ArrayList<>();

			while (position < input.length()) {
				char currentChar = input.charAt(position);

				if (tokens.isEmpty() && currentChar == '/') {
					tokens.add(parseRegexAddress());
				}
				else if (currentChar == 's'
						&& (tokens.isEmpty() || tokens.get(tokens.size() - 1).getType() == TokenType.ADDRESS)) {
					tokens.add(new Token(TokenType.COMMAND, "s"));
					position++;
					tokens.add(parseDelimiter());
				}
				else if (isDelimiter(currentChar)) {
					tokens.add(new Token(TokenType.DELIMITER, String.valueOf(currentChar)));
					position++;
				}
				else if (currentChar == 'g' && position == input.length() - 1) {
					tokens.add(new Token(TokenType.FLAG, "g"));
					position++;
				}
				else {
					String value = readUntilDelimiter(tokens.get(tokens.size() - 1).getValue().charAt(0));
					tokens.add(new Token(TokenType.PATTERN, value));
				}
			}

			tokens.add(new Token(TokenType.EOF, ""));
			return tokens;
		}

		@Override
		public Iterator<Token> iterator() {
			return tokenize().iterator();
		}

		private Token parseRegexAddress() {
			StringBuilder regexBuilder = new StringBuilder();
			position++; // Skip the initial '/'

			while (position < input.length()) {
				char currentChar = input.charAt(position);
				if (currentChar == '/') {
					position++; // Skip the closing '/'
					return new Token(TokenType.ADDRESS, regexBuilder.toString());
				}
				if (currentChar == '\\' && position + 1 < input.length() && input.charAt(position + 1) == '/') {
					regexBuilder.append('/');
					position += 2;
				}
				else {
					regexBuilder.append(currentChar);
					position++;
				}
			}

			throw new IllegalArgumentException("Unterminated regex address");
		}

		private Token parseDelimiter() {
			if (position < input.length()) {
				char delimiter = input.charAt(position);
				if (delimiter == '\\' || delimiter == '\n') {
					throw new IllegalArgumentException("Invalid delimiter: " + delimiter);
				}
				position++;
				return new Token(TokenType.DELIMITER, String.valueOf(delimiter));
			}
			throw new IllegalArgumentException("Expected delimiter after command");
		}

		private boolean isDelimiter(char c) {
			return c != '\\' && c != '\n' && !Character.isWhitespace(c);
		}

		private String readUntilDelimiter(char delimiter) {
			StringBuilder value = new StringBuilder();
			while (position < input.length()) {
				char currentChar = input.charAt(position);
				if (currentChar == delimiter) {
					break;
				}
				if (currentChar == '\\' && position + 1 < input.length() && input.charAt(position + 1) == delimiter) {
					value.append(delimiter);
					position += 2;
				}
				else {
					value.append(currentChar);
					position++;
				}
			}
			return value.toString();
		}

	}

	sealed interface Command permits SubstitutionCommand {

		@Nullable
		String execute(String key);

	}

	record SubstitutionCommand(@Nullable Address address, String pattern, String replacement,
			boolean isGlobal) implements Command {
		@Override
		public @Nullable String execute(String key) {
			if (address == null || address.matches(key)) {
				return isGlobal ? key.replaceAll(pattern, replacement) : key.replaceFirst(pattern, replacement);
			}
			return key; // If the address does not match, return the original key
		}
	}

	static class SedParser {

		private final Iterator<Token> tokenIterator;

		private Token currentToken;

		public SedParser(Iterator<Token> tokenIterator) {
			this.tokenIterator = tokenIterator;
			advance(); // Initialize with the first token
		}

		private void advance() {
			if (tokenIterator.hasNext()) {
				currentToken = tokenIterator.next();
			}
			else {
				currentToken = new Token(TokenType.EOF, "");
			}
		}

		public Command parse() {
			Address address = null;

			// Check if the current token is an address (e.g., /match/)
			if (currentToken.getType() == TokenType.ADDRESS) {
				address = new Address(currentToken.getValue());
				advance(); // Move to the next token, which should be the command
			}

			// Parse the function (e.g., substitution command)
			if (currentToken.getType() == TokenType.COMMAND && "s".equals(currentToken.getValue())) {
				advance(); // Move past the 's' command
				return parseSubstitution(address);
			}

			throw new IllegalArgumentException("Unsupported command or invalid syntax");
		}

		private SubstitutionCommand parseSubstitution(Address address) {
			if (currentToken.getType() != TokenType.DELIMITER) {
				throw new IllegalArgumentException("Expected delimiter after 's'");
			}
			char delimiter = currentToken.getValue().charAt(0);
			advance(); // Move past the delimiter

			String pattern = readUntilDelimiter(delimiter);
			String replacement = readUntilDelimiter(delimiter);

			boolean isGlobal = false;
			if (currentToken.getType() == TokenType.FLAG && "g".equals(currentToken.getValue())) {
				isGlobal = true;
				advance(); // Move past the flag
			}

			return new SubstitutionCommand(address, pattern, replacement, isGlobal);
		}

		private String readUntilDelimiter(char delimiter) {
			StringBuilder value = new StringBuilder();
			while (currentToken.getType() != TokenType.DELIMITER && currentToken.getType() != TokenType.EOF) {
				value.append(currentToken.getValue());
				advance();
			}
			if (currentToken.getType() == TokenType.DELIMITER && currentToken.getValue().charAt(0) == delimiter) {
				advance(); // Move past the delimiter
			}
			else {
				throw new IllegalArgumentException("Expected closing delimiter");
			}
			return value.toString();
		}

	}

}
