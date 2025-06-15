package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

/**
 * Splash screen for the DS-Sim application
 * 
 * @author Paul C. Buetow
 */
public class VSSplashScreen extends JWindow {
    private static final int DISPLAY_TIME = 3000; // 3 seconds
    
    /**
     * Creates and shows the splash screen
     */
    public VSSplashScreen() {
        initSplashScreen();
    }
    
    /**
     * Initialize the splash screen components
     */
    private void initSplashScreen() {
        try {
            // Load the splash image from resources
            InputStream imageStream = getClass().getResourceAsStream("/splash.png");
            if (imageStream == null) {
                // Fallback if image not found
                showTextSplash();
                return;
            }
            
            BufferedImage splashImage = ImageIO.read(imageStream);
            
            // Reduce size by 60% (scale to 40% of original)
            int scaledWidth = (int)(splashImage.getWidth() * 0.4);
            int scaledHeight = (int)(splashImage.getHeight() * 0.4);
            
            Image scaledImage = splashImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            ImageIcon splashIcon = new ImageIcon(scaledImage);
            
            JLabel splashLabel = new JLabel(splashIcon);
            splashLabel.setHorizontalAlignment(JLabel.CENTER);
            
            getContentPane().add(splashLabel, BorderLayout.CENTER);
            
            // Set window properties
            setSize(scaledWidth, scaledHeight);
            setLocationRelativeTo(null); // Center on screen
            
        } catch (IOException e) {
            // Fallback to text splash if image loading fails
            showTextSplash();
        }
    }
    
    /**
     * Fallback text-based splash screen
     */
    private void showTextSplash() {
        JLabel textLabel = new JLabel("DS-Sim", JLabel.CENTER);
        textLabel.setFont(new Font("Arial", Font.BOLD, 24));
        textLabel.setPreferredSize(new Dimension(300, 100));
        
        getContentPane().add(textLabel, BorderLayout.CENTER);
        setSize(300, 100);
        setLocationRelativeTo(null);
    }
    
    /**
     * Shows the splash screen for the specified duration
     */
    public void showSplash() {
        setVisible(true);
        
        // Use a timer to hide the splash screen after the specified time
        Timer timer = new Timer(DISPLAY_TIME, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }
}