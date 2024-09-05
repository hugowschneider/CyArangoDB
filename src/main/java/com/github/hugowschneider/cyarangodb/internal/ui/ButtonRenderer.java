package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * A custom cell renderer for a JTable that uses a JButton.
 * This renderer displays a button with the specified text in each cell.
 */
public class ButtonRenderer extends JButton implements TableCellRenderer {

    /**
     * Constructs a new ButtonRenderer with the specified text.
     *
     * @param text the text to display on the button
     */
    public ButtonRenderer(String text) {
        setOpaque(true);
        setText(text);
    }

    /**
     * Returns the component used for rendering the cell.
     *
     * @param table      the JTable that is asking the renderer to draw
     * @param value      the value of the cell to be rendered
     * @param isSelected true if the cell is to be rendered with highlighting
     * @param hasFocus   if true, render cell appropriately
     * @param row        the row of the cell being rendered
     * @param column     the column of the cell being rendered
     * @return the component for rendering the cell
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}