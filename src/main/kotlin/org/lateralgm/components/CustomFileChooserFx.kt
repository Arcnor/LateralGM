/*
 * Copyright (C) 2016 Edu Garcia
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */
package org.lateralgm.components

import javafx.stage.FileChooser
import org.lateralgm.components.impl.CustomFileFilter
import org.lateralgm.util.UIHelper
import java.io.File
import java.util.concurrent.Callable
import java.util.prefs.Preferences
import javax.swing.JFrame

class CustomFileChooserFx(node: String, private val propertyName: String) {
	private val fc = FileChooser()
	private val prefs: Preferences

	init {
		prefs = Preferences.userRoot().node(node)
		fc.initialDirectory = File(prefs.get(propertyName, fc.initialDirectory?.absolutePath))
	}

	// FIXME: Parent window should be set
	fun showOpenDialog(frame: JFrame) = UIHelper.callJavaFX(Callable { fc.showOpenDialog(null) })

	// FIXME: Parent window should be set
	fun showSaveDialog(frame: JFrame, initialDir: File?): File? = UIHelper.callJavaFX(Callable {
		fc.initialDirectory = initialDir
		fc.showSaveDialog(null)
	})

	fun setFilterSet(filterSet: Iterable<CustomFileFilter>) {
		val filters = fc.extensionFilters
		filters.clear()
		filters.addAll(filterSet.toJavaFx())
	}
}

fun Iterable<CustomFileFilter>.toJavaFx(): Iterable<FileChooser.ExtensionFilter> {
	return this.map { FileChooser.ExtensionFilter(it.description, *it.extensions) }
}
