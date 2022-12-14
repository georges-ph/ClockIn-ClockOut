package ga.jundbits.clock_in_clock_out;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class Utils {

    public static Bitmap generateCode(Activity activity, String code) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        QRGEncoder qrgEncoder = new QRGEncoder(code, null, QRGContents.Type.TEXT, width);
        qrgEncoder.setColorWhite(Color.BLACK);
        qrgEncoder.setColorBlack(Color.WHITE);
        return qrgEncoder.getBitmap();

    }

    public static void saveUserID(Context context, int id) {

        SharedPreferences preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("user_id", id);
        editor.apply();

    }

    public static int readUserID(Context context) {

        SharedPreferences preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        return preferences.getInt("user_id", 0);

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
