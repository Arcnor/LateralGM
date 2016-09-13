/*
 * Copyright (C) 2007 Clam <clamisgood@gmail.com>
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.resources.sub;

import org.lateralgm.resources.library.LibAction;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionContainer {
	public List<Action> actions = new ArrayList<Action>();

	public Action addAction() {
		return addAction(null);
	}

	// adds an action set to the properties of given LibAction
	public Action addAction(LibAction libAction) {
		Action act = new Action(libAction);
		actions.add(act);
		return act;
	}
}
