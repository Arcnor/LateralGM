/*
 * Copyright (C) 2007 Clam <clamisgood@gmail.com>
 * Copyright (C) 2014 Robert B. Colton
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.components;

import org.lateralgm.messages.Messages;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class CustomFileChooser extends JFileChooser {
	private static final long serialVersionUID = 1L;
	private Preferences prefs;
	private String propertyName;

	public CustomFileChooser(String node, String propertyName) {
		this.propertyName = propertyName;
		prefs = Preferences.userRoot().node(node);
		setCurrentDirectory(new File(prefs.get(propertyName, getCurrentDirectory().getAbsolutePath())));
	}

	public boolean getFileExists() {
		boolean fileExists = false;
		if (this.isMultiSelectionEnabled()) {
			for (File f : this.getSelectedFiles()) {
				if (f.exists()) {
					fileExists = true;
					break;
				}
			}
		} else {
			fileExists = this.getSelectedFile().exists();
		}
		return fileExists;
	}

	@Override
	public void approveSelection() {
		if (this.getDialogType() == JFileChooser.OPEN_DIALOG) {
			boolean fileExists = getFileExists();
			if (!fileExists) {
				JOptionPane.showMessageDialog(this,
						Messages.getString("FileChooser.NOT_FOUND_MESSAGE"),
						Messages.getString("FileChooser.NOT_FOUND_TITLE"),
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		} else if (this.getDialogType() == JFileChooser.SAVE_DIALOG) {
			boolean fileExists = getFileExists();
			if (fileExists) {
				if (JOptionPane.showConfirmDialog(this,
						Messages.getString("FileChooser.CONFIRM_OVERWRITE_MESSAGE"),
						Messages.getString("FileChooser.CONFIRM_OVERWRITE_TITLE"),
						JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
		}
		super.approveSelection();
		saveDir();
	}

	@Override
	public void cancelSelection() {
		super.cancelSelection();
		saveDir();
	}

	private void saveDir() {
		prefs.put(propertyName, getCurrentDirectory().getAbsolutePath());
	}

	/**
	 * Sets the given <code>FilterSet</code> to be the current set
	 * of chooseable file filters. The first item in the list will be set as
	 * the currently selected filter.
	 *
	 * @param fs The list of filters to use
	 */
	public void setFilterSet(List<FileFilter> fs) {
		if (fs == null) throw new IllegalArgumentException("null FilterSet");
		resetChoosableFileFilters();
		fs.forEach(this::addChoosableFileFilter);
		if (fs.size() > 0) setFileFilter(fs.get(0));
	}
}
