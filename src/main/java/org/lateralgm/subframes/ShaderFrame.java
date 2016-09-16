/**
 * @file ShaderFrame.java
 * @brief Class implementing the shader frame and a tabbed CodeTextArea
 * @section License
 * <p>
 * Copyright (C) 2013-2014 Robert B. Colton
 * This file is a part of the LateralGM IDE.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.lateralgm.subframes;

import org.lateralgm.components.CodeTextArea;
import org.lateralgm.components.MarkerCache;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.components.impl.TextAreaFocusTraversalPolicy;
import org.lateralgm.file.FileChangeMonitor;
import org.lateralgm.file.FileChangeMonitor.FileUpdateEvent;
import org.lateralgm.joshedit.Code;
import org.lateralgm.joshedit.DefaultTokenMarker;
import org.lateralgm.joshedit.JoshText.LineChangeListener;
import org.lateralgm.joshedit.lexers.GLESTokenMarker;
import org.lateralgm.joshedit.lexers.GLSLTokenMarker;
import org.lateralgm.joshedit.lexers.HLSLTokenMarker;
import org.lateralgm.main.LGM;
import org.lateralgm.main.Prefs;
import org.lateralgm.main.UpdateSource.UpdateEvent;
import org.lateralgm.main.UpdateSource.UpdateListener;
import org.lateralgm.messages.Messages;
import org.lateralgm.resources.Shader;
import org.lateralgm.resources.Shader.PShader;
import org.lateralgm.ui.swing.util.SwingExecutor;
import org.lateralgm.util.PlatformHelper;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ShaderFrame extends InstantiableResourceFrame<Shader, PShader> {
	private static final long serialVersionUID = 1L;
	public JToolBar tool;
	public JTabbedPane editors;
	public CodeTextArea vcode;
	public CodeTextArea fcode;
	public JButton edit;
	public JPanel status;
	public JCheckBox precompileCB;
	public JComboBox<String> typeCombo;
	public String currentLang = "";

	private ShaderEditor fragmentEditor;
	private ShaderEditor vertexEditor;

	public ShaderFrame(Shader res, ResNode node) {
		super(res, node);
		setSize(700, 430);
		setLayout(new BorderLayout());

		vcode = new CodeTextArea((String) res.get(PShader.VERTEX), MarkerCache.getMarker(MarkerCache.Language.GLSLES));
		fcode = new CodeTextArea((String) res.get(PShader.FRAGMENT), MarkerCache.getMarker(MarkerCache.Language.GLSLES));

		editors = new JTabbedPane();
		editors.addTab("Vertex", vcode);
		editors.addTab("Fragment", fcode);
		add(editors, BorderLayout.CENTER);

		// Setup the toolbar
		tool = new JToolBar();
		tool.setFloatable(false);
		tool.setAlignmentX(0);
		add(tool, BorderLayout.NORTH);

		tool.add(save);
		tool.addSeparator();

		if (Prefs.useExternalScriptEditor) {
			vcode.setEnabled(false);
			edit = new JButton(LGM.getIconForKey("ShaderFrame.EDIT")); //$NON-NLS-1$
			edit.setToolTipText(Messages.getString("ShaderFrame.EDIT"));
			edit.addActionListener(this);
			tool.add(edit);

			tool.addSeparator();
		}

		this.addEditorButtons(tool);

		tool.addSeparator();
		tool.add(new JLabel(Messages.getString("ShaderFrame.TYPE")));
		String[] typeOptions = {"GLSLES", "GLSL", "HLSL9", "HLSL11"};
		typeCombo = new JComboBox<String>(typeOptions);
		typeCombo.setMaximumSize(typeCombo.getPreferredSize());
		typeCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				updateLexer();
			}
		});
		typeCombo.setSelectedItem(res.getType());
		tool.add(typeCombo);
		precompileCB = new JCheckBox(Messages.getString("ShaderFrame.PRECOMPILE"));
		precompileCB.setSelected(res.getPrecompile());
		precompileCB.setOpaque(false);
		tool.addSeparator();
		tool.add(precompileCB);
		tool.addSeparator();
		name.setColumns(13);
		name.setMaximumSize(name.getPreferredSize());
		tool.add(new JLabel(Messages.getString("ShaderFrame.NAME"))); //$NON-NLS-1$
		tool.add(name);

		status = new JPanel(new FlowLayout());
		BoxLayout layout = new BoxLayout(status, BoxLayout.X_AXIS);
		status.setLayout(layout);
		status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 11));
		final JLabel caretPos = new JLabel(" INS | UTF-8 | " + (vcode.getCaretLine() + 1) + " : "
				+ (vcode.getCaretColumn() + 1));
		status.add(caretPos);
		vcode.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				caretPos.setText(" INS | UTF-8 | " + (vcode.getCaretLine() + 1) + " : "
						+ (vcode.getCaretColumn() + 1));
			}
		});
		fcode.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				caretPos.setText(" INS | UTF-8 | " + (fcode.getCaretLine() + 1) + " : "
						+ (fcode.getCaretColumn() + 1));
			}
		});
		add(status, BorderLayout.SOUTH);

		setFocusTraversalPolicy(new TextAreaFocusTraversalPolicy(vcode.text));
		updateLexer();
	}

	private void updateLexer() {
		//TODO: This should be moved into the base CodeTextArea, as a feature of JoshEdit
		String val = typeCombo.getSelectedItem().toString();
		if (val.equals(currentLang)) {
			return;
		}
		DefaultTokenMarker marker = null;
		switch (val) {
			case "GLSLES":
				marker = new GLESTokenMarker();
				break;
			case "GLSL":
				marker = new GLSLTokenMarker();
				break;
			case "HLSL9":
				marker = new HLSLTokenMarker();
				break;
			case "HLSL11":
				marker = new HLSLTokenMarker();
				break;
			default:

				break;
		}
		//TODO: Both of these calls will utilize the same lexer, but they both
		//will recompose the list of completions. Should possibly add an abstract
		//GetCompletions() to the DefaultTokenMarker class, so that all code editors
		//can utilize the same completions list to save memory.
		vcode.setTokenMarker(marker);
		fcode.setTokenMarker(marker);
		currentLang = val;
		repaint();
	}

	public void commitChanges() {
		res.put(PShader.VERTEX, vcode.getTextCompat());
		res.put(PShader.FRAGMENT, fcode.getTextCompat());
		res.setName(name.getText());
		res.put(PShader.TYPE, typeCombo.getSelectedItem());
		res.put(PShader.PRECOMPILE, precompileCB.isSelected());
	}

	public void fireInternalFrameEvent(int id) {
		if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSED)
			LGM.currentFile.updateSource.removeListener(vcode);
		LGM.currentFile.updateSource.removeListener(fcode);
		super.fireInternalFrameEvent(id);
	}

	public void dispose() {
		if (fragmentEditor != null) {
			fragmentEditor.stop();
			fragmentEditor = null;
		}
		if (vertexEditor != null) {
			vertexEditor.stop();
			vertexEditor = null;
		}
		super.dispose();
	}

	;

	private JButton makeToolbarButton(String name) {
		String key = "JoshText." + name;
		JButton b = new JButton(LGM.getIconForKey(key));
		b.setToolTipText(Messages.getString(key));
		b.setRequestFocusEnabled(false);
		b.setActionCommand(key);
		b.addActionListener(this);
		return b;
	}

	public void addEditorButtons(JToolBar tb) {
		tb.add(makeToolbarButton("LOAD"));
		tb.add(makeToolbarButton("SAVE"));
		tb.add(makeToolbarButton("PRINT"));
		tb.addSeparator();

		tb.add(makeToolbarButton("CUT"));
		tb.add(makeToolbarButton("COPY"));
		tb.add(makeToolbarButton("PASTE"));
		tb.addSeparator();

		final JButton undoButton = makeToolbarButton("UNDO");
		tb.add(undoButton);
		final JButton redoButton = makeToolbarButton("REDO");
		tb.add(redoButton);
		// need to set the default state unlike the component popup
		undoButton.setEnabled(vcode.text.canUndo());
		redoButton.setEnabled(vcode.text.canRedo());
		LineChangeListener linelistener = new LineChangeListener() {

			@Override
			public void linesChanged(Code code, int start, int end) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						CodeTextArea selectedCode = getSelectedCode();
						undoButton.setEnabled(selectedCode.text.canUndo());
						redoButton.setEnabled(selectedCode.text.canRedo());
					}
				});
			}
		};
		editors.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				CodeTextArea selectedCode = getSelectedCode();
				if (selectedCode == null) return;
				undoButton.setEnabled(selectedCode.text.canUndo());
				redoButton.setEnabled(selectedCode.text.canRedo());
			}
		});

		fcode.text.addLineChangeListener(linelistener);
		vcode.text.addLineChangeListener(linelistener);
		tb.addSeparator();
		tb.add(makeToolbarButton("FIND"));
		tb.add(makeToolbarButton("GOTO"));
	}

	public CodeTextArea getSelectedCode() {
		int stab = editors.getSelectedIndex();

		if (stab == 0) {
			return vcode;
		} else if (stab == 1) {
			return fcode;
		}
		// do not know which tab you have selected
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		super.actionPerformed(ev);
		if (ev.getSource() == edit) {
			try {
				int stab = editors.getSelectedIndex();
				if (stab == 0) {
					if (vertexEditor == null) {
						vertexEditor = new ShaderEditor(EditorType.VERTEX);
					} else {
						vertexEditor.start();
					}
				} else if (stab == 1) {
					if (fragmentEditor == null) {
						fragmentEditor = new ShaderEditor(EditorType.FRAGMENT);
					} else {
						fragmentEditor.start();
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			return;
		}

		String com = ev.getActionCommand();

		CodeTextArea selectedCode = getSelectedCode();

		switch (com) {
			case "JoshText.LOAD":
				selectedCode.text.Load();
				break;
			case "JoshText.SAVE":
				selectedCode.text.Save();
				break;
			case "JoshText.PRINT":
				try {
					selectedCode.Print();
				} catch (PrinterException e) {
					LGM.showDefaultExceptionHandler(e);
				}
				break;
			case "JoshText.UNDO":
				selectedCode.text.Undo();
				break;
			case "JoshText.REDO":
				selectedCode.text.Redo();
				break;
			case "JoshText.CUT":
				selectedCode.text.Cut();
				break;
			case "JoshText.COPY":
				selectedCode.text.Copy();
				break;
			case "JoshText.PASTE":
				selectedCode.text.Paste();
				break;
			case "JoshText.FIND":
				selectedCode.text.ShowFind();
				break;
			case "JoshText.GOTO":
				selectedCode.aGoto();
				break;
			case "JoshText.SELALL":
				selectedCode.text.SelectAll();
				break;
		}
	}

	private enum EditorType {VERTEX, FRAGMENT}

	private class ShaderEditor implements UpdateListener {
		public final FileChangeMonitor monitor;
		private final EditorType type;
		private final File f;

		public ShaderEditor(EditorType type) throws IOException {
			this.type = type;
			f = File.createTempFile(res.getName(), "." +
					(type == EditorType.VERTEX ? "vert" : "frag"), LGM.tempDir); //$NON-NLS-1$
			f.deleteOnExit();
			monitor = new FileChangeMonitor(f, SwingExecutor.INSTANCE);
			monitor.updateSource.addListener(this, true);
			start();
		}

		public void start() throws IOException {
			FileWriter out = null;
			try {
				out = new FileWriter(f);
				out.write(type == EditorType.VERTEX ? vcode.getTextCompat() : fcode.getTextCompat());
			} finally {
				if (out != null) {
					out.close();
				}
			}

			out.close();
			if (!Prefs.useExternalScriptEditor || Prefs.externalScriptEditorCommand == null)
				try {
					PlatformHelper.openEditor(monitor.file);
				} catch (UnsupportedOperationException e) {
					throw new UnsupportedOperationException("no internal or system script editor", e);
				}
			else
				Runtime.getRuntime().exec(
						String.format(Prefs.externalScriptEditorCommand, monitor.file.getAbsolutePath()));
		}

		public void stop() {
			monitor.stop();
			monitor.file.delete();
		}

		public void updated(UpdateEvent e) {
			if (!(e instanceof FileUpdateEvent)) return;
			switch (((FileUpdateEvent) e).flag) {
				case CHANGED:
					StringBuffer sb = new StringBuffer(1024);
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new FileReader(monitor.file));
						char[] chars = new char[1024];
						int len = 0;
						while ((len = reader.read(chars)) > -1)
							sb.append(chars, 0, len);
					} catch (IOException ioe) {
						LGM.showDefaultExceptionHandler(ioe);
						return;
					} finally {
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException ex) {
								LGM.showDefaultExceptionHandler(ex);
							}
						}
					}
					String s = sb.toString();
					if (type == EditorType.VERTEX) {
						res.put(PShader.VERTEX, s);
						vcode.setText(s);
					} else {
						res.put(PShader.FRAGMENT, s);
						fcode.setText(s);
					}
					break;
				case DELETED:
					if (type == EditorType.VERTEX) {
						vertexEditor = null;
					} else {
						fragmentEditor = null;
					}
			}
		}
	}
}
