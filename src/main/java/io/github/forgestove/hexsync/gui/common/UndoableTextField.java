package io.github.forgestove.hexsync.gui.common;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
public class UndoableTextField extends JTextField {
	private final UndoManager undoManager = new UndoManager();
	public UndoableTextField(String text) {
		super(text);
		initUndo();
	}
	private void initUndo() {
		getDocument().addUndoableEditListener(undoManager);
		getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
		getActionMap().put(
			"Undo", new AbstractAction() {
				public void actionPerformed(ActionEvent event) {
					if (undoManager.canUndo()) undoManager.undo();
				}
			}
		);
		getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
		getActionMap().put(
			"Redo", new AbstractAction() {
				public void actionPerformed(ActionEvent event) {
					if (undoManager.canRedo()) undoManager.redo();
				}
			}
		);
	}
}
