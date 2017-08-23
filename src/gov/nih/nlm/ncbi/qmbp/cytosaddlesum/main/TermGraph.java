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

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.Configuration;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.Semantics;
import cytoscape.view.CyNetworkView;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualStyle;
import cytoscape.task.TaskMonitor;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ResultsPanelManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;


public class TermGraph {
    
    // Code for storing/retrieving of data from term networks
    
    
    private static String[] standardSections = {"QUERY AND DATABASE SUMMARY",
	   	        	                "NOMENCLATURE WARNINGS",
				                "UNKNOWN IDS",
				                "TERM RELATIONSHIPS",
				                "NODE PROPERTIES"};
    public CyNetwork termNetwork;
    public String queryPrefix;
    public static String[] hitHeadings = {"Term ID", "Name",
		          	          "Associations", "Score",
				          "E-value", "URL"};

    
    public TermGraph(HashMap<String, ArrayList<ArrayList>> parsedOutput){
	queryPrefix = getNewQueryPrefix();
        createNetwork(parsedOutput);        
    }

    public TermGraph(CyNetwork termNetwork) {
        this.termNetwork = termNetwork;
        queryPrefix = getStoredQueryPrefix();
    }
   
    private static String getNewQueryPrefix() {
        return String.format("SSUM%03d", Configuration.getCount());
    }
    
    private String getStoredQueryPrefix() {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        String networkId = termNetwork.getIdentifier();
        return cyNetworkAttrs.getStringAttribute(networkId, "SaddleSumQueryId");
    }
    
    private static String getNodeId(String queryPrefix, String termId) {
        return String.format("%s-%s", queryPrefix, termId);
    }
    
    private String getTermId(String nodeId) {
        // Inverse of getNodeId
        int k = 1 + queryPrefix.length();
        return nodeId.substring(k);
    }
    
    private static HashMap<String, HashMap<String, String>> extractNodeProperties(
                HashMap<String, ArrayList<ArrayList>> parsedOutput) {

        HashMap<String, HashMap<String, String>> nodes = 
            new HashMap<String, HashMap<String, String>> ();
        
	if (!parsedOutput.containsKey("NODE PROPERTIES")) {
            return nodes;
        }
        
        // We need to iterate over all namespaces and join their results together.
        // To do so, we take the set of keys of parsedOutput and remove all
        // sections with known semantics. All other sections represent namespace
        // tables        
	HashSet<String> sectionNames = new HashSet(parsedOutput.keySet());
        sectionNames.removeAll(Arrays.asList(standardSections));
        
        for (String namespace : sectionNames) {
            ArrayList<ArrayList> sectionData = parsedOutput.get(namespace);
            for (ArrayList<String> nodeLine : sectionData) {
		if (nodeLine.size() == 1) {
                    // this is the last line which serves as separator
                    continue;
                }
                HashMap<String, String> nodeProps = 
                        new HashMap<String, String>();

		nodeProps.put("termid", nodeLine.get(0));
		nodeProps.put("desc", nodeLine.get(1));
		nodeProps.put("assoc", nodeLine.get(2));
		nodeProps.put("score", nodeLine.get(3));
		nodeProps.put("evalue", nodeLine.get(4));
                nodeProps.put("color", "0");
		nodeProps.put("ns", namespace);

                nodes.put(nodeLine.get(0), nodeProps);
            }
        }

	// Now insert non-significant nodes that are part of the relationship
	// graph and update colors.
        
        // NODE PROPERTIES SECTION contains:
        // 0 - term ID
        // 1 - description
        // 2 - color (discrete, 8)
        // 3 - URL (ignored)
        // 4 - list of original nodes (optional)
        
      	ArrayList<ArrayList> nodeProperties = parsedOutput.get("NODE PROPERTIES");
        for (ArrayList<String> line : nodeProperties) {
            String id = line.get(0);
            HashMap<String, String> nodeProps;
            if (!nodes.containsKey(id)) {
                nodeProps = new HashMap<String, String>();
                nodeProps.put("termid", id);
                nodeProps.put("desc", line.get(1));
                nodes.put(id, nodeProps);
            }
            else {
                nodeProps = nodes.get(id);
            }
	    nodeProps.put("color", line.get(2));
            nodeProps.put("url", line.get(3));
            if (line.size() >= 5) {                
                nodeProps.put("origNodes", line.get(4));
            }
        }
	return nodes;
    }

    private static void setParamsAsNetworkAttrs(List paramData, 
                                                char paramType,
                                                CyNetwork parentNetwork,
                                                String queryPrefix) {
        
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        for (int i=0; i < paramData.size(); i++) {
            String attrName = String.format("%s%c%02d", queryPrefix, 
                                            paramType, i);
            cyNetworkAttrs.setListAttribute(parentNetwork.getIdentifier(), 
                                            attrName, 
                                            (List) paramData.get(i));            
        }       
    }
    
    private static void setNetworkAttrs(
            HashMap<String, ArrayList<ArrayList>> parsedOutput,
            CyNetwork relationshipNetwork,
            String queryPrefix) {
                
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        String networkId = relationshipNetwork.getIdentifier();
        
        setParamsAsNetworkAttrs(parsedOutput.get("QUERY AND DATABASE SUMMARY"), 
                                'S', relationshipNetwork, queryPrefix);
        
        // Process unknown ids
        ArrayList<ArrayList> unknownIdsSection = parsedOutput.get("UNKNOWN IDS");
        if (unknownIdsSection.size() > 0) {
            // Need to insert space next to commas
	    String unknownIdsString = (String) unknownIdsSection.get(0).get(0); 
            String[] unknownIds = unknownIdsString.split(",");
            Integer unknownIdsCount = unknownIds.length;
            StringBuilder builder = new StringBuilder();
            builder.append(unknownIds[0]);
            for (int i=1; i < unknownIds.length; i++) {
                builder.append(", ").append(unknownIds[i]);               
            }
            cyNetworkAttrs.setAttribute(networkId, queryPrefix + "U00",
                                        builder.toString());
            cyNetworkAttrs.setAttribute(networkId, queryPrefix + "U01",
                                        unknownIdsCount);
        }

        // Process Warnings - need to flatten into a list of strings
        ArrayList<ArrayList> warningsSection = parsedOutput.get("NOMENCLATURE WARNINGS");
        ArrayList<String> warnings = new ArrayList<String>();
        for (ArrayList<String> line : warningsSection) {
            warnings.add(line.get(0));
        }
        cyNetworkAttrs.setListAttribute(networkId, queryPrefix + "W00", warnings);
        
        cyNetworkAttrs.setAttribute(networkId, "SaddleSumQueryId", queryPrefix);
    }
    
    
    private void createNetwork(HashMap<String, ArrayList<ArrayList>> parsedOutput){

        HashMap<String, HashMap<String, String>> nodes = 
            extractNodeProperties(parsedOutput);

        String title = String.format("SaddleSum Query %s", queryPrefix);
	termNetwork = Cytoscape.createNetwork(title, false);
	CyAttributes cyNodeAttrs = Cytoscape.getNodeAttributes();

        // Set general summary attributes
        setNetworkAttrs(parsedOutput, termNetwork, queryPrefix);
        
        // Insert nodes and set their attributes        
        for (String termId : nodes.keySet()) {
            HashMap<String, String> nodeProps = nodes.get(termId);
            
            // This avoids the problem of term nodes from different queries 
            // being labeled with the same term ID
            String nodeId = getNodeId(queryPrefix, termId);
            CyNode node = Cytoscape.getCyNode(nodeId, true);
            termNetwork.addNode(node);

	    cyNodeAttrs.setAttribute(nodeId, "TermId", nodeProps.get("termid"));
	    cyNodeAttrs.setAttribute(nodeId, "Description", 
                                     nodeProps.get("desc"));
            
            // We only use 8 colors so we need to map everyting above 7 down to
            // 7.
            Integer color = new Integer(nodeProps.get("color"));
            if (color > 7) {
                color = 7;
            }
	    cyNodeAttrs.setAttribute(nodeId, "Color", color); 

            if (nodeProps.containsKey("ns")) {
                cyNodeAttrs.setAttribute(nodeId, "Namespace", 
                                         nodeProps.get("ns"));
            }
            if (nodeProps.containsKey("assoc")) {
                cyNodeAttrs.setAttribute(nodeId, "Associations", 
                                         new Integer(nodeProps.get("assoc")));                
            }
            if (nodeProps.containsKey("score")) {
                cyNodeAttrs.setAttribute(nodeId, "Score", 
                                         new Double(nodeProps.get("score")));                
            }
            if (nodeProps.containsKey("evalue")) {
                cyNodeAttrs.setAttribute(nodeId, "E-value", 
                                         new Double(nodeProps.get("evalue")));                
            }           
            if (nodeProps.containsKey("url")) {
                cyNodeAttrs.setAttribute(nodeId, "URL", 
                                         nodeProps.get("url"));                
            }           
            if (nodeProps.containsKey("origNodes")) {
                String [] onodes = nodeProps.get("origNodes").split(",");
                List<String> origNodes = Arrays.asList(onodes);
                cyNodeAttrs.setListAttribute(nodeId, "origNodes", origNodes);
            }           
        }
                        
        // Set term relationships
     	ArrayList<ArrayList> termRelationships = parsedOutput.get("TERM RELATIONSHIPS");
        for (ArrayList<String> currentLine : termRelationships) {            
            
            String src = getNodeId(queryPrefix, currentLine.get(0));
            CyNode srcNode = Cytoscape.getCyNode(src, false);
            String dst = getNodeId(queryPrefix, currentLine.get(2));
            CyNode dstNode = Cytoscape.getCyNode(dst, false);
            String edgeType = currentLine.get(1);
            
	    CyEdge edge = Cytoscape.getCyEdge(srcNode, dstNode,
					      Semantics.INTERACTION,
					      edgeType,
					      true);
	    termNetwork.addEdge(edge);            
        }
    }

    public void showNetwork(TaskMonitor taskMonitor){
	//Create the view

        String title = String.format("SaddleSum Query %s View", 
                                     queryPrefix);

	CyNetworkView outputView = Cytoscape.createNetworkView(termNetwork,
							       title);

        taskMonitor.setStatus("Laying out term relationship network...");
        taskMonitor.setPercentCompleted(90);

        //Set the layout
        CyLayoutAlgorithm hierLayout = CyLayouts.getLayout("hierarchical");
        hierLayout.doLayout();

        taskMonitor.setStatus("Coloring term relationship network...");
        taskMonitor.setPercentCompleted(95);

        //Set the vizmap scheme
        setVizMapScheme();
        outputView.redrawGraph(true, true);
    }

    public void updateNetwork(TaskMonitor taskMonitor){

        taskMonitor.setStatus("Coloring term relationship network...");
        taskMonitor.setPercentCompleted(0);

        CyNetworkView outputView;
        String networkId = termNetwork.getIdentifier();
        if (Cytoscape.viewExists(networkId)) {
            outputView = Cytoscape.getNetworkView(networkId);
        }
        else {
            String title = String.format("SaddleSum Query %s View", 
                ResultsPanelManager.findQueryPrefix(networkId));
                outputView = Cytoscape.createNetworkView(termNetwork, title);                    
        }
                
        //Set the vizmap scheme
        setVizMapScheme();
        outputView.redrawGraph(true, true);        
    }
    
    private void setVizMapScheme(){

	VisualMappingManager manager = Cytoscape.getVisualMappingManager();
	CalculatorCatalog catalog = manager.getCalculatorCatalog();
	VisualStyle vs = catalog.getVisualStyle("CytoSaddleSum");

        if (vs == null) {
            URL propsURL = getClass().getResource("/gov/nih/nlm/ncbi/qmbp/cytosaddlesum/resources/CytoSaddleSum.vizmap.props");
            Cytoscape.firePropertyChange(Cytoscape.VIZMAP_LOADED, null, propsURL);
            vs = catalog.getVisualStyle("CytoSaddleSum");
        }
	manager.setVisualStyle(vs);
    }
    
    
    
    // Methods to extract stored data from any term network.
    
    public Vector<Vector> getParamsAsNetworkAttrs(char paramType) {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        String networkId = termNetwork.getIdentifier();
        Vector<Vector> paramData = new Vector<Vector>();
        String attrPrefix = String.format("%s%c", queryPrefix, paramType);
        List<String> attrNames = CyAttributesUtils.getAttributeNamesForObj(
                                   networkId, cyNetworkAttrs);
        ArrayList<String> validNames = new ArrayList();
        for (String attrName : attrNames) {
            if (attrName.startsWith(attrPrefix)) {
                validNames.add(attrName);
            }            
        }
        Collections.sort(validNames);
        for (String attrName :  validNames) {
            List row = cyNetworkAttrs.getListAttribute(networkId, attrName);
            paramData.add(new Vector(row));
        }
        return paramData;
    }
    
    public List<String> getWarnings() {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();
        List<String> warnings = cyNetworkAttrs.getListAttribute(
                termNetwork.getIdentifier(), queryPrefix + "W00");
        return warnings;
    }
    
    public String getUnknownIds() {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();        
        String unknownIds = cyNetworkAttrs.getStringAttribute(
                termNetwork.getIdentifier(), queryPrefix + "U00");
        return unknownIds;        
    }
    
    public Integer getUnknownIdsCount() {
        CyAttributes cyNetworkAttrs = Cytoscape.getNetworkAttributes();        
        Integer unknownIdsCount = cyNetworkAttrs.getIntegerAttribute(
                    termNetwork.getIdentifier(), queryPrefix + "U01");
        return unknownIdsCount;
    }

    public HashMap<String, Vector<Vector>> getNamespaceTables(boolean includeUrl) {

        HashMap<String, Vector<Vector>> tables = new HashMap<String, Vector<Vector>>();
	CyAttributes cyNodeAttrs = Cytoscape.getNodeAttributes();
        
        for (int rootIndex : termNetwork.getNodeIndicesArray()) {
            CyNode node = (CyNode) termNetwork.getNode(rootIndex);
            String nodeId = node.getIdentifier();

            String ns = cyNodeAttrs.getStringAttribute(nodeId, "Namespace");
            if (ns == null) {
                continue;
            }
            Vector rows;
            Vector data = new Vector();
            if (!tables.containsKey(ns)) {
                rows = new Vector();
                tables.put(ns, rows);
            }
            else {
                rows = tables.get(ns);
            }
            
            String termid = cyNodeAttrs.getStringAttribute(nodeId, "TermId");
            String desc = cyNodeAttrs.getStringAttribute(nodeId, "Description");
            Integer assoc = cyNodeAttrs.getIntegerAttribute(nodeId, "Associations");
            Double score = cyNodeAttrs.getDoubleAttribute(nodeId, "Score");
            Double evalue = cyNodeAttrs.getDoubleAttribute(nodeId, "E-value");
            
            data.add(termid);
            data.add(desc);
            data.add(assoc);
            data.add(score);
            data.add(evalue);

            if (includeUrl) {
                String url = cyNodeAttrs.getStringAttribute(nodeId, "URL");
                if (url == null) {
                    url = "";
                }
                data.add(url);
            }            
            
            rows.add(data);
        }
        Comparator cmp =  new Comparator(){
                public int compare(Object o1, Object o2) {
                    Vector objs1 = (Vector) o1;
                    Vector objs2 = (Vector) o2;
                    Double val1 = (Double) objs1.get(4);
                    Double val2 = (Double) objs2.get(4);
                    if (val1 < val2) {
                        return -1;
                    }
                    if (val1 > val2) {
                        return 1;
                    }
                    return 0;
                }
            };
      	for (String namespace : tables.keySet()) {
            Vector tbl = tables.get(namespace);
            Collections.sort(tbl, cmp);
        }
        return tables;
    }

    public Vector<String> getHitTableHeadings(boolean includeUrl) {
  	    
        List<String> columnNames = Arrays.asList(hitHeadings);
        if (!includeUrl) {
            columnNames = columnNames.subList(0, 5);
        }
        Vector columnNamesVector = new Vector(columnNames);
        return columnNamesVector;
    }
    
    public String getRelationshipGraphString(String edgeFmt) {
        
        int[] nodeIndices = termNetwork.getNodeIndicesArray();
        int[] edgeIndices = termNetwork.getEdgeIndicesArray();
        CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
        HashMap<Integer, String> termIds = new HashMap<Integer, String>();
        StringBuilder builder = new StringBuilder();
        
        // Collect node identifiers and map them to term identifiers
	for (int i = 0; i < nodeIndices.length; i++) {
            int nodeIx = nodeIndices[i];
	    String nodeId = termNetwork.getNode(nodeIx).getIdentifier();
            termIds.put(nodeIx, getTermId(nodeId));
	}

        // Now write all edges
        for (int j : edgeIndices) {
            int src = termNetwork.getEdgeSourceIndex(j);
            int tgt = termNetwork.getEdgeTargetIndex(j);
            String edgeId = termNetwork.getEdge(j).getIdentifier();
            String edgeType = edgeAttrs.getStringAttribute(edgeId,
                                Semantics.INTERACTION);
            builder.append(String.format(edgeFmt, termIds.get(src),
                                        edgeType, termIds.get(tgt)));
        }
        return builder.toString();
    }
   
    public ArrayList<Object []> getNodeProperties() {
        ArrayList<Object []> nodeProps = new ArrayList<Object[]>();       
	CyAttributes cyNodeAttrs = Cytoscape.getNodeAttributes();
        
        for (int rootIndex : termNetwork.getNodeIndicesArray()) {
            Object [] nodeData = new Object[5];
            String nodeId = termNetwork.getNode(rootIndex).getIdentifier();
            nodeData[0] = cyNodeAttrs.getStringAttribute(nodeId, "TermId");
            nodeData[1] = cyNodeAttrs.getStringAttribute(nodeId, "Description");
            nodeData[2] = cyNodeAttrs.getIntegerAttribute(nodeId, "Color");
            
            String url = cyNodeAttrs.getStringAttribute(nodeId, "URL");
            if (url == null) {
                url = "";
            }
            nodeData[3] = url;
            
            List<String> origNodes = cyNodeAttrs.getListAttribute(nodeId,"origNodes");
            if (origNodes == null || origNodes.isEmpty()) {
                nodeData[4] = "";
            }
            else {
                StringBuilder builder = new StringBuilder(origNodes.get(0));
                for (String item : origNodes.subList(1, origNodes.size())) {
                    builder.append(",");
                    builder.append(item);
                }
                nodeData[4] = builder.toString();
            }
            nodeProps.add(nodeData);
        }
        return nodeProps;
    }
    
}
