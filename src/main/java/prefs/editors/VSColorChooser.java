package prefs.editors;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefs.VSPrefs;

/**
 * The class VSColorChooser, is for selecting a color within an editor.
 *
 * @author Paul C. Buetow
 */
public class VSColorChooser extends JPanel implements ChangeListener {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The color chooser. */
    protected JColorChooser colorChooser;

    /** The color. */
    private Color color;

    /** The val field. */
    private JTextField valField;

    /**
     * Instantiates a new VSColorChooser object.
     *
     * @param prefs the prefs
     * @param valField the val field
     */
    public VSColorChooser(VSPrefs prefs, JTextField valField) {
        super(new BorderLayout());
        this.color = valField.getBackground();
        this.valField = valField;

        colorChooser = new JColorChooser(Color.yellow);
        colorChooser.setColor(color);
        colorChooser.getSelectionModel().addChangeListener(this);
        colorChooser.setBorder(BorderFactory.createTitledBorder(
                                   prefs.getString("lang.colorchooser2")));
        add(colorChooser, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see javax.swing.event.ChangeListener#stateChanged(
     *	javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        Color newColor = colorChooser.getColor();
        valField.setBackground(newColor);
        valField.repaint();
    }
}
