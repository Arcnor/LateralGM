/*
 * Copyright (C) 2006 IsmAvatar <IsmAvatar@gmail.com>
 * Copyright (C) 2007 Quadduc <quadduc@gmail.com>
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.components;

import org.lateralgm.main.LGM;
import org.lateralgm.main.Listener;
import org.lateralgm.messages.Messages;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.ActionListener;

import static org.lateralgm.components.GmMenuBar.setTextAndAlt;

public class GmMenu extends JMenu {
	private static final long serialVersionUID = 1L;

	public GmMenu(String s) {
		super();
		setTextAndAlt(this, s);
	}

	public GmMenu() {
		// TODO Auto-generated constructor stub
	}

	public JMenuItem addItem(String key) {
		return addItem(key, Listener.getInstance());
	}

	public JMenuItem addItem(String key, ActionListener listener) {
		JMenuItem item = new JMenuItem();
		if (key != null) {
			setTextAndAlt(item, Messages.getString(key));
			item.setIcon(LGM.getIconForKey(key));
			item.setActionCommand(key);
		}
		item.setAccelerator(KeyStroke.getKeyStroke(Messages.getKeyboardString(key)));
		item.addActionListener(listener);
		add(item);
		return item;
	}

	public GmMenu addMenu(String key) {
		return addMenu(key, Listener.getInstance());
	}

	public GmMenu addMenu(String key, ActionListener listener) {
		GmMenu item = new GmMenu();
		if (key != null) {
			setTextAndAlt(item, Messages.getString(key));
			item.setIcon(LGM.getIconForKey(key));
			item.setActionCommand(key);
		}
		//item.setAccelerator(KeyStroke.getKeyStroke(Messages.getKeyboardString(key)));
		item.addActionListener(listener);
		add(item);
		return item;
	}
}
