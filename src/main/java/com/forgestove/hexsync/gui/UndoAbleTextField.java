package com.forgestove.hexsync.gui;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
public class UndoAbleTextField extends JTextField {
	private final UndoManager undoManager = new UndoManager();
	public UndoAbleTextField(String text) {
		super(text);
		initUndo();
	}
	private void initUndo() {
		this.getDocument().addUndoableEditListener(undoManager);
		this.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
		this.getActionMap().put("Undo", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				if (undoManager.canUndo()) undoManager.undo();
			}
		});
		this.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
		this.getActionMap().put("Redo", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				if (undoManager.canRedo()) undoManager.redo();
			}
		});
	}
}
