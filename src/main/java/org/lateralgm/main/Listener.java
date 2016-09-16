/*
 * Copyright (C) 2007, 2008, 2009, 2010 IsmAvatar <IsmAvatar@gmail.com>
 * Copyright (C) 2007 TGMG <thegamemakerguru@gmail.com>
 * Copyright (C) 2007, 2008 Clam <clamisgood@gmail.com>
 * Copyright (C) 2008, 2009 Quadduc <quadduc@gmail.com>
 * Copyright (C) 2013, 2014 Robert B. Colton
 *
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.main;

import org.lateralgm.components.AboutBox;
import org.lateralgm.components.PackageResourcesDialog;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.ResourceList;
import org.lateralgm.messages.Messages;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.subframes.ConfigurationManager;
import org.lateralgm.util.PlatformHelper;
import org.lateralgm.util.UIHelper;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import static org.lateralgm.main.Util.deRef;

public class Listener extends TransferHandler implements ActionListener, CellEditorListener {
	private static final long serialVersionUID = 1L;
	public FileChooser fc = new FileChooser();
	MListener mListener = new MListener();

	private Listener() {
	}

	public static Listener getInstance() {
		return LazyHolder.INSTANCE;
	}

	public static void addResource(JTree tree, Class<?> r) {
		addResource(tree, r, null, null);
	}

	public static void addResource(JTree tree, Class<?> r, Resource<?, ?> res) {
		addResource(tree, r, res, null);
	}

	public static void addResource(JTree tree, Class<?> r, ResNode parent) {
		addResource(tree, r, null, parent);
	}

	protected static void addResource(JTree tree, Class<?> r, Resource<?, ?> res, ResNode parent) {
		// TODO: Java selection models allow for -1 meaning no selected item.
		// This can cause issues here when say a user presses the create script button
		// on the toolbar after having just deleted a resource.
		// The assumption is that addResource adds a resource to the trees primary node
		// that matches the resource kind, this is how it behaves in GM, and that
		// insertResource will insert a resource to the last selected path component.
		// This is why I am changing the behavior of this function, another possible solution
		// is to add a selection change listener to the tree to ensure that it is never negative.
		// - Robert B. Colton
		if (parent != null) {
			putNode(tree, parent, parent, r, parent.getChildCount(), res);
			return;
		}
		for (ResNode rn : LGM.root.getChildren()) {
			if (rn.status == ResNode.STATUS_PRIMARY && r == rn.kind) {
				putNode(tree, rn, rn, r, rn.getChildCount(), res);
				return;
			}
		}
	}

	public static void insertResource(JTree tree, Class<?> r) {
		insertResource(tree, r, null, (ResNode) tree.getLastSelectedPathComponent(), 0);
	}

	public static void insertResource(JTree tree, Class<?> r, Resource<?, ?> res) {
		insertResource(tree, r, res, (ResNode) tree.getLastSelectedPathComponent(), 0);
	}

	public static void insertResource(JTree tree, Class<?> r, ResNode node) {
		insertResource(tree, r, null, node, 0);
	}

	protected static void insertResource(JTree tree, Class<?> r, Resource<?, ?> res, ResNode node,
	                                     int offset) {
		if (node == null) addResource(tree, r, res, node);
		ResNode parent = (ResNode) node.getParent();
		if (parent == null) addResource(tree, r, res, node);
		if (parent.isRoot()) {
			addResource(tree, r, res, node);
			return;
		}
		int pos = parent.getIndex(node) + offset;
		putNode(tree, node, parent, r, pos, res);
	}

	public static void putNode(JTree tree, ResNode node, ResNode parent, Class<?> r, int pos,
	                           Resource<?, ?> res) {
		if (r == null) {
			String msg = Messages.getString("Listener.INPUT_GROUPNAME"); //$NON-NLS-1$
			String name = JOptionPane.showInputDialog(LGM.frame, msg,
					Messages.getString("Listener.DEFAULT_GROUPNAME")); //$NON-NLS-1$
			if (name == null || name.isEmpty()) return;
			ResNode g = new ResNode(name, ResNode.STATUS_GROUP, parent.kind);
			parent.insert(g, pos);
			tree.expandPath(new TreePath(parent.getPath()));
			tree.setSelectionPath(new TreePath(g.getPath()));
			tree.updateUI();
			return;
		}

		if (node.kind != r) {
			parent = getPrimaryParent(r);
			pos = parent.getChildCount();
		}

		Resource<?, ?> resource = res == null ? LGM.currentFile.resMap.get(parent.kind).getResource()
				: res;
		ResNode g = new ResNode(resource.getName(), ResNode.STATUS_SECONDARY, parent.kind,
				resource.reference);
		parent.insert(g, pos);
		tree.expandPath(new TreePath(parent.getPath()));
		tree.setSelectionPath(new TreePath(g.getPath()));
		tree.updateUI();
		g.openFrame(true);
	}

	/**
	 * Deletes the given resource nodes, including groups, and
	 * returns the index (row) of the last node that was deleted.
	 *
	 * @param resources An array of resource nodes to delete.
	 */
	protected static int deleteResources(ResNode[] resources, JTree tree) {
		HashSet<Resource<?, ?>> rs = new HashSet<Resource<?, ?>>();
		int last = -1;
		for (ResNode resource : resources) {
			if (resource == null) {
				continue;
			}
			if (resource.status == ResNode.STATUS_SECONDARY) {
				if (resource.frame != null) resource.frame.dispose();
				Resource<?, ?> res = deRef((ResourceReference<?>) resource.getRes());
				if (res != null) {
					rs.add(res);
					((ResourceList<?>) LGM.currentFile.resMap.get(resource.kind)).remove(res);
				}
				last = tree.getRowForPath(new TreePath(resource));
			} else if (resource.status == ResNode.STATUS_GROUP) {
				final Vector<ResNode> children = resource.getChildren();
				if (children != null) {
					deleteResources(children.toArray(new ResNode[children.size()]), tree);
				}
				resource.removeFromParent();
				last = tree.getRowForPath(new TreePath(resource));
			}
		}
		for (Resource<?, ?> r : rs)
			r.dispose();
		return last;
	}

	protected static void deleteSelectedResources(JTree tree) {
		String msg = Messages.getString("Listener.CONFIRM_DELETERESOURCE"); //$NON-NLS-1$
		if (JOptionPane.showConfirmDialog(LGM.frame, msg,
				Messages.getString("Listener.CONFIRM_DELETERESOURCE_TITLE"), //$NON-NLS-1$
				JOptionPane.YES_NO_OPTION) == 0) {
			TreePath[] selections = tree.getSelectionPaths();

			//NOTE: Must be obtained before the for loop deletes the path.
			int row = -1;

			ResNode[] resources = new ResNode[selections.length];
			for (int i = 0; i < selections.length; i++) {
				resources[i] = (ResNode)selections[i].getLastPathComponent();

			}
			row = deleteResources(resources, tree);

			if (row != -1) {
				tree.setSelectionPath(tree.getNextMatch("", row, Position.Bias.Forward));
			}
			tree.updateUI();
		}
	}

	public static ResNode getPrimaryParent(Class<?> kind) {
		for (int i = 0; i < LGM.root.getChildCount(); i++)
			if (((ResNode) LGM.root.getChildAt(i)).kind == kind) return (ResNode) LGM.root.getChildAt(i);
		return null;
	}

	public static void sortNodeChildrenAlphabetically(DefaultMutableTreeNode node, boolean recursive) {
		ArrayList<DefaultMutableTreeNode> children = (ArrayList<DefaultMutableTreeNode>) Collections.list(node.children());
		Comparator<DefaultMutableTreeNode> comp = new Comparator<DefaultMutableTreeNode>() {
			public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
				return o1.toString().compareTo(o2.toString());
			}
		};
		Collections.sort(children, comp);
		node.removeAllChildren();
		Iterator<DefaultMutableTreeNode> childrenIterator = children.iterator();
		while (childrenIterator.hasNext()) {
			DefaultMutableTreeNode child = childrenIterator.next();
			node.add(child);
			if (recursive && child.getChildCount() > 0) {
				sortNodeChildrenAlphabetically(child, recursive);
			}
		}
	}

	public DefaultMutableTreeNode findNode(DefaultMutableTreeNode parent, String name,
	                                       boolean recursive) {
		Enumeration<DefaultMutableTreeNode> enumeration =
				recursive ? parent.preorderEnumeration() : parent.children();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode child = enumeration.nextElement();
			if (child.toString().equals(name)) return child;
		}

		return null;
	}

	public void actionPerformed(ActionEvent e) {
		JTree tree = LGM.tree;
		TreePath treepath = tree.getSelectionPath();
		ResNode node = null;
		if (treepath != null) {
			node = (ResNode) treepath.getLastPathComponent();
		}

		String[] args = e.getActionCommand().split(" ");
		String com = args[0];
		String[] parts = com.split("\\.");
		final String command = parts[1];
		switch (command) {
			case "NEW": {
				final String title = Messages.getString("Listener.CONFIRM_NEW_TITLE");
				final String message = Messages.getString("Listener.CONFIRM_NEW");

				final UIHelper.DialogAction result = UIHelper.showMessageDialog(
						LGM.frame, UIHelper.DialogType.CONFIRMATION, UIHelper.DialogAction.YES_NO_SET,
						title, message, null, UIHelper.DialogAction.NO
				);
				if (result == UIHelper.DialogAction.YES) {
					fc.newFile();
				}
				return;
			}
			case "OPEN":
				try {
					fc.open(args.length > 1 ? new URI(args[1]) : null);
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				return;
			case "CLEARRECENT":
				PrefsStore.clearRecentFiles();
				return;
			case "OPENRECENT":
				try {
					fc.open(new File(new URI(args[1])));
				} catch (URISyntaxException e1) {
					LGM.showDefaultExceptionHandler(e1);
				}
				return;
			case "SAVE":
				fc.save(LGM.currentFile.uri, LGM.currentFile.format);
				return;
			case "EXPLORELATERALGM": {
				String userDir = System.getProperty("user.dir");
				if (userDir == null) {
					return;
				}
				try {
					PlatformHelper.openFile(new File(userDir));
				} catch (IOException e1) {
					LGM.showDefaultExceptionHandler(e1);
				}
				break;
			}
			case "EXPLOREPROJECT": {
				String userDir = LGM.currentFile.getDirectory();
				if (userDir == null) {
					return;
				}
				File f = new File(userDir);
				if (!f.exists()) {
					f = new File(System.getProperty("user.dir"));
				}
				try {
					PlatformHelper.openFile(f);
				} catch (IOException e1) {
					LGM.showDefaultExceptionHandler(e1);
				}
				break;
			}
			case "PREFERENCES":
				LGM.showPreferences();
				return;
			case "CST":
				LGM.showConstantsFrame(LGM.currentFile.defaultConstants);
				return;
			case "GMI":
				LGM.showGameInformation();
				return;
			case "GMS":
				LGM.showGameSettings(LGM.getSelectedConfig());
				return;
			case "PKG":
				LGM.showExtensionPackages();
				return;
			case "SAVEAS":
				fc.saveNewFile();
				return;
			case "EVENT_BUTTON":
				Object o = e.getSource();
				if (o instanceof JToggleButton) LGM.showEventPanel();
				return;
			case "EXIT":
				LGM.onMainFrameClosed();

				return;
			case "RENAME":
				if (tree.getCellEditor().isCellEditable(null))
					tree.startEditingAtPath(tree.getLeadSelectionPath());
				return;
			case "DELETE":
				deleteSelectedResources(tree);
				return;
			case "PACKAGE":
				PackageResourcesDialog.getInstance().setVisible(true);
				break;
			case "DEFRAGIDS":
				final String title = Messages.getString("Listener.CONFIRM_DEFRAGIDS_TITLE");
				final String message = Messages.getString("Listener.CONFIRM_DEFRAGIDS");

				final UIHelper.DialogAction result = UIHelper.showMessageDialog(
						LGM.frame, UIHelper.DialogType.WARNING, UIHelper.DialogAction.YES_NO_SET,
						title, message, null, UIHelper.DialogAction.NO
				);
				if (result == UIHelper.DialogAction.YES) {
					LGM.currentFile.defragIds();
				}
				break;
			case "EXPAND":
				for (int m = 0; m < tree.getRowCount(); m++)
					tree.expandRow(m);
				return;
			case "COLLAPSE":
				for (int m = tree.getRowCount() - 1; m >= 0; m--)
					tree.collapseRow(m);
				return;
			case "SORT":
				if (node == null) {
					return;
				}
				sortNodeChildrenAlphabetically(node, false);
				LGM.tree.expandPath(new TreePath(node.getPath()));
				LGM.tree.updateUI();
				return;
			case "DOCUMENTATION":
			case "WEBSITE":
			case "COMMUNITY":
			case "ISSUE":   //$NON-NLS-2$
				String uri = Prefs.documentationURI;
				if (command.equals("WEBSITE")) {
					uri = Prefs.websiteURI;
				} else if (command.equals("COMMUNITY")) {
					uri = Prefs.communityURI;
				} else if (command.equals("ISSUE")) {
					uri = Prefs.issueURI;
				}
				//uri = uri.replace('\\','/').replace(" ","%20");
				try {
					PlatformHelper.showDocumentation(new URI(uri));
				} catch (URISyntaxException e1) {
					UIHelper.showMessageDialog(LGM.frame,
							UIHelper.DialogType.ERROR,
							UIHelper.DialogAction.OK_SET,
							Messages.getString("HelpDialog.MALFORMED_TITLE"),
							Messages.format("HelpDialog.MALFORMED_MESSAGE", uri));
				} catch (IOException ioe) {
					UIHelper.showMessageDialog(LGM.frame,
							UIHelper.DialogType.ERROR,
							UIHelper.DialogAction.OK_SET,
							Messages.getString("HelpDialog.UNAVAILABLE_TITLE"),
							Messages.format("HelpDialog.UNAVAILABLE_MESSAGE", uri));
				}
				return;
			case "CONFIG_MANAGE":
				ConfigurationManager.getInstance().setVisible(true);
				return;
			case "ABOUT":
				new AboutBox(LGM.frame).setVisible(true);
				return;
			case "DUPLICATE":
				if (node == null) {
					return;
				}
				//NOTE: This check is needed because the user may select some non-instantiable resource in the tree
				//then mistakenly hit the Duplicate option under the Edit menu.
				if (!node.isInstantiable()) return;
				ResourceList<?> rl = (ResourceList<?>) LGM.currentFile.resMap.get(node.kind);
				if (node.frame != null) node.frame.commitChanges();
				Resource<?, ?> resource = rl.duplicate(node.getRes().get());
				Listener.insertResource(tree, node.kind, resource, node, 1);
				return;
			case "PROPERTIES":
				if (node == null) {
					return;
				}
				if (node.status == ResNode.STATUS_SECONDARY) node.openFrame();
				return;
			case "FIND":
				String name = JOptionPane.showInputDialog(LGM.frame,
						Messages.getString("FindResourceDialog.MESSAGE"),
						Messages.getString("FindResourceDialog.TITLE"),
						JOptionPane.PLAIN_MESSAGE);
				if (name != null) {
					// find the resource with the name
					DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
					ResNode resn = (ResNode) findNode(root, name, true);
					if (resn != null) {
						tree.expandPath(new TreePath(resn.getPath()));
						tree.setSelectionPath(new TreePath(resn.getPath()));
						tree.updateUI();
						if (resn.status == ResNode.STATUS_SECONDARY) resn.openFrame();
					} else {
						JOptionPane.showMessageDialog(LGM.frame, "Resource not found: " + name);
					}
				}
				return;
			default:
				if (com.contains(".INSERT_") || com.contains(".ADD_")) {  //$NON-NLS-2$
					if (com.endsWith("GROUP")) {
						if (com.contains(".INSERT_"))
							insertResource(tree, null);
						else
							addResource(tree, null);
						return;
					}
					//we no longer do it this way for resources
					throw new UnsupportedOperationException(com);
				}
				break;
		}
	}

	protected Transferable createTransferable(JComponent c) {
		ResNode n = (ResNode) ((JTree) c).getLastSelectedPathComponent();

		if (n.status == ResNode.STATUS_PRIMARY) return null;
		if (!n.isInstantiable()) return null;
		return n;
	}

	public int getSourceActions(JComponent c) {
		return MOVE;
	}

	// Fails if the drag node can not be dropped on the drop node.
	public boolean canImportNode(ResNode dragNode, ResNode dropNode) {
		if (dragNode == dropNode) return false;
		if (Prefs.groupKind && dropNode.kind != dragNode.kind) return false;
		return true;
	}

	// Checks if data can be imported, fails if only one of the nodes can not be
	// imported.
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDataFlavorSupported(ResNode.NODE_FLAVOR)) return false;
		// the above method uses equals(), which does not work as expected
		for (DataFlavor f : support.getDataFlavors())
			if (f != ResNode.NODE_FLAVOR) return false;
		TreePath drop = ((JTree.DropLocation) support.getDropLocation()).getPath();
		if (drop == null) return false;
		ResNode dropNode = (ResNode) drop.getLastPathComponent();
		if (dropNode.status == ResNode.STATUS_SECONDARY) return false;

		// Check each of the nodes being dragged.
		TreePath[] paths = ((JTree) support.getComponent()).getSelectionPaths();
		for (TreePath path : paths) {
			ResNode dragNode = (ResNode) path.getLastPathComponent();
			if (!canImportNode(dragNode, dropNode)) {
				return false;
			} else {
				continue;
			}
		}

		return true;
	}

	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) return false;
		JTree.DropLocation drop = (JTree.DropLocation) support.getDropLocation();
		ResNode dropNode = (ResNode) drop.getPath().getLastPathComponent();
		TreePath[] paths = ((JTree) support.getComponent()).getSelectionPaths();
		DefaultTreeModel model = (DefaultTreeModel) ((JTree) support.getComponent()).getModel();
		int ancestors = 0;
		ArrayList<ResNode> nodes = new ArrayList<ResNode>(paths.length);

		// Collect all of the nodes being dragged and remove them from the tree, also
		// keeping a count of how many of them will not be changing parents.
		for (TreePath treePath : paths) {
			ResNode dragNode = (ResNode) treePath.getLastPathComponent();
			if (dropNode == dragNode.getParent()) ancestors++;
			nodes.add(dragNode);
			model.removeNodeFromParent(dragNode);
		}

		int dropIndex = drop.getChildIndex();
		// If we are dropping directly onto a node append the dragged nodes to the very end.
		if (dropIndex == -1) {
			dropIndex = dropNode.getChildCount();
			// This keeps the selection order from being screwed up.
		} else if (dropIndex > dropNode.getChildCount() - ancestors) {
			dropIndex -= ancestors;
			if (dropIndex < 0) dropIndex = 0;
		}
		// Finally, reinsert all of the dragged nodes.
		for (ResNode dragNode : nodes) {
			dropNode.insert(dragNode, dropIndex++);
		}

		LGM.tree.expandPath(new TreePath(dropNode.getPath()));
		LGM.tree.updateUI();
		return true;
	}

	public void editingCanceled(ChangeEvent e) { //Unused
	}

	public void editingStopped(ChangeEvent e) {
		ResNode node = (ResNode) LGM.tree.getLastSelectedPathComponent();
		if (node.status == ResNode.STATUS_SECONDARY && node.isEditable()) {
			String txt = ((String) node.getUserObject()).replaceAll("\\W", "").replaceAll("^([0-9]+)", "");  //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Resource<?, ?> r = deRef((ResourceReference<?>) node.getRes());
			if (r != null) r.setName(txt);
		}
	}

	private static final class LazyHolder {
		public static final Listener INSTANCE = new Listener();
	}

	public static class ResourceAdder implements ActionListener {
		public boolean insert;
		public Class<?> k;

		public ResourceAdder(boolean insert, Class<?> k) {
			this.insert = insert;
			this.k = k;
		}

		public void actionPerformed(ActionEvent e) {
			if (insert)
				Listener.insertResource(LGM.tree, k);
			else
				Listener.addResource(LGM.tree, k);
		}
	}

	public static class NodeMenuListener implements ActionListener {
		ResNode node;

		public NodeMenuListener(ResNode node) {
			this.node = node;
		}

		public void actionPerformed(ActionEvent e) {
			JTree tree = LGM.tree;
			String com = e.getActionCommand();
			switch (com) {
				case "Listener.TREE_PROPERTIES":
					if (node.status == ResNode.STATUS_SECONDARY) node.openFrame();
					return;
				case "Listener.TREE_SORT":
					sortNodeChildrenAlphabetically(node, false);
					LGM.tree.expandPath(new TreePath(node.getPath()));
					LGM.tree.updateUI();
					return;
				case "Listener.TREE_DELETE":
					deleteSelectedResources(tree);
					return;
				case "Listener.TREE_RENAME":
					if (tree.getCellEditor().isCellEditable(null))
						tree.startEditingAtPath(tree.getLeadSelectionPath());
					return;
				case "Listener.TREE_CREATE_RESOURCE":
					addResource(tree, node.kind, node);
					return;
				case "Listener.TREE_CREATE_GROUP":
					addResource(tree, null, node);
					return;
				case "Listener.TREE_INSERT_RESOURCE":
					insertResource(tree, node.kind, node);
					return;
				case "Listener.TREE_INSERT_GROUP":
					insertResource(tree, null, node);
					return;
				case "Listener.TREE_DUPLICATE_RESOURCE":
					ResourceList<?> rl = (ResourceList<?>) LGM.currentFile.resMap.get(node.kind);
					if (node.frame != null) node.frame.commitChanges();
					Resource<?, ?> resource = rl.duplicate(node.getRes().get());
					Listener.insertResource(tree, node.kind, resource, node, 1);
					return;
			}
		}
	}

	private static class MListener extends MouseAdapter {
		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getSource() != LGM.tree) return;
			TreePath path = LGM.tree.getPathForLocation(e.getX(), e.getY());
			if (path == null) return;

			//Check to see if we have clicked on a different node then the one
			//currently selected.
			TreePath[] paths = LGM.tree.getSelectionPaths();
			boolean inpath = false;

			if (paths != null) {
				for (TreePath path1 : paths) {
					if (path1.equals(path)) {
						inpath = true;
					}
				}
			}

			if (e.getModifiers() == InputEvent.BUTTON1_MASK && inpath) {
				LGM.tree.setSelectionPath(path);
			}

			ResNode node = (ResNode) path.getLastPathComponent();
			if (node == null) return;
			//Isn't Java supposed to handle ctrl+click for us? For some reason it doesn't.
			if (e.getModifiers() == InputEvent.BUTTON3_MASK && e.getClickCount() == 1) {
				// Yes the right click button does change the selection,
				// go ahead and experiment with Eclipse, CodeBlocks, Visual Studio
				// or Qt. Swing's default component popup listener does not do this
				// indicating it is an inconsistency with the framework compared to
				// other GUI libraries.
				if (!inpath) {
					LGM.tree.setSelectionPath(path);
				}
				node.showMenu(e);
				return;
			}
			if (e.getModifiers() == InputEvent.BUTTON1_MASK && e.getClickCount() == 2) {
				// kind must be a Resource kind
				if (node.status != ResNode.STATUS_SECONDARY) return;
				node.openFrame();
				return;
			}
		}
	}
}
