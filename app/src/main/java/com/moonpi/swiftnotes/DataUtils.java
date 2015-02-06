package com.moonpi.swiftnotes;

import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


    /*
    *   JSON file structure:
    *
    *   root_OBJ:{
    *       notes_ARR:[
    *           newNote_OBJ:{
    *             "title":"", "body":"", "colour":"", "favoured":true/false,
    *                   "fontSize":14/18/22, "hideBody":true/false},
    *           newNote_OBJ:{
    *             "title":"", "body":"", "colour":"", "favoured":true/false,
    *                   "fontSize":14/18/22, "hideBody":true/false}, etc
    *       ]
    *   };
    */


public class DataUtils {

    public static final String NOTES_FILE_NAME = "notes.json";
    public static final String NOTES_ARRAY_NAME = "notes";

    // Used for backup and restore
    public static final String BACKUP_FOLDER_PATH = "/Swiftnotes";
    public static final String BACKUP_FILE_NAME = "swiftnotes_backup.json";

    // Note data constants used in intents and in key-value store
    public static final int NEW_NOTE_REQUEST = 50000;
    public static final String NOTE_REQUEST_CODE = "requestCode";
    public static final String NOTE_TITLE = "title";
    public static final String NOTE_BODY = "body";
    public static final String NOTE_COLOUR = "colour";
    public static final String NOTE_FAVOURED = "favoured";
    public static final String NOTE_FONT_SIZE = "fontSize";
    public static final String NOTE_HIDE_BODY = "hideBody";


    /**
     * Wrap 'notes' array into a root object and store in file 'toFile'
     * @param toFile File to store notes into
     * @param notes Array of notes to be saved
     * @return true if successfully saved, false otherwise
     */
    public static boolean saveData(File toFile, JSONArray notes) {
        Boolean successful = false;

        JSONObject root = new JSONObject();

        // If passed notes not null, wrap in root JSONObject
        if (notes != null) {
            try {
                root.put(NOTES_ARRAY_NAME, notes);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // If passed notes null, return false
        else
            return false;

        // If file is backupPath and it doesn't exist, create file
        if (toFile == MainActivity.getBackupPath()) {
            if (isExternalStorageReadable() && isExternalStorageWritable()) {
                if (!toFile.exists()) {
                    try {
                        toFile.createNewFile();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // If external storage not readable/writable, return false
            else
                return false;
        }

        // If file is localPath and it doesn't exist, create file
        else if (toFile == MainActivity.getLocalPath()) {
            if (!toFile.exists()) {
                try {
                    toFile.createNewFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        FileWriter writer = null;

        try {
            // Initialize FileWriter and write root object to file
            writer = new FileWriter(toFile);

            writer.write(root.toString());

            // If we got to this stage without throwing an exception, set successful to true
            successful = true;

        } catch (IOException e) {
            // If something went wrong before, set successful to false
            successful = false;
            e.printStackTrace();

        } finally {
            // Finally, if writer not null, flush and close it
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return successful;
    }



    /**
     * Read from file 'fromFile' and return parsed JSONArray of notes
     * @param fromFile File we are reading from
     * @return JSONArray of notes
     */
    public static JSONArray retrieveData(File fromFile) {
        JSONArray notes = null;

        // If file is backupPath and it doesn't exist, return null
        if (fromFile == MainActivity.getBackupPath()) {
            if (isExternalStorageReadable()) {
                if (!fromFile.exists()) {
                    return null;
                }
            }
        }

        /*
         * If file is localPath and it doesn't exist ->
         * Initialize notes JSONArray as new and save into local file
         */
        else if (fromFile == MainActivity.getLocalPath()) {
            if (!fromFile.exists()) {
                notes = new JSONArray();

                Boolean successfulSaveToLocal = saveData(fromFile, notes);

                // If save successful, return new notes
                if (successfulSaveToLocal) {
                    return notes;
                }

                // Else, return null
                return null;
            }
        }


        JSONObject root = null;
        BufferedReader bReader = null;

        try {
            // Initialize FileReader, read from 'fromFile' and store into root object
            FileReader reader = new FileReader(fromFile);

            bReader = new BufferedReader(reader);

            root = new JSONObject(bReader.readLine());

        } catch (IOException | JSONException e) {
            e.printStackTrace();

        } finally {
            // Finally, if reader not null, close buffer
            if (bReader != null) {
                try {
                    bReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // If root is not null, get notes array from root object
        if (root != null) {
            try {
                notes = root.getJSONArray(NOTES_ARRAY_NAME);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Return fetches notes < May return null! >
        return notes;
    }



    /**
     * Create new JSONArray of notes from 'from' without the notes at positions in 'selectedNotes'
     * @param from Main notes array to delete from
     * @param selectedNotes ArrayList of integers which represent note positions to be deleted
     * @return New JSONArray of notes without the notes at positions 'selectedNotes'
     */
    public static JSONArray deleteNotes(JSONArray from, ArrayList<Integer> selectedNotes) {
        // Init new JSONArray
        JSONArray newNotes = new JSONArray();

        // Look through main notes
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position
            if (!selectedNotes.contains(i)) {
                // Put into new array
                try {
                    newNotes.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new notes
        return newNotes;
    }



    /**
     * Check if external storage is writable or not
     * @return true if writable, false otherwise
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Check if external storage is readable or not
     * @return true if readable, false otherwise
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
