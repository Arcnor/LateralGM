/*
 * Copyright (C) 2008 Quadduc <quadduc@gmail.com>
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.resources;

import org.lateralgm.main.UpdateSource;
import org.lateralgm.main.UpdateSource.UpdateTrigger;

import java.lang.ref.WeakReference;

public final class ResourceReference<R extends Resource<R, ?>> {
	final UpdateTrigger updateTrigger = new UpdateTrigger();
	public final UpdateSource updateSource = new UpdateSource(this, updateTrigger);
	private WeakReference<R> reference;

	ResourceReference(R resource) {
		set(resource);
	}

	void set(R resource) {
		reference = new WeakReference<R>(resource);
		updateTrigger.fire();
	}

	public R get() {
		return reference.get();
	}
}
