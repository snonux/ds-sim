package prefs.editors;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

import core.VSInternalProcess;
import events.VSRegisteredEvents;
import prefs.VSPrefs;
import protocols.VSAbstractProtocol;

/**
 * The class VSProcessEditor, is for editing a VSInternalProcess object.
 *
 * @author Paul C. Buetow
 */
public class VSProcessEditor extends VSAbstractBetterEditor {
    /** The process. */
    private VSInternalProcess process;

    /** The TAKEOVE r_ button. */
    public static boolean TAKEOVER_BUTTON;

    /**
     * Instantiates a new VSProcessEditor object.
     *
     * @param prefs the prefs
     * @param process the process
     */
    public VSProcessEditor(VSPrefs prefs, VSInternalProcess process) {
        super(prefs, process, prefs.getString("lang.name") + " - " +
              prefs.getString("lang.prefs.process"));;
        this.process = process;
        disposeFrameWithParentIfExists();
        makeProtocolVariablesEditable();
    }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractBetterEditor#addToButtonPanelFront(
     *	javax.swing.JPanel)
     */
    protected void addToButtonPanelFront(JPanel buttonPanel) {
        JButton takeoverButton = new JButton(
            prefs.getString("lang.takeover"));
        takeoverButton.setMnemonic(prefs.getInteger("keyevent.takeover"));
        takeoverButton.addActionListener(this);
        buttonPanel.add(takeoverButton);
    }

    /**
     * Make protocol variables editable.
     */
    protected void makeProtocolVariablesEditable() {
        ArrayList<String> editableProtocolsClassnames =
            VSRegisteredEvents.getEditableProtocolsClassnames();

        //String protocolString = " " + prefs.getString("lang.protocol");
        String clientString = " " + prefs.getString("lang.client");
        String serverString = " " + prefs.getString("lang.server");

        for (String protocolClassname : editableProtocolsClassnames) {
            String protocolShortname =
                VSRegisteredEvents.getShortnameByClassname(
                    protocolClassname);
            VSAbstractProtocol protocol =
                process.getProtocolObject(protocolClassname);
            protocol.onClientInit();
            protocol.onServerInit();

            ArrayList<String> clientVariables =
                VSRegisteredEvents.getProtocolClientVariables(
                    protocolClassname);
            if (clientVariables != null)
                addToEditor(protocolShortname + clientString,
                            protocolShortname, protocol, clientVariables);

            ArrayList<String> serverVariables =
                VSRegisteredEvents.getProtocolServerVariables(
                    protocolClassname);
            if (serverVariables != null)
                addToEditor(protocolShortname + serverString,
                            protocolShortname, protocol, serverVariables);
        }
    }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractBetterEditor#actionPerformed(
     *	java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals(prefs.getString("lang.ok"))) {
            savePrefs();
            process.updateFromPrefs();

        } else if (actionCommand.equals(prefs.getString("lang.takeover"))) {
            savePrefs();
            process.updateFromPrefs();

        } else {
            super.actionPerformed(e);
        }
    }
}
