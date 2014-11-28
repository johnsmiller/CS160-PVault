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
    protected static boolean userCreate()
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

    protected static boolean userLogin()
    {
        serverConnect();
        //TODO: Send user credientials
        serverDisconnect();
        return false;
    }

    protected static boolean fileUpload()
    {
        serverConnect();
        //TODO: Send file
        serverDisconnect();
        return false;
    }

    protected static boolean fileRename()
    {
        serverConnect();
        //TODO: send old file name, new file name, & rename command
        serverDisconnect();
        return false;
    }

    protected static boolean fileDelete()
    {
        serverConnect();
        //TODO: send file name & delete command
        serverDisconnect();
        return false;
    }

    protected static Object[] fileRestore()
    {
        serverConnect();
        //TODO: send restore command
        //TODO: read in object(s)
        //TODO: restore objects to local database?? Or just return them?
        serverDisconnect();
        return null;
    }

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
