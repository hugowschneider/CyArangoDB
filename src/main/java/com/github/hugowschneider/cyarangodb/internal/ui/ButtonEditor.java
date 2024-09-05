package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A custom cell editor for a JTable that uses a JButton.
 * When the button is pressed, it triggers an action event.
 */
public class ButtonEditor extends DefaultCellEditor {
    /**
     * The label of the button.
     */
    private String label;

    /**
     * Indicates whether the button has been pushed.
     */
    private boolean isPushed;

    /**
     * The action command to be sent when the button is pressed.
     */
    private String action;

    /**
     * The JButton used for editing.
     */
    private JButton button;

    /**
     * The ActionListener to be notified when the button is pressed.
     */
    private ActionListener actionListener;

    /**
     * Constructs a new ButtonEditor.
     *
     * @param checkBox       the JCheckBox used to delegate the cell editing
     * @param action         the action command to be sent when the button is pressed
     * @param actionListener the ActionListener to be notified when the button is pressed
     */
    public ButtonEditor(JCheckBox checkBox, String action, ActionListener actionListener) {
        super(checkBox);
        this.action = action;
        this.actionListener = actionListener;
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                fireEditingStopped();
            }
        });
    }

    /**
     * Returns the component used for editing.
     *
     * @param table      the JTable that is asking the editor to edit
     * @param value      the value of the cell to be edited
     * @param isSelected true if the cell is to be rendered with highlighting
     * @param row        the row of the cell being edited
     * @param column     the column of the cell being edited
     * @return the component for editing
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(action);
        isPushed = true;
        return button;
    }

    /**
     * Returns the value contained in the editor.
     *
     * @return the value contained in the editor
     */
    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action));
        }
        isPushed = false;
        return label;
    }

    /**
     * Stops editing and returns true to indicate that editing has stopped.
     *
     * @return true to indicate that editing has stopped
     */
    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    /**
     * Notifies all listeners that editing has stopped.
     */
    @Override
    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }
}