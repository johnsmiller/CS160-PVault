/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.util.ArrayList;
import java.util.List;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

/**
 *
 * @author John
 */
public class Model {
    private final int RESET_PASSWORD_LENGTH = 6;
    
    private static Model modelInstance;
    
    protected static Model getInstance()
    {
        if(modelInstance == null)
            modelInstance = new Model();
        return modelInstance;
    }
    
    private Model()
    {
        
    }
    
    private void updateFtpUser(String username, String password) throws FtpException
    {
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        List<Authority> authorities=new ArrayList<Authority>();
        authorities.add(new WritePermission());
        authorities.add(new ConcurrentLoginPermission(Integer.MAX_VALUE,Integer.MAX_VALUE));
        user.setAuthorities(authorities);
        int split = username.indexOf("@");
        user.setHomeDirectory(username.substring(0, split).toLowerCase() + "\'AT\'" + username.substring(split+1).toLowerCase());
        
        FTPServer.getInstance().getUserManager().save(user);
    }
    
    protected synchronized boolean addUser(String username, String password) throws FtpException
    {      
        updateFtpUser(username, password);
        
        return true;
    }
    
    protected synchronized boolean removeUser(String username) throws FtpException
    {
        FTPServer.getInstance().getUserManager().delete(username);
        
        return true; 
    }
    
    protected synchronized boolean isUser(String username) throws FtpException
    {
        return FTPServer.getInstance().getUserManager().doesExist(username);
    }
    
    /**
     * Note: will return null if user does not exist
     * @param username
     * @return the new password or null if user does not exist
     */
    protected synchronized String resetUserPassword(String username) throws FtpException
    {
        if(!FTPServer.getInstance().getUserManager().doesExist(username))
            return null;
        String password = "";
        for(int i = 0; i < RESET_PASSWORD_LENGTH; i++)
        {
            password += Math.floor(Math.random()*10);
        }
        
        updateFtpUser(username, SealObject.encryptPass(password));
        
        System.out.println("New password generated for " + username + "\nNew Password is: " + password);
        
        return password;
    }
    
    protected synchronized boolean loginUser(String username, String password) throws FtpException
    {
        try{
            FTPServer.getInstance().getUserManager().authenticate(new UsernamePasswordAuthentication(username, password));
            return true;
        } catch (AuthenticationFailedException ex) {
            return false;
        }
    }
    
    protected synchronized boolean changeUserPassword(String username, String oldPassword, String newPassword) throws FtpException
    {
        if(loginUser(username, oldPassword))
        {            
            updateFtpUser(username, newPassword);
            
            return true;
        }
        return false;
    }
}
