/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.util.HashMap;

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
        //TODO: recover saved model?
        //initialize global variables
        users = new HashMap<>();
    }
    
    protected synchronized boolean addUser(String username, String password)
    {
        if(users.containsKey(username))
            return false;
        users.put(username, password);
        return true;
    }
    
    protected synchronized boolean removeUser(String username)
    {
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
    protected synchronized String resetUserPassword(String username)
    {
        if(!users.containsKey(username))
            return null;
        String ret = "";
        for(int i = 0; i < RESET_PASSWORD_LENGTH; i++)
        {
            ret += Math.floor(Math.random()*10);
        }
        
        users.put(username, SealObject.encryptPass(ret));
        
        return ret;
    }
    
    protected synchronized boolean loginUser(String username, String password)
    {
        return users.containsKey(username) && password.equals(users.get(username));
    }
    
    protected synchronized boolean changeUserPassword(String username, String oldPassword, String newPassword)
    {
        if(loginUser(username, oldPassword))
        {
            users.put(username, newPassword);
            return true;
        }
        return false;
    }
    
    
}
