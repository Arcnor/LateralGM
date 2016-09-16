/*
 * Copyright (C) 2016 Edu Garcia
 *
 * This file is part of LateralGM.
 *
 * LateralGM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LateralGM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License (COPYING) for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.lateralgm.util

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI

object PlatformHelper {
	private val desktop = Desktop.getDesktop()

	@JvmStatic
	@Throws(IOException::class)
	fun showDocumentation(location: URI) {
		desktop.browse(location)
	}

	/**
	 * Launches the associated editor application and opens a file for
	 * editing.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun openEditor(file: File) {
		desktop.edit(file)
	}

	/**
	 * Launches the associated application to open the file.
	 *
	 * If the specified file is a directory, the file manager of the current platform is launched to open it.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun openFile(file: File) {
		desktop.open(file)
	}
}