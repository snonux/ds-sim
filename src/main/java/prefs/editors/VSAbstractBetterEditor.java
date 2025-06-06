package prefs.editors;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import prefs.VSPrefs;

/**
 * The class VSAbstractBetterEditor, is an improved VSAbstractEditor.
 *
 * @author Paul C. Buetow
 */
public abstract class VSAbstractBetterEditor extends VSAbstractEditor {
    /** The content pane. */
    private Container contentPane;

    /** The title. */
    private String title;

    /**
     * An simple constructor.
     *
     * @param prefs the prefs
     * @param prefsToEdit the prefs to edit
     * @param title the title
     */
    public VSAbstractBetterEditor(VSPrefs prefs, VSPrefs prefsToEdit,
                                  String title) {
        super(prefs, prefsToEdit);
        this.title = title;
        this.contentPane = createContentPane();
    }

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the content pane.
     *
     * @return the content pane
     */
    public Container getContentPane() {
        contentPane.setBackground(Color.WHITE);
        return contentPane;
    }

    /**
     * Creates the content pane.
     *
     * @return the j panel
     */
    private JPanel createContentPane() {
        JPanel panel  = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel editPanel = getEditPanel();
        JPanel buttonPanel = getButtonPanel();

        panel.add(editPanel);
        panel.add(buttonPanel);

        return panel;
    }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractEditor#addToButtonPanelFront(
     *	javax.swing.JPanel)
     */
    protected void addToButtonPanelFront(JPanel buttonPanel) { }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractEditor#addToButtonPanelLast(
     *	javax.swing.JPanel)
     */
    protected void addToButtonPanelLast(JPanel buttonPanel) { }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractEditor#addToEditTableLast()
     */
    protected void addToEditTableLast() { }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractEditor#actionPerformed(
     *	java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        //String actionCommand = e.getActionCommand();
        /* More action in the super class!!! */
        super.actionPerformed(e);
    }
}
