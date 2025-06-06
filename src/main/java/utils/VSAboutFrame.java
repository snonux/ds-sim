package utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

//import utils.*;
import prefs.VSPrefs;

/**
 * The class VSAboutFrame. This class is only for the about window which
 * shows up if selected in the GUI.
 *
 * @author Paul C. Buetow
 */
public class VSAboutFrame extends VSFrame {
    /** The prefs. */
    private VSPrefs prefs;

    /**
     * Instantiates a new VSAboutFrame object.
     *
     * @param prefs the prefs
     * @param relativeTo the component to open the about window relative to
     */
    public VSAboutFrame(VSPrefs prefs, Component relativeTo) {
        super(prefs.getString("lang.name") + " - "
              + prefs.getString("lang.about"), relativeTo);
        this.prefs = prefs;

        disposeWithParent();
        setContentPane(createContentPane());
        setSize(350, 250);
        setResizable(false);
        setVisible(true);
    }

    /**
     * Creates the content pane.
     *
     * @return the container
     */
    public Container createContentPane() {
        Container contentPane = getContentPane();

        VSInfoArea infoArea = new VSInfoArea(
            prefs.getString("lang.about.info"));
        JPanel buttonPane = createButtonPanel();
        JScrollPane scrollPane = new JScrollPane(infoArea);

        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        return contentPane;
    }

    /**
     * Creates the button panel.
     *
     * @return the panel
     */
    public JPanel createButtonPanel() {
        JPanel buttonPane = new JPanel();
        buttonPane.setBackground(Color.WHITE);

        JButton closeButton = new JButton(
            prefs.getString("lang.close"));
        closeButton.setMnemonic(prefs.getInteger("keyevent.close"));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String actionCommand = e.getActionCommand();
                if (actionCommand.equals(prefs.getString("lang.close")))
                    dispose();
            }
        });
        buttonPane.add(closeButton);

        return buttonPane;
    }
}
