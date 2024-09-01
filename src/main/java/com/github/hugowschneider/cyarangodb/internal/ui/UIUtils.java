package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.imageio.ImageIO;
import java.awt.Window;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;

public class UIUtils {
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