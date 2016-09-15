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

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import java.awt.Component
import java.util.EnumSet
import java.util.Optional
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.FutureTask

var Alert.defaultButton: ButtonType
	get() {
		val pane = dialogPane
		for (buttonType in buttonTypes) {
			val button = pane.lookupButton(buttonType) as Button
			if (button.isDefaultButton) {
				return buttonType
			}
		}
		return ButtonType.OK
	}

	set(value) {
		val pane = dialogPane
		for (buttonType in buttonTypes) {
			(pane.lookupButton(buttonType) as Button).isDefaultButton = buttonType === value
		}
	}

object UIHelper {
	enum class DialogAction {
		NO_RESULT,
		YES, NO, APPLY, CANCEL, CLOSE, FINISH, NEXT, OK, PREVIOUS;

		companion object {
			@JvmStatic
			val YES_NO = EnumSet.of(YES, NO)
		}

		fun toJavaFX(): ButtonType {
			return when (this) {
				UIHelper.DialogAction.NO_RESULT -> throw UnsupportedOperationException()
				UIHelper.DialogAction.YES -> ButtonType.YES
				UIHelper.DialogAction.NO -> ButtonType.NO
				UIHelper.DialogAction.APPLY -> ButtonType.APPLY
				UIHelper.DialogAction.CANCEL -> ButtonType.CANCEL
				UIHelper.DialogAction.CLOSE -> ButtonType.CLOSE
				UIHelper.DialogAction.FINISH -> ButtonType.FINISH
				UIHelper.DialogAction.NEXT -> ButtonType.NEXT
				UIHelper.DialogAction.OK -> ButtonType.OK
				UIHelper.DialogAction.PREVIOUS -> ButtonType.PREVIOUS
			}
		}
	}

	enum class DialogType {
		CONFIRMATION, ERROR, INFORMATION, NONE, WARNING;

		fun toJavaFX(): Alert.AlertType {
			return when(this) {
				CONFIRMATION -> Alert.AlertType.CONFIRMATION
				ERROR -> Alert.AlertType.ERROR
				INFORMATION -> Alert.AlertType.INFORMATION
				NONE -> Alert.AlertType.NONE
				WARNING -> Alert.AlertType.WARNING
			}
		}
	}

	@JvmStatic
	@JvmOverloads
	fun showConfirmationDialog(parent: Component, dialogType: DialogType = DialogType.INFORMATION, actions: EnumSet<DialogAction>, title: String, message: String, header: String? = null, defaultAction: DialogAction? = null): DialogAction {
		// FIXME: We need to set the dialog as modal, but it's impossible as long as we have an AWT component as parent (added that parameter just to not lose the information about who's the parent)
		return callJavaFX(Callable<Optional<ButtonType>> {
			val alert = Alert(dialogType.toJavaFX())
			alert.title = title
			alert.contentText = message
			alert.buttonTypes.clear()
			alert.buttonTypes.addAll(actions.map { it.toJavaFX() })
			if (defaultAction != null) {
				alert.defaultButton = defaultAction.toJavaFX()
			}
			if (header != null) {
				alert.headerText = header
			}

			alert.showAndWait()
		})
	}

	private fun callJavaFX(callable: Callable<Optional<ButtonType>>): DialogAction {
		// FIXME: The FutureTask doesn't work twice for some reason...
		val task = FutureTask(callable)

		Platform.runLater(task)
		return task.get().toLGMDialogResult()
	}
}

fun Optional<ButtonType>.toLGMDialogResult(): UIHelper.DialogAction {
	if (!this.isPresent) return UIHelper.DialogAction.NO_RESULT

	return when (this.get()) {
		ButtonType.APPLY -> UIHelper.DialogAction.APPLY
		ButtonType.CANCEL -> UIHelper.DialogAction.CANCEL
		ButtonType.CLOSE -> UIHelper.DialogAction.CLOSE
		ButtonType.FINISH -> UIHelper.DialogAction.FINISH
		ButtonType.NEXT -> UIHelper.DialogAction.NEXT
		ButtonType.NO -> UIHelper.DialogAction.NO
		ButtonType.OK -> UIHelper.DialogAction.OK
		ButtonType.PREVIOUS -> UIHelper.DialogAction.PREVIOUS
		ButtonType.YES -> UIHelper.DialogAction.YES
		else -> throw LGMUIException("Unknown ButtonType")
	}
}

class LGMUIException(message: String) : Throwable(message)
