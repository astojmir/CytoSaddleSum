//
// ===========================================================================
//
//                            PUBLIC DOMAIN NOTICE
//               National Center for Biotechnology Information
//
//  This software/database is a "United States Government Work" under the
//  terms of the United States Copyright Act.  It was written as part of
//  the author's official duties as a United States Government employee and
//  thus cannot be copyrighted.  This software/database is freely available
//  to the public for use. The National Library of Medicine and the U.S.
//  Government have not placed any restriction on its use or reproduction.
//
//  Although all reasonable efforts have been taken to ensure the accuracy
//  and reliability of the software and data, the NLM and the U.S.
//  Government do not and cannot warrant the performance or results that
//  may be obtained by using this software or data. The NLM and the U.S.
//  Government disclaim all warranties, express or implied, including
//  warranties of performance, merchantability or fitness for any particular
//  purpose.
//
//  Please cite the author in any work or product based on this material.
//
// ===========================================================================
//
// Code authors:  Aleksandar Stojmirovic, Alexander Bliskovsky
//


package gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main;

import cytoscape.CyNetwork;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ResultsPanelManager;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.Configuration;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyMenus;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.JMenu;
import javax.swing.SwingConstants;
import javax.swing.event.MenuEvent;


public class CytoSaddleSum extends CytoscapePlugin {

    private InPanel queryPanel = null;
    private String title = "CytoSaddleSum";
    private String docUrl = "http://www.ncbi.nlm.nih.gov/CBBresearch/Yu/mn/enrich/doc/documentation.html";
    
    public CytoSaddleSum() {
        
        ResultsPanelManager.initResultsPanelManager();
        Configuration.initConfiguration(); 

        // Plugins menu item
        CyMenus menus = Cytoscape.getDesktop().getCyMenus();
        addAction(menus, title, new SaddleSumFormAction("Query Form"));
        addSeparator(menus, title);
        addAction(menus, title, new AboutAction(("About")));
        addAction(menus, title, new DocsAction(("Documentation")));
        
        // Import/Export menu
        addExportAction(menus, "txt");
        addExportAction(menus, "tab");
        addImportAction(menus);
    }

    private static void addAction(CyMenus aMenus, String aSubMenu, 
                                  CytoscapeAction aAction) {
            aAction.setPreferredMenu("Plugins." + aSubMenu);
            aMenus.addCytoscapeAction(aAction);
    }

    private void addExportAction(CyMenus aMenus, String extension) {
            CytoscapeAction aAction = new SaddleSumExportAction(extension);
            aAction.setPreferredMenu("File.Export");
            aMenus.addCytoscapeAction(aAction);
    }

    private void addImportAction(CyMenus aMenus) {
            CytoscapeAction aAction = new SaddleSumImportAction();
            aAction.setPreferredMenu("File.Import");
            aMenus.addCytoscapeAction(aAction);
    }

    private static void addSeparator(CyMenus aMenus, String aSubMenu) {
        JMenu menuItem = null;
        for (final Component cmp : aMenus.getOperationsMenu().getMenuComponents()) {
                if (cmp instanceof JMenu && aSubMenu.equals(((JMenu) cmp).getText())) {
                        menuItem = (JMenu) cmp;
                        break;
                }
        }
        if (menuItem != null) {
                menuItem.addSeparator();
        }        
    }
    
    public class SaddleSumFormAction extends CytoscapeAction {
        public SaddleSumFormAction(String itemName) {
            super(itemName);
        }
        public void actionPerformed(ActionEvent e) {
            CytoPanel ctrlPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
            if (queryPanel == null) {
                queryPanel = new InPanel();
                ctrlPanel.add("SaddleSum", queryPanel);
            }
            if (ctrlPanel.getState() == CytoPanelState.HIDE) {
                ctrlPanel.setState(CytoPanelState.DOCK);
            }
            ctrlPanel.setSelectedIndex(ctrlPanel.indexOfComponent(queryPanel));            
        }
    }
    
    public class AboutAction extends CytoscapeAction {
        public AboutAction(String itemName) {
            super(itemName);
        }
        public void actionPerformed(ActionEvent e) {
            AboutDialog aboutDialog = new AboutDialog(Cytoscape.getDesktop(), 
                                                      true);
            aboutDialog.setLocationRelativeTo(Cytoscape.getDesktop());
            aboutDialog.setVisible(true);
        }
    }
    
    public class DocsAction extends CytoscapeAction {
        public DocsAction(String itemName) {
            super(itemName);
        }
        public void actionPerformed(ActionEvent e) {
            cytoscape.util.OpenBrowser.openURL(docUrl);
        }
    }

    public class SaddleSumExportAction extends CytoscapeAction {
        private String extension;
        private String title;
        public SaddleSumExportAction(String extension) {
            super("SaddleSum Results as " + extension.toUpperCase() + " File...");
            this.extension = extension;
            title = "Export SaddleSum Results as " + extension.toUpperCase() + " File";
        }
        public void actionPerformed(ActionEvent e) {
            CyNetwork termNetwork = Cytoscape.getCurrentNetwork();
            MainTasks.exportResults(termNetwork, extension, title);
        }
        @Override
        public void menuSelected(MenuEvent e) {
            CyNetwork cyNetwork = Cytoscape.getCurrentNetwork();
            String networkId = cyNetwork.getIdentifier();
            if( ResultsPanelManager.findQueryPrefix(networkId) != null ) {
                enableForNetwork();
            }
            else {
                setEnabled(false);
            }
        }

    }
    
    public class SaddleSumImportAction extends CytoscapeAction {
        public SaddleSumImportAction() {
            super("Import SaddleSum Results from TAB File...");
            title = "Export SaddleSum Results from TAB File";
        }
        public void actionPerformed(ActionEvent e) {
            MainTasks.importResults(title);
        }
    }
    
}
