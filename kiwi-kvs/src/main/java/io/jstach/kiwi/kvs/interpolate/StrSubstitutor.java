package io.jstach.kiwi.kvs.interpolate;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

class StrSubstitutor {

	/**
	 * Constant for the default escape character.
	 */
	private static final char DEFAULT_ESCAPE = '$';

	/**
	 * Constant for the default variable prefix.
	 */
	private static final StrMatcher DEFAULT_PREFIX = StrMatcher.stringMatcher("${");

	/**
	 * Constant for the default variable suffix.
	 */
	private static final StrMatcher DEFAULT_SUFFIX = StrMatcher.stringMatcher("}");

	/**
	 * Constant for the default value delimiter of a variable.
	 *
	 * @since 3.2
	 */
	private static final StrMatcher DEFAULT_VALUE_DELIMITER = StrMatcher.stringMatcher(":-");

	/**
	 * Stores the escape character.
	 */
	private final char escapeChar;

	/**
	 * Stores the variable prefix.
	 */
	private final StrMatcher prefixMatcher;

	/**
	 * Stores the variable suffix.
	 */
	private final StrMatcher suffixMatcher;

	/**
	 * Stores the default variable value delimiter
	 */
	private final StrMatcher valueDelimiterMatcher;

	/**
	 * Variable resolution is delegated to an implementor of VariableResolver.
	 */
	private final StrLookup variableResolver;

	/**
	 * The flag whether substitution in variable names is enabled.
	 */
	private final boolean enableSubstitutionInVariables;

	interface StrLookup {

		public abstract @Nullable String lookup(String key, boolean defaultValue);

	}

	/**
	 * Creates a new instance and initializes it.
	 * @param variableResolver the variable resolver, may be null
	 */
	StrSubstitutor(final StrLookup variableResolver) {
		this(variableResolver, DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ESCAPE, DEFAULT_VALUE_DELIMITER, true);
	}

	private StrSubstitutor(StrLookup variableResolver, StrMatcher prefixMatcher, StrMatcher suffixMatcher,
			char escapeChar, StrMatcher valueDelimiterMatcher, boolean enableSubstitutionInVariables) {
		super();
		this.variableResolver = variableResolver;
		this.prefixMatcher = prefixMatcher;
		this.suffixMatcher = suffixMatcher;
		this.escapeChar = escapeChar;
		this.valueDelimiterMatcher = valueDelimiterMatcher;
		this.enableSubstitutionInVariables = enableSubstitutionInVariables;
	}

	// -----------------------------------------------------------------------
	/**
	 * Replaces all the occurrences of variables with their matching values from the
	 * resolver using the given source string as a template.
	 * @param source the string to replace in, null returns null
	 * @return the result of the replace operation
	 */
	String replace(final String source) {
		final StringBuilder buf = new StringBuilder(source);
		if (!substitute(buf, 0, source.length())) {
			return source;
		}
		return buf.toString();
	}

	private boolean substitute(final StringBuilder buf, final int offset, final int length) {
		return substitute(buf, offset, length, null) > 0;
	}

	private int substitute(final StringBuilder buf, final int offset, final int length,
			@Nullable List<String> priorVariables) {
		final StrMatcher pfxMatcher = this.prefixMatcher;
		final StrMatcher suffMatcher = this.suffixMatcher;
		final char escape = this.escapeChar;
		final StrMatcher valueDelimMatcher = this.valueDelimiterMatcher;
		final boolean substitutionInVariablesEnabled = this.enableSubstitutionInVariables;

		final boolean top = priorVariables == null;
		boolean altered = false;
		int lengthChange = 0;
		// CharSequence chars = buf;
		int bufEnd = offset + length;
		int pos = offset;
		while (pos < bufEnd) {
			final int startMatchLen = pfxMatcher.isMatch(buf, pos, offset, bufEnd);
			if (startMatchLen == 0) {
				pos++;
				continue;
			}
			// found variable start marker
			if (pos > offset && buf.charAt(pos - 1) == escape) {
				// escaped
				buf.deleteCharAt(pos - 1);
				lengthChange--;
				altered = true;
				bufEnd--;
				continue;
			}
			// find suffix
			final int startPos = pos;
			pos += startMatchLen;
			int endMatchLen = 0;
			int nestedVarCount = 0;
			while (pos < bufEnd) {
				if (substitutionInVariablesEnabled
						&& (endMatchLen = pfxMatcher.isMatch(buf, pos, offset, bufEnd)) != 0) {
					// found a nested variable start
					nestedVarCount++;
					pos += endMatchLen;
					continue;
				}
				endMatchLen = suffMatcher.isMatch(buf, pos, offset, bufEnd);
				if (endMatchLen == 0) {
					pos++;
					continue;
				}
				// found variable end marker
				if (nestedVarCount == 0) {
					String varNameExpr = buf.subSequence(startPos + startMatchLen, pos).toString();
					// String varNameExpr = new String(chars, startPos +
					// startMatchLen, pos - startPos - startMatchLen);
					if (substitutionInVariablesEnabled) {
						final StringBuilder bufName = new StringBuilder(varNameExpr);
						substitute(bufName, 0, bufName.length());
						varNameExpr = bufName.toString();
					}
					pos += endMatchLen;
					final int endPos = pos;

					String varName = varNameExpr.toString();
					String varDefaultValue = null;

					int valueDelimiterMatchLen = 0;
					for (int i = 0; i < varNameExpr.length(); i++) {
						// if there's any nested variable when
						// nested variable substitution
						// disabled, then stop resolving name
						// and default value.
						if (!substitutionInVariablesEnabled
								&& pfxMatcher.isMatch(varNameExpr, i, i, varNameExpr.length()) != 0) {
							break;
						}
						if ((valueDelimiterMatchLen = valueDelimMatcher.isMatch(varNameExpr, i)) != 0) {
							varName = varNameExpr.substring(0, i);
							varDefaultValue = varNameExpr.substring(i + valueDelimiterMatchLen);
							break;
						}
					}

					// on the first call initialize priorVariables
					if (priorVariables == null) {
						priorVariables = new ArrayList<>();
						priorVariables.add(buf.subSequence(offset, bufEnd).toString());
					}

					// handle cyclic substitution
					checkCyclicSubstitution(varName, priorVariables);
					priorVariables.add(varName);

					// resolve the variable
					String varValue = resolveVariable(varName, buf, startPos, endPos, varDefaultValue != null);
					if (varValue == null) {
						varValue = varDefaultValue;
					}
					if (varValue != null) {
						// recursive replace
						final int varLen = varValue.length();
						buf.replace(startPos, endPos, varValue);
						altered = true;
						int change = substitute(buf, startPos, varLen, priorVariables);
						change = change + varLen - (endPos - startPos);
						pos += change;
						bufEnd += change;
						lengthChange += change;
					}

					// remove variable from the cyclic stack
					priorVariables.remove(priorVariables.size() - 1);
					break;
				}
				nestedVarCount--;
				pos += endMatchLen;

			}

		}
		if (top) {
			return altered ? 1 : 0;
		}
		return lengthChange;
	}

	/**
	 * Checks if the specified variable is already in the stack (list) of variables.
	 * @param varName the variable name to check
	 * @param priorVariables the list of prior variables
	 */
	private void checkCyclicSubstitution(final String varName, final List<String> priorVariables) {
		if (!priorVariables.contains(varName)) {
			return;
		}
		final StringBuilder buf = new StringBuilder();
		buf.append("Infinite loop in property interpolation of ");
		buf.append(priorVariables.remove(0));
		buf.append(": ");
		boolean first = true;
		for (String pv : priorVariables) {
			if (first) {
				first = false;
			}
			else {
				buf.append("->");
			}
			buf.append(pv);
		}
		throw new IllegalStateException(buf.toString());
	}

	private @Nullable String resolveVariable(final String variableName, final StringBuilder buf, final int startPos,
			final int endPos, boolean defaultValue) {
		final StrLookup resolver = variableResolver;
		return resolver.lookup(variableName, defaultValue);
	}

}
