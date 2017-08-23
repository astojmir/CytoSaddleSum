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
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.view.CyNetworkView;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ResultsPanelManager;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main.TermGraph;
import java.util.Vector;
import java.util.Arrays;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class OutputPanel extends JPanel {
    
    private TermGraph termGraph;
    private CyNetwork termNetwork;
    private String queryPrefix;
    private Integer nomenclatureWarningsCount = 0;
    private Integer unknownIdsCount = 0;
    private Integer resultSectionCount = 0;
    private final ArrayList<JTable> resultTables;
    private final ArrayList<SelectionListener> tableListeners;


    public OutputPanel(CyNetwork termNetwork) {
	super();
        this.termNetwork = termNetwork;
        termGraph = new TermGraph(termNetwork);
        queryPrefix = termGraph.queryPrefix;
        resultTables = new ArrayList<JTable>();
        tableListeners = new ArrayList<SelectionListener>();
                
        setFont(new java.awt.Font("Arial", 0, 14));
        setMinimumSize(new java.awt.Dimension(380, 400));

        initSummaryPanel();
        initWarningsPanel();
        initUnknownIdsPanel();
        initResultsPanel();

	/*********************
         *   Tabbed Pane     *
         ********************/

	JTabbedPane outputTabbedPane = new JTabbedPane();
        outputTabbedPane.setForeground(new java.awt.Color(0, 51, 102));
        outputTabbedPane.setFont(new java.awt.Font("Arial", 0, 14));
        outputTabbedPane.setMinimumSize(new java.awt.Dimension(380, 400));
        outputTabbedPane.setPreferredSize(new java.awt.Dimension(380, 400));
               
	if (resultSectionCount > 0) {
	    outputTabbedPane.add("Results", resultsScrollPane);           
        }
        else {
	    outputTabbedPane.add("No Results", resultsScrollPane);
	}
        
	outputTabbedPane.add("Summary", summaryPanel);

	String warningTabString = "Warnings (" + nomenclatureWarningsCount + ")";
	String uidsString = "Unknown IDs (" + unknownIdsCount + ")";
	outputTabbedPane.add(warningTabString, warningsPanel);
	outputTabbedPane.add(uidsString, unknownIdsPanel);

	if (resultSectionCount == 0) {
	    outputTabbedPane.setSelectedIndex(1);
        }
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(outputTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 443, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(outputTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
        );
    }
    
    
    private void initSummaryPanel() {
	summaryPanel = new JPanel();
        summaryPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        summaryPanel.setLayout(new javax.swing.BoxLayout(summaryPanel, javax.swing.BoxLayout.LINE_AXIS));
        

        Vector summaryData = termGraph.getParamsAsNetworkAttrs('S');
	String[] summaryColumnNames = {"Field", "Value"};
	Vector summaryColumnNamesVector = new Vector(Arrays.asList(summaryColumnNames));
        
        jScrollPane2 = new JScrollPane();
        jScrollPane2.setFont(new java.awt.Font("Arial", 0, 12));

        summaryTable = new JTable();    
        summaryTable.setFont(new java.awt.Font("Arial", 0, 12));
        summaryTable.setModel(new javax.swing.table.DefaultTableModel(
            summaryData,
            summaryColumnNamesVector)
        {
            @Override
            public Class getColumnClass(int columnIndex) {
                return java.lang.String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
        summaryTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        summaryTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(summaryTable);

        summaryPanel.add(jScrollPane2);        
    }
    
    private void initWarningsPanel() {

        warningsPanel = new JPanel();
        warningsPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        warningsPanel.setLayout(new javax.swing.BoxLayout(warningsPanel, javax.swing.BoxLayout.LINE_AXIS));
        
        warningsPane = new JScrollPane();
        warningsPane.setFont(new java.awt.Font("Arial", 0, 12));

        List<String> warnings = termGraph.getWarnings();       
	Vector wColumnNames = new Vector();
	wColumnNames.add("Warning");
        Vector wRowData = new Vector();
	

        if (warnings != null) {
            nomenclatureWarningsCount = warnings.size();
            for (String wrn : warnings) {
                Vector tempVector = new Vector();
                tempVector.add(wrn);
                wRowData.add(tempVector);
            }
        }
	warningsTable = new JTable();    
        warningsTable.setFont(new java.awt.Font("Arial", 0, 12));
        warningsTable.setModel(new javax.swing.table.DefaultTableModel(
            wRowData,
            wColumnNames)
        {
            @Override
            public Class getColumnClass(int columnIndex) {
                return java.lang.String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
	warningsTable.getTableHeader().setVisible(false);
	warningsTable.setFillsViewportHeight(true);
	warningsTable.getTableHeader().setPreferredSize(new Dimension(-1, 0));

        warningsPane.setViewportView(warningsTable);
        warningsPanel.add(warningsPane);        
    }
    
    private void initUnknownIdsPanel() {

        unknownIdsPanel = new JPanel();
        unknownIdsPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        unknownIdsPanel.setLayout(new javax.swing.BoxLayout(unknownIdsPanel, javax.swing.BoxLayout.LINE_AXIS));

        uidScrollPane = new JScrollPane();
        
        unknownIdsText = new JTextArea();
        unknownIdsText.setColumns(20);
        unknownIdsText.setEditable(false);
        unknownIdsText.setFont(new java.awt.Font("Arial", 0, 12));
        unknownIdsText.setLineWrap(true);
        unknownIdsText.setRows(5);
        unknownIdsText.setWrapStyleWord(true);

        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();        
        String unknownIds = termGraph.getUnknownIds();
        
        if (unknownIds != null) {
            unknownIdsCount = termGraph.getUnknownIdsCount();
            unknownIdsText.setText(unknownIds);
        }
        uidScrollPane.setViewportView(unknownIdsText);
        unknownIdsPanel.add(uidScrollPane);        
    }
    
    private void initResultsPanel() {

        resultsPanel = new JPanel();
	resultsScrollPane = new JScrollPane();
	resultsScrollPane.setViewportView(resultsPanel);
	resultsPanel.setLayout(new GridBagLayout());

	GridBagConstraints c = new GridBagConstraints();

	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 1;
	c.weighty = 0.1;
	c.anchor = GridBagConstraints.NORTH;
	c.fill = GridBagConstraints.HORIZONTAL;
	Insets defaultInsets = new Insets(0, 10, 0, 10);
	Insets offsetInsets = new Insets(15,10,5,10);

        
        Vector columnNamesVector = termGraph.getHitTableHeadings(false);
        HashMap<String, Vector<Vector>> tables = termGraph.getNamespaceTables(false);
	for (String namespace : tables.keySet()) {
            resultSectionCount++;
            final Vector<Vector> rowData = tables.get(namespace);

	    JLabel sectionLabel = new JLabel(namespace.replace('_', ' ').toUpperCase());
            sectionLabel.setFont(new java.awt.Font("Arial", 1, 14));
            sectionLabel.setForeground(new java.awt.Color(51, 102, 153));

	    final JTable outputTable = new JTable();
            resultTables.add(outputTable);
            outputTable.setModel(new javax.swing.table.DefaultTableModel(
                rowData,
                columnNamesVector)
                    {
                        @Override
                        public Class getColumnClass(int columnIndex) {
                            return java.lang.String.class;
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                            return false;
                        }
                    });
            outputTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            outputTable.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getComponent().isEnabled() && 
                        e.getButton() == MouseEvent.BUTTON1 && 
                        e.getClickCount() == 2)
                    {
                        Point p = e.getPoint();
                        int rowIndex = outputTable.rowAtPoint(p);
                        int row = outputTable.convertRowIndexToModel(rowIndex);
                        try {
                            String termId = (String) rowData.get(row).get(0);
                        
                            // Select the corresponding node in termNetwork and
                            // (if possible), all entity nodes in the main Network
                            String termNodeId = selectTermNode(termId);
                            selectMainNodes(termNodeId);
                        }
                        catch (Exception ex) {}
                    }
                    
                }
            });


	    int rows = rowData.size();
	    int rowHeight = outputTable.getRowHeight();
	    int preferredHeight = rowHeight * (Math.min(15, rows) + 2 );
	    outputTable.setFillsViewportHeight(true);
            outputTable.setFont(new java.awt.Font("Arial", 0, 12));
	    JScrollPane tableScrollPane = new JScrollPane(outputTable);
	    Dimension d = outputTable.getPreferredSize();
	    tableScrollPane.setPreferredSize(new Dimension(d.width,
							   preferredHeight));

	    outputTable.getColumnModel().getColumn(0).setPreferredWidth(220);
	    outputTable.getColumnModel().getColumn(1).setPreferredWidth(600);
	    outputTable.getColumnModel().getColumn(2).setPreferredWidth(100);
	    outputTable.getColumnModel().getColumn(3).setPreferredWidth(100);
	    outputTable.getColumnModel().getColumn(4).setPreferredWidth(200);

            SelectionListener listener = new SelectionListener(outputTable);
            outputTable.getSelectionModel().addListSelectionListener(listener);
            tableListeners.add(listener);

	    c.insets = offsetInsets;
	    resultsPanel.add(sectionLabel, c);
	    c.gridy++;

	    c.insets = defaultInsets;
	    resultsPanel.add(tableScrollPane, c);

	    c.gridy++;
	}

	c.weighty = 1;
	c.gridheight = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 10, 10, 10);
	resultsPanel.add(new JLabel(""), c);        
    }
    
    private String selectTermNode(String TermId) {
        CyAttributes cyNodeAttrs = Cytoscape.getNodeAttributes();
        for (int rootIndex : termNetwork.getNodeIndicesArray()) {
            CyNode node = (CyNode) termNetwork.getNode(rootIndex);
            String nodeId = node.getIdentifier();

            String tid = cyNodeAttrs.getStringAttribute(nodeId, "TermId");
            if (tid != null && TermId.equals(tid)) {
                termNetwork.unselectAllNodes();
                termNetwork.unselectAllEdges();
                termNetwork.setSelectedNodeState(node, true);
                String networkId = termNetwork.getIdentifier();
                Cytoscape.getDesktop().setFocus(networkId);   
                CyNetworkView view = Cytoscape.getNetworkView(networkId);
                view.redrawGraph(false, true);
                return nodeId;
            }
        }
        return null;
    }
    
    private void selectMainNodes(String termNodeId) {
        CyNetwork mainNetwork = ResultsPanelManager.findMasterNetwork(queryPrefix);
        if (mainNetwork == null || termNodeId == null) {
            return;
        }
        CyAttributes cyNodeAttrs = Cytoscape.getNodeAttributes();     
        List<String> origNodeIds = cyNodeAttrs.getListAttribute(termNodeId, 
                                                                "origNodes");
        if (origNodeIds == null || origNodeIds.isEmpty()) {
            return;
        }
        HashSet<String> origNodeIdsSet = new HashSet<String>(origNodeIds);
        mainNetwork.unselectAllNodes();
        mainNetwork.unselectAllEdges();
        for (int rootIndex : mainNetwork.getNodeIndicesArray()) {
            CyNode node = (CyNode) mainNetwork.getNode(rootIndex);
            String nodeId = node.getIdentifier();
            String nodeName = cyNodeAttrs.getStringAttribute(nodeId, 
                                                      Semantics.CANONICAL_NAME);
            if (nodeName != null && origNodeIdsSet.contains(nodeName)) {
                mainNetwork.setSelectedNodeState(node, true);
            }
        }        
        String networkId = mainNetwork.getIdentifier();
        Cytoscape.getDesktop().setFocus(networkId);   
        CyNetworkView view = Cytoscape.getNetworkView(networkId);
        view.redrawGraph(false, true);
    }
    
    private class SelectionListener implements ListSelectionListener {
        JTable table;

        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(JTable table) {
            this.table = table;
        }
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                for (int i=0; i < resultTables.size(); i++) {
                    JTable tbl = resultTables.get(i);
                    if (tbl != table && tbl.getSelectedRowCount() > 0) {
                        ListSelectionModel slc = tbl.getSelectionModel();
                        slc.removeListSelectionListener(tableListeners.get(i));
                        tbl.clearSelection();
                        slc.addListSelectionListener(tableListeners.get(i));
                    }                    
                }
            }
        }
    }
    
    public CyNetwork getTermNetwork() {
        return termNetwork;
    }

    public String getQueryPrefix() {
        return queryPrefix;
    }
    
    
    private JPanel summaryPanel;
    private JScrollPane jScrollPane2;
    private JTable summaryTable;
    
    private JPanel warningsPanel;
    private JScrollPane warningsPane;
    private JTable warningsTable;
    
    private JPanel unknownIdsPanel;
    private JScrollPane uidScrollPane;
    private JTextArea unknownIdsText;
    
    private JPanel resultsPanel;
    private JScrollPane resultsScrollPane;
    
    
}
