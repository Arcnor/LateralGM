/*
 * Copyright (C) 2016 Edu Garcia
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */
package org.lateralgm.components

import javafx.application.Platform
import javafx.stage.FileChooser
import org.lateralgm.components.impl.CustomFileFilter
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.prefs.Preferences
import javax.swing.JFrame

class CustomFileChooserFx(node: String, private val propertyName: String) {
	private val fc = FileChooser()
	private val prefs: Preferences

	init {
		prefs = Preferences.userRoot().node(node)
		fc.initialDirectory = File(prefs.get(propertyName, fc.initialDirectory?.absolutePath))
	}

	fun showOpenDialog(frame: JFrame): File? {
		val fixmeResult: Array<File?> = kotlin.arrayOfNulls(1)
		val countdownLatch = CountDownLatch(1)
		Platform.runLater({
			// FIXME: Parent window should be set
			fixmeResult[0] = fc.showOpenDialog(null)
			countdownLatch.countDown()
		})
		countdownLatch.await()
		return fixmeResult[0]
	}

	fun showSaveDialog(frame: JFrame): File? {
		// FIXME: Parent window should be set
		return fc.showSaveDialog(null)
	}

	fun setFilterSet(filterSet: Iterable<CustomFileFilter>) {
		val filters = fc.extensionFilters
		filters.clear()
		filters.addAll(filterSet.toJavaFx())
	}
}

fun Iterable<CustomFileFilter>.toJavaFx(): Iterable<FileChooser.ExtensionFilter> {
	return this.map { FileChooser.ExtensionFilter(it.description, *it.extensions) }
}
