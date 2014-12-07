/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

/**
 *
 * @author John
 */
public class FTPServer {
    /*
    Functions:
    fileUpload() //Add file to database
    fileRename() //Rename existing file
    fileDelete() //Delete existing file
    fileRestore() //return all existing files
    */
    private static FTPServer serverInstance;
    
    private FtpServer server;
    private UserManager userManager;
    
    private static final String PROPERTIES_FILE_NAME = "myusers.properties";
    
    
    private FTPServer() throws FtpException
    {
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File file = new File(PROPERTIES_FILE_NAME);
        if(!file.exists())
            createFile();
        
        userManagerFactory.setFile(file);
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
        userManager = userManagerFactory.createUserManager(); 
        
        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userManager);
        server = serverFactory.createServer();
        server.start();
        
        System.out.println("FTP Server Running");
    }
    
    protected static FTPServer getInstance() throws FtpException
    {
        if(serverInstance == null)
            serverInstance = new FTPServer();
        return serverInstance;
    }
    
    protected UserManager getUserManager()
    {
        return userManager;
    }

    private void createFile() {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PROPERTIES_FILE_NAME), "utf-8"));
        } catch (IOException ex) {
          // report
        } finally {
           try {writer.close();} catch (Exception ex) {}
}
    }
}
