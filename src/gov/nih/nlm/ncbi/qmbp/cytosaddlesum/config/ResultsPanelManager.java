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

package gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config;

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main.OutputPanel;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;

/**
 *
 * @author stojmira
 */
public class ResultsPanelManager {
    
    private static HashMap<String, OutputPanel> panels;
    private static NetworkDestroyListener listener;
    
    public static void initResultsPanelManager() {
        panels = new HashMap<String, OutputPanel>();
        listener = new NetworkDestroyListener();
        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(
            Cytoscape.NETWORK_DESTROYED, listener);
    }
    
    public static void addResults(CyNetwork termNetwork, CyNetwork mainNetwork) {

        OutputPanel outPanel = new OutputPanel(termNetwork);
        String queryPrefix = outPanel.getQueryPrefix();
        if (mainNetwork != null) {
            insertQueryToMasterNetwork(queryPrefix, mainNetwork);
        }
        panels.put(queryPrefix, outPanel);
                
        CytoPanel resPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
        resPanel.add(queryPrefix, outPanel);
        resPanel.setSelectedIndex(resPanel.indexOfComponent(outPanel));
        if (resPanel.getState() == CytoPanelState.HIDE) {
                resPanel.setState(CytoPanelState.DOCK);
        }
    }
    
    public static void removeResults(String queryPrefix) {
        OutputPanel outPanel = panels.get(queryPrefix);
        if (outPanel != null) {
            panels.remove(queryPrefix);            
            CytoPanel resPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
            resPanel.remove(outPanel);
        }        
    }
        
    public static boolean hasResults(String queryPrefix) {
        return panels.containsKey(queryPrefix);        
    }

    public static boolean hasStoredResults(String termNetworkId) {
        String queryPrefix = findQueryPrefix(termNetworkId);
        if (queryPrefix != null && !panels.containsKey(queryPrefix)) {
            return true;
        }
        return false;
    }
    
    public static String findQueryPrefix(String termNetworkId) {
       CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
       String attr = cyNetworkAttrs.getStringAttribute(termNetworkId, 
                                                       "SaddleSumQueryId");
       return attr;        
    }
    
    public static CyNetwork findMasterNetwork(String queryPrefix) {
       CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
       Set<CyNetwork> allNetworks = Cytoscape.getNetworkSet();
       for (CyNetwork network : allNetworks) {
           String networkId = network.getIdentifier();
           List<String> queries = cyNetworkAttrs.getListAttribute(networkId, 
                                                            "SaddleSumQueries");
           if (queries != null && queries.contains(queryPrefix)) {
               return network;
           }
       }
       return null;
    }
    
    private static void insertQueryToMasterNetwork(String queryPrefix,
                                                   CyNetwork mainNetwork) {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        String networkId = mainNetwork.getIdentifier();
        List<String> queries = cyNetworkAttrs.getListAttribute(networkId, 
                                                            "SaddleSumQueries");
        if (queries != null && queries.contains(queryPrefix)) {
            return;
        }
        if (queries == null) {
            queries = new ArrayList<String>();
        }
        queries.add(queryPrefix);
        cyNetworkAttrs.setListAttribute(networkId, "SaddleSumQueries", queries);
    }

    private static class NetworkDestroyListener implements PropertyChangeListener {

        public NetworkDestroyListener() {
            super();
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_DESTROYED)) {
                String networkId = (String) e.getNewValue();
                String queryPrefix = findQueryPrefix(networkId);
                if (queryPrefix != null) {
                    removeResults(queryPrefix);
                }
            }
        }
    }
    
    
}
