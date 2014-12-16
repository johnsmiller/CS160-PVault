package com.sjsu.techknowgeek.pvault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class MainActivity extends ListActivity implements SearchView.OnQueryTextListener, SearchManager.OnDismissListener, SearchManager.OnCancelListener, AdapterView.OnItemLongClickListener {

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int TIMEOUT_MILI = 300000;
    private static final int MAX_PICTURE_SIZE = 2048; //Max size for image view/texture via GPL

    private static Long Last_Sys_Time;
    private static MainActivity curInstance;
    private static File Most_Recent_Photo_File;
    private static SearchView searchView;

    File userDir;

    /*
    LIST ITEM METHODS BEGIN
     */

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call getListView().getItemAtPosition(position)
     * if they need to access the data associated with the selected item.
     *
     * @param l	The ListView where the click happened
     * @param v	The view that was clicked within the ListView
     * @param position	The position of the view in the list
     * @param id	The row id of the item that was clicked
     */

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Object obj = getListView().getItemAtPosition(position);
        File picture = new File(userDir, obj.toString());

        Bitmap pictureBitmap =  decodeSampledBitmapFromResource(picture, MAX_PICTURE_SIZE, MAX_PICTURE_SIZE);

        ImageView imageView = new ImageView(this);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(pictureBitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(imageView);
        builder.setTitle(obj.toString());
        builder.create().show();
    }

    /**Method from
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     *
     * @param file picture file to decode
     * @param maxWidth Maximum width supported
     * @param maxHeight Maximum height supported
     * @return the new bitmap
     */
    public static Bitmap decodeSampledBitmapFromResource(File file, int maxWidth, int maxHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap ret = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if(ret.getWidth()<ret.getHeight()) //If already portrait, just return
            return ret;

        //Else, we rotate
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        return Bitmap.createBitmap(ret, 0, 0, ret.getWidth(), ret.getHeight(), matrix, true);
    }

    /**Method from
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     *
     * @param options Factory options to retrieve picture size
     * @param maxWidth Maximum width supported
     * @param maxHeight Maximum height supported
     * @return maxSampleSize
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int maxWidth, int maxHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while(((height / inSampleSize) > maxHeight) || ((width / inSampleSize) > maxWidth)) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    /**
     * Callback method to be invoked when an item in this view has been
     * clicked and held.
     *
     * @param parent   The AbsListView where the click happened
     * @param view     The view within the AbsListView that was clicked
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        //Display options menu
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] options = {"Rename File", "Delete File"};
        builder.setTitle(R.string.file_options)
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) //Rename option
                            renameItemDialog(position);
                        else if(which == 1)
                            deleteItemDialog(position);
                    }
                });
        builder.create().show();
        return true;
    }

    private void renameItemDialog(int position)
    {
        final Object obj = getListView().getItemAtPosition(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter the new file name")
        .setMessage("File associations (ex: .jpg) are kept automatically");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //maintain file type
                String strIn = input.getText().toString() + obj.toString().substring(obj.toString().lastIndexOf("."));
                if(TextUtils.isEmpty(strIn) || new File(userDir, strIn).exists())
                {
                    Toast.makeText(getApplicationContext(), "Error encountered with rename", Toast.LENGTH_LONG).show();
                } else {
                    File curFile = new File(userDir, obj.toString());
                    File newFile = new File(userDir, strIn);
                    //noinspection ResultOfMethodCallIgnored
                    curFile.renameTo(newFile);
                    ServerConnection.fileRename(curFile.getName(), newFile.getName());
                    updateListView(null);
                }
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
        input.requestFocus();
    }

    private void deleteItemDialog(int position)
    {
        final Object obj = getListView().getItemAtPosition(position);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete file?");
        builder.setMessage(obj.toString());
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //noinspection ResultOfMethodCallIgnored
                new File(userDir, obj.toString()).delete();
                ServerConnection.fileDelete(obj.toString());
                updateListView(null);
                dialog.dismiss();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void changePasswordDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password")
                .setMessage("Enter current & new password (twice)");

        // Set up the input
        LinearLayout linearLayout= new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText pass0 = new EditText(this);
        pass0.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass0.setHint("Enter Current Password");
        linearLayout.addView(pass0);

        final EditText pass1 = new EditText(this);
        pass1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass1.setHint("Enter Password");
        linearLayout.addView(pass1);

        final EditText pass2 = new EditText(this);
        pass2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass2.setHint("Reenter Password");
        linearLayout.addView(pass2);

        builder.setView(linearLayout);

        // Set up the buttons
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Get Passwords
                String curPass = SealObject.encryptPass(pass0.getText().toString());
                String passStr1 = SealObject.encryptPass(pass1.getText().toString());
                String passStr2 = SealObject.encryptPass(pass2.getText().toString());

                if(!ServerConnection.checkPass(curPass) || passStr1 == null || !passStr1.equals(passStr2) || !LoginActivity.isPasswordValid(passStr1))
                {
                    Toast.makeText(getApplicationContext(), "Error: Current or new password is invalid", Toast.LENGTH_LONG).show();
                } else {
                    if(ServerConnection.userPasswordChange(ServerConnection.getUserName(), curPass, passStr1)){
                        Toast.makeText(getApplicationContext(), "Success! Password changed", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "A server error occurred. Connect to the internet and try again.", Toast.LENGTH_LONG).show();
                    }
                }
                //dialog.dismiss();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.dismiss();
            }
        });

        builder.show();
        pass0.requestFocus();

    }

    /*
    LIST ITEM METHODS END
     */


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
            Toast.makeText(this, "Sorry, no camera found on this system", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Sorry, I couldn't create a file to save the picture.", Toast.LENGTH_LONG).show();
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
        //noinspection ResultOfMethodCallIgnored
        image.createNewFile();
        return image;
    }

    /**
     * Called when camera intent returns
     * @param requestCode code given to identify request to this app
     * @param resultCode result of operation
     * @param data unused for photo
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (REQUEST_TAKE_PHOTO):
                if ((resultCode == Activity.RESULT_OK) && Most_Recent_Photo_File.exists()) {
                    File destination = new File(userDir, Most_Recent_Photo_File.getName());
                    //MOVE TO APP'S INTERNAL DIRECTORY
                    copyFile(Most_Recent_Photo_File, destination);
                    ServerConnection.fileUpload(destination, false);
                    //noinspection ResultOfMethodCallIgnored
                    Most_Recent_Photo_File.delete();
                } else
                    Toast.makeText(this, "Sorry: Capture was either cancelled or interrupted", Toast.LENGTH_LONG).show();
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
        Collections.addAll(files, list);

        ArrayAdapter<String> fileList =
                new ArrayAdapter<>(this, R.layout.arrayadapter_textview, files);

        setListAdapter(fileList);
    }

    protected static void refreshViewFromStaticContext()
    {
        curInstance.updateListView(null);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        updateListView(query);
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        updateListView(newText);
        return true;
    }

    /**
     * This method will be called when the search UI is dismissed. To make use of it, you must
     * implement this method in your activity, and call
     * {@link android.app.SearchManager#setOnDismissListener} to register it.
     */
    @Override
    public void onDismiss() {
        searchView.setQuery("", true);
        searchView.clearFocus();
    }

    /**
     * This method will be called when the search UI is canceled. To make use if it, you must
     * implement this method in your activity, and call
     * {@link android.app.SearchManager#setOnCancelListener} to register it.
     */
    @Override
    public void onCancel() {
        searchView.setQuery("", true);
        searchView.clearFocus();
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
        searchManager.setOnDismissListener(this);
        searchManager.setOnCancelListener(this);

        // Assumes current activity is the searchable activity
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);

        // Get the search close button image view
        int searchCloseButtonId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_close_btn", null, null);
        ImageView closeButton = (ImageView) searchView.findViewById(searchCloseButtonId);

        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery("", true);
                searchView.clearFocus();
            }
        });
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
        switch (item.getItemId()) {

            //noinspection SimplifiableIfStatement
            case R.id.action_change_password:
                changePasswordDialog();
                return true;

            case R.id.action_search:
                return true;

            case R.id.action_download_all:
                ServerConnection.fileRestore(userDir);
                return true;

            case R.id.action_upload_all:
                ServerConnection.fileUpload(userDir, true);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        curInstance = this;

        if(!new File(this.getApplicationInfo().dataDir + "/app_"+ServerConnection.getUserName()).exists()) {
            Toast.makeText(getApplicationContext(), "Files found on server. Commencing download", Toast.LENGTH_SHORT).show();
            //download all existing data
            ServerConnection.fileRestore(getDir(ServerConnection.getUserName(), MODE_PRIVATE));
        }

        userDir = getDir(ServerConnection.getUserName(), MODE_PRIVATE);

        updateListView(null);

        getListView().setOnItemLongClickListener(this);

        // Get the intent, verify the action and get the query
        handleIntent(getIntent());
    }
    /*
    STANDARD ANDROID ACTIVITY METHODS END
     */
}
