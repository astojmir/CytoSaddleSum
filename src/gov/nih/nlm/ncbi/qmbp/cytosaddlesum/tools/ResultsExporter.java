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

package gov.nih.nlm.ncbi.qmbp.cytosaddlesum.tools;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.main.TermGraph;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;

public class ResultsExporter {

    private TermGraph termGraph;
    private static String dashedLine = "-----------------------------------------------------------------------------------------\n";
    
    public ResultsExporter(CyNetwork termNetwork) {
	super();
        termGraph = new TermGraph(termNetwork);
    }
    
    public void exportData(String fileName, String type) {
        
        try { 
            FileWriter writer = new FileWriter(fileName);
            if ("tab".equals(type)) {
                exportAsTab(writer);
            }
            else {
                exportAsTxt(writer);
            }
            writer.close();
        }
        catch (IOException e) {
            String errMsg = "Could not export data to " + fileName + ".";
 	    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
					  errMsg,
					  "Export SaddleSum Results",
					  JOptionPane.ERROR_MESSAGE);
        }               
    }
    
    public void exportAsTxt(FileWriter writer) throws IOException {

        writer.write(dashedLine);
        writer.write("               SADDLESUM RESULTS\n");
        writer.write(dashedLine);

        writeHeader(writer, "%-48.48s %s\n", "\n**** %s ****\n");
        writeWarnings(writer, "%s\n",  "\n**** %s ****\n");
        writeUnknownIds(writer,  ", ", "\n**** %s ****\n", 80);
        writeTermHits(writer, "%-15.15s %-40.40s %6.6s %12.12s %10.10s%.0s\n",
                      "\n**** %s ****\n", true);
        writer.write(dashedLine);                
    }

    public void exportAsTab(FileWriter writer) throws IOException {
        
        writeHeader(writer, "%s\t%s\n", "#\n# %s\n#\n");
        writeWarnings(writer, "%s\n", "#\n# %s\n#\n");
        writeUnknownIds(writer, ",", "#\n# %s\n#\n", 30000);
        writeTermRelationships(writer, "%s\t%s\t%s\n", "%s\t%s\t%d\t%s\t%s\n",
                               "#\n# %s\n#\n");
        writeTermHits(writer, "%s\t%s\t%s\t%s\t%s\t%s\n", "#\n# %s\n#\n", false);
    }
    
    private void writeHeader(FileWriter writer, String fmt, String headingFmt) 
                             throws IOException{
        
        writer.write(String.format(headingFmt, "QUERY AND DATABASE SUMMARY"));
        Vector<Vector> headerData = termGraph.getParamsAsNetworkAttrs('S');
        for (Vector line : headerData) {
            String key = (String) line.get(0);
            String val = (String) line.get(1);
            writer.write(String.format(fmt, key, val));
        }        
    }
    
    private void writeWarnings(FileWriter writer, String fmt, String headingFmt) 
                               throws IOException{
        
        writer.write(String.format(headingFmt, "NOMENCLATURE WARNINGS"));
        List<String> warnings = termGraph.getWarnings();
        for (String wrn : warnings) {
            writer.write(String.format(fmt, wrn));
        }                
    }
    
    private void writeUnknownIds(FileWriter writer, String sep, 
                                 String headingFmt, int breakAfter) 
                                 throws IOException{
        
        writer.write(String.format(headingFmt, "UNKNOWN IDS"));
        
        String unknownIdsString = termGraph.getUnknownIds(); 
        if (unknownIdsString != null) {
           
            String[] unknownIds = unknownIdsString.split(", ");

            if (unknownIds.length == 1) {
                writer.write(unknownIdsString + "\n");            
            }
            else {

                StringBuilder builder = new StringBuilder();
                int sepLen = sep.length();            
                int lineChars = 0;

                // Add separator after all but the last item
                for (int i=0; i < unknownIds.length; i++) {
                    if (lineChars + unknownIds[i].length() + sepLen > breakAfter) {
                        builder.append("\n");
                        lineChars = 0;
                    }                    
                    builder.append(unknownIds[i]);

                    // I don't care that this may be inefficient
                    if (i != unknownIds.length - 1) {
                        builder.append(sep);
                    }
                    lineChars += unknownIds[i].length() + sepLen;
                }                
                                
                writer.write(builder.toString() + "\n"); 
            }
        }
        
    }
    
    private void writeTermRelationships(FileWriter writer, String edgeFmt, 
                                        String nodeFmt, String headingFmt)
                                        throws IOException{
        
        writer.write(String.format(headingFmt, "TERM RELATIONSHIPS"));
        writer.write(termGraph.getRelationshipGraphString(edgeFmt));
        
        writer.write(String.format(headingFmt, "NODE PROPERTIES"));
        List<Object []> nodeProps = termGraph.getNodeProperties();
        for (Object [] row : nodeProps) {
            writer.write(String.format(nodeFmt, row));
        }        
    }
    
    private void writeTermHits(FileWriter writer, String fmt, String headingFmt,
                               boolean printTableHeadings) 
                               throws IOException{

        HashMap<String, Vector<Vector>> tables = termGraph.getNamespaceTables(true);
	for (String namespace : tables.keySet()) {
            Vector<Vector> rowData = tables.get(namespace);
            
            writer.write(String.format(headingFmt, namespace));
            if (printTableHeadings) {
                writer.write(String.format(fmt, (Object []) TermGraph.hitHeadings));
                writer.write(dashedLine);
            }
            
            for (Vector rowVector : rowData) {
                Object [] row = new Object[6];
                row[0] = rowVector.get(0); // Term ID
                row[1] = rowVector.get(1); // Name
                row[2] = String.format("%d", rowVector.get(2)); // Associations
                row[3] = String.format("%.4f", rowVector.get(3)); // Score
                row[4] = String.format("%.2e", rowVector.get(4)); // E-value
                row[5] = rowVector.get(5); // URL
                writer.write(String.format(fmt, row));                                
            }
            writer.write(dashedLine);
        }        
    }

}
