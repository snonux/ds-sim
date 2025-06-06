package prefs.editors;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import prefs.VSPrefs;
import utils.VSFrame;

/**
 * The class VSEditorFrame, this is a wrapper around an VSAbstractEditor
 * object, which should be displayed in its own frame.
 *
 * @author Paul C. Buetow
 */
public class VSEditorFrame extends VSFrame implements ActionListener {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The editor. */
    private VSAbstractBetterEditor editor;

    /** The prefs. */
    private VSPrefs prefs;

    /**
     * Instantiates a new VSEditorFrame object.
     *
     * @param prefs the prefs
     * @param relativeTo the relative to
     * @param editor the editor
     */
    public VSEditorFrame(VSPrefs prefs, Component relativeTo,
                         VSAbstractBetterEditor editor) {
        super(editor.getTitle(), relativeTo);
        this.prefs = prefs;
        this.editor = editor;
        init();
    }

    /**
     * Inits the VSEditorFrame object.
     */
    private void init() {
        editor.setFrame(this);
        fillButtonPanel(editor.getButtonPanel());
        setContentPane(editor.getContentPane());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(prefs.getInteger("div.window.prefs.xsize"),
                prefs.getInteger("div.window.prefs.ysize"));
        setResizable(false);
        setVisible(true);
    }

    /**
     * Fills the button panel.
     *
     * @param buttonPanel the button panel
     */
    private void fillButtonPanel(JPanel buttonPanel) {
        JButton okButton = new JButton(
            prefs.getString("lang.ok"));
        okButton.setMnemonic(prefs.getInteger("keyevent.ok"));
        okButton.addActionListener(this);
        buttonPanel.add(okButton, 0);

        JButton cancelButton = new JButton(
            prefs.getString("lang.cancel"));
        cancelButton.setMnemonic(prefs.getInteger("keyevent.cancel"));
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton, 1);
        buttonPanel.repaint();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(
     *	java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals(prefs.getString("lang.ok"))) {
            editor.actionPerformed(e);
            dispose();

        } else if (actionCommand.equals(prefs.getString("lang.cancel"))) {
            editor.actionPerformed(e);
            dispose();

        } else {
            editor.actionPerformed(e);
        }
    }
}
