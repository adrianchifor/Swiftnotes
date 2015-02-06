package com.moonpi.swiftnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, Toolbar.OnMenuItemClickListener,
        AbsListView.MultiChoiceModeListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {


    private static File localPath, backupPath;

    // Layout components
    private ListView listView;
    private TextView noNotes;
    private Toolbar toolbar;

    private JSONArray notes, tempNotesBackup;
    private NoteAdapter adapter;
    private ArrayList<Integer> checkedArray = new ArrayList<Integer>();

    private SearchView searchView;

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    private Boolean searchActive = false;

    // To keep track of real indexes of filtered notes
    private ArrayList<Integer> realIndexesOfSearchResults;

    private float newNoteButtonBaseYCoordinate;

    private AlertDialog backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        MainActivity.localPath = new File(getFilesDir() + "/" + DataUtils.NOTES_FILE_NAME);

        File backupFolder = new File(Environment.getExternalStorageDirectory() + DataUtils.BACKUP_FOLDER_PATH);

        if (DataUtils.isExternalStorageReadable() && DataUtils.isExternalStorageWritable())
            if (!backupFolder.exists())
                backupFolder.mkdir();

        MainActivity.backupPath = new File(backupFolder, DataUtils.BACKUP_FILE_NAME);

        // Init notes array
        notes = new JSONArray();

        // Retrieve from local path
        JSONArray tempNotes = DataUtils.retrieveData(localPath);

        // If not null, equal main notes to retrieve notes and backup, required for search
        if (tempNotes != null) {
            notes = tempNotes;
            tempNotesBackup = notes;
        }


        setContentView(R.layout.activity_main);

        // Init toolbar
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        if (toolbar != null)
            initToolbar();

        // Init layout components
        listView = (ListView)findViewById(R.id.listView);
        final ImageButton newNote = (ImageButton) findViewById(R.id.newNote);
        noNotes = (TextView)findViewById(R.id.noNotes);

        newNoteButtonBaseYCoordinate = newNote.getY();

        // Initialize NoteAdapter with notes array
        adapter = new NoteAdapter(this, notes);
        listView.setAdapter(adapter);

        // Set item click, multi choice and scroll listeners
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If scrolling, hide newNote button
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    newNote.animate().cancel();
                    newNote.animate().translationYBy(400);
                }

                // If not scrolling and first item of list view is visible, show newNote button
                else {
                    if (view.getFirstVisiblePosition() == 0) {
                        newNote.animate().cancel();
                        newNote.animate().translationY(newNoteButtonBaseYCoordinate);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });


        newNote.setOnClickListener(this);

        // If no notes, show 'Press + to add new note' text, invisible otherwise
        if (notes.length() == 0)
            noNotes.setVisibility(View.VISIBLE);

        else
            noNotes.setVisibility(View.INVISIBLE);

        initDialogs(this);
    }


    /**
     * Initialize toolbar with required components such as
     * - title, elevation (21+), menu/OnMenuItemClickListener, searchView -
     */
    protected void initToolbar() {
        toolbar.bringToFront();

        toolbar.setTitle(getResources().getString(R.string.app_name));

        if (Build.VERSION.SDK_INT >= 21)
            toolbar.setElevation(10.0f);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.main);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            MenuItem searchMenu = menu.findItem(R.id.action_search);

            if (searchMenu != null) {
                searchView = (SearchView) searchMenu.getActionView();

                if (searchView != null) {
                    searchView.setQueryHint(getResources().getString(R.string.action_search));
                    searchView.setOnCloseListener(this);
                    searchView.setOnQueryTextListener(this);
                }
            }
        }
    }


    /**
     * Implementation of AlertDialogs such as
     * - backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog -
     * @param context The Activity context of the dialogs; in this case MainActivity context
     */
    protected void initDialogs(Context context) {
        /*
         * Backup check dialog ->
         *  If not sure, dismiss
         *  if yes, check if notes length > 0
         *    If yes, save current notes to backup file in backupPath
         */
        backupCheckDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_backup)
                .setMessage(R.string.dialog_check_backup_if_sure)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If note array not empty, continue
                        if (notes.length() > 0) {
                            boolean backupSuccessful = DataUtils.saveData(backupPath, notes);

                            if (backupSuccessful)
                                showBackupSuccessfulDialog();

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
                        dialog.dismiss();
                    }
                })
                .create();


        // Dialog to display backup was successfully created on backupPath
        backupOKDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_backup_created_title)
                .setMessage(getResources().getString(R.string.dialog_backup_created) + " "
                        + backupPath.getAbsolutePath())
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        /*
         * Restore check dialog ->
         *  If not sure, dismiss
         *  if yes, check if backup notes exists
         *    If not, display restore failed dialog
         *    If yes, retrieve notes from backup file and store into local file
         */
        restoreCheckDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_restore)
                .setMessage(R.string.dialog_check_restore_if_sure)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONArray tempNotes = DataUtils.retrieveData(backupPath);

                        // If backup file exists, save backup notes to local file path
                        if (tempNotes != null) {
                            boolean restoreSuccessful = DataUtils.saveData(localPath, tempNotes);

                            if (restoreSuccessful) {
                                notes = tempNotes;
                                tempNotesBackup = notes;

                                adapter = new NoteAdapter(MainActivity.this, notes);
                                listView.setAdapter(adapter);

                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_restore_successful),
                                        Toast.LENGTH_SHORT);
                                toast.show();

                                // If no notes, show 'Press + to add new note' text, invisible otherwise
                                if (notes.length() == 0)
                                    noNotes.setVisibility(View.VISIBLE);

                                else
                                    noNotes.setVisibility(View.INVISIBLE);
                            }

                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_restore_unsuccessful),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        }

                        // If backup file doesn't exist, show restore failed dialog
                        else {
                            showRestoreFailedDialog();
                        }
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        // Dialog to display restore failed when no backup file found
        restoreFailedDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_restore_failed_title)
                .setMessage(R.string.dialog_restore_failed)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    // Method to dismiss backup check and show backup successful dialog
    protected void showBackupSuccessfulDialog() {
        backupCheckDialog.dismiss();
        backupOKDialog.show();
    }

    // Method to dismiss restore check and show restore failed dialog
    protected void showRestoreFailedDialog() {
        restoreCheckDialog.dismiss();
        restoreFailedDialog.show();
    }



    /**
     * If newNote button clicked -> Start EditActivity intent with NEW_NOTE_REQUEST as request
     * @param v View clicked, in our case just newNote
     */
    @Override
    public void onClick(View v) {
        // If new note button pressed, start edit note activity with new note request code
        if (v.getId() == R.id.newNote) {
            Intent intent = new Intent(this, EditActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            intent.putExtra(DataUtils.NOTE_REQUEST_CODE, DataUtils.NEW_NOTE_REQUEST);

            startActivityForResult(intent, DataUtils.NEW_NOTE_REQUEST);
        }
    }


    /**
     * If item clicked in list view -> Start EditActivity intent with position as request
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        try {
            // Package current note content and send to EditActivity
            intent.putExtra(DataUtils.NOTE_TITLE, notes.getJSONObject(position).getString(DataUtils.NOTE_TITLE));
            intent.putExtra(DataUtils.NOTE_BODY, notes.getJSONObject(position).getString(DataUtils.NOTE_BODY));
            intent.putExtra(DataUtils.NOTE_COLOUR, notes.getJSONObject(position).getString(DataUtils.NOTE_COLOUR));
            intent.putExtra(DataUtils.NOTE_FONT_SIZE, notes.getJSONObject(position).getInt(DataUtils.NOTE_FONT_SIZE));

            if (notes.getJSONObject(position).has(DataUtils.NOTE_HIDE_BODY)) {
                intent.putExtra(DataUtils.NOTE_HIDE_BODY,
                        notes.getJSONObject(position).getBoolean(DataUtils.NOTE_HIDE_BODY));
            }

            else
                intent.putExtra(DataUtils.NOTE_HIDE_BODY, false);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        if (searchActive) {
            intent.putExtra(DataUtils.NOTE_REQUEST_CODE, realIndexesOfSearchResults.get(position));
            startActivityForResult(intent, realIndexesOfSearchResults.get(position));
        }

        else {
            intent.putExtra(DataUtils.NOTE_REQUEST_CODE, position);
            startActivityForResult(intent, position);
        }
    }


    /**
     * Item clicked in Toolbar menu callback method
     * @param menuItem Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();

        // 'Backup notes' pressed, display backupCheckDialog
        if (id == R.id.action_backup) {
            backupCheckDialog.show();

            return true;
        }

        // 'Restore notes' pressed, display restoreCheckDialog
        if (id == R.id.action_restore) {
            restoreCheckDialog.show();

            return true;
        }

        // 'Rate app' pressed, create new dialog to ask the user if he wants to go to PlayStore
        // If yes, start PlayStore and go to app link < If Exception thrown, open in Browser >
        if (id == R.id.action_rate_app) {
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
                                        Uri.parse("http://play.google.com/store/apps/details?id="
                                                + appPackageName)));
                            }
                        }
                    })
                    .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }


    /**
     * During multi-choice delete selection mode, callback method if items checked changed
     * @param mode ActionMode of selection
     * @param position Position checked
     * @param id ID of item, if exists
     * @param checked true if checked, false otherwise
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // If item checked, add to array
        if (checked)
            checkedArray.add(position);

        // If item unchecked
        else {
            int index = -1;

            // Loop through array and find index of item unchecked
            for (int i = 0; i < checkedArray.size(); i++) {
                if (position == checkedArray.get(i)) {
                    index = i;
                    break;
                }
            }

            // If index was found, remove the item
            if (index != -1)
                checkedArray.remove(index);
        }

        // Set Toolbar title to 'x Selected'
        mode.setTitle(checkedArray.size() + " " +
                getResources().getString(R.string.action_delete_selected_number));

        adapter.notifyDataSetChanged();
    }

    /**
     * Callback method when 'Delete' icon pressed
     * @param mode ActionMode of selection
     * @param item MenuItem clicked, in our case just delete
     * @return true if clicked, false otherwise
     */
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.dialog_delete))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Pass notes and checked items for deletion array to 'deleteNotes'
                            notes = DataUtils.deleteNotes(notes, checkedArray);

                            // Create and set new adapter with new notes array
                            adapter = new NoteAdapter(MainActivity.this, notes);
                            listView.setAdapter(adapter);

                            // Attempt to save notes to localPath file
                            Boolean saveSuccessful = DataUtils.saveData(localPath, notes);

                            // If save successful, toast successfully deleted
                            if (saveSuccessful) {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_deleted), Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            // Smooth scroll to top
                            listView.post(new Runnable() {
                                public void run() {
                                    listView.smoothScrollToPosition(0);
                                }
                            });

                            // If no notes, show 'Press + to add new note' text, invisible otherwise
                            if (notes.length() == 0)
                                noNotes.setVisibility(View.VISIBLE);

                            else
                                noNotes.setVisibility(View.INVISIBLE);

                            // Do notes backup
                            tempNotesBackup = notes;

                            mode.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }

    // Long click detected on list view item, start selection ActionMode
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate 'delete' menu, which has a 'Delete' item
        mode.getMenuInflater().inflate(R.menu.delete, menu);
        return true;
    }

    // Callback method for selection ActionMode exit or finish
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Reset checked items array and notify adapter to refresh listView
        checkedArray = new ArrayList<Integer>();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }


    /**
     * Callback method for 'searchView' widget text change
     * @param s String which changed
     * @return true if text changed and logic finished, false otherwise
     */
    @Override
    public boolean onQueryTextChange(String s) {
        // Initialize search
        searchActive = true;
        notes = tempNotesBackup;

        // Disable long clicks for listView to stop deletion
        listView.setLongClickable(false);

        // Turn string into lowercase
        s = s.toLowerCase();

        // If query text length longer than 0
        if (s.length() > 0) {
            // Create new JSONArray and reset realIndexes array
            JSONArray notesFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main notes list
            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = null;

                // Get note at position i
                try {
                    note = notes.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If note not null and title/body contain query text
                // -> Put in new notes array and add to realIndexes array
                if (note != null) {
                    try {
                        if (note.getString(DataUtils.NOTE_TITLE).toLowerCase().contains(s) ||
                            note.getString(DataUtils.NOTE_BODY).toLowerCase().contains(s)) {

                            notesFound.put(note);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Finally, equal main notes array with filtered notes array
            notes = notesFound;

            // Create and set adapter to refresh listView
            NoteAdapter searchAdapter = new NoteAdapter(MainActivity.this, notesFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();

            for (int i = 0; i < tempNotesBackup.length(); i++) {
                realIndexesOfSearchResults.add(i);
            }

            notes = tempNotesBackup;
            listView.setAdapter(adapter);
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }


    // If 'searchView' widget closed, call 'searchEnded' method
    @Override
    public boolean onClose() {
        searchEnded();

        return false;
    }

    // Make sure 'searchView' is collapsed, disable search conditions, reset adapter and listView clicks
    protected void searchEnded() {
        searchView.onActionViewCollapsed();
        searchActive = false;
        notes = tempNotesBackup;
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
    }



    /**
     * Callback method when EditActivity finished adding new note or editing existing note
     * @param requestCode requestCode for intent sent, in our case either NEW_NOTE_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param data Data bundle passed back from EditActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // If search was active, call 'searchEnded' method
            if (searchActive)
                searchEnded();

            Bundle mBundle = data.getExtras();

            if (mBundle != null) {
                // If new note was saved
                if (requestCode == DataUtils.NEW_NOTE_REQUEST) {
                    JSONObject newNote = null;

                    try {
                        // Add new note to array
                        newNote = new JSONObject();
                        newNote.put(DataUtils.NOTE_TITLE, mBundle.getString(DataUtils.NOTE_TITLE));
                        newNote.put(DataUtils.NOTE_BODY, mBundle.getString(DataUtils.NOTE_BODY));
                        newNote.put(DataUtils.NOTE_COLOUR, mBundle.getString(DataUtils.NOTE_COLOUR));
                        newNote.put(DataUtils.NOTE_FAVOURED, false);
                        newNote.put(DataUtils.NOTE_FONT_SIZE, mBundle.getInt(DataUtils.NOTE_FONT_SIZE));
                        newNote.put(DataUtils.NOTE_HIDE_BODY, mBundle.getBoolean(DataUtils.NOTE_HIDE_BODY));

                        notes.put(newNote);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newNote not null, save notes array to localPath file and notify adapter
                    if (newNote != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = DataUtils.saveData(localPath, notes);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_new_note), Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        // If no notes, show 'Press + to add new note' text, invisible otherwise
                        if (notes.length() == 0)
                            noNotes.setVisibility(View.VISIBLE);

                        else
                            noNotes.setVisibility(View.INVISIBLE);
                    }
                }

                // If existing note was updated (saved)
                else {
                    JSONObject newNote = null;

                    try {
                        // Update array with new note data
                        newNote = notes.getJSONObject(requestCode);
                        newNote.put(DataUtils.NOTE_TITLE, mBundle.getString(DataUtils.NOTE_TITLE));
                        newNote.put(DataUtils.NOTE_BODY, mBundle.getString(DataUtils.NOTE_BODY));
                        newNote.put(DataUtils.NOTE_COLOUR, mBundle.getString(DataUtils.NOTE_COLOUR));
                        newNote.put(DataUtils.NOTE_FONT_SIZE, mBundle.getInt(DataUtils.NOTE_FONT_SIZE));
                        newNote.put(DataUtils.NOTE_HIDE_BODY, mBundle.getBoolean(DataUtils.NOTE_HIDE_BODY));

                        // Update note at position
                        notes.put(requestCode, newNote);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newNote not null, save notes array to localPath file and notify adapter
                    if (newNote != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = DataUtils.saveData(localPath, notes);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_note_saved), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }

                // Backup notes
                tempNotesBackup = notes;
            }
        }


        else if (resultCode == RESULT_CANCELED) {
            Bundle mBundle = null;

            // If data is not null and has "request" extra, get bundle
            if (data != null && data.hasExtra("request"))
                mBundle = data.getExtras();

            if (requestCode == DataUtils.NEW_NOTE_REQUEST) {
                if (mBundle != null) {
                    // If new note discarded, show toast
                    if (mBundle.getString("request").equals("discard")) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_empty_note_discarded), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }



    /**
     *          --- Custom ListView Notes Adapter ---
     */
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
            if (this.adapterData != null)
                return this.adapterData.length();

            else
                return 0;
        }

        @Override
        public JSONObject getItem(int position) {
            if (this.adapterData != null)
                return this.adapterData.optJSONObject(position);

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
            if (convertView == null)
                convertView = this.parentActivity.getLayoutInflater().inflate(R.layout.list_view_note, null);

            // Initialize layout items
            RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
            LayerDrawable roundedCard = (LayerDrawable) getResources().getDrawable(R.drawable.rounded_card);
            TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
            TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
            ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

            // Get Note object at position
            JSONObject noteObject = getItem(position);

            if (noteObject != null) {
                // If noteObject not empty, initialize variable first
                String title = getResources().getString(R.string.note_title);
                String body = getResources().getString(R.string.note_body);
                String colour = String.valueOf(getResources().getColor(R.color.white));
                int fontSize = 18;
                Boolean hideBody = false;
                Boolean favoured = false;

                try {
                    // Get noteObject data and store in variables
                    title = noteObject.getString(DataUtils.NOTE_TITLE);
                    body = noteObject.getString(DataUtils.NOTE_BODY);
                    colour = noteObject.getString(DataUtils.NOTE_COLOUR);

                    if (noteObject.has(DataUtils.NOTE_FONT_SIZE))
                        fontSize = noteObject.getInt(DataUtils.NOTE_FONT_SIZE);

                    if (noteObject.has(DataUtils.NOTE_HIDE_BODY))
                        hideBody = noteObject.getBoolean(DataUtils.NOTE_HIDE_BODY);

                    favoured = noteObject.getBoolean(DataUtils.NOTE_FAVOURED);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (favoured)
                    favourite.setImageResource(R.drawable.ic_fav);

                else
                    favourite.setImageResource(R.drawable.ic_unfav);


                // If search is active, hide favourite (star) button; Otherwise show
                if (searchActive)
                    favourite.setVisibility(View.INVISIBLE);

                else
                    favourite.setVisibility(View.VISIBLE);


                titleView.setText(title);

                // If hidBody is true, set body text to empty
                if (hideBody)
                    bodyView.setText("");

                // Else, set body text to normal and set text size to 'fontSize' int -> SP
                else {
                    bodyView.setText(body);
                    bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                }


                // If current note is selected for deletion, highlight
                if (checkedArray.contains(position)) {
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(getResources().getColor(R.color.theme_primary));
                }

                // If current note is not selected, set background colour to normal
                else {
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Color.parseColor(colour));
                }

                relativeLayout.setBackground(roundedCard);


                final Boolean finalFavoured = favoured;
                favourite.setOnClickListener(new View.OnClickListener() {
                    // If favourite button (star) was clicked
                    @Override
                    public void onClick(View v) {
                        // If note was not favoured
                        if (!finalFavoured) {
                            // Create new object and set favoured to true at position
                            JSONObject newFavourite = null;

                            try {
                                newFavourite = notes.getJSONObject(position);
                                newFavourite.put(DataUtils.NOTE_FAVOURED, true);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (newFavourite != null) {
                                // If favoured note is not at position 0
                                // Sort notes array so favoured note is first
                                if (position > 0) {
                                    JSONArray newArray = new JSONArray();

                                    try {
                                        newArray.put(0, newFavourite);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    // Copy contents to new sorted array without favoured element
                                    for (int i = 0; i < notes.length(); i++) {
                                        if (i != position) {
                                            try {
                                                newArray.put(notes.get(i));

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    // Equal main notes array with new sorted array
                                    notes = newArray;
                                    adapter.adapterData = notes;
                                    adapter.notifyDataSetChanged();

                                    // Smooth scroll to top
                                    listView.post(new Runnable() {
                                        public void run() {
                                            listView.smoothScrollToPosition(0);
                                        }
                                    });
                                }

                                // If favoured note was first, just update object in notes array
                                else {
                                    try {
                                        notes.put(position, newFavourite);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                        // If note was favoured, set favoured to false and store into JSON
                        else {
                            JSONObject newFavourite = null;

                            try {
                                newFavourite = notes.getJSONObject(position);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (newFavourite != null) {
                                try {
                                    newFavourite.put(DataUtils.NOTE_FAVOURED, false);
                                    notes.put(position, newFavourite);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                adapter.notifyDataSetChanged();
                            }
                        }

                        tempNotesBackup = notes;
                        DataUtils.saveData(localPath, notes);
                    }
                });
            }

            return convertView;
        }
    }


    // Static method to return static File at localPath
    public static File getLocalPath() {
        return MainActivity.localPath;
    }

    // Static method to return static File at backupPath
    public static File getBackupPath() {
        return MainActivity.backupPath;
    }
}
