/*
 * Copyright (C) 2007 IsmAvatar <IsmAvatar@gmail.com>
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.components.impl;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class CustomFileFilter extends FileFilter implements FilenameFilter {
	protected ArrayList<String> ext = new ArrayList<>();
	private String desc;

	public CustomFileFilter(String desc, String... ext) {
		this.desc = desc;
		Collections.addAll(this.ext, ext);
	}

	/**
	 * Gets the extension part of the given filename, including the period
	 *
	 * @param filename
	 * @return the extension, including period
	 */
	public static String getExtension(String filename) {
		int p = filename.lastIndexOf(".");
		if (p == -1) return null;
		return filename.substring(p).toLowerCase(Locale.ENGLISH);
	}

	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		return accept(f, f.getPath());
	}

	public boolean accept(File dir, String name) {
		if (ext.size() == 0) return true;
		//if (f.isDirectory()) return true;
		String s = getExtension(name);
		if (s == null) return false;
		return ext.contains("*" + s);
	}

	public String getDescription() {
		return desc;
	}

	public String[] getExtensions() {
		return ext.toArray(new String[0]);
	}
}
