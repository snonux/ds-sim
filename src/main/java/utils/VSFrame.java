package utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * The class VSFrame. All frames of the simulator extend this VSFrame class.
 * This class makes sure that all 'subwindows' get closed if its parent gets
 * closed. And it also makes sure to open new windows relative to its parent.
 *
 * @author Paul C. Buetow
 */
public class VSFrame extends JFrame {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The Constant X_LOCATION_OFFSET. */
    private final static int X_LOCATION_OFFSET = 40;

    /** The Constant Y_LOCATION_OFFSET. */
    private final static int Y_LOCATION_OFFSET = 80;

    /** The parent window/component. */
    private Component parent;

    /** True, if the current window will get disposed with its parent.  */
    private boolean dispose;

    /**
     * Instantiates a VSFrame object.
     *
     * @param title the title
     * @param parent the parent
     */
    public VSFrame(String title, Component parent) {
        super(title);
        init(parent);
    }

    /**
     * Instantiates a new VSFrame object.
     *
     * @param title the title
     */
    public VSFrame(String title) {
        super(title);
        init(null);
    }

    /**
     * Inits the VSFrame.
     *
     * @param parent the parent
     */
    private void init(Component parent) {
        this.parent = parent;
        this.dispose = false;
    }

    /**
     * Dispose with its parent.
     */
    public void disposeWithParent() {
        if (!dispose && parent != null && parent instanceof Window) {
            Window window = (Window) parent;
            window.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent we) {
                    VSFrame.this.dispose();
                }
            });
        }
        dispose = true;
    }

    /**
     * Sets the correct location of the window.
     */
    private void setCorrectLocation() {
        int x = 0, y = 0;
        final Dimension screenSize =
            Toolkit.getDefaultToolkit().getScreenSize();

        if (parent == null) {
            x = (int) (screenSize.width - getWidth()) / 2;
            y = 50;//(int) (screenSize.height - getHeight()) / 2;

        } else {
            final Point location = parent.getLocation();
            x = (int) location.getX() + X_LOCATION_OFFSET;
            y = (int) location.getY() + Y_LOCATION_OFFSET;
        }

        if (x + super.getWidth() >= screenSize.width)
            x = screenSize.width - super.getWidth();
        else if (x < 0)
            x = 0;

        if (y + super.getHeight() >= screenSize.height)
            y = screenSize.height - super.getHeight();

        super.setLocation(x, y);
    }

    /* (non-Javadoc)
     * @see java.awt.Window#setSize(int, int)
     */
    public void setSize(int width, int height) {
        super.setSize(width, height);
        setCorrectLocation();
    }
}
