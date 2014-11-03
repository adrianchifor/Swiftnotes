package com.moonpi.swiftnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener{

    /*
    Copyright © 2014 MoonPi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    */

    /*
    JSON file structure:
    root_OBJ:{
        notes_ARR:[
            newNote_OBJ:{
                "title":"", "body":"", "colour":"", "favoured":true/false, "fontSize":14/18/22},
            newNote_OBJ:{
                "title":"", "body":"", "colour":"", "favoured":true/false, "fontSize":14/18/22}, etc
        ]
    };
    */

    private static final int NEW_NOTE_REQUEST = 15000; // requestCode for new note activity
    private static final int DIALOG_BACKUP_CHECK = 1;
    private static final int DIALOG_RESTORE_CHECK = 2;
    private static final int DIALOG_BACKUP_OK = 3;
    private static final int DIALOG_RESTORE_FAILED = 4;

    private boolean backupSuccessful = false;
    private boolean restoreSuccessful = false;
    private String backupFilePath = "";

    private int actionBarTitle = 0;
    private Typeface lobsterTwo;

    private ListView listView;
    private TextView noNotes;

    private JSONObject root;
    private JSONArray notes;


    // Custom notes adapter class
    private class NoteAdapter extends BaseAdapter implements ListAdapter {

        Activity parentActivity;
        JSONArray adapterData;
        LayoutInflater inflater;

        public NoteAdapter(Activity activity, JSONArray adapterData) {
            this.parentActivity = activity;
            this.adapterData = adapterData;
            this.inflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (this.adapterData != null) {
                return this.adapterData.length();
            }

            else
                return 0;
        }

        @Override
        public JSONObject getItem(int position) {
            if (this.adapterData != null) {
                return this.adapterData.optJSONObject(position);
            }

            else
                return null;
        }

        @Override
        public long getItemId(int position) {
            JSONObject jsonObject = getItem(position);

            return jsonObject.optLong("id");
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = this.parentActivity.getLayoutInflater().inflate(R.layout.list_view_note, null);
            }

            // Initialize layout items
            RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
            TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
            TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
            ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

            JSONObject jsonData = getItem(position);

            if (jsonData != null) {
                try {
                    // Get JSON data to variables
                    String title = jsonData.getString("title");
                    String body = jsonData.getString("body");
                    String colour = jsonData.getString("colour");
                    final Boolean favoured = jsonData.getBoolean("favoured");

                    // Set note views background colours to colour in JSON file
                    relativeLayout.setBackgroundColor(Color.parseColor(colour));

                    favourite.setOnClickListener(new View.OnClickListener() {
                        // If favourite was clicked
                        @Override
                        public void onClick(View v) {
                            if (!favoured)
                                setFavourite(position, true);

                            else
                                setFavourite(position, false);
                        }
                    });

                    if (favoured)
                        favourite.setImageResource(R.drawable.ic_fav);

                    else
                        favourite.setImageResource(R.drawable.ic_unfav);

                    titleView.setText(title);
                    bodyView.setText(body);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return convertView;
        }
    }

    private NoteAdapter adapter;


    // setFavourite method, sets a favourite in JSON file to true or false, in a specific position
    // And sorts notes array
    private void setFavourite(int position, Boolean favoured) {
        try {
            if (favoured) {
                JSONObject newFavourite = notes.getJSONObject(position);

                newFavourite.put("favoured", true);

                // Sort notes array so favoured note is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();
                    newArray.put(0, newFavourite);

                    // Copy contents to new sorted array
                    for (int i = 0; i < notes.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(notes.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    notes = newArray;
                    adapter.adapterData = notes;
                    adapter.notifyDataSetChanged();

                    try {
                        root.put("notes", notes);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    writeToJSON();

                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                else {
                    notes.put(position, newFavourite);
                    adapter.notifyDataSetChanged();
                }
            }

            // If un-favoured, set favoured to false and store into JSON
            else {
                JSONObject newFavourite = notes.getJSONObject(position);

                newFavourite.put("favoured", false);

                notes.put(position, newFavourite);
                adapter.notifyDataSetChanged();
            }

            writeToJSON();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // Write content to JSON file
    protected void writeToJSON() {
        try {
            BufferedWriter bWrite = new BufferedWriter(new OutputStreamWriter
                    (openFileOutput("notes.json", Context.MODE_PRIVATE)));
            bWrite.write(root.toString());
            bWrite.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read content from JSON file
    protected void readFromJSON() {
        try {
            BufferedReader bRead = new BufferedReader(new InputStreamReader
                    (openFileInput("notes.json")));
            root = new JSONObject(bRead.readLine());

        } catch (FileNotFoundException e) {
            e.printStackTrace();

            root = new JSONObject();

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get notes array from root
        try {
            notes = root.getJSONArray("notes");

        } catch (JSONException e) {
            e.printStackTrace();

            notes = new JSONArray();

            try {
                root.put("notes", notes);

            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get lobster_two asset and create typeface
        // Set action bar title to lobster_two typeface
        lobsterTwo = Typeface.createFromAsset(getAssets(), "lobster_two.otf");

        actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");

        TextView actionBarTitleView = null;
        if (actionBarTitle != 0)
            actionBarTitleView = (TextView) getWindow().findViewById(actionBarTitle);

        if (actionBarTitleView != null) {
            if (lobsterTwo != null)
                actionBarTitleView.setTypeface(lobsterTwo);
        }

        readFromJSON();
        writeToJSON();
        readFromJSON();

        File folder = new File(Environment.getExternalStorageDirectory() + "/Swiftnotes");
        File backupFile = new File(folder, "swiftnotes_backup.json");

        if (backupFile.exists())
            backupFilePath = backupFile.getAbsolutePath();


        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.listView);
        ImageButton newNote = (ImageButton) findViewById(R.id.newNote);
        noNotes = (TextView)findViewById(R.id.noNotes);

        // Initialize NoteAdapter with notes array
        adapter = new NoteAdapter(this, notes);

        registerForContextMenu(listView);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        newNote.setOnClickListener(this);

        // If no notes, show 'Press + to add new note' text, invisible otherwise
        if (notes.length() == 0)
            noNotes.setVisibility(View.VISIBLE);

        else
            noNotes.setVisibility(View.INVISIBLE);
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_BACKUP_CHECK) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.action_backup)
                    .setMessage(R.string.dialog_check_backup_if_sure)
                    .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // If note array not empty, continue
                            if (notes.length() > 0) {
                                if (isExternalStorageWritable()) {
                                    // Check if Swiftntotes folder exists, if not, create directory
                                    File folder = new File(Environment.getExternalStorageDirectory() + "/Swiftnotes");
                                    boolean folderCreated = true;

                                    if (!folder.exists()) {
                                        folderCreated = folder.mkdir();
                                    }

                                    // Check if backup file exists, if yes, delete and create new, if not, just create new
                                    File backupFile = new File(folder, "swiftnotes_backup.json");
                                    boolean backupFileCreated = false;

                                    if (backupFile.exists()) {
                                        boolean backupFileDeleted = backupFile.delete();

                                        if (backupFileDeleted) {
                                            try {
                                                backupFileCreated = backupFile.createNewFile();
                                                backupFilePath = backupFile.getAbsolutePath();

                                            } catch (IOException e) {
                                                e.printStackTrace();

                                                backupFileCreated = false;
                                            }
                                        }
                                    }

                                    // If backup file doesn't exist, create new
                                    else {
                                        try {
                                            backupFileCreated = backupFile.createNewFile();
                                            backupFilePath = backupFile.getAbsolutePath();

                                        } catch (IOException e) {
                                            e.printStackTrace();

                                            backupFileCreated = false;
                                        }
                                    }

                                    // Check if notes.json exists
                                    File notesFile = new File(getFilesDir() + "/notes.json");
                                    boolean notesFileCreated = false;

                                    if (notesFile.exists())
                                        notesFileCreated = true;

                                    //If everything exists, stream content from notes.json to backup file
                                    if (folderCreated && backupFileCreated && notesFileCreated) {
                                        backupSuccessful = true;

                                        InputStream is = null;
                                        OutputStream os = null;

                                        try {
                                            is = new FileInputStream(notesFile);
                                            os = new FileOutputStream(backupFile);

                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            backupSuccessful = false;
                                        }

                                        if (is != null && os != null) {
                                            byte[] buf = new byte[1024];
                                            int len;

                                            try {
                                                while ((len = is.read(buf)) > 0) {
                                                    os.write(buf, 0, len);
                                                }

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                backupSuccessful = false;
                                            }

                                            try {
                                                is.close();
                                                os.close();

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                backupSuccessful = false;
                                            }
                                        }


                                        if (backupSuccessful) {
                                            showBackupSuccessfulDialog();
                                        }

                                        else {
                                            Toast toast = Toast.makeText(getApplicationContext(),
                                                    getResources().getString(R.string.toast_backup_failed),
                                                    Toast.LENGTH_SHORT);
                                            toast.show();
                                        }
                                    }

                                    // Either folder or files weren't successfully created, toast failed
                                    else {
                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                getResources().getString(R.string.toast_backup_failed),
                                                Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                }

                                // If external storage not writable, toast failed
                                else {
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getResources().getString(R.string.toast_backup_failed),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }

                            // If notes array is empty, toast backup failed
                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_backup_no_notes),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }


        else if (id == DIALOG_BACKUP_OK) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_backup_created_title)
                    .setMessage(getResources().getString(R.string.dialog_backup_created) + " "
                            + backupFilePath)
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }


        else if (id == DIALOG_RESTORE_CHECK) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.action_restore)
                    .setMessage(R.string.dialog_check_restore_if_sure)
                    .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (isExternalStorageReadable()) {
                                File folder = new File(Environment.getExternalStorageDirectory() + "/Swiftnotes");
                                boolean folderExists = false;

                                if (folder.exists()) {
                                    folderExists = true;
                                }

                                File backupFile = new File(folder, "swiftnotes_backup.json");
                                boolean backupFileExists = false;

                                if (backupFile.exists()) {
                                    backupFileExists = true;
                                    backupFilePath = backupFile.getAbsolutePath();
                                }

                                File notesFile = new File(getFilesDir() + "/notes.json");
                                boolean notesFileExists = false;

                                if (notesFile.exists())
                                    notesFileExists = true;

                                if (folderExists && backupFileExists && notesFileExists) {
                                    restoreSuccessful = true;

                                    InputStream is = null;
                                    OutputStream os = null;

                                    try {
                                        is = new FileInputStream(backupFile);
                                        os = new FileOutputStream(notesFile);

                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                        restoreSuccessful = false;
                                    }

                                    if (is != null && os != null) {
                                        byte[] buf = new byte[1024];
                                        int len;

                                        try {
                                            while ((len = is.read(buf)) > 0) {
                                                os.write(buf, 0, len);
                                            }

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            restoreSuccessful = false;
                                        }

                                        try {
                                            is.close();
                                            os.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            restoreSuccessful = false;
                                        }
                                    }


                                    if (restoreSuccessful) {
                                        readFromJSON();
                                        writeToJSON();
                                        readFromJSON();

                                        adapter.notifyDataSetChanged();

                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                getResources().getString(R.string.toast_restore_successful),
                                                Toast.LENGTH_SHORT);
                                        toast.show();

                                        // Recreate Activity so adapter can inflate the notes
                                        recreate();
                                    }

                                    else {
                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                getResources().getString(R.string.toast_restore_unsuccessful),
                                                Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                }

                                // Either folder or files weren't successfully created, dialog failed
                                else {
                                    showRestoreFailedDialog();
                                }
                            }

                            // If external storage not readable, toast failed
                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_restore_unsuccessful),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }


        else if (id == DIALOG_RESTORE_FAILED) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_restore_failed_title)
                    .setMessage(R.string.dialog_restore_failed)
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        return null;
    }

    protected void showBackupSuccessfulDialog() {
        dismissDialog(DIALOG_BACKUP_CHECK);
        MainActivity.this.showDialog(DIALOG_BACKUP_OK);
    }

    protected void showRestoreFailedDialog() {
        dismissDialog(DIALOG_RESTORE_CHECK);
        MainActivity.this.showDialog(DIALOG_RESTORE_FAILED);
    }


    // deleteNote method, reconstructs the notes array without the un-required element
    protected void deleteNote(Context context, final int position) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_delete_title)
                    .setMessage(getResources().getString(R.string.dialog_delete) +
                        " '" + notes.getJSONObject(position).getString("title") + "'?")
                    .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            JSONArray newArray = new JSONArray();

                            // Copy contents to new array, without the removed item
                            for (int i = 0; i < notes.length(); i++) {
                                if (i != position) {
                                    try {
                                        newArray.put(notes.get(i));

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            notes = newArray;
                            adapter.adapterData = notes;
                            adapter.notifyDataSetChanged();

                            try {
                                root.put("notes", notes);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            writeToJSON();

                            // If no notes, show 'Press + to add new note' text, invisible otherwise
                            if (notes.length() == 0)
                                noNotes.setVisibility(View.VISIBLE);

                            else
                                noNotes.setVisibility(View.INVISIBLE);

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_deleted), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    })
                    .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .show();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        // If new note button pressed, start edit note activity with new note request code
        if (v.getId() == R.id.newNote) {
            Intent intent = new Intent(this, EditActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            intent.putExtra("requestCode", NEW_NOTE_REQUEST);

            startActivityForResult(intent, NEW_NOTE_REQUEST);
        }
    }

    // Item clicked in listView, start EditActivity and pass extras
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        try {
            // Package current note content and send to edit note activity
            intent.putExtra("title", notes.getJSONObject(position).getString("title"));
            intent.putExtra("body", notes.getJSONObject(position).getString("body"));
            intent.putExtra("colour", notes.getJSONObject(position).getString("colour"));
            intent.putExtra("fontSize", notes.getJSONObject(position).getInt("fontSize"));
            intent.putExtra("requestCode", position);

            startActivityForResult(intent, position);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle mBundle = data.getExtras();

            if (mBundle != null) {
                // If new note was saved
                if (requestCode == NEW_NOTE_REQUEST) {
                    try {
                        // Add new note to array
                        JSONObject newNote = new JSONObject();
                        newNote.put("title", mBundle.getString("title"));
                        newNote.put("body", mBundle.getString("body"));
                        newNote.put("colour", mBundle.getString("colour"));
                        newNote.put("favoured", false);
                        newNote.put("fontSize", mBundle.getInt("fontSize"));

                        notes.put(newNote);
                        adapter.notifyDataSetChanged();

                        writeToJSON();

                        // If no notes, show 'Press + to add new note' text, invisible otherwise
                        if (notes.length() == 0)
                            noNotes.setVisibility(View.VISIBLE);

                        else
                            noNotes.setVisibility(View.INVISIBLE);

                        Toast toast = Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_new_note), Toast.LENGTH_SHORT);
                        toast.show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // If existing note was saved
                else {
                    try {
                        // Update array with new data
                        JSONObject newNote = notes.getJSONObject(requestCode);
                        newNote.put("title", mBundle.getString("title"));
                        newNote.put("body", mBundle.getString("body"));
                        newNote.put("colour", mBundle.getString("colour"));
                        newNote.put("fontSize", mBundle.getInt("fontSize"));

                        // Update note at position
                        notes.put(requestCode, newNote);
                        adapter.notifyDataSetChanged();

                        writeToJSON();

                        Toast toast = Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_note_saved), Toast.LENGTH_SHORT);
                        toast.show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        else if (resultCode == RESULT_CANCELED) {
            Bundle mBundle = null;

            if (data != null && data.hasExtra("request"))
                mBundle = data.getExtras();

            if (requestCode == NEW_NOTE_REQUEST) {
                if (mBundle != null) {
                    // If new note discarded, show toast
                    if (mBundle.getString("request").equals("discard")) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_empty_note_discarded), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }

            else {
                if (mBundle != null) {
                    // If delete pressed in EditActivity, call deleteNote method with
                    // requestCode as position
                    if (mBundle.getString("request").equals("delete"))
                        deleteNote(this, requestCode);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(getResources().getString(R.string.action_delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Get info about long-pressed item
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // If delete pressed in context menu, call deleteNote method with position as argument
        if (item.getTitle() ==  getResources().getString(R.string.action_delete)) {
            deleteNote(this, info.position);

            return true;
        }

        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // 'Backup notes' pressed, ask user if sure
        // If yes, copy contents from notes.json to swiftnotes_backup.json on external storage
        if (id == R.id.action_backup) {
            MainActivity.this.showDialog(DIALOG_BACKUP_CHECK);

            return true;
        }

        // 'Restore notes' pressed, ask user if sure
        // If yes, copy content from swiftnotes_backup.json from external storage to notes.json
        else if (id == R.id.action_restore) {
            MainActivity.this.showDialog(DIALOG_RESTORE_CHECK);

            return true;
        }

        else if (id == R.id.action_rate_app) {
            final String appPackageName = getPackageName();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_rate_title)
                    .setMessage(R.string.dialog_rate_message)
                    .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + appPackageName)));

                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    }).setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }).show();

            return true;
        }


        return false;
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    // When activity is resumed, re-typeface action bar title
    @Override
    protected void onResume() {
        super.onResume();

        TextView actionBarTitleView = null;
        if (actionBarTitle != 0)
            actionBarTitleView = (TextView) getWindow().findViewById(actionBarTitle);

        if (actionBarTitleView != null) {
            if (lobsterTwo != null)
                actionBarTitleView.setTypeface(lobsterTwo);
        }
    }
}
