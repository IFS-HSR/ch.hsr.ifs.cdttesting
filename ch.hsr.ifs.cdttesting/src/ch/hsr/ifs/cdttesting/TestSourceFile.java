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
package ch.hsr.ifs.cdttesting;

import helpers.FileHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.TextSelection;

/**
 * @author Emanuel Graf, Lukas Felber
 * 
 */
public class TestSourceFile {

	private static final String REPLACEMENT = ""; //$NON-NLS-1$
	private final String name;
	private final StringBuilder source = new StringBuilder();
	private StringBuilder expectedSource;
	private final String separator = System.getProperty("line.separator"); //$NON-NLS-1$
	private int selectionStart = -1;
	private int selectionEnd = -1;

	protected static final String selectionStartRegex = "/\\*\\$\\*/"; //$NON-NLS-1$
	protected static final String selectionEndRegex = "/\\*\\$\\$\\*/"; //$NON-NLS-1$
	protected static final String selectionStartLineRegex = "(.*)(" + selectionStartRegex + ")(.*)"; //$NON-NLS-1$ //$NON-NLS-2$
	protected static final String selectionEndLineRegex = "(.*)(" + selectionEndRegex + ")(.*)"; //$NON-NLS-1$ //$NON-NLS-2$

	public TestSourceFile(String name) {
		super();
		this.name = FileHelper.useSystemSeparators(name);
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
		Matcher start = createMatcherFromString(selectionStartLineRegex, code);
		if (start.matches()) {
			selectionStart = start.start(2) + source.length();
			code = code.replaceAll(selectionStartRegex, REPLACEMENT);
		}
		Matcher end = createMatcherFromString(selectionEndLineRegex, code);
		if (end.matches()) {
			selectionEnd = end.start(2) + source.length();
			code = code.replaceAll(selectionEndRegex, REPLACEMENT);
		}
		source.append(code);
		source.append(separator);
	}

	public void addLineToExpectedSource(String code) {
		expectedSource.append(code);
		expectedSource.append(separator);
	}

	public TextSelection getSelection() {
		if (selectionStart < 0 || selectionEnd < 0) {
			return null;
		} else {
			return new TextSelection(selectionStart, selectionEnd - selectionStart);
		}
	}

	protected static Matcher createMatcherFromString(String pattern, String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	public void initExpectedSource() {
		expectedSource = new StringBuilder();
	}
}
