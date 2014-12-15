package com.sjsu.techknowgeek.pvault;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Comparator;
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

    protected static boolean checkPass(String pass)
    {
        return loginPassword != null && loginPassword.equals(pass);
    }

    /**
     * Returns true if new user successfully created on server
     * @return true if new user created, false if user exists or server error
     */
    protected static boolean userCreate(String userName, String password)
    {
        //Send new user command and user credientials
        String command = "newUser:" + userName + "," + password;
        String result = sendCommand(command);
        if(result != null && result.contains(SUCCESS_MESSAGE))
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
        String result = sendCommand(command);

        if(result == null || result.contains(FAILURE_MESSAGE))
            return -1;
        if(result.contains(SUCCESS_MESSAGE)) {
            loginUserName = userName.toLowerCase();
            loginPassword = password;
            return 1;
        }
        return 0; //Non-standard error message sent, assume user does not exist on server
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
        return (result != null) && result.contains(SUCCESS_MESSAGE);
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
        String result = sendCommand(command);
        if((result != null) && result.contains(SUCCESS_MESSAGE)) {
            loginPassword = newPassword;
            return true;
        }
        return false;
    }

    /**
     * Returns true if operation is successful, false otherwise. False may indicate server
     * connection error
     * @return true if successful
     */
    protected static void fileUpload(File file, boolean isDir)
    {
        //Start file upload task
        if(isDir)
            new UploadFileTask().execute(file.listFiles());
        else
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
    protected static void fileRestore(File parentDirectory)
    {
        //start restore command
        DownloadFilesTask download = new DownloadFilesTask();
        download.execute(parentDirectory);
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
            Log.i("isFTPConnected", String.valueOf(status));
            if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                mFtpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();
                if(!mFtpClient.changeWorkingDirectory("/"))
                    mFtpClient.makeDirectory("/");
                FTPFile[] mFileArray = mFtpClient.listFiles();
                Log.i("Directory Size: ", String.valueOf(mFileArray.length));
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

        }
        try {
            ftpClient.disconnect();
        } catch (IOException e) {

        }
    }

    private static class DownloadFilesTask extends AsyncTask<File, Integer, Boolean> {
        protected Boolean doInBackground(File... params) {
            //Download all the things!!!!!!
            FTPClient mFTPClient = ftpServerConnect();
            if(mFTPClient==null)
                return false;

            File parentDir = params[0];
            if (!parentDir.exists())
                parentDir.mkdir();

            OutputStream outputStream = null;

            try {
                FTPFile[] ftpFiles = mFTPClient.listFiles();
                if(ftpFiles != null && ftpFiles.length > 0)
                {
                    for(int i = 0; i<ftpFiles.length; i++)
                    {
                        try {
                            onProgressUpdate(i/ftpFiles.length);
                            outputStream = new BufferedOutputStream(new FileOutputStream(parentDir + "/" + ftpFiles[i].getName()));
                            mFTPClient.retrieveFile(ftpFiles[i].getName(), outputStream);
                            outputStream.close();
                        } catch (IOException ex) {
                            Log.i("Download Files", "Unable to download " + ftpFiles[i].getName() + " from server", ex);
                        }

                    }
                    Log.i("Download Files", "FTP Command Completed Successfully");
                    ftpServerDisconnect(mFTPClient);
                    return true;
                }
            } catch (Exception ex) {
                Log.e("Download Files", "FTP Command encountered an error", ex);
            } finally {
                ftpServerDisconnect(mFTPClient);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {

                    }
                }
            }
            return false;
        }

        protected void onProgressUpdate(Integer... progress) {
            //TODO: Update File Download Progress??
        }

        protected void onPostExecute(Boolean v) {
            MainActivity.refreshViewFromStaticContext();
        }
    }

    private static class UploadFileTask extends AsyncTask<File, Integer, Boolean> {
        protected Boolean doInBackground(File... params) {

            //Upload all of the things!!!!!!

            FTPClient mFTPClient = ftpServerConnect();
            if(mFTPClient==null)
                return false;

            String[] fileNames = null;

            try{
                fileNames = mFTPClient.listNames();
            } catch (IOException ex)
            {

            }

            for(File f : params) {
                try {
                    if(Arrays.binarySearch(fileNames, f.getName())>=0)
                    {
                        mFTPClient.deleteFile(f.getName());
                    }
                    BufferedInputStream srcFileStream = new BufferedInputStream(new FileInputStream(f));
                    boolean status = mFTPClient.storeUniqueFile(f.getName(),
                            srcFileStream);
                    Log.i("FTP Upload Status", String.valueOf(status));
                    srcFileStream.close();
                }  catch (Exception e) {
                    Log.i("Upload Files", "Uploading " + f.getName() + " may have failed", e);
                }
            }
            ftpServerDisconnect(mFTPClient);
            Log.i("Upload Files", "FTP Command Completed Successfully");
            return true;
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
                else if(params.length==2) //rename command
                    status = mFTPClient.rename(params[0], params[1]);
                ftpServerDisconnect(mFTPClient);
                Log.i("Modify Files", "FTP Command Completed Successfully");
                return status;
            } catch (Exception ex) {
                Log.e("Modify Files", "FTP Encountered an error");
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
            return sendCommand(params[0]);
        }

        protected void onProgressUpdate(Void... progress) {

        }

        protected void onPostExecute(String response) {

        }
    }


    private static String sendCommand(String command) {
        String result = null;

        if(Looper.getMainLooper().getThread() == Thread.currentThread())
        {
            try {
                result = new ServerCommandTask().execute(command).get();

            } catch (InterruptedException | ExecutionException e) {

            } finally {
                return result;
            }
        }

        Socket socket;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(SERVER_IP, SERVER_MESSAGING_PORT), SERVER_TIMEOUT);
            if (socket != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                writer.write(command+"\n");
                writer.flush();
                result = reader.readLine();
                socket.close();
                Log.i("Command", "Server Command Completed Successfully");
            } else
                Log.e("Socket", "Socket was null!");
        } catch (Exception e) {
            Log.i("Messaging Server Error", "Sending Message Failed", e);
        }
        return result;
    }
}
