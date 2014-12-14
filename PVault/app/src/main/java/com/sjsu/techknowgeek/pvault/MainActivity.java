package com.sjsu.techknowgeek.pvault;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.internal.view.menu.ListMenuItemView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends ListActivity implements SearchView.OnQueryTextListener{

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int TIMEOUT_MILI = 300000;

    private static Long Last_Sys_Time;
    private static MainActivity curInstance;
    private static File Most_Recent_Photo_File;
    private static String curQuery;

    File userDir;

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Toast.makeText(this, "Clicked row " + position, Toast.LENGTH_SHORT).show();
    }

    /*
    CAPTURE PICTURE METHODS BEGIN
     */

    /**
     * Method called by button to initiate camera capture intent
     * @param view unused variable required by android
     */
    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Sorry, no camera found on this system", Toast.LENGTH_LONG);
            return;
        }
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Continue only if the File was successfully created
        if (photoFile == null) {
            Toast.makeText(this, "Sorry, I couldn't create a file to save the picture.", Toast.LENGTH_LONG);
            return;
        }

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photoFile));
        Most_Recent_Photo_File = photoFile;
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
    }

    /**
     * Helper method for dispatchTakePictureIntent method
     * @return a new file created as a temporary placeholder
     * @throws IOException if task did not complete successfully
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File tempDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);//AUGH! Camera cannot access app-only files. WHYY ME!!!!!!!
        File image = new File(tempDir /* directory */, imageFileName+ /* prefix */".jpg"/* suffix */);
        image.createNewFile();
        return image;
    }

    /**
     * Called when camera intent returns
     * @param requestCode code given to identify request to this app
     * @param resultCode result of operation
     * @param data
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (REQUEST_TAKE_PHOTO):
                if (resultCode == Activity.RESULT_OK && Most_Recent_Photo_File.exists()) {
                    File destination = new File(userDir, Most_Recent_Photo_File.getName());
                    //MOVE TO APP'S INTERNAL DIRECTORY
                    copyFile(Most_Recent_Photo_File, destination);
                    ServerConnection.fileUpload(destination);
                    Most_Recent_Photo_File.delete();
                } else
                    Toast.makeText(this, "Sorry: Capture was either cancelled or interrupted", Toast.LENGTH_LONG);
                break;
            default:
                break;
        }
    }

    public static void copyFile(File src, File dst)
    {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dst).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } if (outChannel != null) {
                try {
                    outChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /*
    CAPTURE PICTURE METHODS END
     */

    /*
    SEARCH METHODS BEGIN
     */

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            updateListView(query);
        }
    }

    private void updateListView(final String query)
    {
        String[] list;
        if(query == null || query.length()==0)
            list = userDir.list();

        else {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.toLowerCase().contains(query.toLowerCase());
                }
            };

            list = userDir.list(filter);
        }

        ArrayList<String> files = new ArrayList<>();
        for(String s : list)
            files.add(s);

        ArrayAdapter<String> fileList =
                new ArrayAdapter<String>(this, R.layout.arrayadapter_textview, files);

        setListAdapter(fileList);

        curQuery = query.toLowerCase();
    }

    protected static void refreshViewFromStaticContext()
    {
        curInstance.updateListView(null);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        updateListView(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
    /*
    SEARCH METHODS END
     */

    /*
    STANDARD ANDROID ACTIVITY METHODS BEGIN
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);

        return true;
    }


    @Override
    public void onPause()
    {
        Last_Sys_Time = System.currentTimeMillis();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(Last_Sys_Time != null && System.currentTimeMillis() - Last_Sys_Time > TIMEOUT_MILI)
        {
            this.finish();
        }
        else {
            updateListView(null);
        }
    }
/*
    @Override
    public void onStop()
    {
        super.onStop();
        this.finish();
    }
*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search || id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        curInstance = this;

        //TODO: prompt to download all files
        if(!new File(this.getApplicationInfo().dataDir + "/app_"+ServerConnection.getUserName()).exists()) {
            //prompt to download all existing data
            System.out.println("Downloading existing files");

            //download all existing data
            ServerConnection.fileRestore(getDir(ServerConnection.getUserName(), MODE_PRIVATE));
        }


        userDir = getDir(ServerConnection.getUserName(), MODE_PRIVATE);

        updateListView(null);

        // Get the intent, verify the action and get the query
        handleIntent(getIntent());

    }
    /*
    STANDARD ANDROID ACTIVITY METHODS END
     */
}
