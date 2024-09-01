package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer(String text) {
        setOpaque(true);
        setText(text);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}