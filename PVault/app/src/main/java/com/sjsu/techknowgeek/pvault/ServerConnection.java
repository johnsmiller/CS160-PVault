package com.sjsu.techknowgeek.pvault;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

/**
 * Created by John on 11/20/2014.
 * Warning: this program does not take into account concurrent commands/FTP connections.
 */
public class ServerConnection{
    private static final String SERVER_IP = "192.168.0.102";
    private static final Integer SERVER_MESSAGING_PORT = 7890;
    private static final int SERVER_TIMEOUT = 10000;
    private static final String SUCCESS_MESSAGE = "OK";
    private static final String FAILURE_MESSAGE = "SORRY";

    private static String loginUserName;
    private static String loginPassword;

    protected static String getUserName()
    {
        return loginUserName;
    }

    /**
     * Returns true if new user successfully created on server
     * @return true if new user created, false if user exists or server error
     */
    protected static boolean userCreate(String userName, String password)
    {
        //TODO: Send new user command and user credientials
        String command = "newUser:" + userName + "," + password;
        String result = sendCommand(command);
        if(result.contains(SUCCESS_MESSAGE))
        {
            loginUserName = userName.toLowerCase();
            loginPassword = password;
            return true;
        }
        return false;
    }

    /**
     * Returns 1 if successful, -1 is incorrect or server didn't respond, 0 if user does
     * not exist
     * @param userName email address / user name to send to server
     * @param password password for user
     * @return 1 if successful, -1 is incorrect or server didn't respond, 0 if user does
     * not exist
     */
    protected static int userLogin(String userName, String password)
    {
        //Send user login command and user credientials
        //return null if user does not exist, true if login successful, false otherwise
        String command = "loginUser:" + userName + "," + password;
        System.out.println(command);
        String result = sendCommand(command);
        System.out.println(result);
        if(result == null || result.contains(FAILURE_MESSAGE))
            return -1;
        if(result.contains(SUCCESS_MESSAGE)) {
            loginUserName = userName.toLowerCase();
            loginPassword = password;
            return 1;
        }
        return 0;
    }

    /**
     *
     * @param userName
     * @return true if password reset, false if server error or user does not exist
     */
    protected static Boolean userPasswordReset(String userName)
    {
        //Send password reset command and username
        String command = "resetPassword:" + userName;
        String result = sendCommand(command);
        return result.contains(SUCCESS_MESSAGE);
    }

    /**
     *
     * @param userName
     * @param oldPassword
     * @param newPassword
     * @return true if password successfully changed, false if server error or incorrect oldPassword
     */
    protected  static boolean userPasswordChange(String userName, String oldPassword, String newPassword)
    {
        //Send password change command, username, old password, and new password
        String command = "changePassword:" + userName + "," + oldPassword + "," + newPassword;
        try {
            String result = new ServerCommandTask().execute(command).get();
            return result.contains(SUCCESS_MESSAGE);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static void fileUpload(File file)
    {
        //Start file upload task
        new UploadFileTask().execute(file);
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static void fileRename(String oldFileName, String newFileName)
    {
        //send rename file command, old file name, & new file name
        new ModifyFilesTask().execute(oldFileName,newFileName);
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static void fileDelete(String fileName)
    {
        //send delete command
        new ModifyFilesTask().execute(fileName);
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static void fileRestore()
    {
        //start restore command
        DownloadFilesTask download = new DownloadFilesTask();
        download.execute();
    }

    private static FTPClient ftpServerConnect()
    {
        if(loginUserName == null || loginPassword == null)
            return null;

        boolean status = false;
        try {
            FTPClient mFtpClient = new FTPClient();
            mFtpClient.setConnectTimeout(SERVER_TIMEOUT);
            mFtpClient.connect(InetAddress.getByName(SERVER_IP));
            status = mFtpClient.login(loginUserName, loginPassword);
            Log.e("isFTPConnected", String.valueOf(status));
            if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                mFtpClient.setFileType(FTP.ASCII_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();
                FTPFile[] mFileArray = mFtpClient.listFiles();
                Log.e("Directory Size: ", String.valueOf(mFileArray.length));
                return mFtpClient;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void ftpServerDisconnect(FTPClient ftpClient)
    {

        try {
            ftpClient.logout();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class DownloadFilesTask extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... params) {
            //Download all the things!!!!!!
            FTPClient mFTPClient = ftpServerConnect();
            if(mFTPClient==null)
                return false;

            File parentDir = new File(loginUserName);
            if (!parentDir.exists())
                parentDir.mkdir();
            OutputStream outputStream = null;
            try {
                FTPFile[] ftpFiles = mFTPClient.listFiles();
                if(ftpFiles != null && ftpFiles.length > 0)
                {
                    for(int i = 0; i<ftpFiles.length; i++)
                    {
                        onProgressUpdate(i/ftpFiles.length);
                        outputStream = new BufferedOutputStream(new FileOutputStream(loginUserName + "/" + ftpFiles[i].getName()));
                        mFTPClient.retrieveFile(ftpFiles[i].getName(), outputStream);
                        outputStream.close();
                    }
                    ftpServerDisconnect(mFTPClient);
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                ftpServerDisconnect(mFTPClient);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        protected void onProgressUpdate(Integer... progress) {
            //TODO: Update File Download Progress??
        }

        protected void onPostExecute(Void v) {

        }
    }

    private static class UploadFileTask extends AsyncTask<File, Integer, Boolean> {
        protected Boolean doInBackground(File... params) {

            //Upload one of the things!!!!!!

            FTPClient mFTPClient = ftpServerConnect();
            if(mFTPClient==null)
                return false;

            try {
                FileInputStream srcFileStream = new FileInputStream(params[0]);
                boolean status = mFTPClient.storeFile("",
                        srcFileStream);
                Log.e("Status", String.valueOf(status));
                srcFileStream.close();
                ftpServerDisconnect(mFTPClient);
                return status;
            } catch (Exception e) {
                e.printStackTrace();
                ftpServerDisconnect(mFTPClient);
                return false;
            }
        }

        protected void onProgressUpdate(Integer... progress) {
            //TODO: Update File Download Progress??
        }

        protected void onPostExecute(Void v) {

        }
    }

    private static class ModifyFilesTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... params) {
            //Download all the things!!!!!!
            FTPClient mFTPClient = ftpServerConnect();
            if(mFTPClient==null)
                return false;


            OutputStream outputStream = null;
            try {
                boolean status = false;
                if(params.length==1) //delete command
                    status = mFTPClient.deleteFile(params[0]);
                else if(params.length==2)
                    status = mFTPClient.rename(params[0], params[1]);
                ftpServerDisconnect(mFTPClient);
                return status;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                ftpServerDisconnect(mFTPClient);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        protected void onProgressUpdate(Integer... progress) {
            //TODO: Update File Edit Progress??
        }

        protected void onPostExecute(Void v) {

        }
    }

    private static class ServerCommandTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            //Command all the things!!!!!!
            System.out.println("Command Beginning Execution");
            String output = "";
            Socket socket;
            try {
                System.out.println("Creating Socket");
                socket = new Socket(SERVER_IP, SERVER_MESSAGING_PORT);
                System.out.println("Socket Created");
                if (socket != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

                   BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream()));

                    System.out.println("Writing to Socket");
                    writer.write(params[0]+"\n");
                    writer.flush();
                    System.out.println("Wrote to Socket");
                    output = reader.readLine();
                    System.out.println("Read from Socket");
                    socket.close();
                    System.out.println("Closed Socket");
                } else
                    System.out.println("Socket was null!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return output;
            }
        }

        protected void onProgressUpdate(Void... progress) {

        }

        protected void onPostExecute(String response) {

        }
    }


    private static String sendCommand(String command) {
        System.out.println("Command Beginning Execution");
        String result = null;
        Socket socket;
        try {
            System.out.println("Creating Socket");
            socket = new Socket(SERVER_IP, SERVER_MESSAGING_PORT);
            System.out.println("Socket Created");
            if (socket != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                System.out.println("Writing to Socket");
                writer.write(command+"\n");
                writer.flush();
                System.out.println("Wrote to Socket");
                result = reader.readLine();
                System.out.println("Read from Socket");
                socket.close();
                System.out.println("Closed Socket");
            } else
                System.out.println("Socket was null!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
