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

import gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config.Configuration;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class BackendTools {

    public static HashMap<String, ArrayList<ArrayList>> parseSaddleSumOutput(String saddleSumOutput){

	HashMap<String, ArrayList<ArrayList>> parsedOutput = 
            new HashMap<String, ArrayList<ArrayList>>();
	ArrayList<ArrayList> sectionVector = null;

	String[] lines = saddleSumOutput.split("\n");
	String currentSectionName = new String();
	String currentSectionBuf = new String();
	int hashCount = 0;

	for(String line : lines) {

	    if (line.startsWith("#")){
		hashCount++;

		if (hashCount == 2) {

		    if (sectionVector != null){
			// Add the previous section to output
			// only if this current section is not
			// the first one.
			parsedOutput.put(currentSectionName, sectionVector);
		    }

		    // This cuts the # from the line
		    currentSectionBuf = line.substring(2);
		}
		else if (hashCount == 3) {
		    currentSectionName = currentSectionBuf;
		    sectionVector = new ArrayList<ArrayList> ();
		}
		else if (hashCount == 4) {
                    hashCount = 1;
                }
	    }

	    else {
		hashCount = 0;
		List lineList = Arrays.asList(line.split("\t"));
		ArrayList<String> lineVector = new ArrayList<String>(lineList);
		sectionVector.add(lineVector);
	    }
	}

	// After we reach the end of the output, we need to
	// manually add the last vector.
	parsedOutput.put(currentSectionName, sectionVector);
	return parsedOutput;
    }

    public static List<ETDFile> discoverTermDatabases(){
	if (Configuration.isLocalQuery()) {
            return discoverLocalDatabases();
	}
        return discoverWebDatabases();
    }

    private static List<ETDFile> discoverLocalDatabases() {
        ArrayList<ETDFile> etdFiles = new ArrayList<ETDFile> ();
        String TDBPath = Configuration.getTDBPath();
        String pathToFile;
        String fileTitle;

        File etdFolder = new File(TDBPath);
        String[] files = etdFolder.list();
        if (files == null) {
            return etdFiles; 
        }
        for (int i = 0; i < files.length; i++){
            if (files[i].matches("^.+\\.etd$")){
                pathToFile = TDBPath + "/" + files[i];
                fileTitle = getETDTitle(pathToFile);
                ETDFile newETDFile = new ETDFile(pathToFile, fileTitle);
                etdFiles.add(newETDFile);
            }
        }
        return etdFiles;        
    }
    
    private static List<ETDFile> discoverWebDatabases() {
        ArrayList<ETDFile> etdFiles = new ArrayList<ETDFile> ();
        
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet query = new HttpGet(Configuration.getServerUrl() + "?view=d");
            HttpResponse response = client.execute(query);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStreamReader content = new InputStreamReader(entity.getContent());
                BufferedReader queryResponse = new BufferedReader(content);
                String buf;
                while((buf = queryResponse.readLine()) != null){
                    String[] fileInfo = buf.split("\t");
                    ETDFile newEtdFile = new ETDFile(fileInfo[1], fileInfo[0]);
                    etdFiles.add(newEtdFile);
                }
            }
        }
        catch (Exception e) {
            // If control gets here something has failed. 
            // For now return an empty vector
            etdFiles = new ArrayList<ETDFile> ();
        }
        return etdFiles;        
    }
    
    private static String getETDTitle(String filePath){
	Process ETDDataProc = null;
	BufferedReader inStream = null;

	// If the title is not found in the etd info program,
	// the string is unchanged and the user will see "TITLE NOT FOUND".
	String ETDTitle = "TITLE NOT FOUND";
	String PATH_TO_EXEC = Configuration.getETDInfoPath();

	try {
	    ETDDataProc = Runtime.getRuntime().exec(PATH_TO_EXEC + " -F tab " + filePath);
	}
	catch(IOException e){
//	    e.printStackTrace();
	}

	try {
	    inStream = new BufferedReader(new InputStreamReader
					  (ETDDataProc.getInputStream()));

	    String lineRead = inStream.readLine();

	    while (lineRead != null) {
		if (lineRead.startsWith("Database name")) {
		    ETDTitle = lineRead.substring(14);
		    break;
		}
		lineRead = inStream.readLine();

	    }
	}
	catch (IOException e) {
//	    e.printStackTrace();
	}

	return ETDTitle;
    }

    public static String runSaddleSum(Hashtable argsTable, String argsString,
				      Vector selectedNodes) {
	if (Configuration.isLocalQuery()) {
            return runSaddleSumLocal(argsString, selectedNodes);
        }
	else {
            return runSaddleSumWeb(argsTable, selectedNodes);
        }
    }

    public static String runSaddleSumWeb(Hashtable args, Vector selectedNodes){

	DefaultHttpClient httpClient = new DefaultHttpClient();
	HttpPost query = new HttpPost(Configuration.getServerUrl());
	HttpResponse response;
	HttpEntity entity;
	StringBuffer nodeInfo;
	List <NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

	nodeInfo = new StringBuffer();
        for (String nodeLine : (Vector<String>) selectedNodes) {
            nodeInfo.append(nodeLine).append("\r\n");
        }
        
	nameValuePairs.add(new BasicNameValuePair("view", "a"));
	nameValuePairs.add(new BasicNameValuePair("termdb", (String)args.get("tdbFile")));
	nameValuePairs.add(new BasicNameValuePair("raw_weights", nodeInfo.toString()));
	nameValuePairs.add(new BasicNameValuePair("MAX_FILE_SIZE", "256"));
	nameValuePairs.add(new BasicNameValuePair("cutoff_Evalue", (String)args.get("eValCut")));
	nameValuePairs.add(new BasicNameValuePair("min_term_size",(String)args.get("minTerm")));
	nameValuePairs.add(new BasicNameValuePair("effective_tdb_size", (String)args.get("tdbSize")));
	nameValuePairs.add(new BasicNameValuePair("stats", (String)args.get("stats")));
	nameValuePairs.add(new BasicNameValuePair("transform_weights", (String)args.get("transf")));
	if (((String)args.get("cutoff")).startsWith("r")) {
	    nameValuePairs.add(new BasicNameValuePair("cutoff_type", "rank"));
	    nameValuePairs.add(new BasicNameValuePair("wght_cutoff", ((String)args.get("cutoff")).substring(2)));
	}
	else if (((String)args.get("cutoff")).startsWith("w")) {
	    nameValuePairs.add(new BasicNameValuePair("cutoff_type", "wght"));
	    nameValuePairs.add(new BasicNameValuePair("wght_cutoff", ((String)args.get("cutoff")).substring(2)));
	}
	else{
	    nameValuePairs.add(new BasicNameValuePair("cutoff_type", "none"));
	}

	nameValuePairs.add(new BasicNameValuePair("output", "tab"));

	try {
	query.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
	response = httpClient.execute(query);
	entity = response.getEntity();

	if (entity != null) {

	    BufferedReader queryResponse = new BufferedReader(new InputStreamReader(entity.getContent()));
	    StringBuilder output = new StringBuilder();
	    String buf;
	    while((buf = queryResponse.readLine()) != null) {
                output.append(buf).append("\n");
            }
	    return output.toString();
	}
	else{
//	    System.out.println("ENTITY IS NULL.");
	    return null;
	}
	}
	catch (Exception e) {
//	    System.out.println("EXCEPTION: " + e.getMessage());
	    return null;
	}
    }

    public static String runSaddleSumLocal(String args, Vector selectedNodes){
	Process SaddleSumExec = null;
        int exitValue;
	PrintWriter outStream = null;
	BufferedReader inStream = null;
	BufferedReader errStream = null;

	StringBuffer outputBuffer = new StringBuffer();

	String PATH_TO_EXEC = Configuration.getExecPath();

	try {
	    SaddleSumExec = Runtime.getRuntime().exec(PATH_TO_EXEC + " " +  args);
	}
	catch(IOException e){
	    outputBuffer.append("ERROR: ").append(e.getMessage());
//	    e.printStackTrace();
	    return outputBuffer.toString();
	}

	try {
	    inStream = new BufferedReader(new InputStreamReader
					  (SaddleSumExec.getInputStream()));
	    errStream = new BufferedReader(new InputStreamReader
					   (SaddleSumExec.getErrorStream()));
	    outStream = new PrintWriter(SaddleSumExec.getOutputStream());

            for (String nodeLine : (Vector<String>) selectedNodes) {
		outStream.print(nodeLine + "\n");                
            }
	    outStream.flush();
	    outStream.close();

	    String lineRead;

            // First attemt to read stdout
	    while (true) {
		lineRead = inStream.readLine();

		if (lineRead == null){
		    break;
		}
		outputBuffer.append(lineRead).append("\n");
	    }

            // Then read stderr
	    lineRead = errStream.readLine();

	    // If there is an error in the stream, we do not return the
	    // normal output; only the error.

	    // Append the word ERROR: to the front so that CytoSaddleSum knows we have
	    // encountered an error.

	    if (lineRead != null) {
                outputBuffer = new StringBuffer();
		outputBuffer.append("ERROR:");
		outputBuffer.append(lineRead).append("\n");

		while(true) {
		    lineRead = errStream.readLine();
		    if (lineRead == null){
			break;
		    }
		    if (lineRead.startsWith("GO")) {
                        outputBuffer.append(lineRead).append("\n");}
		}
		return outputBuffer.toString();
	    }

	}
	catch (IOException e) {
	    outputBuffer.append("Error in reading inputs");
	}
	return outputBuffer.toString();
    }
}
