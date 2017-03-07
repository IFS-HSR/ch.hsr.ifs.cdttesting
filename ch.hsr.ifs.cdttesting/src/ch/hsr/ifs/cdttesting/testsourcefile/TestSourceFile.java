/*******************************************************************************
 * Copyright (c) 2008 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Institute for Software - initial API and implementation
 *******************************************************************************/
package ch.hsr.ifs.cdttesting.testsourcefile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.TextSelection;

/**
 * @author Emanuel Graf, Lukas Felber
 *
 */
public class TestSourceFile {

	private static final String REPLACEMENT = "";
	private final String name;
	private final StringBuilder source = new StringBuilder();
	private StringBuilder expectedSource;
	private static final String NL = System.getProperty("line.separator");
	private int selectionStart = -1;
	private int selectionEnd = -1;
	private boolean firstInsert = true;

	protected static final String selectionStartRegex = "/\\*\\$\\*/";
	protected static final String selectionEndRegex = "/\\*\\$\\$\\*/";
	protected static final String selectionStartLineRegex = "(.*)(" + selectionStartRegex + ")(.*)";
	protected static final String selectionEndLineRegex = "(.*)(" + selectionEndRegex + ")(.*)";

	public TestSourceFile(final String name) {
		super();
		this.name = useSystemSeperator(name);
	}

	private String useSystemSeperator(final String name) {
		final char systemSeparator = File.separatorChar;
		return name.replace('\\', systemSeparator).replace('/', systemSeparator);
	}

	public String getExpectedSource() {
		if (expectedSource != null) {
			return expectedSource.toString();
		} else {
			return getSource();
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getSource() {
		return source.toString();
	}

	public void addLineToSource(String code) {
		final Matcher start = createMatcherFromString(selectionStartLineRegex, code);
		final int nlLength = firstInsert ? 0 : NL.length();
		if (start.matches()) {
			selectionStart = start.start(2) + nlLength + source.length();
			code = code.replaceAll(selectionStartRegex, REPLACEMENT);
		}
		final Matcher end = createMatcherFromString(selectionEndLineRegex, code);
		if (end.matches()) {
			selectionEnd = end.start(2) + nlLength + source.length();
			code = code.replaceAll(selectionEndRegex, REPLACEMENT);
		}
		append(source, code);
	}

	private void append(final StringBuilder builder, final String code) {
		if (firstInsert) {
			firstInsert = false;
		} else {
			builder.append(NL);
		}
		builder.append(code);
	}

	public void addLineToExpectedSource(final String code) {
		append(expectedSource, code);
	}

	public TextSelection getSelection() {
		if (selectionStart < 0 || selectionEnd < 0) {
			return null;
		} else {
			return new TextSelection(selectionStart, selectionEnd - selectionStart);
		}
	}

	protected static Matcher createMatcherFromString(final String pattern, final String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	public void initExpectedSource() {
		expectedSource = new StringBuilder();
		firstInsert = true;
	}
}
