package io.jstach.ezkv.kvs.interpolate;

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

import java.util.Arrays;

interface StrMatcher {

	static StrMatcher stringMatcher(final String str) {
		if (str.isEmpty()) {
			return NoMatcher.NO_MATCHER;
		}
		return new StringMatcher(str);
	}

	public abstract int isMatch(CharSequence buffer, int pos, int bufferStart, int bufferEnd);

	default int isMatch(CharSequence buffer, final int pos) {
		return isMatch(buffer, pos, 0, buffer.length());
	}

	// -----------------------------------------------------------------------
	/**
	 * Class used to define a set of characters for matching purposes.
	 */
	static final class StringMatcher implements StrMatcher {

		/** The string to match, as a character array. */
		private final char[] chars;

		StringMatcher(final String str) {
			super();
			chars = str.toCharArray();
		}

		@Override
		public int isMatch(CharSequence buffer, int pos, int bufferStart, int bufferEnd) {
			final int len = chars.length;
			if (pos + len > bufferEnd) {
				return 0;
			}
			for (int i = 0; i < chars.length; i++, pos++) {
				if (chars[i] != buffer.charAt(pos)) {
					return 0;
				}
			}
			return len;
		}

		@Override
		public String toString() {
			return super.toString() + ' ' + Arrays.toString(chars);
		}

	}

	// -----------------------------------------------------------------------
	/**
	 * Class used to match no characters.
	 */
	static enum NoMatcher implements StrMatcher {

		NO_MATCHER;

		@Override
		public int isMatch(CharSequence buffer, int pos, int bufferStart, int bufferEnd) {
			return 0;
		}

	}

}
