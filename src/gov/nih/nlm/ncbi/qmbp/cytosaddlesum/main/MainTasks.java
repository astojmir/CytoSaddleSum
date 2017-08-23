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
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.Configuration;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.tools.BackendTools;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.CyFileFilter;
import cytoscape.util.FileUtil;
import cytoscape.view.CytoscapeDesktop;

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.ResultsPanelManager;
import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.tools.ResultsExporter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Hashtable;


public class MainTasks {

    public static String querySaddleSum(String saddleSumQuery, 
                                        Hashtable queryTable, 
                                        Vector selectedNodes) {
        
     	SaddleSumQuery task = new SaddleSumQuery(saddleSumQuery, queryTable, 
                                                 selectedNodes);        
        // Execute Task in New Thread; pops open JTask Dialog Box.
        TaskManager.executeTask(task, getTaskConfig());
        return task.errMsg;
    }

    public static void restoreResultsPanel(CyNetwork termNetwork) {

        SaddleSumRestore task = new SaddleSumRestore(termNetwork);
        TaskManager.executeTask(task, getTaskConfig());
    }
    
    public static void exportResults(CyNetwork termNetwork, String extension,
                                     String title) {
        
        CyFileFilter filter = new CyFileFilter(extension);
        File chosenFile = FileUtil.getFile(title,
                                           FileUtil.SAVE,
                                           new CyFileFilter[] { filter });
        if (chosenFile == null) {
            return;
        }
        String fileName = chosenFile.getAbsolutePath();
        if (!fileName.endsWith("." + extension)) {
            fileName = fileName + "." + extension;
        }
        
        ExportResults task = new ExportResults(termNetwork, fileName, extension);       
        TaskManager.executeTask(task, getTaskConfig());                
    }

    public static void importResults(String title) {
        
        CyFileFilter filter = new CyFileFilter("tab");
        File chosenFile = FileUtil.getFile(title,
                                           FileUtil.LOAD,
                                           new CyFileFilter[] { filter });
        if (chosenFile == null) {
            return;
        }
        String fileName = chosenFile.getAbsolutePath();
        if (!fileName.endsWith(".tab")) {
            fileName = fileName + ".tab";
        }
        
        ImportResults task = new ImportResults(fileName);
        TaskManager.executeTask(task, getTaskConfig());                
    }
    
    private static JTaskConfig getTaskConfig() {
        // Configure JTask Dialog Pop-Up Box
        JTaskConfig jTaskConfig = new JTaskConfig();
        jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayCloseButton(true);
        jTaskConfig.displayCancelButton(true);

        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);
        jTaskConfig.displayTimeElapsed(true);
        jTaskConfig.displayTimeRemaining(false);
        return jTaskConfig;
    }
    
    public static class SaddleSumQuery implements Task {
        
        private cytoscape.task.TaskMonitor taskMonitor;
        private String saddleSumQuery;
        private Hashtable queryTable;
        private Vector selectedNodes;
        private CyNetwork mainNetwork;
        
        public String errMsg = null;
        private boolean isHalted = false;

        public SaddleSumQuery(String saddleSumQuery, Hashtable queryTable, 
                              Vector selectedNodes) {
            this.saddleSumQuery = saddleSumQuery;
            this.queryTable = queryTable;
            this.selectedNodes = selectedNodes;
            
            // Hopefully, this will not cause a race condition - this ought
            // to be the network used for weights
            mainNetwork = Cytoscape.getCurrentNetwork();
        }
        
        public void setTaskMonitor(TaskMonitor monitor)
                        throws IllegalThreadStateException {
            taskMonitor = monitor;
        }

        public void halt() {
            isHalted = true;
        }

        public String getTitle() {
            return "Running SaddleSum Query";
        }

        public void run() {
                                                       
            try {

                if (isHalted) {
                    return;
                }
                taskMonitor.setPercentCompleted(10);
                taskMonitor.setStatus("Querying SaddleSum...");
                String saddleSumOutput;
   		saddleSumOutput = BackendTools.runSaddleSum(queryTable,
                                                                saddleSumQuery,
								selectedNodes);

                if (saddleSumOutput.startsWith("ERROR:")) {
                    errMsg = saddleSumOutput;
                    return;
                }
                Configuration.incrementCount();

                if (isHalted) {
                    return;
                }
                taskMonitor.setPercentCompleted(60);
                taskMonitor.setStatus("Processing results...");
                
                HashMap<String, ArrayList<ArrayList>> parsedOutput;
                parsedOutput = BackendTools.parseSaddleSumOutput(saddleSumOutput);
                TermGraph graph = new TermGraph(parsedOutput);
                
                if (isHalted) {
                    return;
                }
                // First add result panels, after that show the graph
                taskMonitor.setPercentCompleted(80);
                taskMonitor.setStatus("Building result tables...");                               
                ResultsPanelManager.addResults(graph.termNetwork, mainNetwork);

                // Always show relationship graph, even if there are no results
                graph.showNetwork(taskMonitor);
                
                // We want to fire change of focus event
                CytoscapeDesktop cyDesktop = Cytoscape.getDesktop();
                cyDesktop.setFocus(graph.termNetwork.getIdentifier());

            }
            catch (Exception ex) {
                taskMonitor.setException(ex, null);
            }
        }
    }

    public static class SaddleSumRestore implements Task {
        
        private cytoscape.task.TaskMonitor taskMonitor;
        private CyNetwork termNetwork;
        private boolean isHalted = false;

        public SaddleSumRestore(CyNetwork termNetwork) {
            this.termNetwork = termNetwork;
        }
        
        public void setTaskMonitor(TaskMonitor monitor)
                        throws IllegalThreadStateException {
            taskMonitor = monitor;
        }

        public void halt() {
            isHalted = true;
        }

        public String getTitle() {
            return "Restoring SaddleSum Results";
        }

        public void run() {
                                                       
            try {
                if (isHalted) {
                    return;
                }
                TermGraph graph = new TermGraph(termNetwork);
                graph.updateNetwork(taskMonitor);

                if (isHalted) {
                    return;
                }
                taskMonitor.setPercentCompleted(50);
                taskMonitor.setStatus("Building result tables...");                
                
                ResultsPanelManager.addResults(graph.termNetwork, null);
                
                // We want to fire change of focus event to update input panel
                CytoscapeDesktop cyDesktop = Cytoscape.getDesktop();
                cyDesktop.setFocus(graph.termNetwork.getIdentifier());
            }
            catch (Exception ex) {
                taskMonitor.setException(ex, null);
            }
        }
    }

    public static class ExportResults implements Task {
        
        private cytoscape.task.TaskMonitor taskMonitor;
        private boolean isHalted = false;
        private ResultsExporter exporter;
        private String fileName;
        private String fileType;
        
        public ExportResults(CyNetwork termNetwork, String fileName, 
                             String fileType) {
            
            exporter = new ResultsExporter(termNetwork);
            this.fileName = fileName;
            this.fileType = fileType;
        }
        
        public void setTaskMonitor(TaskMonitor monitor)
                        throws IllegalThreadStateException {
            taskMonitor = monitor;
        }

        public void halt() {
            isHalted = true;
        }

        public String getTitle() {
            return "Exporting SaddleSum Results";
        }

        public void run() {                                                       
            try {
                if (isHalted) {
                    return;
                }
                taskMonitor.setStatus("Exporting results ...");                
                exporter.exportData(fileName, fileType);
            }
            catch (Exception ex) {
                taskMonitor.setException(ex, null);
            }
        }
    }

    public static class ImportResults implements Task {
        
        private cytoscape.task.TaskMonitor taskMonitor;
        private boolean isHalted = false;
        String fileName;

        public ImportResults(String fileName) {
            this.fileName = fileName;
        }
        
        public void setTaskMonitor(TaskMonitor monitor)
                        throws IllegalThreadStateException {
            taskMonitor = monitor;
        }

        public void halt() {
            isHalted = true;
        }

        public String getTitle() {
            return "Importing SaddleSum Results";
        }

        public void run() {
                                                       
            try {
                if (isHalted) {
                    return;
                }
                taskMonitor.setPercentCompleted(10);
                taskMonitor.setStatus("Reading File...");

                FileInputStream fileStream = new FileInputStream(fileName);
                InputStreamReader reader = new InputStreamReader(fileStream, 
                                                                 "US-ASCII");
                BufferedReader inStream = new BufferedReader(reader);
                
                StringBuilder outputBuffer = new StringBuilder();
                while (true) {
                    String lineRead = inStream.readLine();
                    if (lineRead == null){
                        break;
                    }
                    outputBuffer.append(lineRead).append("\n");
                }               
                fileStream.close();
                
                String saddleSumOutput = outputBuffer.toString();
                Configuration.incrementCount();

                if (isHalted) {
                    return;
                }
                taskMonitor.setPercentCompleted(60);
                taskMonitor.setStatus("Processing results...");
                
                HashMap<String, ArrayList<ArrayList>> parsedOutput;
                parsedOutput = BackendTools.parseSaddleSumOutput(saddleSumOutput);
                TermGraph graph = new TermGraph(parsedOutput);
                
                if (isHalted) {
                    return;
                }
                // First add result panels, after that show the graph
                taskMonitor.setPercentCompleted(80);
                taskMonitor.setStatus("Building result tables...");                               
                ResultsPanelManager.addResults(graph.termNetwork, null);

                // Always show relationship graph, even if there are no results
                graph.showNetwork(taskMonitor);
                
                // We want to fire change of focus event
                CytoscapeDesktop cyDesktop = Cytoscape.getDesktop();
                cyDesktop.setFocus(graph.termNetwork.getIdentifier());

            }
            catch (Exception ex) {
                taskMonitor.setException(ex, null);
            }
        }
    }
    
}
