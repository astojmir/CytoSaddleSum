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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * InPanel.java
 *
 * Created on Aug 30, 2011, 1:11:05 PM
 */
package gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main;

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ConfDialog;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.Configuration;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.tools.ETDFile;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.tools.BackendTools;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main.SaddleSumNetworkModel;

import cytoscape.Cytoscape;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CytoscapeDesktop;

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ResultsPanelManager;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

/**
 *
 * @author stojmira
 */
public class InPanel extends javax.swing.JPanel {

    private List<ETDFile> termDBs;
    private String ssumCommand;
    private Hashtable optionsTable;
    private Vector selectedNodes;
    private String currentNetworkId = null;
    private PropertyChangeListener networkListener;
    
    
    /** Creates new form InPanel */
    public InPanel() {
	initComponents();
        // reconfigureDatasets();
        resetFields();

        // Register a listener for network changes
        networkListener = new CurrentNetworkListener();
        CytoscapeDesktop cyDesktop = Cytoscape.getDesktop();
        cyDesktop.getSwingPropertyChangeSupport().addPropertyChangeListener(
                CytoscapeDesktop.NETWORK_VIEW_FOCUSED, networkListener);
        
        DatasetRefresh task = new DatasetRefresh();

        JTaskConfig jTaskConfig = new JTaskConfig();
        jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayCloseButton(false);
        jTaskConfig.displayCancelButton(false);
        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);
        jTaskConfig.displayTimeElapsed(true);
        jTaskConfig.displayTimeRemaining(false);

        TaskManager.executeTask(task, jTaskConfig);        
    }

    private String validateInput() {

        String cutoffType = cutoffDropdown.getSelectedItem().toString();
	Color errorColor = new Color(242,94,101);
	Color defaultColor = new Color(255,255,255);
	Color dropErrorColor = new Color(226,18,29);
	int errorCode;

	evalBox.setBackground(defaultColor);
	minTermDBSizeBox.setBackground(defaultColor);
	effTermDBSizeBox.setBackground(defaultColor);
	cutoffBox.setBackground(defaultColor);
	gmtFileBox.setBackground(defaultColor);
	cutoffDropdown.setBackground(tdbDropdown.getBackground());

        if (weightAttrDropdown.getItemCount() == 0) {
            return "The current network does not contain any node attribute" +
                    "\nthat can be used as weights for SaddleSum.";
        }
        
	if (Configuration.isLocalQuery() && 
            tdbDropdown.getSelectedIndex() == 0) {
	    // If custom file is selected, verify that the file exists
	    File gmtFile = new File(gmtFileBox.getText());

	    if (gmtFile.isDirectory()) {
		gmtFileBox.setBackground(errorColor);
		return "You must select a GMT file for input.";
	    }
	    else if (!gmtFile.exists()) {
		gmtFileBox.setBackground(errorColor);
		return "The specified GMT file does not exist.";
	    }
	}

	// Validate that E-value cutoff is a parsable number
	try {
	    if (new Float(evalBox.getText()) < 0){
		evalBox.setBackground(errorColor);
		errorCode = 1;
	    }
	    else {
                errorCode = 0;
            }
	}
	catch (NumberFormatException e) {
	    evalBox.setBackground(errorColor);
	    return "E-value cutoff must be a number.";

	}
	if (errorCode == 1){
	    evalBox.setBackground(errorColor);
	    return "E-Value must be positive.";
	}

	// Validate that Minimum term size is a parsable integer

	try {
	    int minTdbSize = new Integer(minTermDBSizeBox.getText());

	    if (minTdbSize < 1 || minTdbSize > 10000000){
		errorCode = 2;
	    }
	    else {
                errorCode = 0;
            }
	}
	catch (NumberFormatException e) {
	    minTermDBSizeBox.setBackground(errorColor);
	    return "Minimum term size must be an integer (do not use commas).";
	}
	if (errorCode == 2) {
	    minTermDBSizeBox.setBackground(errorColor);
	    return "Minimum term size must be between 1 and 10,000,000";
	}

	try {
	    Integer test = new Integer(effTermDBSizeBox.getText());
	}
	catch (NumberFormatException e) {
	    effTermDBSizeBox.setBackground(errorColor);
	    return "Effective term DB size must be an integer.";
	}

	// If selected statistic is Fisher's, a cutoff type and value must be specified.
	if ("One-Sided Fischer's Exact Test".equals(statsDropdown.getSelectedItem().toString())) {
	    if ("No Cutoff".equals(cutoffType)){
		cutoffDropdown.setBackground(dropErrorColor);
		return "You must use a cutoff with the One-Sided Fisher's Exact Test.";
	    }
	}

        if ("By Rank".equals(cutoffType)){
	    // Validate that the number is an integer
	    try {
		Integer test = new Integer(cutoffBox.getText());
	    }
	    catch (NumberFormatException e) {
		cutoffBox.setBackground(errorColor);
		return "Rank cutoff must be an integer.";
	    }
	}
	else if ("By Weight".equals(cutoffType)) {
	    // Validate that the the input is a number
	    try {
		Float test = new Float(cutoffBox.getText());
	    }
	    catch (NumberFormatException e) {
		cutoffBox.setBackground(errorColor);
		return "Weight cutoff must be a number.";
	    }
	}

	return null;
    }

    private void buildArgs() {

        String[] fields = new String[9];
	int termDBIndex = tdbDropdown.getSelectedIndex();
        fields[0] = tdbDropdown.getSelectedItem().toString();
        fields[1] = evalBox.getText();
        fields[2] = minTermDBSizeBox.getText();
        fields[3] = effTermDBSizeBox.getText();
        fields[4] = statsDropdown.getSelectedItem().toString();
        fields[5] = transformDropdown.getSelectedItem().toString();
        fields[6] = cutoffDropdown.getSelectedItem().toString();
        fields[7] = cutoffBox.getText();

        String weightAttr = (String) weightAttrDropdown.getSelectedItem();
	selectedNodes = SaddleSumNetworkModel.getNetworkNodes(weightAttr);

	if (discretizeCheckBox.isSelected()){
	    fields[8] = "true";
	}
	else {
	    fields[8] = "false";
	}

	// Build Command Line arguments from fields.
	StringBuilder cmdlineArgs = new StringBuilder();
	Hashtable arguments = new Hashtable();

	cmdlineArgs.append("-m ").append(fields[2]).append(" "); // Min Term Size
	arguments.put("minTerm", fields[2]);
	cmdlineArgs.append("-e ").append(fields[1]).append(" "); // E-value Cutoff
	arguments.put("eValCut", fields[1]);
	cmdlineArgs.append("-n ").append(fields[3]).append(" "); // Effective TDB Size
	arguments.put("tdbSize", fields[3]);

	cmdlineArgs.append("-s ");                   // Statistics Type

	if ("Lugananni-Rice".equals(fields[4])){
	    arguments.put("stats", "wsum");
	    cmdlineArgs.append("wsum ");
	}

	else if("One-Sided Fischer's Exact Test".equals(fields[4])){
	    cmdlineArgs.append("hgem ");
	    arguments.put("stats", "hgem");
	}

	if ("Flip Signs".equals(fields[5])){              // Transformation
	    cmdlineArgs.append("-t flip ");
	    arguments.put("transf", "flip");
	}
	else if ("Take Absolute Value".equals(fields[5])){
	    cmdlineArgs.append("-t abs ");
	    arguments.put("transf", "abs");
	}
	else {
            arguments.put("trasf", "none");
        }

	if ("true".equals(fields[8])){                    // Discretize weights
	    cmdlineArgs.append("-d ");
	    arguments.put("disc", true);
	}
	else {
            arguments.put("disc", false);
        }

	if ("By Rank".equals(fields[6])){                 // Cutoff
	    cmdlineArgs.append("-r ").append(fields[7]).append(" ");
	    arguments.put("cutoff", "r:"+fields[7]);
	}
	else if ("By Weight".equals(fields[6])){
	    cmdlineArgs.append("-w ").append(fields[7]).append(" ");
	    arguments.put("cutoff", "w:"+fields[7]);
	}
	else {
            arguments.put("cutoff", "none");
        }

	cmdlineArgs.append("-F tab ");               // Print in Tab

	cmdlineArgs.append("- ");                    // Use standard input for weights files

	if (Configuration.isLocalQuery() && termDBIndex == 0) {
	    // Pass the custom database.
	    String namespace = "Custom";
	    String pathToFile = gmtFileBox.getText();

	    String argument = namespace + ":" + pathToFile;

	    cmdlineArgs.append(argument);
	}
	else {
            if (Configuration.isLocalQuery()) {
                termDBIndex--;
            }
            ETDFile currentTermDB = termDBs.get(termDBIndex);            
	    // Pass the selected .etd file.
	    cmdlineArgs.append(currentTermDB.path).append(" ");         // Specify Term Database
	    arguments.put("tdbFile", currentTermDB.path);
	}
	optionsTable = arguments;
	ssumCommand = cmdlineArgs.toString();        
    }
    
    private void reconfigureDatasets() {
        String[] names;
	termDBs = BackendTools.discoverTermDatabases();
	if (Configuration.isLocalQuery()) {
	    	names = new String[termDBs.size() + 1];
		names[0] = "[Custom GMT File]";
		for (int i = 0; i < termDBs.size(); i++) {
		    ETDFile currentTermDB = termDBs.get(i);
		    names[i + 1] = currentTermDB.name;
		}
	}
	else {
	    names = new String[termDBs.size()];
	    for (int i = 0; i < termDBs.size(); i++) {
		ETDFile currentTermDB = termDBs.get(i);
		names[i] = currentTermDB.name;
	    }
	}

	tdbDropdown.setModel(new DefaultComboBoxModel(names));
     	tdbDropdown.setSelectedIndex(0);
	toggleCustomFields();

	if (Configuration.isLocalQuery()) {
            tdbLabel.setText("Local Term Database");
	}
	else {
            tdbLabel.setText("Web Term Database");
	}
    }
    
    private void resetFields() {
        if (tdbDropdown.getItemCount() > 0) {
            tdbDropdown.setSelectedIndex(0);
        }
        evalBox.setText("0.01");
        minTermDBSizeBox.setText("2");
        effTermDBSizeBox.setText("0");
        statsDropdown.setSelectedIndex(0);
        transformDropdown.setSelectedIndex(0);
        cutoffDropdown.setSelectedIndex(0);
        cutoffBox.setText("");
        discretizeCheckBox.setSelected(false);
	refreshWeightList();
        
        CyNetwork net = Cytoscape.getCurrentNetwork();
        if (ResultsPanelManager.hasStoredResults(net.getIdentifier())) {
            restoreButton.setEnabled(true);
        }
        else {
            restoreButton.setEnabled(false);
        }                    
    }

    private HashSet getNodeAttributeNames() {
	CyAttributes attributes = Cytoscape.getNodeAttributes();
	CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        currentNetworkId = currentNetwork.getIdentifier();
	HashSet<String> nodeAttributeNames = new HashSet();

	Iterator<CyNode> nodeIter = currentNetwork.nodesIterator();

	while(nodeIter.hasNext()) {
	    CyNode node = nodeIter.next();
	    String id = node.getIdentifier();
	    List<String> attributesList = CyAttributesUtils.getAttributeNamesForObj(id,
									attributes);
	    nodeAttributeNames.addAll(attributesList);
	}

	return nodeAttributeNames;
    }

    private void refreshWeightList() {
	CyAttributes attributes = Cytoscape.getNodeAttributes();
	HashSet<String> attributeNames = getNodeAttributeNames();
	DefaultComboBoxModel model = new DefaultComboBoxModel();
        boolean hasWeightAttributes = false;

        for (String attribute : attributeNames) {    
            if (attributes.getType(attribute) == CyAttributes.TYPE_FLOATING) {
		model.addElement(attribute);
                hasWeightAttributes = true;
	    }
	}
	weightAttrDropdown.setModel(model);
        
        if (hasWeightAttributes) {
            queryButton.setEnabled(true);
        }
        else {
            queryButton.setEnabled(false);
        }                            
    }

    private void toggleCustomFields() {
	if (Configuration.isLocalQuery() && tdbDropdown.getSelectedIndex() == 0) {
	    gmtFileBox.setEnabled(true);
	    gmtFileButton.setEnabled(true);
	    jLabel12.setEnabled(true);
	}
	else {
	    gmtFileBox.setEnabled(false);
	    gmtFileButton.setEnabled(false);
	    jLabel12.setEnabled(false);
	}
    }    

    private class DatasetRefresh implements Task {
        
        private cytoscape.task.TaskMonitor taskMonitor;

        public void setTaskMonitor(TaskMonitor monitor)
                        throws IllegalThreadStateException {
            taskMonitor = monitor;
        }

        public void halt() {
            // No halting is allowed.
        }

        public String getTitle() {
            return "SaddleSum";
        }

        public void run() {
            taskMonitor.setPercentCompleted(-1);
            taskMonitor.setStatus("Retrieving database info.");
            try {
                reconfigureDatasets();
            }
            catch (Exception ex) {
                taskMonitor.setException(ex, null);
            }
        }
    }
    
    private class CurrentNetworkListener implements  PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop.equalsIgnoreCase(CytoscapeDesktop.NETWORK_VIEW_FOCUSED)) {
                String networkId = (String) e.getNewValue();
                if (!networkId.equals(currentNetworkId)) {
                    refreshWeightList();
                    if (ResultsPanelManager.hasStoredResults(networkId)) {
                        restoreButton.setEnabled(true);
                    }
                    else {
                        restoreButton.setEnabled(false);
                    }
                    currentNetworkId = networkId;
                }
            }
        }
    }
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tdbDropdown = new javax.swing.JComboBox();
        gmtFileBox = new javax.swing.JTextField();
        gmtFileButton = new javax.swing.JButton();
        weightAttrDropdown = new javax.swing.JComboBox();
        evalBox = new javax.swing.JTextField();
        minTermDBSizeBox = new javax.swing.JTextField();
        effTermDBSizeBox = new javax.swing.JTextField();
        statsDropdown = new javax.swing.JComboBox();
        transformDropdown = new javax.swing.JComboBox();
        cutoffDropdown = new javax.swing.JComboBox();
        cutoffBox = new javax.swing.JTextField();
        discretizeCheckBox = new javax.swing.JCheckBox();
        queryButton = new javax.swing.JButton();
        resetButton = new javax.swing.JButton();
        configButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        bannerLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        tdbLabel = new javax.swing.JLabel();
        evalLabel = new javax.swing.JLabel();
        statsLabel = new javax.swing.JLabel();
        minTermDBSizeLabel = new javax.swing.JLabel();
        effTermDBSizeLabel = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        restoreButton = new javax.swing.JButton();

        setFont(new java.awt.Font("Arial", 0, 14));
        setMinimumSize(new java.awt.Dimension(465, 655));
        setPreferredSize(new java.awt.Dimension(465, 655));

        tdbDropdown.setFont(new java.awt.Font("Arial", 0, 14));
        tdbDropdown.setToolTipText("Choose a term database");
        tdbDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tdbDropdownActionPerformed(evt);
            }
        });

        gmtFileBox.setFont(new java.awt.Font("Arial", 0, 14));
        gmtFileBox.setToolTipText("Enter a database file in GMT format here.");

        gmtFileButton.setFont(new java.awt.Font("Arial", 0, 15));
        gmtFileButton.setText("Browse ...");
        gmtFileButton.setToolTipText("Select file");
        gmtFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gmtFileButtonActionPerformed(evt);
            }
        });

        weightAttrDropdown.setFont(new java.awt.Font("Arial", 0, 14));
        weightAttrDropdown.setToolTipText("Choose a node weight attribute");

        evalBox.setFont(new java.awt.Font("Arial", 0, 14));
        evalBox.setText("0.01");
        evalBox.setToolTipText("Set the largest E-value for a term to be considered significant. ");

        minTermDBSizeBox.setFont(new java.awt.Font("Arial", 0, 14));
        minTermDBSizeBox.setText("2");
        minTermDBSizeBox.setToolTipText("<html>Set the minimum number of entities for a term to be considered.<br>\nOnly entites with supplied weights count towards the term size.</html>");

        effTermDBSizeBox.setFont(new java.awt.Font("Arial", 0, 14));
        effTermDBSizeBox.setText("0");
        effTermDBSizeBox.setToolTipText("<html>Set the effective term database size for applying Bonferroni correction<br> \n(i.e. calculating E-values) to P-values output by the algorithm. <br>\nIf the value set is 0 (default), the total number of terms considered is used.</html>");

        statsDropdown.setFont(new java.awt.Font("Arial", 0, 14));
        statsDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Lugananni-Rice", "One-Sided Fischer's Exact Test" }));
        statsDropdown.setToolTipText("<html>Set statistical method used to evaluate term enrichment statistics.<br> \n<b>Lugananni-Rice</b> is the default SaddleSum method, while <b>One-sided Fisher's Exact<br> \ntest</b> is a widely used alternative that requires a cutoff to be set.</html> ");

        transformDropdown.setFont(new java.awt.Font("Arial", 0, 14));
        transformDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Transformation", "Flip Signs", "Take Absolute Value" }));
        transformDropdown.setToolTipText("<html>Apply a transformation to each of the provided weights prior to applying<br>\nother processing options and calculating enrichment statistics.</html> ");

        cutoffDropdown.setFont(new java.awt.Font("Arial", 0, 14));
        cutoffDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Cutoff", "By Rank", "By Weight" }));
        cutoffDropdown.setToolTipText("<html>Choose a type of cutoff to apply. The value of cutoff should be entered in the next field.<br> \n<b>By rank</b> sets all weights ranked lower than the chosen value to 0.<br> \nIf there are several weights tied at the cutoff, keep all of them.<br> \n<b>By weight</b> sets all weights smaller than the chosen value to 0.</html> ");

        cutoffBox.setFont(new java.awt.Font("Arial", 0, 14));
        cutoffBox.setToolTipText("<html>Enter the cutoff value. The interpretation of the <br>\nvalue depends on the cutoff type chosen above.</html>");

        discretizeCheckBox.setToolTipText("<html>Discretize weights. Set all weights greater than 0 to 1 <br>\nand all those smaller than 0 to 0. </html>");

        queryButton.setFont(new java.awt.Font("Arial", 0, 15));
        queryButton.setText("QUERY");
        queryButton.setToolTipText("Run SaddleSum");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        resetButton.setFont(new java.awt.Font("Arial", 0, 15));
        resetButton.setText("RESET");
        resetButton.setToolTipText("Reset the form");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        configButton.setFont(new java.awt.Font("Arial", 0, 15));
        configButton.setText("CONFIG");
        configButton.setToolTipText("Configure local and web paths");
        configButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configButtonActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Arial", 1, 14));
        jLabel7.setForeground(new java.awt.Color(51, 102, 153));
        jLabel7.setText("STATISTICAL PARAMETERS");

        bannerLabel.setBackground(new java.awt.Color(51, 51, 51));
        bannerLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gov/nih/nlm/ncbi/qmbp/cytosaddlesum/resources/banner-saddlesum.png"))); // NOI18N
        bannerLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        bannerLabel.setOpaque(true);
        bannerLabel.setPreferredSize(new java.awt.Dimension(465, 77));

        jLabel6.setFont(new java.awt.Font("Arial", 1, 14));
        jLabel6.setForeground(new java.awt.Color(51, 102, 153));
        jLabel6.setText("TERM DATABASE AND WEIGHTS");

        tdbLabel.setFont(new java.awt.Font("Arial", 0, 14));
        tdbLabel.setForeground(new java.awt.Color(0, 51, 102));
        tdbLabel.setLabelFor(tdbDropdown);
        tdbLabel.setText("Term Database");
        tdbLabel.setToolTipText(tdbDropdown.getToolTipText());
        tdbLabel.setFocusTraversalPolicyProvider(true);

        evalLabel.setFont(new java.awt.Font("Arial", 0, 14));
        evalLabel.setForeground(new java.awt.Color(0, 51, 102));
        evalLabel.setLabelFor(evalBox);
        evalLabel.setText("E-value Cutoff");
        evalLabel.setToolTipText(evalBox.getToolTipText());
        evalLabel.setFocusTraversalPolicyProvider(true);

        statsLabel.setFont(new java.awt.Font("Arial", 0, 14));
        statsLabel.setForeground(new java.awt.Color(0, 51, 102));
        statsLabel.setLabelFor(tdbDropdown);
        statsLabel.setText("Statistics");
        statsLabel.setToolTipText(statsDropdown.getToolTipText());
        statsLabel.setFocusTraversalPolicyProvider(true);

        minTermDBSizeLabel.setFont(new java.awt.Font("Arial", 0, 14));
        minTermDBSizeLabel.setForeground(new java.awt.Color(0, 51, 102));
        minTermDBSizeLabel.setLabelFor(minTermDBSizeBox);
        minTermDBSizeLabel.setText("Minimum Term Size");
        minTermDBSizeLabel.setToolTipText(minTermDBSizeBox.getToolTipText());
        minTermDBSizeLabel.setFocusTraversalPolicyProvider(true);

        effTermDBSizeLabel.setFont(new java.awt.Font("Arial", 0, 14));
        effTermDBSizeLabel.setForeground(new java.awt.Color(0, 51, 102));
        effTermDBSizeLabel.setLabelFor(effTermDBSizeBox);
        effTermDBSizeLabel.setText("Effective Term DB Size");
        effTermDBSizeLabel.setToolTipText(effTermDBSizeBox.getToolTipText());
        effTermDBSizeLabel.setFocusTraversalPolicyProvider(true);

        jLabel12.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel12.setForeground(new java.awt.Color(0, 51, 102));
        jLabel12.setLabelFor(gmtFileBox);
        jLabel12.setText("GMT Database File");
        jLabel12.setToolTipText("Enter the filename of a database in GMT format");
        jLabel12.setFocusTraversalPolicyProvider(true);

        jLabel13.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel13.setForeground(new java.awt.Color(0, 51, 102));
        jLabel13.setLabelFor(gmtFileBox);
        jLabel13.setText("Node Weight Attribute");
        jLabel13.setToolTipText(weightAttrDropdown.getToolTipText());
        jLabel13.setFocusTraversalPolicyProvider(true);

        jLabel14.setFont(new java.awt.Font("Arial", 1, 14));
        jLabel14.setForeground(new java.awt.Color(51, 102, 153));
        jLabel14.setText("WEIGHT PROCESSING PARAMETERS");

        jLabel15.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel15.setForeground(new java.awt.Color(0, 51, 102));
        jLabel15.setLabelFor(transformDropdown);
        jLabel15.setText("Transformation");
        jLabel15.setToolTipText(transformDropdown.getToolTipText());
        jLabel15.setFocusTraversalPolicyProvider(true);

        jLabel16.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel16.setForeground(new java.awt.Color(0, 51, 102));
        jLabel16.setLabelFor(cutoffDropdown);
        jLabel16.setText("Apply Cutoff");
        jLabel16.setToolTipText(cutoffDropdown.getToolTipText());
        jLabel16.setFocusTraversalPolicyProvider(true);

        jLabel17.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel17.setForeground(new java.awt.Color(0, 51, 102));
        jLabel17.setLabelFor(cutoffBox);
        jLabel17.setText("Cutoff Value");
        jLabel17.setToolTipText(cutoffBox.getToolTipText());
        jLabel17.setFocusTraversalPolicyProvider(true);

        jLabel18.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel18.setForeground(new java.awt.Color(0, 51, 102));
        jLabel18.setLabelFor(discretizeCheckBox);
        jLabel18.setText("Discretize Weights");
        jLabel18.setToolTipText(discretizeCheckBox.getToolTipText());
        jLabel18.setFocusTraversalPolicyProvider(true);

        restoreButton.setFont(new java.awt.Font("Arial", 0, 15));
        restoreButton.setText("LOAD");
        restoreButton.setToolTipText("Restore stored ITM from current network attributes.");
        restoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restoreButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jLabel15)
                                .add(jLabel16)
                                .add(jLabel17))
                            .add(70, 70, 70)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(statsDropdown, 0, 240, Short.MAX_VALUE)
                                .add(transformDropdown, 0, 240, Short.MAX_VALUE)
                                .add(cutoffDropdown, 0, 240, Short.MAX_VALUE)
                                .add(weightAttrDropdown, 0, 240, Short.MAX_VALUE)
                                .add(tdbDropdown, 0, 240, Short.MAX_VALUE)
                                .add(discretizeCheckBox)
                                .add(effTermDBSizeBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                                .add(minTermDBSizeBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                                .add(evalBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                                .add(cutoffBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE))
                            .add(8, 8, 8))
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                            .add(gmtFileBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(gmtFileButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jLabel7)
                    .add(jLabel14)
                    .add(jLabel6)
                    .add(layout.createSequentialGroup()
                        .add(queryButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(resetButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(configButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(restoreButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(effTermDBSizeLabel)
                    .add(evalLabel)
                    .add(minTermDBSizeLabel)
                    .add(tdbLabel)
                    .add(jLabel12)
                    .add(jLabel13)
                    .add(statsLabel)
                    .add(jLabel18))
                .add(31, 31, 31))
            .add(bannerLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(bannerLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tdbLabel)
                    .add(tdbDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel12)
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(gmtFileBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(gmtFileButton))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(weightAttrDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(evalLabel)
                    .add(evalBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(minTermDBSizeLabel)
                    .add(minTermDBSizeBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(effTermDBSizeLabel)
                    .add(effTermDBSizeBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statsLabel)
                    .add(statsDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jLabel14)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel15)
                    .add(transformDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel16)
                    .add(cutoffDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel17)
                    .add(cutoffBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel18)
                    .add(discretizeCheckBox))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(queryButton)
                    .add(resetButton)
                    .add(configButton)
                    .add(restoreButton))
                .addContainerGap(73, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void gmtFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gmtFileButtonActionPerformed
	JFileChooser gmtFilesChooser = new JFileChooser(gmtFileBox.getText());
        gmtFilesChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int rValue = gmtFilesChooser.showOpenDialog(this);
        if (rValue == JFileChooser.APPROVE_OPTION){
            gmtFileBox.setText(gmtFilesChooser.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_gmtFileButtonActionPerformed

private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queryButtonActionPerformed

	String errorMessage = validateInput();
	if (errorMessage != null) {
	    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
					  errorMessage,
					  "SaddleSum: Invalid Input",
					  JOptionPane.ERROR_MESSAGE);
	    return;
	}

        buildArgs();
        
        String errMsg = MainTasks.querySaddleSum(ssumCommand, optionsTable, 
                                            selectedNodes);
        
        if (errMsg != null) {
	    JOptionPane.showMessageDialog(this,
					  errMsg,
					  "SaddleSum Error",
					  JOptionPane.ERROR_MESSAGE);
	    return;
	}
}//GEN-LAST:event_queryButtonActionPerformed

private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        resetFields();
}//GEN-LAST:event_resetButtonActionPerformed

private void configButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configButtonActionPerformed
    
	ConfDialog newConfig = new ConfDialog(Cytoscape.getDesktop(), true);
	newConfig.setVisible(true);
        if (Configuration.getIsChanged()) {
            Configuration.setIsChanged(Boolean.FALSE);
            DatasetRefresh task = new DatasetRefresh();

            JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(false);
            jTaskConfig.displayCancelButton(false);
            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);
            jTaskConfig.displayTimeElapsed(true);
            jTaskConfig.displayTimeRemaining(false);

            TaskManager.executeTask(task, jTaskConfig);
        }
}//GEN-LAST:event_configButtonActionPerformed

    private void tdbDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tdbDropdownActionPerformed
	toggleCustomFields();
}//GEN-LAST:event_tdbDropdownActionPerformed

private void restoreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restoreButtonActionPerformed
        currentNetworkId = null; // to ensure update of restoreButton
        MainTasks.restoreResultsPanel(Cytoscape.getCurrentNetwork());
}//GEN-LAST:event_restoreButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bannerLabel;
    private javax.swing.JButton configButton;
    private javax.swing.JTextField cutoffBox;
    private javax.swing.JComboBox cutoffDropdown;
    private javax.swing.JCheckBox discretizeCheckBox;
    private javax.swing.JTextField effTermDBSizeBox;
    private javax.swing.JLabel effTermDBSizeLabel;
    private javax.swing.JTextField evalBox;
    private javax.swing.JLabel evalLabel;
    private javax.swing.JTextField gmtFileBox;
    private javax.swing.JButton gmtFileButton;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JTextField minTermDBSizeBox;
    private javax.swing.JLabel minTermDBSizeLabel;
    private javax.swing.JButton queryButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton restoreButton;
    private javax.swing.JComboBox statsDropdown;
    private javax.swing.JLabel statsLabel;
    private javax.swing.JComboBox tdbDropdown;
    private javax.swing.JLabel tdbLabel;
    private javax.swing.JComboBox transformDropdown;
    private javax.swing.JComboBox weightAttrDropdown;
    // End of variables declaration//GEN-END:variables
}
