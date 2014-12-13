package com.sjsu.techknowgeek.pvault;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.internal.view.menu.ListMenuItemView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SearchView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;


public class MainActivity extends ListActivity implements SearchView.OnQueryTextListener{

    File userDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: prompt to download all files
        if(!new File(this.getApplicationInfo().dataDir + "/"+ServerConnection.getUserName()).exists()) {
            //prompt to download all existing data
            //create folder before doing so
            getDir(ServerConnection.getUserName(), MODE_PRIVATE);
            //download all existing data
        }


        userDir = getDir(ServerConnection.getUserName(), MODE_PRIVATE);

        updateListView(null);

        // Get the intent, verify the action and get the query
        handleIntent(getIntent());

    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            updateListView(query);
        }
    }


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
    public void onPause()
    {
        super.onPause();
        this.finish();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        this.finish();
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
                    return filename.contains(query);
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
    }

    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        updateListView(query);
        return true;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}
