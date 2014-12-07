/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
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
    
    private HashMap<String, String> users;
    
    protected static Model getInstance()
    {
        if(modelInstance == null)
            modelInstance = new Model();
        return modelInstance;
    }
    
    private Model()
    {
        //TODO: recover saved model? PROBLEM: FTP SERVER WILL REMEMBER USERS BUT MODEL CURRENTLY DOES NOT
        //SOLUTION: USE FTP SERVER AS MODEL??
        //initialize global variables
        users = new HashMap<>();
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
        user.setHomeDirectory(username.substring(0, username.indexOf("@")));
        
        FTPServer.getInstance().getUserManager().save(user);
    }
    
    protected synchronized boolean addUser(String username, String password) throws FtpException
    {
        if(users.containsKey(username))
            return false;
        users.put(username, password);
        
        updateFtpUser(username, password);
        
        return true;
    }
    
    protected synchronized boolean removeUser(String username) throws FtpException
    {
        FTPServer.getInstance().getUserManager().delete(username);
        
        return (users.remove(username) != null); 
    }
    
    protected synchronized boolean isUser(String username)
    {
        return users.containsKey(username);
    }
    
    /**
     * Note: will return null if user does not exist
     * @param username
     * @return the new password or null if user does not exist
     */
    protected synchronized String resetUserPassword(String username) throws FtpException
    {
        if(!users.containsKey(username))
            return null;
        String password = "";
        for(int i = 0; i < RESET_PASSWORD_LENGTH; i++)
        {
            password += Math.floor(Math.random()*10);
        }
        
        users.put(username, SealObject.encryptPass(password));
        
        updateFtpUser(username, password);
        
        return password;
    }
    
    protected synchronized boolean loginUser(String username, String password)
    {
        return users.containsKey(username) && password.equals(users.get(username));
    }
    
    protected synchronized boolean changeUserPassword(String username, String oldPassword, String newPassword) throws FtpException
    {
        if(loginUser(username, oldPassword))
        {
            users.put(username, newPassword);
            
            updateFtpUser(username, newPassword);
            
            return true;
        }
        return false;
    }
}
