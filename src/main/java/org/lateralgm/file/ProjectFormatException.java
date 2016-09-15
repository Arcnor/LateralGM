/**
 * @file ProjectFormatException.java
 * @brief Class that abstracts the format specific exception messages and provides more detailed stack traces.
 * @section License
 * <p>
 * Copyright (C) 2013-2014 Robert B. Colton
 * This file is a part of the LateralGM IDE.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.lateralgm.file;

public class ProjectFormatException extends Exception {
	private static final long serialVersionUID = 1L;
	public ProjectFile file;
	public Exception e;

	public ProjectFormatException(ProjectFile file, String message) {
		super(message);
		this.file = file;
	}

	public ProjectFormatException(ProjectFile file, Exception e) {
		super(e.getClass().getName() + ": " + e.getLocalizedMessage());
		this.setStackTrace(e.getStackTrace());
		this.e = e;
		this.file = file;
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		if (e != null) return e.getStackTrace();
		return super.getStackTrace();
	}
}
