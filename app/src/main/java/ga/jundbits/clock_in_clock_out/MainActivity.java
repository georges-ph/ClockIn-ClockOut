package ga.jundbits.clock_in_clock_out;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.karumi.dexter.PermissionToken;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;
    private EditText mainEditText;
    private Button mainStartButton;
    private Button mainScanButton;
    private ImageView mainImageView;
    private int currentUserID;
    private final int SCANNER_CODE = 1100100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();
        setupToolbar();
        setupOthers();
        setOnClicks();

    }

    private void initVars() {

        mainToolbar = findViewById(R.id.main_toolbar);
        mainEditText = findViewById(R.id.main_edit_text);
        mainStartButton = findViewById(R.id.main_start_button);
        mainScanButton = findViewById(R.id.main_scan_button);
        mainImageView = findViewById(R.id.main_image_view);

        currentUserID = Utils.readUserID(this);

    }

    private void setupToolbar() {

        setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));

    }

    private void setupOthers() {

        if (currentUserID == SCANNER_CODE)
            setupForScanner();
        else if (currentUserID != 0)
            setupForUser();

    }

    private void setupForScanner() {

        mainEditText.setVisibility(View.GONE);
        mainStartButton.setVisibility(View.GONE);
        mainScanButton.setVisibility(View.VISIBLE);
        mainImageView.setVisibility(View.GONE);

    }

    private void setupForUser() {

        mainEditText.setVisibility(View.GONE);
        mainStartButton.setVisibility(View.GONE);
        mainScanButton.setVisibility(View.GONE);
        mainImageView.setVisibility(View.VISIBLE);

        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss a");

        String code = currentUserID + "," + formatter.format(date);

        Bitmap bitmap = Utils.generateCode(this, code);
        mainImageView.setImageBitmap(bitmap);

    }

    private void setOnClicks() {

        mainScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Utils.checkCameraPermission(MainActivity.this, new Utils.CameraPermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        scanCode();
                    }

                    @Override
                    public void onPermissionDenied() {
                        Toast.makeText(MainActivity.this, "Camera permission is needed to scan QR Codes", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                });

            }
        });

        mainStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Utils.closeKeyboard(MainActivity.this);

                String editText = mainEditText.getText().toString();

                if (TextUtils.isEmpty(editText))
                    Toast.makeText(MainActivity.this, "Enter an ID", Toast.LENGTH_SHORT).show();
                else {

                    currentUserID = Integer.parseInt(editText);
                    Utils.saveUserID(MainActivity.this, currentUserID);

                    if (currentUserID == SCANNER_CODE)
                        setupForScanner();
                    else if (currentUserID != 0)
                        setupForUser();

                }

            }
        });

    }

    private void scanCode() {

        Intent codeScannerIntent = new Intent(this, CodeScannerActivity.class);
        startActivity(codeScannerIntent);

    }

}