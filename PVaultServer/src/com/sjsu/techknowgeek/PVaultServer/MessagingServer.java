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
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;



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
        
    }

    public static MessagingServer getMessagingServerInstance()
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

                if (message.contains("register")) {
                    ret = register();
                } else if (message.contains("results:")) {
                   ret = result();
                } else if (message.contains("statistics:")) {
                    ret = statistics();
                }
                ret += ".\n";
                System.out.println("Sending: " + ret + " to the client");
                writer.write(ret);
                writer.flush();
                System.out.println("Sent");

            } catch (IOException ie) {
                ie.printStackTrace();
            } finally {
                if (server != null) {
                    try {
                        server.close();
                        System.out.println("Socket Closed");
                    } catch (IOException ex) {
                        Logger.getLogger(fingerciseserver.FingerciseServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    
    public static enum CommandEnum
    {
        createUser, loginUser, resetUserPassword, changeUserPassword, deleteFile, renameFile;
    }

    public static class ServerCommand
    {
        CommandEnum command;
        String userName;
        String userPassword;
        String newUserPassword;

        public ServerCommand(CommandEnum cmd, String usrName, String usrPassword, String newUsrPassword)
        {
            command = cmd;
            userName = usrName;
            userPassword = usrPassword;
            newUserPassword = newUsrPassword;
        }
    }
}
