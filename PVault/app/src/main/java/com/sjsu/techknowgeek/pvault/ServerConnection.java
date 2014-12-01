package com.sjsu.techknowgeek.pvault;

/**
 * Created by John on 11/20/2014.
 */
public class ServerConnection {
    private static String serverIP = "127.0.0.1";
    //TODO: private static server session variable

    /**
     * Returns true if new user successfully created on server
     * @return true if new user created, false if user exists or server error
     */
    protected static boolean userCreate(String userName, String password)
    {
        serverConnect();
        /*
        TODO: Send user credientials
            Return true if user created
            False otherwise
         */
        serverDisconnect();
        return false;
    }

    /**
     * Returns true if operation is successful, false if user exists but no password, null if user
     * does not exist
     * @param userName email address / user name to send to server
     * @param password password for user
     * @return true if successful
     */
    protected static Boolean userLogin(String userName, String password)
    {
        serverConnect();
        //TODO: Send user credientials
        serverDisconnect();
        //TODO: return null if user does not exist, true if login successful, false otherwise
        return null;
    }

    protected static Boolean userPasswordReset(String userName)
    {
        serverConnect();
        //TODO: Send username and password reset command
        serverDisconnect();
        return false;
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static boolean fileUpload()
    {
        serverConnect();
        //TODO: Send file
        serverDisconnect();
        return false;
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static boolean fileRename()
    {
        serverConnect();
        //TODO: send old file name, new file name, & rename command
        serverDisconnect();
        return false;
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static boolean fileDelete()
    {
        serverConnect();
        //TODO: send file name & delete command
        serverDisconnect();
        return false;
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static Object[] fileRestore()
    {
        serverConnect();
        //TODO: send restore command
        //TODO: read in object(s)
        //TODO: restore objects to local database?? Or just return them?
        serverDisconnect();
        return null;
    }

    /**
     * Returns true if operation is successful, false otherwise. False indicates server
     * connection error
     * @return true if successful
     */
    private static boolean serverConnect()
    {
        //TODO: Server connection logic here
        return false;
    }

    private static void serverDisconnect()
    {
        //TODO: Server disconnect logic here
    }

}
