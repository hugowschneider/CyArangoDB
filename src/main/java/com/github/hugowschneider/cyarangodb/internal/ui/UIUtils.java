package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.imageio.ImageIO;
import java.awt.Window;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;

/**
 * Utility class for UI-related operations.
 */
public class UIUtils {

    /**
     * Constructs a new UIUtils.
     */
    public UIUtils() {
    }

    /**
     * Sets the icon image of a window.
     *
     * @param frame        the window frame to set the icon image for
     * @param resourcePath the path to the icon image resource
     */
    public static void setIconImage(Window frame, String resourcePath) {
        try {
            URL iconURL = frame.getClass().getClassLoader().getResource(resourcePath);
            if (iconURL != null) {
                Image icon = ImageIO.read(iconURL);
                frame.setIconImage(icon);
            } else {
                System.err.println("Icon resource not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}