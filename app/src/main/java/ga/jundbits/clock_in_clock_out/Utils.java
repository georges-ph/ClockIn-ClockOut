package ga.jundbits.clock_in_clock_out;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class Utils {

    public static Bitmap generateCode(Activity activity, String code) {
        if (code == null || code.isBlank()) return null;

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            QRGEncoder qrgEncoder = new QRGEncoder(code.trim(), null, QRGContents.Type.TEXT, width);
            qrgEncoder.setColorWhite(Color.BLACK);
            qrgEncoder.setColorBlack(Color.TRANSPARENT);
            return qrgEncoder.getBitmap();
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveProfile(Context context, Profile profile) {
        SharedPreferences preferences = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("profile", profile.toJson());
        editor.apply();
    }

    public static Profile readProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE);
        return Profile.fromJson(preferences.getString("profile", null));
    }

    public static boolean isCSV(Context context, Uri uri) {
        String displayName = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return displayName != null && displayName.toLowerCase().endsWith(".csv");
    }

    public static List<String> readFile(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            return lines;
        }
    }

    public static void writeToFile(Context context, String data) {

        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE_MMMM_dd_yyyy");

        File appStorageDirectory = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
        File usersFile = new File(appStorageDirectory, "users_" + formatter.format(date) + ".csv");

        if (!appStorageDirectory.exists())
            appStorageDirectory.mkdirs();

        try {

            FileWriter writer = new FileWriter(usersFile, true);
            writer.write(data + "\n");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void closeKeyboard(Activity activity) {

        View closeKeyboardView = activity.getCurrentFocus();
        if (closeKeyboardView != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(closeKeyboardView.getWindowToken(), 0);
        }

    }

}
