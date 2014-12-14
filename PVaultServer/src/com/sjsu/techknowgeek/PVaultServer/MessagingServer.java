/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsu.techknowgeek.PVaultServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.ftpserver.ftplet.FtpException;



/**
 *
 * @author John
 */
public class MessagingServer implements Runnable{
    /*
    functions:
    userLogin() //return "<Username> successfully logged in" if correct username and password
    userCreate() //return "<Username> successfully created" if new user created
    userPasswordReset() //return "<Username> password updated to <newPassword>" if password reset
    userPasswordChange() //return "<Username> password successfully updated" if password changed
    
    */
    private static MessagingServer msgServer;
    
    private static final int PORT = 7890;
    private static final int NUM_CONNECT = 50;
    private static final String WELCOME_MESSAGE = "MessagingServer";
    private static final String SUCCESS_MESSAGE = "OK\n";
    private static final String FAILURE_MESSAGE = "SORRY\n";
    private String message;

    private MessagingServer() {
        new Thread(this).start();
    }

    public static MessagingServer getInstance()
    {
        if(msgServer == null)
            msgServer = new MessagingServer();
        return msgServer;
    }

    @Override
    public void run() {
        while (true) {
            ServerSocket server = null;
            try {
                server = new ServerSocket(PORT, NUM_CONNECT);
                System.out.println("Messaging Socket Opened");
                Socket client = server.accept();

                System.out.println("Client Connected: " + client.getRemoteSocketAddress().toString());
                
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(client.getOutputStream()));

                //System.out.println("Sending Welcome");
                //writer.write(WELCOME_MESSAGE);
                //System.out.println("Sent Welcome");
                
                System.out.println("Message waiting");
                message = reader.readLine();
                System.out.println("Message: " + message);
                
                String ret = FAILURE_MESSAGE;

                //"changePassword", "resetPassword", "loginUser", "newUser"
                if (message.contains("newUser")) {
                    ret = newUser();
                } else if (message.contains("loginUser")) {
                   ret = loginUser();
                } else if (message.contains("resetPassword")) {
                    ret = resetPassword();
                } else if (message.contains("changePassword")) {
                   ret = changePassword();
                }        
                System.out.println("Sending: " + ret + " to the client");
                writer.write(ret+"\n");
                writer.flush();
                System.out.println("Sent");

            } catch (IOException ie) {
                System.out.println("Messaging Server Encountered an Error:" + ie.getLocalizedMessage());
            } finally {
                if (server != null) {
                    try {
                        server.close();
                        System.out.println("Socket Closed");
                    } catch (IOException ex) {
                        System.out.println("Messaging Server Encountered an Error:" + ex.getLocalizedMessage());
                    }
                }
            }
        }
    }

    private String newUser() {
        int userNameStart = message.indexOf(":")+1, delimiter = message.indexOf(",");
        String newUser = message.substring(userNameStart, delimiter);
        String password = message.substring(delimiter+1);
        
        try {
            return (Model.getInstance().addUser(newUser, password))? SUCCESS_MESSAGE : FAILURE_MESSAGE;
        } catch (FtpException ex) {
            System.out.println("Model's Add User Encountered an FTP Error: " + ex.getLocalizedMessage());
            return FAILURE_MESSAGE;
        }
    }

    private String loginUser() {
        int userNameStart = message.indexOf(":")+1, delimiter = message.indexOf(",");
        String user = message.substring(userNameStart, delimiter);
        String password = message.substring(delimiter+1);
        
        try {
            if(!Model.getInstance().isUser(user))
                return "NO_SUCH_USER";
            return (Model.getInstance().loginUser(user, password))? SUCCESS_MESSAGE : FAILURE_MESSAGE;
        } catch (FtpException ex) {
            System.out.println("Model's Login User Encountered an FTP Error: " + ex.getLocalizedMessage());
            return FAILURE_MESSAGE;
        }
    }

    private String resetPassword() {
        int userNameStart = message.indexOf(":")+1;
        String user = message.substring(userNameStart);
        
        try {
            return (Model.getInstance().resetUserPassword(user)!=null)? SUCCESS_MESSAGE : FAILURE_MESSAGE;
        } catch (FtpException ex) {
            System.out.println("Model's Reset User Password Encountered an FTP Error: " + ex.getLocalizedMessage());
            return FAILURE_MESSAGE;
        }
    }

    private String changePassword() {
        int userNameStart = message.indexOf(":")+1, delimiter1 = message.indexOf(","), delimiter2 = message.indexOf(",", delimiter1+1);
        String user = message.substring(userNameStart, delimiter1);
        String oldPassword = message.substring(delimiter1+1);
        String newPassword = message.substring(delimiter2+1);
        
        try {
            return (Model.getInstance().changeUserPassword(user, oldPassword, newPassword))? SUCCESS_MESSAGE : FAILURE_MESSAGE;
        } catch (FtpException ex) {
            System.out.println("Model's Change User Password Encountered an FTP Error: " + ex.getLocalizedMessage());
            return FAILURE_MESSAGE;
        }
    }   
    
}
