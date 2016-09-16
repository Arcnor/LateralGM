/*
 * Copyright (C) 2006-2011 IsmAvatar <IsmAvatar@gmail.com>
 * Copyright (C) 2006, 2007 TGMG <thegamemakerguru@gmail.com>
 * Copyright (C) 2007, 2008 Quadduc <quadduc@gmail.com>
 * Copyright (C) 2006, 2007, 2008 Clam <clamisgood@gmail.com>
 * Copyright (C) 2013, 2014, 2015 Robert B. Colton
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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License (COPYING) for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.lateralgm.main

import org.lateralgm.components.GmTreeGraphics
import org.lateralgm.components.impl.ResNode
import org.lateralgm.messages.Messages
import org.lateralgm.resources.GameSettings
import org.lateralgm.resources.InstantiableResource
import org.lateralgm.resources.Resource
import javax.swing.AbstractButton
import javax.swing.Box
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JToggleButton
import javax.swing.JToolBar

class LGMToolbar : JToolBar() {
	var configsCombo: JComboBox<GameSettings>
	var eventButton: AbstractButton

	val selectedGameSettings: GameSettings
		get() {
			return configsCombo.selectedItem as GameSettings
		}

	init {
		isFloatable = true
		add(makeButton("Toolbar.NEW"))
		add(makeButton("Toolbar.OPEN"))
		add(makeButton("Toolbar.SAVE"))
		add(makeButton("Toolbar.SAVEAS"))
		addSeparator()
		for (k: Class<out Resource<*, *>> in Resource.kinds) {
			if (InstantiableResource::class.java.isAssignableFrom(k)) {
				val ico = ResNode.ICON[k] ?: GmTreeGraphics.getBlankIcon()

				val but = JButton(ico)
				but.toolTipText = Messages.format("Toolbar.ADD", Resource.kindNames[k])
				but.addActionListener(Listener.ResourceAdder(false, k))
				add(but)
			}
		}
		addSeparator()
		add(makeButton("Toolbar.CST"))
		add(makeButton("Toolbar.GMI"))
		add(makeButton("Toolbar.PKG"))
		addSeparator()
		add(JLabel(Messages.getString("Toolbar.CONFIGURATIONS")))
		configsCombo = JComboBox<GameSettings>()
		configsCombo.model = DefaultComboBoxModel(LGM.currentFile.gameSettings)
		configsCombo.maximumSize = configsCombo.preferredSize
		add(configsCombo)
		add(makeButton("Toolbar.CONFIG_MANAGE"))
		addSeparator()
		add(makeButton("Toolbar.GMS"))
		addSeparator()
		add(makeButton("Toolbar.PREFERENCES"))
		add(makeButton("Toolbar.DOCUMENTATION"))
		add(Box.createHorizontalGlue()) //right align after this
		eventButton = makeButton(JToggleButton(), "Toolbar.EVENT_BUTTON")
		add(eventButton)
	}


	private fun makeButton(key: String): JButton {
		val but = JButton()
		makeButton(but, key)
		return but
	}

	private fun makeButton(but: AbstractButton, key: String): AbstractButton {
		val ico = LGM.getIconForKey(key)
		if (ico != null)
			but.icon = ico
		else
			but.icon = GmTreeGraphics.getBlankIcon()
		but.actionCommand = key
		but.toolTipText = Messages.getString(key)
		but.addActionListener(Listener.getInstance())

		return but
	}

	// FIXME: This should not be necessary, this should go!!
	fun refreshConfigsCombo() {
		configsCombo.updateUI()
	}

	// FIXME: This should not be necessary, this should go!!
	fun validateConfigsComboSelection() {
		// Make sure the JCombo on the main toolbar wasn't selecting what we just deleted
		if (configsCombo.selectedIndex >= configsCombo.itemCount || configsCombo.selectedIndex < 0) {
			configsCombo.selectedIndex = 0
		}
	}

	fun setEventButtonSelected(selected: Boolean) {
		eventButton.isSelected = selected
	}
}