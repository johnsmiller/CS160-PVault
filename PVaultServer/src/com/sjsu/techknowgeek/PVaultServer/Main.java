/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.ftplet.FtpException;

/**
 *
 * @author John
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Model.getInstance().addUser("user2@example", "password");
            System.out.println(Model.getInstance().loginUser("user2@example", "password"));
        } catch (FtpException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
