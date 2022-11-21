package ga.jundbits.clock_in_clock_out;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private EditText mainEditText;
    private Button mainStartButton;
    private ImageView mainImageView;
    private int currentUserID;
    private final int SCANNER_CODE = 100100;
    private boolean shouldShowInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();
        if (currentUserID != 0)
            showCode();
        setOnClicks();

    }

    private void initVars() {

        mainEditText = findViewById(R.id.main_edit_text);
        mainStartButton = findViewById(R.id.main_start_button);
        mainImageView = findViewById(R.id.main_image_view);

        currentUserID = Utils.readUserID(this);

    }

    private void showCode() {

        mainEditText.setVisibility(View.GONE);
        mainStartButton.setVisibility(View.GONE);
        mainImageView.setVisibility(View.VISIBLE);

        generateCode();

        // Change the QR code every minute
        Handler handler = new Handler();
        int delay = 1000 * 60;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                generateCode();
                handler.postDelayed(this, delay);
            }
        }, delay);

    }

    private void generateCode() {

        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss a");

        String code = currentUserID + "," + formatter.format(date);

        Bitmap bitmap = Utils.generateCode(MainActivity.this, code);
        mainImageView.setImageBitmap(bitmap);

    }

    private void showInput() {

        mainEditText.setVisibility(View.VISIBLE);
        mainStartButton.setVisibility(View.VISIBLE);
        mainImageView.setVisibility(View.GONE);

    }

    private void setOnClicks() {

        mainStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Utils.closeKeyboard(MainActivity.this);

                String editText = mainEditText.getText().toString();

                if (TextUtils.isEmpty(editText))
                    Toast.makeText(MainActivity.this, "Enter an ID", Toast.LENGTH_SHORT).show();
                else {

                    currentUserID = Integer.parseInt(editText);

                    if (currentUserID == SCANNER_CODE) {

                        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            scanCode();
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                        }

                    } else if (currentUserID != 0) {

                        Utils.saveUserID(MainActivity.this, currentUserID);
                        showCode();

                    }

                }

            }
        });

        Handler handler = new Handler();
        Runnable runnable = () -> shouldShowInput = false;

        mainImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                shouldShowInput = true;
                handler.postDelayed(runnable, 1000);
                return true;

            }
        });

        mainImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (shouldShowInput) {
                    showInput();
                    shouldShowInput = false;
                    handler.removeCallbacks(runnable);
                }

            }
        });

    }

    private void scanCode() {

        Intent codeScannerIntent = new Intent(this, CodeScannerActivity.class);
        startActivity(codeScannerIntent);

    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {

        boolean canScanCode = true;

        for (boolean isGranted : result.values()) {
            if (!isGranted) {
                canScanCode = false;
                break;
            }
        }

        if (canScanCode)
            scanCode();
        else
            Toast.makeText(getApplicationContext(), "Permission denied. Cannot scan QR codes without camera and storage permissions", Toast.LENGTH_LONG).show();

    });

}