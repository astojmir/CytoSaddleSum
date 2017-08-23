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

import java.util.Vector;
import java.util.Set;
import cytoscape.Cytoscape;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;

public class SaddleSumNetworkModel{

    public SaddleSumNetworkModel(){

    }
    public static Vector getNetworkNodes(String weightAttribute){
	Vector nodes = new Vector();
	CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
	CyAttributes attributes = Cytoscape.getNodeAttributes();
	Set<CyNode> selectedNodes = currentNetwork.getSelectedNodes();
        
        
        for (CyNode node : selectedNodes) {
            String nodeId = node.getIdentifier();
            String nodeName = attributes.getStringAttribute(nodeId,
                                                     Semantics.CANONICAL_NAME);
            Double weight = attributes.getDoubleAttribute(nodeId, 
                                                          weightAttribute);
            if (weight == null) {
                weight = 0.0;
            }
	    String nodeLine = nodeName + "\t" + weight;
	    nodes.add(nodeLine);
            
        }
	return nodes;
    }
}
