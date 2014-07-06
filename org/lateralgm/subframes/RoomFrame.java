/*
 * Copyright (C) 2007, 2008, 2010, 2011 IsmAvatar <IsmAvatar@gmail.com>
 * Copyright (C) 2007, 2008 Clam <clamisgood@gmail.com>
 * Copyright (C) 2008, 2009 Quadduc <quadduc@gmail.com>
 * Copyright (C) 2013, 2014 Robert B. Colton
 * Copyright (C) 2014, egofree
 * 
 * This file is part of LateralGM.
 * LateralGM is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.lateralgm.subframes;

import static java.lang.Integer.MAX_VALUE;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.lateralgm.main.Util.deRef;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.lateralgm.components.ColorSelect;
import org.lateralgm.components.NumberField;
import org.lateralgm.components.ResourceMenu;
import org.lateralgm.components.impl.EditorScrollPane;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.components.visual.RoomEditor;
import org.lateralgm.components.visual.RoomEditor.CommandHandler;
import org.lateralgm.components.visual.RoomEditor.PRoomEditor;
import org.lateralgm.main.LGM;
import org.lateralgm.main.UpdateSource;
import org.lateralgm.main.UpdateSource.UpdateEvent;
import org.lateralgm.main.UpdateSource.UpdateListener;
import org.lateralgm.messages.Messages;
import org.lateralgm.resources.Background;
import org.lateralgm.resources.Background.PBackground;
import org.lateralgm.resources.GmObject;
import org.lateralgm.resources.GmObject.PGmObject;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Room;
import org.lateralgm.resources.Room.PRoom;
import org.lateralgm.resources.Room.Piece;
import org.lateralgm.resources.Sprite;
import org.lateralgm.resources.sub.BackgroundDef;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Instance;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.Tile;
import org.lateralgm.resources.sub.Tile.PTile;
import org.lateralgm.resources.sub.View;
import org.lateralgm.resources.sub.View.PView;
import org.lateralgm.subframes.CodeFrame.CodeHolder;
import org.lateralgm.ui.swing.propertylink.ButtonModelLink;
import org.lateralgm.ui.swing.propertylink.FormattedLink;
import org.lateralgm.ui.swing.propertylink.PropertyLinkFactory;
import org.lateralgm.ui.swing.util.ArrayListModel;
import org.lateralgm.util.AddPieceInstance;
import org.lateralgm.util.MovePieceInstance;
import org.lateralgm.util.PropertyLink;
import org.lateralgm.util.PropertyMap.PropertyUpdateEvent;
import org.lateralgm.util.PropertyMap.PropertyUpdateListener;
import org.lateralgm.util.RemovePieceInstance;

public class RoomFrame extends InstantiableResourceFrame<Room,PRoom> implements ListSelectionListener,
		CommandHandler, UpdateListener, FocusListener, ChangeListener
	{
	private static final long serialVersionUID = 1L;
	private static final ImageIcon CODE_ICON = LGM.getIconForKey("RoomFrame.CODE"); //$NON-NLS-1$

	private final RoomEditor editor;
	private final EditorScrollPane editorPane;
	public final JTabbedPane tabs;
	public JLabel statX, statY, statId, statSrc;
	
	//ToolBar
	private JButton zoomIn, zoomOut, undo, redo;
	private JToggleButton gridVis;
	JToggleButton gridIso;
	
	//Objects
	public JCheckBox oUnderlying, oLocked;
	private ButtonModelLink<PInstance> loLocked;
	public JList<Instance> oList;
	private Instance lastObj = null; //non-guaranteed copy of oList.getLastSelectedValue()
	private JButton addObjectButton, deleteObjectButton;
	public ResourceMenu<GmObject> oNew, oSource;
	private PropertyLink<PInstance,ResourceReference<GmObject>> loSource;
	public NumberField objectHorizontalPosition, objectVerticalPosition;
	private FormattedLink<PInstance> loX, loY;
	private JButton oCreationCode;
	
	//Settings
	private JTextField sCaption;
	private JCheckBox sPersistent;
	private JButton sCreationCode, sShow;
	private JPopupMenu sShowMenu;
	public HashMap<CodeHolder,CodeFrame> codeFrames = new HashMap<CodeHolder,CodeFrame>();

	private JCheckBoxMenuItem sSObj, sSTile, sSBack, sSFore, sSView;
	//Tiles
	public JCheckBox tUnderlying, tLocked;
	private ButtonModelLink<PTile> ltLocked;
	public TileSelector tSelect;
	private JScrollPane tScroll;
	public JList<Tile> tList;
	private Tile lastTile = null; //non-guaranteed copy of tList.getLastSelectedValue()
	private JButton deleteTileButton;
	public ResourceMenu<Background> taSource, teSource;
	private PropertyLink<PTile,ResourceReference<Background>> ltSource;
	public NumberField tsX, tsY, tileHorizontalPosition, tileVerticalPosition, taDepth, teDepth;
	private FormattedLink<PTile> ltsX, ltsY, ltX, ltY, ltDepth;
	//Backgrounds
	private JCheckBox bDrawColor, bVisible, bForeground, bTileH, bTileV, bStretch;
	private ButtonModelLink<PBackgroundDef> lbVisible, lbForeground, lbTileH, lbTileV, lbStretch;
	private ColorSelect bColor;
	private JList<JLabel> bList;
	/**Guaranteed valid version of bList.getLastSelectedIndex()*/
	private int lastValidBack = -1;
	private ResourceMenu<Background> bSource;
	private PropertyLink<PBackgroundDef,ResourceReference<Background>> lbSource;
	private NumberField bX, bY, bH, bV;
	private FormattedLink<PBackgroundDef> lbX, lbY, lbH, lbV;
	private final BgDefPropertyListener bdpl = new BgDefPropertyListener();
	//Views
	private JCheckBox vEnabled, vVisible;
	private ButtonModelLink<PView> lvVisible;
	private JList<JLabel> vList;
	/**Guaranteed valid version of vList.getLastSelectedIndex()*/
	private int lastValidView = -1;
	private NumberField vRX, vRY, vRW, vRH;
	private NumberField vPX, vPY, vPW, vPH;
	private FormattedLink<PView> lvRX, lvRY, lvRW, lvRH, lvPX, lvPY, lvPW, lvPH;
	private ResourceMenu<GmObject> vObj;
	private PropertyLink<PView,ResourceReference<GmObject>> lvObj;
	private NumberField vOHBor, vOVBor, vOHSp, vOVSp;
	private FormattedLink<PView> lvOHBor, lvOVBor, lvOHSp, lvOVSp;
	private final ViewPropertyListener vpl = new ViewPropertyListener();

	private final PropertyLinkFactory<PRoomEditor> prelf;
	private JCheckBox vClear;

	// Undo system elements
  public UndoManager undoManager;     
  public UndoableEditSupport undoSupport;
	// Save the original position of a piece when starting to move an object (Used for the undo)
	private Point pieceOriginalPosition = null;
	// Used to record the select piece before losing the focus.
	public Piece selectedPiece = null;
	
	private JToolBar makeToolBar()
		{
		JToolBar tool = new JToolBar();
		tool.setFloatable(false);
		tool.add(save);
		tool.addSeparator();

		zoomIn = new JButton(LGM.getIconForKey("RoomFrame.ZOOM_IN")); //$NON-NLS-1$
		zoomIn.setToolTipText(Messages.getString("RoomFrame.ZOOM_IN"));
		prelf.make(zoomIn,PRoomEditor.ZOOM,1,RoomEditor.ZOOM_MAX);
		tool.add(zoomIn);
		zoomOut = new JButton(LGM.getIconForKey("RoomFrame.ZOOM_OUT")); //$NON-NLS-1$
		zoomOut.setToolTipText(Messages.getString("RoomFrame.ZOOM_OUT"));
		prelf.make(zoomOut,PRoomEditor.ZOOM,-1,RoomEditor.ZOOM_MIN);
		tool.add(zoomOut);
		tool.addSeparator();

		// Action fired when the undo button is clicked
    Action undoAction = new AbstractAction()
    	{
    	  private static final long serialVersionUID = 1L;
    	
				public void actionPerformed(ActionEvent actionEvent) 
    			{
        	undoManager.undo();
        	refreshUndoRedoButtons();
    			}
    	};

		undo = new JButton(LGM.getIconForKey("RoomFrame.UNDO"));
		undo.setToolTipText(Messages.getString("RoomFrame.UNDO"));
		// Bind the ctrl-z keystroke with the undo button
		KeyStroke ctrlZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK);
		undo.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ctrlZ, "ctrlZ");
		undo.getActionMap().put("ctrlZ", undoAction);
		undo.addActionListener(undoAction);
		tool.add(undo);
		
		// Action fired when the redo button is clicked
    Action redoAction = new AbstractAction()
    	{
  	    private static final long serialVersionUID = 1L;
  	  
				public void actionPerformed(ActionEvent actionEvent) 
    			{
      		undoManager.redo();
      		refreshUndoRedoButtons();
    			}
    	};
    	
		redo = new JButton(LGM.getIconForKey("RoomFrame.REDO"));
		redo.setToolTipText(Messages.getString("RoomFrame.REDO"));
		// Bind the ctrl-y keystroke with the redo button
		KeyStroke ctrlY = KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK);
		redo.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ctrlY, "ctrlY");
		redo.getActionMap().put("ctrlY", redoAction);
		redo.addActionListener(redoAction);
		tool.add(redo);
		tool.addSeparator();
		
		gridVis = new JToggleButton(LGM.getIconForKey("RoomFrame.GRID_VISIBLE"));
		gridVis.setToolTipText(Messages.getString("RoomFrame.GRID_VISIBLE"));
		prelf.make(gridVis,PRoomEditor.SHOW_GRID);
		tool.add(gridVis);
		gridIso = new JToggleButton(LGM.getIconForKey("RoomFrame.GRID_ISOMETRIC"));
		gridIso.setToolTipText(Messages.getString("RoomFrame.GRID_ISOMETRIC"));
		plf.make(gridIso,PRoom.ISOMETRIC);
		tool.add(gridIso);
		tool.addSeparator();
		
		// Add the grid sizers
		JLabel lab = new JLabel(Messages.getString("RoomFrame.GRID_X")); //$NON-NLS-1$
		NumberField nf = new NumberField(0,999);
		nf.setMaximumSize(nf.getPreferredSize());
		prelf.make(nf,PRoomEditor.GRID_OFFSET_X);
		tool.add(lab);
		tool.add(nf);

		lab = new JLabel(Messages.getString("RoomFrame.GRID_Y")); //$NON-NLS-1$
		nf = new NumberField(0,999);
		nf.setMaximumSize(nf.getPreferredSize());
		prelf.make(nf,PRoomEditor.GRID_OFFSET_Y);
		tool.add(lab);
		tool.add(nf);

		lab = new JLabel(Messages.getString("RoomFrame.GRID_W")); //$NON-NLS-1$
		nf = new NumberField(1,999);
		nf.setMaximumSize(nf.getPreferredSize());
		plf.make(nf,PRoom.SNAP_X);
		tool.add(lab);
		tool.add(nf);

		lab = new JLabel(Messages.getString("RoomFrame.GRID_H")); //$NON-NLS-1$
		nf = new NumberField(1,999);
		nf.setMaximumSize(nf.getPreferredSize());
		plf.make(nf,PRoom.SNAP_Y);
		tool.add(lab);
		tool.add(nf);

		tool.addSeparator();
		sShowMenu = makeShowMenu();
		sShow = new JButton(Messages.getString("RoomFrame.SHOW")); //$NON-NLS-1$
		sShow.addActionListener(this);
		tool.add(sShow);

		tool.addSeparator();

		return tool;
		}

	private static class ObjectListComponentRenderer implements ListCellRenderer<Instance>
		{
		private final JLabel lab = new JLabel();
		private final ListComponentRenderer lcr = new ListComponentRenderer();

		public ObjectListComponentRenderer()
			{
			lab.setOpaque(true);
			}

		public Component getListCellRendererComponent(JList<? extends Instance> list, Instance val, int ind,
				boolean selected, boolean focus)
			{
			Instance i = (Instance) val;
			ResourceReference<GmObject> ro = i.properties.get(PInstance.OBJECT);
			GmObject o = deRef(ro);
			String name = o == null ? Messages.getString("RoomFrame.NO_OBJECT") : o.getName();
			lcr.getListCellRendererComponent(list,lab,ind,selected,focus);
			lab.setText(name + " " + i.properties.get(PInstance.ID));
			ResNode rn = o == null ? null : o.getNode();
			lab.setIcon(rn == null ? null : rn.getIcon());
			return lab;
			}
		}

	private static class TileListComponentRenderer implements ListCellRenderer<Tile>
		{
		private final JLabel lab = new JLabel();
		private final TileIcon ti = new TileIcon();
		private final ListComponentRenderer lcr = new ListComponentRenderer();

		public TileListComponentRenderer()
			{
			lab.setOpaque(true);
			lab.setIcon(ti);
			}

		public Component getListCellRendererComponent(JList<? extends Tile> list, Tile val, int ind,
				boolean selected, boolean focus)
			{
			Tile t = (Tile) val;
			ResourceReference<Background> rb = t.properties.get(PTile.BACKGROUND);
			Background bg = deRef(rb);
			String name = bg == null ? Messages.getString("RoomFrame.NO_BACKGROUND") : bg.getName();
			lab.setText(name + " " + t.properties.get(PTile.ID));
			ti.tile = t;
			lcr.getListCellRendererComponent(list,lab,ind,selected,focus);
			return lab;
			}

		static class TileIcon implements Icon
			{
			Tile tile;

			public int getIconHeight()
				{
				return tile.getSize().height;
				}

			public int getIconWidth()
				{
				return tile.getSize().width;
				}

			public void paintIcon(Component c, Graphics g, int x, int y)
				{
				ResourceReference<Background> rb = tile.properties.get(PTile.BACKGROUND);
				Background bg = deRef(rb);
				BufferedImage bi = bg == null ? null : bg.getBackgroundImage();
				if (bi != null)
					{
					Point p = tile.getBackgroundPosition();
					Dimension d = tile.getSize();
					g.drawImage(bi,0,0,d.width,d.height,p.x,p.y,p.x + d.width,p.y + d.height,c);
					}
				}
			}
		}

	public JPanel makeObjectsPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		oNew = new ResourceMenu<GmObject>(GmObject.class,
				Messages.getString("RoomFrame.NO_OBJECT"),true,110); //$NON-NLS-1$
		oNew.addActionListener(this);
		oUnderlying = new JCheckBox(Messages.getString("RoomFrame.OBJ_UNDERLYING")); //$NON-NLS-1$
		prelf.make(oUnderlying,PRoomEditor.DELETE_UNDERLYING_OBJECTS);

		oList = new JList<Instance>(new ArrayListModel<Instance>(res.instances));
		oList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		oList.setVisibleRowCount(8);
		oList.setCellRenderer(new ObjectListComponentRenderer());
		oList.setSelectedIndex(0);
		oList.addListSelectionListener(this);
		JScrollPane sp = new JScrollPane(oList);
		addObjectButton = new JButton(Messages.getString("RoomFrame.OBJ_ADD")); //$NON-NLS-1$
		addObjectButton.addActionListener(this);
		deleteObjectButton = new JButton(Messages.getString("RoomFrame.OBJ_DELETE")); //$NON-NLS-1$
		deleteObjectButton.addActionListener(this);

		JPanel edit = new JPanel();
		String title = Messages.getString("RoomFrame.OBJ_INSTANCES"); //$NON-NLS-1$
		edit.setBorder(BorderFactory.createTitledBorder(title));
		GroupLayout layout2 = new GroupLayout(edit);
		layout2.setAutoCreateGaps(true);
		layout2.setAutoCreateContainerGaps(true);
		edit.setLayout(layout2);

		oSource = new ResourceMenu<GmObject>(GmObject.class,
				Messages.getString("RoomFrame.NO_OBJECT"),true,110); //$NON-NLS-1$
		
		oLocked = new JCheckBox(Messages.getString("RoomFrame.OBJ_LOCKED")); //$NON-NLS-1$
		oLocked.setHorizontalAlignment(JCheckBox.CENTER);
		JLabel lObjX = new JLabel(Messages.getString("RoomFrame.OBJ_X")); //$NON-NLS-1$
		objectHorizontalPosition = new NumberField(0);
		objectHorizontalPosition.setColumns(4);
		objectHorizontalPosition.addFocusListener(this);
		JLabel lObjY = new JLabel(Messages.getString("RoomFrame.OBJ_Y")); //$NON-NLS-1$
		objectVerticalPosition = new NumberField(0);
		objectVerticalPosition.setColumns(4);
		objectVerticalPosition.addFocusListener(this);
		oCreationCode = new JButton(Messages.getString("RoomFrame.OBJ_CODE")); //$NON-NLS-1$
		oCreationCode.setIcon(CODE_ICON);
		oCreationCode.addActionListener(this);

		layout2.setHorizontalGroup(layout2.createParallelGroup()
		/**/.addComponent(oSource)
		/**/.addComponent(oLocked)
		/**/.addGroup(layout2.createSequentialGroup()
		/*		*/.addComponent(lObjX)
		/*		*/.addComponent(objectHorizontalPosition)
		/*		*/.addComponent(lObjY)
		/*		*/.addComponent(objectVerticalPosition))
		/**/.addComponent(oCreationCode,DEFAULT_SIZE,DEFAULT_SIZE,MAX_VALUE));
		layout2.setVerticalGroup(layout2.createSequentialGroup()
		/**/.addComponent(oSource)
		/**/.addComponent(oLocked)
		/**/.addGroup(layout2.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lObjX)
		/*		*/.addComponent(objectHorizontalPosition)
		/*		*/.addComponent(lObjY)
		/*		*/.addComponent(objectVerticalPosition))
		/**/.addComponent(oCreationCode));

		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(oNew)
		/**/.addComponent(oUnderlying)
		/**/.addComponent(sp,DEFAULT_SIZE,120,MAX_VALUE)
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(addObjectButton,DEFAULT_SIZE,DEFAULT_SIZE,MAX_VALUE)
		/*		*/.addComponent(deleteObjectButton,DEFAULT_SIZE,DEFAULT_SIZE,MAX_VALUE))
		/**/.addComponent(edit));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(oNew)
		/**/.addComponent(oUnderlying)
		/**/.addComponent(sp)
		/**/.addGroup(layout.createParallelGroup()
		/*		*/.addComponent(addObjectButton)
		/*		*/.addComponent(deleteObjectButton))
		/**/.addComponent(edit));
		
		// Make sure the selected object in the list is activated
		fireObjUpdate();
		return panel;
		
		}

	private JPopupMenu makeShowMenu()
		{
		JPopupMenu showMenu = new JPopupMenu();
		String st = Messages.getString("RoomFrame.SHOW_OBJECTS"); //$NON-NLS-1$
		sSObj = new JCheckBoxMenuItem(st);
		prelf.make(sSObj,PRoomEditor.SHOW_OBJECTS);
		showMenu.add(sSObj);
		st = Messages.getString("RoomFrame.SHOW_TILES"); //$NON-NLS-1$
		sSTile = new JCheckBoxMenuItem(st);
		prelf.make(sSTile,PRoomEditor.SHOW_TILES);
		showMenu.add(sSTile);
		st = Messages.getString("RoomFrame.SHOW_BACKGROUNDS"); //$NON-NLS-1$
		sSBack = new JCheckBoxMenuItem(st);
		prelf.make(sSBack,PRoomEditor.SHOW_BACKGROUNDS);
		showMenu.add(sSBack);
		st = Messages.getString("RoomFrame.SHOW_FOREGROUNDS"); //$NON-NLS-1$
		sSFore = new JCheckBoxMenuItem(st);
		prelf.make(sSFore,PRoomEditor.SHOW_FOREGROUNDS);
		showMenu.add(sSFore);
		st = Messages.getString("RoomFrame.SHOW_VIEWS"); //$NON-NLS-1$
		sSView = new JCheckBoxMenuItem(st);
		prelf.make(sSView,PRoomEditor.SHOW_VIEWS);
		showMenu.add(sSView);
		return showMenu;
		}

	public JPanel makeSettingsPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		JLabel lName = new JLabel(Messages.getString("RoomFrame.NAME")); //$NON-NLS-1$

		JLabel lCaption = new JLabel(Messages.getString("RoomFrame.CAPTION")); //$NON-NLS-1$
		sCaption = new JTextField();
		plf.make(sCaption.getDocument(),PRoom.CAPTION);

		JLabel lWidth = new JLabel(Messages.getString("RoomFrame.WIDTH")); //$NON-NLS-1$
		NumberField sWidth = new NumberField(1,999999);
		plf.make(sWidth,PRoom.WIDTH);

		JLabel lHeight = new JLabel(Messages.getString("RoomFrame.HEIGHT")); //$NON-NLS-1$
		NumberField sHeight = new NumberField(1,999999);
		plf.make(sHeight,PRoom.HEIGHT);

		JLabel lSpeed = new JLabel(Messages.getString("RoomFrame.SPEED")); //$NON-NLS-1$
		NumberField sSpeed = new NumberField(1,9999);
		plf.make(sSpeed,PRoom.SPEED);

		String str = Messages.getString("RoomFrame.PERSISTENT"); //$NON-NLS-1$
		sPersistent = new JCheckBox(str);
		plf.make(sPersistent,PRoom.PERSISTENT);

		str = Messages.getString("RoomFrame.CREATION_CODE"); //$NON-NLS-1$
		sCreationCode = new JButton(str,CODE_ICON);
		sCreationCode.addActionListener(this);

		JPanel pg = makeGridPane();

		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(lName)
		/*		*/.addComponent(name,DEFAULT_SIZE,120,MAX_VALUE))
		/**/.addComponent(lCaption)
		/**/.addComponent(sCaption,DEFAULT_SIZE,120,MAX_VALUE)
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addGroup(layout.createParallelGroup(Alignment.TRAILING)
		/*				*/.addComponent(lWidth)
		/*				*/.addComponent(lHeight)
		/*				*/.addComponent(lSpeed))
		/*		*/.addGroup(layout.createParallelGroup()
		/*				*/.addComponent(sWidth)
		/*				*/.addComponent(sHeight)
		/*				*/.addComponent(sSpeed)))
		/**/.addComponent(sPersistent)
		/**/.addComponent(sCreationCode,DEFAULT_SIZE,DEFAULT_SIZE,MAX_VALUE)
		/**/.addComponent(pg));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lName)
		/*		*/.addComponent(name))
		/**/.addComponent(lCaption)
		/**/.addComponent(sCaption,DEFAULT_SIZE,DEFAULT_SIZE,PREFERRED_SIZE)
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lWidth)
		/*		*/.addComponent(sWidth))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lHeight)
		/*		*/.addComponent(sHeight))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lSpeed)
		/*		*/.addComponent(sSpeed))
		/**/.addComponent(sPersistent)
		/**/.addComponent(sCreationCode)
		/**/.addComponent(pg));
		return panel;
		}

	public JPanel makeGridPane()
		{
		JPanel pg = new JPanel();
		GroupLayout lr = new GroupLayout(pg);
		pg.setLayout(lr);
		pg.setBorder(BorderFactory.createTitledBorder(Messages.getString("RoomFrame.GRID"))); //$NON-NLS-1$

		JLabel lGX = new JLabel(Messages.getString("RoomFrame.GRID_X")); //$NON-NLS-1$
		NumberField sGX = new NumberField(0,999);
		prelf.make(sGX,PRoomEditor.GRID_OFFSET_X);
		JLabel lGY = new JLabel(Messages.getString("RoomFrame.GRID_Y")); //$NON-NLS-1$
		NumberField sGY = new NumberField(0,999);
		prelf.make(sGY,PRoomEditor.GRID_OFFSET_Y);
		JLabel lGW = new JLabel(Messages.getString("RoomFrame.GRID_W")); //$NON-NLS-1$
		NumberField sGW = new NumberField(1,999);
		plf.make(sGW,PRoom.SNAP_X);
		JLabel lGH = new JLabel(Messages.getString("RoomFrame.GRID_H")); //$NON-NLS-1$
		NumberField sGH = new NumberField(1,999);
		plf.make(sGH,PRoom.SNAP_Y);
		lr.setHorizontalGroup(lr.createSequentialGroup().addContainerGap()
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(lGX)
		/*		*/.addComponent(lGY)).addGap(4)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(sGX)
		/*		*/.addComponent(sGY)).addGap(8)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(lGW)
		/*		*/.addComponent(lGH)).addGap(4)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(sGW)
		/*		*/.addComponent(sGH)).addContainerGap());
		lr.setVerticalGroup(lr.createSequentialGroup().addGap(4)
		/**/.addGroup(lr.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lGX)
		/*		*/.addComponent(sGX)
		/*		*/.addComponent(lGW)
		/*		*/.addComponent(sGW)).addGap(4)
		/**/.addGroup(lr.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lGY)
		/*		*/.addComponent(sGY)
		/*		*/.addComponent(lGH)
		/*		*/.addComponent(sGH)).addGap(8));

		return pg;
		}

	public JTabbedPane makeTilesPane()
		{
		JTabbedPane tab = new JTabbedPane();
		tab.addTab(Messages.getString("RoomFrame.TILE_ADD"),makeTilesAddPane());
		tab.addTab(Messages.getString("RoomFrame.TILE_EDIT"),makeTilesEditPane());
		tab.addTab(Messages.getString("RoomFrame.TILE_BATCH"),makeTilesBatchPane());
		tab.setSelectedIndex(0);
		fireTileUpdate();
		return tab;
		}

	public JPanel makeTilesAddPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		taSource = new ResourceMenu<Background>(Background.class,
				Messages.getString("RoomFrame.NO_BACKGROUND"),true,110);
		taSource.addActionListener(this);
		tSelect = new TileSelector();
		tScroll = new JScrollPane(tSelect);
		tScroll.setPreferredSize(tScroll.getSize());
		tUnderlying = new JCheckBox(Messages.getString("RoomFrame.TILE_UNDERLYING")); //$NON-NLS-1$
		prelf.make(tUnderlying,PRoomEditor.DELETE_UNDERLYING_TILES);
		JLabel lab = new JLabel(Messages.getString("RoomFrame.TILE_LAYER"));
		taDepth = new NumberField(Integer.MIN_VALUE,Integer.MAX_VALUE,0);
		taDepth.setMaximumSize(new Dimension(Integer.MAX_VALUE,taDepth.getHeight()));

		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(tScroll)
		/**/.addComponent(taSource)
		/**/.addComponent(tUnderlying)
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addComponent(lab)
		/*	*/.addComponent(taDepth)));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(tScroll)
		/**/.addComponent(taSource)
		/**/.addComponent(tUnderlying)
		/**/.addGroup(layout.createParallelGroup()
		/*	*/.addComponent(lab)
		/*	*/.addComponent(taDepth)));

		return panel;
		}

	//XXX: Extract to own class?
	public static class TileSelector extends JLabel
		{
		private static final long serialVersionUID = 1L;
		public int tx, ty;
		private ResourceReference<Background> bkg;

		public TileSelector()
			{
			super();
			setVerticalAlignment(TOP);
			enableEvents(MouseEvent.MOUSE_PRESSED);
			enableEvents(MouseEvent.MOUSE_DRAGGED);
			}

		public void setBackground(ResourceReference<Background> bkg)
			{
			this.bkg = bkg;
			Background b = deRef(bkg);
			if (b == null)
				{
				setIcon(null);
				setPreferredSize(new Dimension(0,0));
				return;
				}
			setPreferredSize(new Dimension(b.getWidth(),b.getHeight()));
			BufferedImage bi = b.getDisplayImage();
			setIcon(bi == null ? null : new ImageIcon(bi));
			}

		public void paintComponent(Graphics g)
			{
			super.paintComponent(g);
			Background b = deRef(bkg);
			if (b == null) return;
			//BufferedImage img = bkg.getDisplayImage();
			//if (img == null) return;

			Shape oldClip = g.getClip(); //backup the old clip
			Rectangle oldc = g.getClipBounds();
			//Set the clip properly
			g.setClip(new Rectangle(oldc.x,oldc.y,Math.min(oldc.x + oldc.width,b.getWidth()) - oldc.x,
					Math.min(oldc.y + oldc.height,b.getHeight()) - oldc.y));

			if ((Boolean) b.get(PBackground.USE_AS_TILESET))
				{
				g.setXORMode(Color.BLACK);
				g.setColor(Color.WHITE);
				g.drawRect(tx,ty,(Integer) b.get(PBackground.TILE_WIDTH),
						(Integer) b.get(PBackground.TILE_HEIGHT));
				g.setPaintMode(); //just in case
				}
			g.setClip(oldClip); //restore the clip
			}

		protected void processMouseEvent(MouseEvent e)
			{
			if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1
					&& e.getX() < getPreferredSize().width && e.getY() < getPreferredSize().height)
				selectTile(e.getX(),e.getY());
			super.processMouseEvent(e);
			}

		protected void processMouseMotionEvent(MouseEvent e)
			{
			if (e.getID() == MouseEvent.MOUSE_DRAGGED
					&& (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
				selectTile(e.getX(),e.getY());
			super.processMouseMotionEvent(e);
			}

		public void selectTile(int x, int y)
			{
			Background hardBkg = deRef(bkg);
			if (hardBkg == null)
				{
				tx = x;
				ty = y;
				}
			else if (!(Boolean) hardBkg.get(PBackground.USE_AS_TILESET))
				{
				tx = 0;
				ty = 0;
				}
			else
				{
				int w = (Integer) hardBkg.get(PBackground.TILE_WIDTH)
						+ (Integer) hardBkg.get(PBackground.H_SEP);
				int h = (Integer) hardBkg.get(PBackground.TILE_HEIGHT)
						+ (Integer) hardBkg.get(PBackground.V_SEP);
				int ho = hardBkg.get(PBackground.H_OFFSET);
				int vo = hardBkg.get(PBackground.V_OFFSET);
				tx = (int) Math.floor((x - ho) / w) * w + ho;
				ty = (int) Math.floor((y - vo) / h) * h + vo;
				}
			repaint();
			}
		}

	public JPanel makeTilesEditPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		tList = new JList<Tile>(new ArrayListModel<Tile>(res.tiles));
		tList.addListSelectionListener(this);
		tList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tList.setCellRenderer(new TileListComponentRenderer());
		JScrollPane sp = new JScrollPane(tList);
		deleteTileButton = new JButton(Messages.getString("RoomFrame.TILE_DELETE")); //$NON-NLS-1$
		deleteTileButton.addActionListener(this);
		tLocked = new JCheckBox(Messages.getString("RoomFrame.TILE_LOCKED")); //$NON-NLS-1$

		JPanel pSet = new JPanel();
		pSet.setBorder(BorderFactory.createTitledBorder(Messages.getString("RoomFrame.TILESET"))); //$NON-NLS-1$
		GroupLayout psl = new GroupLayout(pSet);
		psl.setAutoCreateGaps(true);
		psl.setAutoCreateContainerGaps(true);
		pSet.setLayout(psl);
		teSource = new ResourceMenu<Background>(Background.class,
				Messages.getString("RoomFrame.NO_BACKGROUND"),true,110); //$NON-NLS-1$
		JLabel ltsx = new JLabel(Messages.getString("RoomFrame.TILESET_X")); //$NON-NLS-1$
		tsX = new NumberField(0);
		tsX.setColumns(4);
		JLabel ltsy = new JLabel(Messages.getString("RoomFrame.TILESET_Y")); //$NON-NLS-1$
		tsY = new NumberField(0);
		tsY.setColumns(4);
		psl.setHorizontalGroup(psl.createParallelGroup()
		/**/.addComponent(teSource)
		/**/.addGroup(psl.createSequentialGroup()
		/*		*/.addComponent(ltsx)
		/*		*/.addComponent(tsX)
		/*		*/.addComponent(ltsy)
		/*		*/.addComponent(tsY)));
		psl.setVerticalGroup(psl.createSequentialGroup()
		/**/.addComponent(teSource)
		/**/.addGroup(psl.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(ltsx)
		/*		*/.addComponent(tsX)
		/*		*/.addComponent(ltsy)
		/*		*/.addComponent(tsY)));

		JPanel pTile = new JPanel();
		pTile.setBorder(BorderFactory.createTitledBorder(Messages.getString("RoomFrame.TILE"))); //$NON-NLS-1$
		GroupLayout ptl = new GroupLayout(pTile);
		ptl.setAutoCreateGaps(true);
		ptl.setAutoCreateContainerGaps(true);
		pTile.setLayout(ptl);
		JLabel ltx = new JLabel(Messages.getString("RoomFrame.TILE_X")); //$NON-NLS-1$
		tileHorizontalPosition = new NumberField(0);
		tileHorizontalPosition.setColumns(4);
		tileHorizontalPosition.addFocusListener(this);
		JLabel lty = new JLabel(Messages.getString("RoomFrame.TILE_Y")); //$NON-NLS-1$
		tileVerticalPosition = new NumberField(0);
		tileVerticalPosition.setColumns(4);
		tileVerticalPosition.addFocusListener(this);
		JLabel ltl = new JLabel(Messages.getString("RoomFrame.TILE_LAYER")); //$NON-NLS-1$
		teDepth = new NumberField(1000000);
		teDepth.setColumns(8);
		ptl.setHorizontalGroup(ptl.createParallelGroup()
		/**/.addGroup(ptl.createSequentialGroup()
		/*		*/.addComponent(ltx)
		/*		*/.addComponent(tileHorizontalPosition)
		/*		*/.addComponent(lty)
		/*		*/.addComponent(tileVerticalPosition))
		/**/.addGroup(ptl.createSequentialGroup()
		/*		*/.addComponent(ltl)
		/*		*/.addComponent(teDepth)));
		ptl.setVerticalGroup(ptl.createSequentialGroup()
		/**/.addGroup(ptl.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(ltx)
		/*		*/.addComponent(tileHorizontalPosition)
		/*		*/.addComponent(lty)
		/*		*/.addComponent(tileVerticalPosition))
		/**/.addGroup(ptl.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(ltl)
		/*		*/.addComponent(teDepth)));

		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(sp,DEFAULT_SIZE,120,MAX_VALUE)
		/**/.addComponent(deleteTileButton,DEFAULT_SIZE,DEFAULT_SIZE,MAX_VALUE)
		/**/.addComponent(tLocked)
		/**/.addComponent(pSet)
		/**/.addComponent(pTile));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(sp,DEFAULT_SIZE,60,MAX_VALUE)
		/**/.addComponent(deleteTileButton)
		/**/.addComponent(tLocked)
		/**/.addComponent(pSet)
		/**/.addComponent(pTile));
		return panel;
		}

	//TODO 1.7?: Batch tile operations
	public static JPanel makeTilesBatchPane()
		{
		JPanel panel = new JPanel();
		//		GroupLayout layout = new GroupLayout(panel);
		//		layout.setAutoCreateGaps(true);
		//		layout.setAutoCreateContainerGaps(true);
		//		panel.setLayout(layout);
		panel.add(new JLabel("<html>This tab will offer ways to<br />"
				+ "perform batch operations on several<br />"
				+ "tiles at once, or regions of tiles.</html>"));

		return panel;
		}

	public JPanel makeBackgroundsPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		bDrawColor = new JCheckBox(Messages.getString("RoomFrame.DRAW_COLOR")); //$NON-NLS-1$
		plf.make(bDrawColor,PRoom.DRAW_BACKGROUND_COLOR);
		JLabel lColor = new JLabel(Messages.getString("RoomFrame.COLOR")); //$NON-NLS-1$
		bColor = new ColorSelect();
		plf.make(bColor,PRoom.BACKGROUND_COLOR);

		JLabel[] backLabs = new JLabel[res.backgroundDefs.size()];
		for (int i = 0; i < backLabs.length; i++)
			{
			backLabs[i] = new JLabel(" " + Messages.getString("RoomFrame.BACK") + i); //$NON-NLS-1$
			boolean v = res.backgroundDefs.get(i).properties.get(PBackgroundDef.VISIBLE);
			backLabs[i].setFont(backLabs[i].getFont().deriveFont(v ? Font.BOLD : Font.PLAIN));
			backLabs[i].setOpaque(true);
			}
		bList = new JList<JLabel>(backLabs);
		bList.setCellRenderer(new ListComponentRenderer());
		bList.addListSelectionListener(this);
		bList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bList.setVisibleRowCount(4);
		JScrollPane sp = new JScrollPane(bList);

		for (BackgroundDef d : res.backgroundDefs)
			d.properties.getUpdateSource(PBackgroundDef.VISIBLE).addListener(bdpl);

		bVisible = new JCheckBox(Messages.getString("RoomFrame.BACK_VISIBLE")); //$NON-NLS-1$
		bForeground = new JCheckBox(Messages.getString("RoomFrame.BACK_FOREGROUND")); //$NON-NLS-1$

		bSource = new ResourceMenu<Background>(Background.class,
				Messages.getString("RoomFrame.NO_BACKGROUND"),true,150); //$NON-NLS-1$

		bTileH = new JCheckBox(Messages.getString("RoomFrame.BACK_TILE_HOR")); //$NON-NLS-1$
		JLabel lbx = new JLabel(Messages.getString("RoomFrame.BACK_X")); //$NON-NLS-1$
		bX = new NumberField(0);
		bX.setColumns(4);
		bTileV = new JCheckBox(Messages.getString("RoomFrame.BACK_TILE_VERT")); //$NON-NLS-1$
		JLabel lby = new JLabel(Messages.getString("RoomFrame.BACK_Y")); //$NON-NLS-1$
		bY = new NumberField(0);
		bY.setColumns(4);
		bStretch = new JCheckBox(Messages.getString("RoomFrame.BACK_STRETCH")); //$NON-NLS-1$
		JLabel lbh = new JLabel(Messages.getString("RoomFrame.BACK_HSPEED")); //$NON-NLS-1$
		bH = new NumberField(-999,999);
		JLabel lbv = new JLabel(Messages.getString("RoomFrame.BACK_VSPEED")); //$NON-NLS-1$
		bV = new NumberField(-999,999);

		bList.setSelectedIndex(0);

		Insets spi = sp.getInsets();
		int spmh = bList.getMaximumSize().height + spi.bottom + spi.top;
		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(bDrawColor)
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(lColor)
		/*		*/.addComponent(bColor))
		/**/.addComponent(sp)
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(bVisible)
		/*		*/.addComponent(bForeground))
		/**/.addComponent(bSource)
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(lbx)
		/*		*/.addComponent(bX)
		/*		*/.addComponent(lby)
		/*		*/.addComponent(bY))
		/**/.addGroup(layout.createSequentialGroup()
		/*		*/.addGroup(layout.createParallelGroup(Alignment.TRAILING)
		/*				*/.addComponent(lbh)
		/*				*/.addComponent(lbv))
		/*		*/.addGroup(layout.createParallelGroup()
		/*				*/.addComponent(bH)
		/*				*/.addComponent(bV)))
		/**/.addComponent(bTileH)
		/**/.addComponent(bTileV)
		/**/.addComponent(bStretch));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(bDrawColor)
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE,false)
		/*		*/.addComponent(lColor)
		/*		*/.addComponent(bColor))
		/**/.addComponent(sp,DEFAULT_SIZE,DEFAULT_SIZE,spmh)
		/**/.addGroup(layout.createParallelGroup()
		/*		*/.addComponent(bVisible)
		/*		*/.addComponent(bForeground))
		/**/.addComponent(bSource)
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lbx)
		/*		*/.addComponent(bX)
		/*		*/.addComponent(lby)
		/*		*/.addComponent(bY))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lbh)
		/*		*/.addComponent(bH))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lbv)
		/*		*/.addComponent(bV))
		/**/.addComponent(bTileH)
		/**/.addComponent(bTileV)
		/**/.addComponent(bStretch));
		return panel;
		}
	
	public JPanel makePhysicsPane() {
		JPanel panel = new JPanel();
		
		JCheckBox phyWorldCB = new JCheckBox(Messages.getString("RoomFrame.PHY_WORLD_ENABLED"));
		plf.make(phyWorldCB,PRoom.PHYSICS_WORLD);
		
		JLabel pixMetersLabel = new JLabel(Messages.getString("RoomFrame.PHY_PIXELSPERMETER") + ":");
		NumberField pixMetersField = new NumberField(0.1000);
		plf.make(pixMetersField,PRoom.PHYSICS_PIXTOMETERS);
		pixMetersField.setColumns(16);
		
		JLabel gravityXLabel = new JLabel(Messages.getString("RoomFrame.PHY_GRAVITY_X") + ":");
		NumberField gravityXField = new NumberField(0.0);
		plf.make(gravityXField,PRoom.PHYSICS_GRAVITY_X);
		gravityXField.setColumns(16);
		
		JLabel gravityYLabel = new JLabel(Messages.getString("RoomFrame.PHY_GRAVITY_Y") + ":");
		NumberField gravityYField = new NumberField(10.0);
		plf.make(gravityYField,PRoom.PHYSICS_GRAVITY_Y);
		gravityYField.setColumns(16);
		
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);
		
		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(phyWorldCB)
		/**/.addGroup(layout.createSequentialGroup()
		/*  */.addComponent(pixMetersLabel)
		/*  */.addComponent(pixMetersField))
		/**/.addGroup(layout.createSequentialGroup()
		/*  */.addComponent(gravityXLabel)
		/*  */.addComponent(gravityXField))
		/**/.addGroup(layout.createSequentialGroup()
		/*  */.addComponent(gravityYLabel)
		/*  */.addComponent(gravityYField))
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(phyWorldCB)
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*  */.addComponent(pixMetersLabel)
		/*  */.addComponent(pixMetersField))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*  */.addComponent(gravityXLabel)
		/*  */.addComponent(gravityXField))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*  */.addComponent(gravityYLabel)
		/*  */.addComponent(gravityYField))
		);
		
		return panel;
	}

	public JPanel makeViewsPane()
		{
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		panel.setLayout(layout);

		vEnabled = new JCheckBox(Messages.getString("RoomFrame.VIEWS_ENABLED")); //$NON-NLS-1$
		plf.make(vEnabled,PRoom.VIEWS_ENABLED);
		vClear = new JCheckBox(Messages.getString("RoomFrame.VIEWS_CLEAR")); //$NON-NLS-1$
		plf.make(vClear,PRoom.VIEWS_CLEAR);

		JLabel[] viewLabs = new JLabel[res.views.size()];
		for (int i = 0; i < viewLabs.length; i++)
			{
			viewLabs[i] = new JLabel(" " + Messages.getString("RoomFrame.VIEW") + i); //$NON-NLS-1$
			boolean v = res.views.get(i).properties.get(PView.VISIBLE);
			viewLabs[i].setFont(viewLabs[i].getFont().deriveFont(v ? Font.BOLD : Font.PLAIN));
			viewLabs[i].setOpaque(true);
			}
		vList = new JList<JLabel>(viewLabs);
		vList.setCellRenderer(new ListComponentRenderer());
		//vList.setVisibleRowCount(4);
		vList.addListSelectionListener(this);
		vList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane sp = new JScrollPane(vList);

		for (View v : res.views)
			{
			v.properties.getUpdateSource(PView.VISIBLE).addListener(vpl);
			v.properties.getUpdateSource(PView.OBJECT).addListener(vpl);
			}
		
		vVisible = new JCheckBox(Messages.getString("RoomFrame.VIEW_ENABLED")); //$NON-NLS-1$

		JTabbedPane tp = makeViewsDimensionsPane();
		JPanel pf = makeViewsFollowPane();

		vList.setSelectedIndex(0);

		Insets spi = sp.getInsets();
		int spmh = vList.getMaximumSize().height + spi.bottom + spi.top;
		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(vEnabled)
		/**/.addComponent(vClear)
		/**/.addComponent(sp)
		/**/.addComponent(vVisible)
		/**/.addComponent(tp)
		/**/.addComponent(pf));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(vEnabled)
		/**/.addComponent(vClear)
		/**/.addComponent(sp,DEFAULT_SIZE,DEFAULT_SIZE,spmh)
		/**/.addComponent(vVisible)
		/**/.addComponent(tp,DEFAULT_SIZE,DEFAULT_SIZE,PREFERRED_SIZE)
		/**/.addComponent(pf));
		return panel;
		}

	private JTabbedPane makeViewsDimensionsPane()
		{
		JPanel pr = new JPanel();
		GroupLayout lr = new GroupLayout(pr);
		pr.setLayout(lr);

		JLabel lRX = new JLabel(Messages.getString("RoomFrame.VIEW_X")); //$NON-NLS-1$
		vRX = new NumberField(0,999999);
		JLabel lRW = new JLabel(Messages.getString("RoomFrame.VIEW_W")); //$NON-NLS-1$
		vRW = new NumberField(1,999999);
		JLabel lRY = new JLabel(Messages.getString("RoomFrame.VIEW_Y")); //$NON-NLS-1$
		vRY = new NumberField(0,999999);
		JLabel lRH = new JLabel(Messages.getString("RoomFrame.VIEW_H")); //$NON-NLS-1$
		vRH = new NumberField(1,999999);
		lr.setHorizontalGroup(lr.createSequentialGroup().addContainerGap()
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(lRX)
		/*		*/.addComponent(lRY)).addGap(4)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(vRX)
		/*		*/.addComponent(vRY)).addGap(8)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(lRW)
		/*		*/.addComponent(lRH)).addGap(4)
		/**/.addGroup(lr.createParallelGroup()
		/*		*/.addComponent(vRW)
		/*		*/.addComponent(vRH)).addContainerGap());
		lr.setVerticalGroup(lr.createSequentialGroup().addGap(4)
		/**/.addGroup(lr.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lRX)
		/*		*/.addComponent(vRX)
		/*		*/.addComponent(lRW)
		/*		*/.addComponent(vRW)).addGap(4)
		/**/.addGroup(lr.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lRY)
		/*		*/.addComponent(vRY)
		/*		*/.addComponent(lRH)
		/*		*/.addComponent(vRH)).addGap(8));

		JPanel pp = new JPanel();
		GroupLayout lp = new GroupLayout(pp);
		pp.setLayout(lp);

		JLabel lPX = new JLabel(Messages.getString("RoomFrame.PORT_X")); //$NON-NLS-1$
		vPX = new NumberField(0,999999);
		JLabel lPW = new JLabel(Messages.getString("RoomFrame.PORT_W")); //$NON-NLS-1$
		vPW = new NumberField(1,999999);
		JLabel lPY = new JLabel(Messages.getString("RoomFrame.PORT_Y")); //$NON-NLS-1$
		vPY = new NumberField(0,999999);
		JLabel lPH = new JLabel(Messages.getString("RoomFrame.PORT_H")); //$NON-NLS-1$
		vPH = new NumberField(1,999999);
		lp.setHorizontalGroup(lp.createSequentialGroup().addContainerGap()
		/**/.addGroup(lp.createParallelGroup()
		/*		*/.addComponent(lPX)
		/*		*/.addComponent(lPY)).addGap(4)
		/**/.addGroup(lp.createParallelGroup()
		/*		*/.addComponent(vPX)
		/*		*/.addComponent(vPY)).addGap(8)
		/**/.addGroup(lp.createParallelGroup()
		/*		*/.addComponent(lPW)
		/*		*/.addComponent(lPH)).addGap(4)
		/**/.addGroup(lp.createParallelGroup()
		/*		*/.addComponent(vPW)
		/*		*/.addComponent(vPH)).addContainerGap());
		lp.setVerticalGroup(lp.createSequentialGroup().addGap(4)
		/**/.addGroup(lp.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lPX)
		/*		*/.addComponent(vPX)
		/*		*/.addComponent(lPW)
		/*		*/.addComponent(vPW)).addGap(4)
		/**/.addGroup(lp.createParallelGroup(Alignment.BASELINE)
		/*		*/.addComponent(lPY)
		/*		*/.addComponent(vPY)
		/*		*/.addComponent(lPH)
		/*		*/.addComponent(vPH)).addGap(8));

		JTabbedPane tp = new JTabbedPane();
		tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tp.addTab(Messages.getString("RoomFrame.VIEW_IN_ROOM"),pr); //$NON-NLS-1$
		tp.addTab(Messages.getString("RoomFrame.PORT"),pp); //$NON-NLS-1$
		return tp;
		}

	private JPanel makeViewsFollowPane()
		{
		JPanel pf = new JPanel();
		pf.setBorder(BorderFactory.createTitledBorder(Messages.getString("RoomFrame.FOLLOW"))); //$NON-NLS-1$
		GroupLayout lf = new GroupLayout(pf);
		pf.setLayout(lf);

		vObj = new ResourceMenu<GmObject>(GmObject.class,
				Messages.getString("RoomFrame.NO_OBJECT"),true,110); //$NON-NLS-1$
		
		JLabel lH = new JLabel(Messages.getString("RoomFrame.VIEW_HORIZONTAL"));
		JLabel lV = new JLabel(Messages.getString("RoomFrame.VIEW_VERTICAL"));
		JLabel lBorder = new JLabel(Messages.getString("RoomFrame.VIEW_BORDER"));
		JLabel lSpeed = new JLabel(Messages.getString("RoomFrame.VIEW_SPEED"));
		vOHBor = new NumberField(0,32000);
		vOHSp = new NumberField(-1,32000);
		vOVBor = new NumberField(0,32000);
		vOVSp = new NumberField(-1,32000);
		lf.setHorizontalGroup(lf.createSequentialGroup().addContainerGap()
		/**/.addGroup(lf.createParallelGroup()
		/*		*/.addComponent(vObj)
		/*		*/.addGroup(lf.createSequentialGroup()
		/*				*/.addGroup(lf.createParallelGroup(Alignment.TRAILING)
		/*						*/.addComponent(lH)
		/*						*/.addComponent(lV)).addGap(4)
		/*				*/.addGroup(lf.createParallelGroup()
		/*						*/.addComponent(lBorder)
		/*						*/.addComponent(vOHBor)
		/*						*/.addComponent(vOVBor)).addGap(4)
		/*				*/.addGroup(lf.createParallelGroup()
		/*						*/.addComponent(lSpeed)
		/*						*/.addComponent(vOHSp)
		/*						*/.addComponent(vOVSp)))).addContainerGap());
		lf.setVerticalGroup(lf.createSequentialGroup().addGap(4)
		/**/.addComponent(vObj).addGap(4)
		/*		*/.addGroup(lf.createParallelGroup(Alignment.BASELINE)
		/*				*/.addComponent(lBorder)
		/*				*/.addComponent(lSpeed)).addGap(4)
		/*		*/.addGroup(lf.createParallelGroup(Alignment.BASELINE)
		/*				*/.addComponent(lH)
		/*				*/.addComponent(vOHBor)
		/*				*/.addComponent(vOHSp)).addGap(4)
		/*		*/.addGroup(lf.createParallelGroup(Alignment.BASELINE)
		/*				*/.addComponent(lV)
		/*				*/.addComponent(vOVBor)
		/*				*/.addComponent(vOVSp)).addGap(8));
		return pf;
		}

	private JPanel makeStatsPane()
		{
		JPanel stat = new JPanel();
		stat.setLayout(new BoxLayout(stat,BoxLayout.X_AXIS));
		stat.setMaximumSize(new Dimension(Integer.MAX_VALUE,11));

		statX = new JLabel(Messages.getString("RoomFrame.STAT_X")); //$NON-NLS-1$
		statX.setMaximumSize(new Dimension(50,14));
		stat.add(statX);
		
		//stat.add(new JLabel("|")); //$NON-NLS-1$
		//visible divider    ^   since JSeparator isn't visible
    JSeparator sep;
    sep = new JSeparator(JSeparator.VERTICAL);
    sep.setMaximumSize(new Dimension(8, sep.getMaximumSize().height));
    stat.add(sep);
    
		statY = new JLabel(Messages.getString("RoomFrame.STAT_Y")); //$NON-NLS-1$
		statY.setMaximumSize(new Dimension(50,13));
		stat.add(statY);
		
    sep = new JSeparator(JSeparator.VERTICAL);
    sep.setMaximumSize(new Dimension(8, sep.getMaximumSize().height));
    stat.add(sep);
    
		statId = new JLabel();
		stat.add(statId);
    
    sep = new JSeparator(JSeparator.VERTICAL);
    sep.setMaximumSize(new Dimension(8, sep.getMaximumSize().height));
    stat.add(sep);
    
		statSrc = new JLabel();
		stat.add(statSrc); //resizes at will, so no Max size

		return stat;
		}

	public RoomFrame(Room res, ResNode node)
		{
		super(res,node);
		editor = new RoomEditor(res,this);
		prelf = new PropertyLinkFactory<PRoomEditor>(editor.properties,null);

		GroupLayout layout = new GroupLayout(getContentPane())
			{
				@Override
				public void layoutContainer(Container parent)
					{
					Dimension m = RoomFrame.this.getMinimumSize();
					Dimension s = RoomFrame.this.getSize();
					Dimension r = new Dimension(Math.max(m.width,s.width),Math.max(m.height,s.height));
					if (!r.equals(s))
						RoomFrame.this.setSize(r);
					else
						super.layoutContainer(parent);
					}
			};
		setLayout(layout);

		JToolBar tools = makeToolBar();

		//conveniently, these tabs happen to have the same indexes as GM's tabs
		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tabs.addTab(Messages.getString("RoomFrame.TAB_OBJECTS"),makeObjectsPane()); //$NON-NLS-1$
		tabs.addTab(Messages.getString("RoomFrame.TAB_SETTINGS"),makeSettingsPane()); //$NON-NLS-1$
		tabs.addTab(Messages.getString("RoomFrame.TAB_TILES"),makeTilesPane()); //$NON-NLS-1$
		String bks = Messages.getString("RoomFrame.TAB_BACKGROUNDS"); //$NON-NLS-1$
		tabs.addTab(bks,makeBackgroundsPane());
		tabs.addTab(Messages.getString("RoomFrame.TAB_VIEWS"),makeViewsPane()); //$NON-NLS-1$
		tabs.addTab(Messages.getString("RoomFrame.TAB_PHYSICS"),makePhysicsPane()); //$NON-NLS-1$
		tabs.setSelectedIndex((Integer) res.get(PRoom.CURRENT_TAB));
		tabs.addChangeListener(this);
		
		res.instanceUpdateSource.addListener(this);
		res.tileUpdateSource.addListener(this);

		editorPane = new EditorScrollPane(editor);
		prelf.make(editorPane,PRoomEditor.ZOOM);
		JPanel stats = makeStatsPane();

		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addComponent(tools)
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addComponent(tabs,PREFERRED_SIZE,PREFERRED_SIZE,PREFERRED_SIZE)
		/*	*/.addGroup(layout.createParallelGroup()
		/*		*/.addComponent(editorPane,200,640,DEFAULT_SIZE)
		/*		*/.addComponent(stats))));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addComponent(tools,PREFERRED_SIZE,PREFERRED_SIZE,PREFERRED_SIZE)
		/**/.addGroup(layout.createParallelGroup()
		/*	*/.addComponent(tabs)
		/*	*/.addGroup(layout.createSequentialGroup()
		/*		*/.addComponent(editorPane,DEFAULT_SIZE,480,DEFAULT_SIZE)
		/*		*/.addComponent(stats))));

    // initialize the undo/redo system
    undoManager= new UndoManager();
    undoSupport = new UndoableEditSupport();
    undoSupport.addUndoableEditListener(new UndoAdapter());
    refreshUndoRedoButtons();
    
		if (res.get(PRoom.REMEMBER_WINDOW_SIZE))
			{
			int h = res.get(PRoom.EDITOR_HEIGHT);
			int w = res.get(PRoom.EDITOR_WIDTH);
			Dimension d = LGM.mdi.getSize();
			if (d.width <= w && d.height <= h)
				maximize = true;
			else
				setSize(800, 520);
			}
		else
			pack();
		}

	private boolean maximize;

	//maximizes over-sized RoomFrames, since setMaximum can't
	//be called until after it's been added to the MDI
	public void setVisible(boolean b)
		{
		super.setVisible(b);
		if (!maximize) return;
		try
			{
			setMaximum(true);
			}
		catch (PropertyVetoException e)
			{
			setSize((Integer) res.get(PRoom.EDITOR_WIDTH),(Integer) res.get(PRoom.EDITOR_HEIGHT));
			e.printStackTrace();
			}
		}

	public static class ListComponentRenderer implements ListCellRenderer<Object>
		{
		public Component getListCellRendererComponent(JList<? extends Object> list, Object val, int ind,
				boolean selected, boolean focus)
			{
			Component lab = (Component) val;
			if (selected)
				{
				lab.setBackground(list.getSelectionBackground());
				lab.setForeground(list.getSelectionForeground());
				}
			else
				{
				lab.setBackground(list.getBackground());
				lab.setForeground(list.getForeground());
				}
			return lab;
			}
		}

	protected boolean areResourceFieldsEqual()
		{
		return (res.backgroundDefs.equals(resOriginal.backgroundDefs)
				&& res.views.equals(resOriginal.views) && res.instances.equals(resOriginal.instances) && res.tiles.equals(resOriginal.tiles));
		}

	public void commitChanges()
		{
		res.setName(name.getText());

		for (CodeFrame cf : codeFrames.values())
			cf.commitChanges();

		if (res.get(PRoom.REMEMBER_WINDOW_SIZE))
			{
			res.put(PRoom.CURRENT_TAB,tabs.getSelectedIndex());
			Dimension s = getSize();
			res.put(PRoom.EDITOR_WIDTH,s.width);
			res.put(PRoom.EDITOR_HEIGHT,s.height);
			}
		}

	public void actionPerformed(ActionEvent e)
		{
		if (editor != null) editor.refresh();
		Object eventSource = e.getSource();

		if (eventSource == sShow)
			{
			sShowMenu.show(sShow,0,sShow.getHeight());
			return;
			}
		
		// If the user has pressed the 'Add' object button
		if (eventSource == addObjectButton)
			{
			// If no object is selected
			if (oNew.getSelected() == null) return;
      // Add the new object instance
			Instance newObject = res.addInstance();
      newObject.properties.put(PInstance.OBJECT,oNew.getSelected());
      newObject.setPosition(new Point());
      
      int numberOfObjects = res.instances.size();

      // Record the effect of adding an object for the undo
      UndoableEdit edit = new AddPieceInstance(this, newObject, numberOfObjects -1);
      // notify the listeners
      undoSupport.postEdit( edit );
      
			oList.setSelectedIndex(numberOfObjects - 1);
			
			return;
			}

		// If the user has pressed the 'Delete' object  button
		if (eventSource == deleteObjectButton)
			{
			int selectedIndex = oList.getSelectedIndex();
			if (selectedIndex == -1) return;
			
			Instance instance = (Instance) oList.getSelectedValue();
			if (instance == null) return;

      // Record the effect of removing an object for the undo
			UndoableEdit edit = new RemovePieceInstance(this, instance, selectedIndex);
      // notify the listeners
			undoSupport.postEdit( edit );
      
			CodeFrame frame = codeFrames.get(res.instances.remove(selectedIndex));
			if (frame != null) frame.dispose();
			oList.setSelectedIndex(Math.min(res.instances.size() - 1,selectedIndex));
			return;
			}

		if (eventSource == taSource)
			{
			tSelect.setBackground(taSource.getSelected());
			return;
			}
		
		// If the user has pressed the 'Delete' tile  button
		if (eventSource == deleteTileButton)
			{
			int selectedIndex = tList.getSelectedIndex();
			if (selectedIndex >= res.tiles.size() || selectedIndex < 0) return;
			
			Tile tile = (Tile) tList.getSelectedValue();

      // Record the effect of removing an object for the undo
			UndoableEdit edit = new RemovePieceInstance(this, tile, selectedIndex);
      // notify the listeners
			undoSupport.postEdit( edit );
			
			res.tiles.remove(selectedIndex);
			tList.setSelectedIndex(Math.min(res.tiles.size() - 1,selectedIndex));
			return;
			}
		
		if (e.getSource() == sCreationCode)
			{
			openCodeFrame(res,Messages.getString("RoomFrame.TITLE_FORMAT_CREATION"),res.getName()); //$NON-NLS-1$
			return;
			}
		if (e.getSource() == oCreationCode)
			{
			if (lastObj != null) openCodeFrame(lastObj);
			return;
			}
		super.actionPerformed(e);
		}

	public void fireObjUpdate()
		{
		Instance selectedInstance = (Instance) oList.getSelectedValue();
		if (lastObj == selectedInstance) return;
		lastObj = selectedInstance;
		PropertyLink.removeAll(loLocked,loSource,loX,loY);
		
		if (selectedInstance != null)
			{
			PropertyLinkFactory<PInstance> iplf = new PropertyLinkFactory<PInstance>(selectedInstance.properties,this);
			loLocked = iplf.make(oLocked,PInstance.LOCKED);
			loSource = iplf.make(oSource,PInstance.OBJECT);
			loX = iplf.make(objectHorizontalPosition,PInstance.X);
			loY = iplf.make(objectVerticalPosition,PInstance.Y);
			}
		}

	@Override
	public Dimension getMinimumSize()
		{
		Dimension p = getContentPane().getSize();
		Dimension l = getContentPane().getMinimumSize();
		Dimension s = getSize();
		l.width += s.width - p.width;
		l.height += s.height - p.height;
		return l;
		}

	public void fireTileUpdate()
		{
		Tile selectedTile = (Tile) tList.getSelectedValue();
		if (lastTile == selectedTile) return;
		lastTile = selectedTile;
		PropertyLink.removeAll(ltDepth,ltLocked,ltSource,ltsX,ltsY,ltX,ltY);
		
		if (selectedTile != null)
			{
			PropertyLinkFactory<PTile> tplf = new PropertyLinkFactory<PTile>(selectedTile.properties,this);
			ltDepth = tplf.make(teDepth,PTile.DEPTH);
			ltLocked = tplf.make(tLocked,PTile.LOCKED);
			ltSource = tplf.make(teSource,PTile.BACKGROUND);
			ltsX = tplf.make(tsX,PTile.BG_X);
			ltsY = tplf.make(tsY,PTile.BG_Y);
			ltX = tplf.make(tileHorizontalPosition,PTile.ROOM_X);
			ltY = tplf.make(tileVerticalPosition,PTile.ROOM_Y);
			}
		}

	public void fireBackUpdate()
		{
		int i = bList.getSelectedIndex();
		if (lastValidBack == i) return;
		if (i < 0)
			{
			bList.setSelectedIndex(lastValidBack < 0 ? 0 : lastValidBack);
			return;
			}
		lastValidBack = i;
		PropertyLink.removeAll(lbVisible,lbForeground,lbSource,lbX,lbY,lbTileH,lbTileV,lbStretch,lbH,
				lbV);
		BackgroundDef b = res.backgroundDefs.get(i);
		PropertyLinkFactory<PBackgroundDef> bdplf = new PropertyLinkFactory<PBackgroundDef>(
				b.properties,this);
		lbVisible = bdplf.make(bVisible,PBackgroundDef.VISIBLE);
		lbForeground = bdplf.make(bForeground,PBackgroundDef.FOREGROUND);
		lbSource = bdplf.make(bSource,PBackgroundDef.BACKGROUND);
		lbX = bdplf.make(bX,PBackgroundDef.X);
		lbY = bdplf.make(bY,PBackgroundDef.Y);
		lbTileH = bdplf.make(bTileH,PBackgroundDef.TILE_HORIZ);
		lbTileV = bdplf.make(bTileV,PBackgroundDef.TILE_VERT);
		lbStretch = bdplf.make(bStretch,PBackgroundDef.STRETCH);
		lbH = bdplf.make(bH,PBackgroundDef.H_SPEED);
		lbV = bdplf.make(bV,PBackgroundDef.V_SPEED);
		}

	public void fireViewUpdate()
		{
		int i = vList.getSelectedIndex();
		if (lastValidView == i) return;
		if (i < 0)
			{
			bList.setSelectedIndex(lastValidView < 0 ? 0 : lastValidView);
			return;
			}
		lastValidView = i;
		PropertyLink.removeAll(lvVisible,lvRX,lvRY,lvRW,lvRH,lvPX,lvPY,lvPW,lvPH,lvObj,lvOHBor,lvOVBor,
				lvOHSp,lvOVSp);
		View view = res.views.get(i);
		PropertyLinkFactory<PView> vplf = new PropertyLinkFactory<PView>(view.properties,this);
		lvVisible = vplf.make(vVisible,PView.VISIBLE);
		lvRX = vplf.make(vRX,PView.VIEW_X);
		lvRY = vplf.make(vRY,PView.VIEW_Y);
		lvRW = vplf.make(vRW,PView.VIEW_W);
		lvRH = vplf.make(vRH,PView.VIEW_H);
		lvPX = vplf.make(vPX,PView.PORT_X);
		lvPY = vplf.make(vPY,PView.PORT_Y);
		lvPW = vplf.make(vPW,PView.PORT_W);
		lvPH = vplf.make(vPH,PView.PORT_H);
		lvObj = vplf.make(vObj,PView.OBJECT);
		lvOHBor = vplf.make(vOHBor,PView.BORDER_H);
		lvOVBor = vplf.make(vOVBor,PView.BORDER_V);
		lvOHSp = vplf.make(vOHSp,PView.SPEED_H);
		lvOVSp = vplf.make(vOVSp,PView.SPEED_V);
		}

	// Display the selected view in the center of the window
	private void showSelectedView()
		{
		if (editorPane == null)
			return;
		
		// If the views are not enabled
		if ((Boolean) editor.roomVisual.room.get(PRoom.VIEWS_ENABLED) == false)
			return;
		
		// Get the selected view
		View view = res.views.get(vList.getSelectedIndex());
		
		// If the view is not visible, don't show it
		if ((Boolean) view.properties.get(PView.VISIBLE) == false)
			return;
	
		// Get the reference to the 'Object following' object
		ResourceReference<GmObject> objectToFollowReference = null;
		
		// If there is 'Object following' object for the selected view
		if (view.properties.get(PView.OBJECT) != null)
			objectToFollowReference = view.properties.get(PView.OBJECT);
		
		Instance instanceToFollow = null;
		
		// If there is an object to follow, get the first instance in the room
		if (objectToFollowReference != null)
			{
			for (Instance instance : editor.roomVisual.room.instances)
				{
					ResourceReference<GmObject> instanceObject = instance.properties.get(PInstance.OBJECT);
					
					if (instanceObject == objectToFollowReference)
						{
						instanceToFollow = instance;
						break;
						}
				}
			}
		
		int zoomLevel = editor.properties.get(PRoomEditor.ZOOM);
		
		// Properties of the view
		int viewHorizontalPosition;
		int viewVerticalPosition;
		int viewWidth;
		int viewHeight;
		
		viewWidth = (Integer) view.properties.get(PView.VIEW_W) * zoomLevel;
		viewHeight = (Integer) view.properties.get(PView.VIEW_H) * zoomLevel;
		
		// If there is an instance to follow, use the instance properties for centering the view
		if (instanceToFollow != null)
			{
			// Get the instance properties
			BufferedImage image = objectToFollowReference.get().getDisplayImage();
			int instanceWidth = image.getHeight() * zoomLevel;
			int instanceHeight = image.getWidth() * zoomLevel;
			int instanceHorizontalPosition = (Integer) instanceToFollow.properties.get(PInstance.X);
			int instanceVerticalPosition = (Integer) instanceToFollow.properties.get(PInstance.Y);
			
			// Calculate the center of the instance
			int objectCenterPositionX = instanceHorizontalPosition + image.getWidth() / 2;
			int objectCenterPositionY = instanceVerticalPosition + image.getHeight() / 2;
			
			viewHorizontalPosition = instanceHorizontalPosition;
			viewVerticalPosition = instanceVerticalPosition;
			
			viewHorizontalPosition = objectCenterPositionX - viewWidth /2;
			viewVerticalPosition = objectCenterPositionY - viewHeight /2;
			
			view.properties.put(PView.VIEW_OBJECT_FOLLOWING_X,viewHorizontalPosition);
			view.properties.put(PView.VIEW_OBJECT_FOLLOWING_Y,viewVerticalPosition);
			}
		else
			{
			// Get the properties of the view
			viewHorizontalPosition = view.properties.get(PView.VIEW_X);
			viewVerticalPosition = view.properties.get(PView.VIEW_Y);
			
			view.properties.put(PView.VIEW_OBJECT_FOLLOWING_X,-1);
			view.properties.put(PView.VIEW_OBJECT_FOLLOWING_Y,-1);

			}
		
		// Get the properties of the viewport
		JViewport viewport = editorPane.getViewport();
		int viewportHeight = viewport.getHeight();
		int viewportWidth = viewport.getWidth();
				
		// Center the view in the viewport
		int newViewPortHorizontalPosition = viewHorizontalPosition - (viewportWidth - viewWidth) / 2;
		int newViewPortVerticalPosition = viewVerticalPosition - (viewportHeight - viewHeight) / 2;
		Point newViewportPosition = new Point(newViewPortHorizontalPosition,newViewPortVerticalPosition);

		// If the new position of the viewport is above the origin coordinates
		if (newViewportPosition.x < editor.getOverallBounds().x)
			newViewportPosition.x = editor.getOverallBounds().x;
	
		if (newViewportPosition.y < editor.getOverallBounds().y)
			newViewportPosition.y =  editor.getOverallBounds().y;
	
		// Take into account the visual offset of the border and the zoom level
		editor.visualToComponent(newViewportPosition,zoomLevel);

		viewport.setViewPosition(newViewportPosition);
		}
	
	// if an item of a listbox has been selected
	public void valueChanged(ListSelectionEvent e)
		{
		if (e.getValueIsAdjusting()) return;

		if (e.getSource() == oList) fireObjUpdate();
		if (e.getSource() == tList) fireTileUpdate();
		if (e.getSource() == bList) fireBackUpdate();
		if (e.getSource() == vList)
			{
			fireViewUpdate();
			showSelectedView();
			}
		}

	public void openCodeFrame(Instance i)
		{
		openCodeFrame(i,Messages.getString("RoomFrame.TITLE_FORMAT_CREATION"),Messages.format(
				"RoomFrame.INSTANCE",i.properties.get(PInstance.ID)));
		}

	public void openCodeFrame(CodeHolder code, String titleFormat, Object titleArg)
		{
		CodeFrame frame = codeFrames.get(code);
		if (frame == null)
			{
			frame = new CodeFrame(code,titleFormat,titleArg);
			codeFrames.put(code,frame);
			frame.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameClosed(InternalFrameEvent e)
						{
						CodeFrame f = ((CodeFrame) e.getSource());
						codeFrames.remove(f.codeHolder);
						f.removeInternalFrameListener(this);
						}
				});
			LGM.mdi.add(frame);
			LGM.mdi.addZChild(this,frame);
			frame.toTop();
			}
		else
			frame.toTop();
		}

	public void removeUpdate(DocumentEvent e)
		{
		CodeFrame f = codeFrames.get(res);
		if (f != null) f.setTitleFormatArg(name.getText());
		super.removeUpdate(e);
		}

	public void insertUpdate(DocumentEvent e)
		{
		CodeFrame f = codeFrames.get(res);
		if (f != null) f.setTitleFormatArg(name.getText());
		super.insertUpdate(e);
		}

	public void dispose()
		{
		super.dispose();
		for (CodeFrame cf : codeFrames.values())
			cf.dispose();
		// XXX: These components could still be referenced by InputContext or similar.
		// Removing their references to this frame is therefore necessary in order to ensure
		// garbage collection.
		oNew.removeActionListener(this);
		oList.removeListSelectionListener(this);
		addObjectButton.removeActionListener(this);
		deleteObjectButton.removeActionListener(this);
		oCreationCode.removeActionListener(this);
		sCreationCode.removeActionListener(this);
		sShow.removeActionListener(this);
		taSource.removeActionListener(this);
		tList.removeListSelectionListener(this);
		deleteTileButton.removeActionListener(this);
		bList.removeListSelectionListener(this);
		vList.removeListSelectionListener(this);
		editorPane.setViewport(null);
		setLayout(null);
		}

	public void updated(UpdateEvent e)
		{
		if (e.source == res.instanceUpdateSource)
			oList.setPrototypeCellValue(null);
		else if (e.source == res.tileUpdateSource) tList.setPrototypeCellValue(null);
		}

	private void bdvListUpdate(boolean isBgDef, UpdateSource s, boolean v)
		{
		int ls = (isBgDef ? res.backgroundDefs : res.views).size();
		for (int i = 0; i < ls; i++)
			{
			UpdateSource s2 = (isBgDef ? res.backgroundDefs.get(i).properties
					: res.views.get(i).properties).updateSource;
			if (s2 != s) continue;
			JList<?> l = isBgDef ? bList : vList;
			JLabel ll = (JLabel) l.getModel().getElementAt(i);
			ll.setFont(ll.getFont().deriveFont(v ? Font.BOLD : Font.PLAIN));
			l.setPrototypeCellValue(null);
			break;
			}
		}

	private class BgDefPropertyListener extends PropertyUpdateListener<PBackgroundDef>
		{
		@Override
		public void updated(PropertyUpdateEvent<PBackgroundDef> e)
			{
			if (e.key == PBackgroundDef.VISIBLE) bdvListUpdate(true,e.source,(Boolean) e.map.get(e.key));
			}
		}

	private class ViewPropertyListener extends PropertyUpdateListener<PView>
		{
		@Override
		public void updated(PropertyUpdateEvent<PView> e)
			{
			if (e.key == PView.VISIBLE) bdvListUpdate(false,e.source,(Boolean) e.map.get(e.key));
			// If the 'Object following' object has been changed, update the display of the view
			if (e.key == PView.OBJECT) showSelectedView();
			}
		}
	
	// When a resource has been updated, reset the undo manager
	public void resetUndoManager()
		{
		undoManager.discardAllEdits();
		refreshUndoRedoButtons();
		}
	
  /**
  * An undo/redo adapter. The adapter is notified when an undo edit occur(e.g. add or remove from the list)
  * The adapter extract the edit from the event, add it to the UndoManager, and refresh the GUI
  */

  private class UndoAdapter implements UndoableEditListener
  {
  	public void undoableEditHappened (UndoableEditEvent evt)
    {
     	UndoableEdit edit = evt.getEdit();
     	undoManager.addEdit( edit );
     	refreshUndoRedoButtons();
     }
  }
  
  /**
  * This method is called after each undoable operation
  * in order to refresh the presentation state of the undo/redo GUI
  */

  public void refreshUndoRedoButtons()
	  {
	     // refresh undo
	     undo.setEnabled(undoManager.canUndo() );
	
	     // refresh redo
	     redo.setEnabled(undoManager.canRedo() );
	  }
  
  // When a text field related to the position of a piece gains the focus
	public void focusGained(FocusEvent event)
		{
		pieceOriginalPosition = null;
		selectedPiece = null;
		
	 	// If we are modifying objects
		if (event.getSource() == objectHorizontalPosition || event.getSource() == objectVerticalPosition)
		 {
			// If no object is selected, return
			int selectedIndex = oList.getSelectedIndex();
			if (selectedIndex == -1) return;
			
			// Save the selected instance
			selectedPiece = (Instance) oList.getSelectedValue();
			
			// Save the position of the object for the undo
			pieceOriginalPosition = new Point (selectedPiece.getPosition());
		 }
		// We are modifying tiles
	 else
		 {
			// If no tile is selected, return
			int selectedIndex = tList.getSelectedIndex();
			if (selectedIndex == -1) return;
			
			// Save the selected tile
			selectedPiece = (Tile) tList.getSelectedValue();
			
			// Save the position of the tile for the undo
			pieceOriginalPosition = new Point (selectedPiece.getPosition());
		 }
		}

	// When a text field related to a piece position has lost the focus
	public void focusLost(FocusEvent event)
		{
			processFocusLost();
		}

	// Save the position of a piece for the undo
	public void processFocusLost()
		{
		if (selectedPiece == null)
			return;
			
	 	// If we are modifying objects
		if (selectedPiece instanceof Instance)
			{
			// If no object is selected, return
			int selectedIndex = oList.getSelectedIndex();
			if (selectedIndex == -1) return;
			
			// Get the new position of the object
			Point objectNewPosition = new Point (selectedPiece.getPosition());
			
			// If the position of the object has been changed
			if (!objectNewPosition.equals(pieceOriginalPosition))
				{
				// Record the effect of moving an object for the undo
				UndoableEdit edit = new MovePieceInstance(this, selectedPiece, pieceOriginalPosition, objectNewPosition);
			  // notify the listeners
			  undoSupport.postEdit( edit );
				}
			}
		 // We are modifying tiles
		 else
			 {
				// If no tile is selected, return
				int selectedIndex = tList.getSelectedIndex();
				if (selectedIndex == -1) return;
				
				// Get the new position of the tile
				Point tileNewPosition = new Point (selectedPiece.getPosition());
				
				// If the position of the tile has been changed
				if (!tileNewPosition.equals(pieceOriginalPosition))
					{
					// Record the effect of moving an tile for the undo
					UndoableEdit edit = new MovePieceInstance(this, selectedPiece, pieceOriginalPosition, tileNewPosition);
				  // notify the listeners
				  undoSupport.postEdit( edit );
					}
			 }
		
			 selectedPiece = null;
		}

	// When a new tab is selected
	public void stateChanged(ChangeEvent event)
		{
    JTabbedPane sourceTabbedPane = (JTabbedPane) event.getSource();
    int index = sourceTabbedPane.getSelectedIndex();
    
    // If the views tab is selected, always display the views
    if (sourceTabbedPane.getTitleAt(index) == Messages.getString("RoomFrame.TAB_VIEWS"))
    	{
    	showSelectedView();
    	editor.roomVisual.setViewsVisible(true);
    	}
    else
    	{
    	editor.roomVisual.setViewsVisible(false);
    	}
		}
}