package ga.jundbits.clock_in_clock_out.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import ga.jundbits.clock_in_clock_out.data.entity.Profile;

public class Utils {

    public static boolean isDarkTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static Bitmap generateCode(Activity activity, String code) {
        if (code == null || code.isBlank()) return null;

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            QRGEncoder qrgEncoder = new QRGEncoder(code.trim(), null, QRGContents.Type.TEXT, width);
            qrgEncoder.setColorWhite(isDarkTheme(activity) ? Color.WHITE : Color.BLACK);
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
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) displayName = cursor.getString(index);
                else displayName = uri.getLastPathSegment();
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

    public static String formatDate(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy hh:mm a");
        return formatter.format(new Date(timestamp));
    }

    public static String getCurrentFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(Calendar.getInstance().getTime());
    }

    public static File createFile(Context context, String filename, String data) throws IOException {
        FileOutputStream stream = context.openFileOutput(filename, Context.MODE_PRIVATE);
        stream.write(data.getBytes());
        stream.flush();
        stream.close();
        return new File(context.getFilesDir().getAbsolutePath() + "/" + filename);
    }

}
