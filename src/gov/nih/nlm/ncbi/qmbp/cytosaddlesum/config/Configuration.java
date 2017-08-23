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

package gov.nih.nlm.ncbi.qmbp.cytosaddlesum.config;

import cytoscape.CytoscapeInit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Properties;


public class Configuration {

    private static final String defaultUrl = 
            "http://www.ncbi.nlm.nih.gov/CBBresearch/Yu/mn/enrich/enrich.cgi";

    private static Properties config;
    private static String pathToConfigFile;

    private static String tdbPath = "";
    private static String execPath = "";
    private static String etdInfoPath = "";
    private static Boolean isLocal = false;
    private static String serverUrl = defaultUrl;
    private static Integer count = 0;
    private static Boolean isChanged = false;    

    public static void initConfiguration () {

	config = new Properties();
        
	String fileName = "SaddleSum.props";
	File configFile = new File(CytoscapeInit.getConfigDirectory(), fileName);
        pathToConfigFile = configFile.getPath();

	boolean loadConfig = true;
	while(loadConfig) {
            try {
                readProps();
                loadConfig = false;
            }
            catch (FileNotFoundException e) {
                // Create a configuration file with the defaults.
                writeProps();
            }
            catch (IOException e) {}
        }
    }

    private static void writeProps() {
        
        config.setProperty("tdbFiles", tdbPath);
        config.setProperty("ssumExec", execPath);
        config.setProperty("etdInfo", etdInfoPath);
        config.setProperty("isLocal", isLocal.toString());
        config.setProperty("serverUrl", serverUrl);
        config.setProperty("count", count.toString());
        config.setProperty("isChanged", isChanged.toString());

        try {
            FileOutputStream out = new FileOutputStream(pathToConfigFile);
            config.store(out, null);
        }
        catch (FileNotFoundException f) { /* Fail */ }
        catch (IOException f) { /* Fail */ }        
    }
    
    
    
    public static void refresh(String tdbPath,
                               String execPath,
                               String etdInfoPath,
                               boolean isLocal,
                               String serverUrl,
                               boolean isChanged) {

        Configuration.tdbPath = tdbPath;
        Configuration.execPath = execPath;
        Configuration.etdInfoPath = etdInfoPath;
        Configuration.isLocal = isLocal;
        Configuration.serverUrl = serverUrl;
        Configuration.isChanged = isChanged;
        
        writeProps();
    }

    private static void readProps() throws FileNotFoundException,
						  IOException {
	InputStream readProp = new FileInputStream(pathToConfigFile);
	config.load(readProp);
        
      	tdbPath = config.getProperty("tdbFiles", "");
	execPath = config.getProperty("ssumExec", "");
	etdInfoPath = config.getProperty("etdInfo", "");
	isLocal = Boolean.valueOf(config.getProperty("isLocal", "false"));
	serverUrl = config.getProperty("serverUrl", defaultUrl);
	count = new Integer(config.getProperty("count", "0"));
	isChanged = Boolean.valueOf(config.getProperty("isChanged", "false"));        
    }

    public static String getTDBPath(){
	return tdbPath;
    }

    public static String getExecPath(){
	return execPath;
    }

    public static String getETDInfoPath() {
	return etdInfoPath;
    }

    public static String getServerUrl() {
	return serverUrl;
    }

    public static boolean isLocalQuery() {
	return isLocal;
    }

    public static int incrementCount() {
	count++;
	if (count >= 1000) {
	    count = 1;
	}
        writeProps();
	return count;
    }

    public static int getCount() {
	return count;
    }

    public static Boolean getIsChanged() {
        return isChanged;
    }

    public static void setIsChanged(Boolean isChanged) {
        Configuration.isChanged = isChanged;
        writeProps();
    }
}
