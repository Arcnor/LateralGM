/*
 * Copyright (C) 2008, 2009 Quadduc <quadduc@gmail.com>
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.main;

import org.lateralgm.util.SetTraverser;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class UpdateSource {
	public final Object owner;
	private final HardListenerTraverser hardTraverser;
	private final WeakListenerTraverser weakTraverser;
	private WeakHashMap<UpdateListener, WeakReference<UpdateListener>> weakReferences;

	public UpdateSource(Object owner, UpdateTrigger t) {
		t.setSource(this);
		this.owner = owner;
		hardTraverser = new HardListenerTraverser();
		weakTraverser = new WeakListenerTraverser();
	}

	public void addListener(UpdateListener l, boolean weak) {
		if (weak) {
			if (weakReferences == null)
				weakReferences = new WeakHashMap<UpdateListener, WeakReference<UpdateListener>>();
			else if (weakReferences.containsKey(l)) return;
			WeakReference<UpdateListener> r = new WeakReference<UpdateListener>(l);
			weakReferences.put(l, r);
			weakTraverser.add(r);
		} else
			hardTraverser.add(l);
	}

	public void addListener(UpdateListener l) {
		addListener(l, true);
	}

	public void removeListener(UpdateListener l) {
		if (weakReferences != null) {
			WeakReference<UpdateListener> r = weakReferences.remove(l);
			if (r != null) weakTraverser.remove(r);
		}
		hardTraverser.remove(l);
	}

	private void fireUpdate(UpdateEvent e) {
		weakTraverser.traverse(e);
		hardTraverser.traverse(e);
	}

	public interface UpdateListener {
		void updated(UpdateEvent e);
	}

	public static class UpdateEvent {
		public final UpdateSource source;
		public final UpdateEvent cause;

		public UpdateEvent(UpdateSource s) {
			this(s, null);
		}

		public UpdateEvent(UpdateSource s, UpdateEvent e) {
			source = s;
			cause = e;
		}
	}

	public static class UpdateTrigger {
		private UpdateSource source;
		private UpdateEvent event;

		private void setSource(UpdateSource s) {
			if (source != null) throw new IllegalStateException();
			source = s;
		}

		public void fire() {
			if (event == null) event = new UpdateEvent(source);
			source.fireUpdate(event);
		}

		public void fire(UpdateEvent e) {
			source.fireUpdate(e);
		}

		public UpdateEvent getEvent() {
			if (event == null) event = new UpdateEvent(source);
			return event;
		}
	}

	private static class HardListenerTraverser extends SetTraverser<UpdateListener, UpdateEvent> {
		@Override
		protected void visit(UpdateListener l, UpdateEvent e) {
			l.updated(e);
		}
	}

	private static class WeakListenerTraverser extends
			SetTraverser<WeakReference<UpdateListener>, UpdateEvent> {
		@Override
		protected void visit(WeakReference<UpdateListener> r, UpdateEvent e) {
			UpdateListener l = r.get();
			if (l == null)
				remove(r);
			else
				l.updated(e);
		}
	}

}
