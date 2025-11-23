package ga.jundbits.clock_in_clock_out.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private FloatingActionButton mainScanFab;
    private Button mainInButton, mainOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initVars();
        setOnClicks();

    }

    private void initVars() {
        mainLayout = findViewById(R.id.main_layout);
        mainScanFab = findViewById(R.id.main_scan_fab);
        mainInButton = findViewById(R.id.main_in_button);
        mainOutButton = findViewById(R.id.main_out_button);
    }

    private void setOnClicks() {

        mainScanFab.setOnClickListener(view -> {

            // Check if camera is available
            if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Snackbar.make(mainLayout, R.string.camera_missing, Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Check for camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Camera permission is granted
                scanQrCode();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show a dialog explaining why the camera permission is needed
                new AlertDialog.Builder(this)
                        .setTitle(R.string.camera_permission_required)
                        .setMessage(R.string.camera_permission_message)
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> requestPermissionLauncher.launch(Manifest.permission.CAMERA))
                        .create().show();
            } else {
                // Ask for camera permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }

        });

        mainInButton.setOnClickListener(view -> {
            try {
                // Load profile from preferences
                Profile profile = Utils.readProfile(this);

                // Set clocking
                profile.setClocking(Clocking.IN);

                // Navigate to ProfileActivity and show the profile
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                profileIntent.putExtra("profile", profile.toJson());
                startActivity(profileIntent);
            } catch (JsonSyntaxException e) {
                Snackbar.make(mainLayout, R.string.invalid_qr_code, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            }
        });

        mainOutButton.setOnClickListener(view -> {
            try {
                // Load profile from preferences
                Profile profile = Utils.readProfile(this);

                // Set clocking
                profile.setClocking(Clocking.OUT);

                // Navigate to ProfileActivity and show the profile
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                profileIntent.putExtra("profile", profile.toJson());
                startActivity(profileIntent);
            } catch (JsonSyntaxException e) {
                Snackbar.make(mainLayout, R.string.invalid_qr_code, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) scanQrCode();
        else
            Snackbar.make(mainLayout, R.string.camera_permission_denied, Snackbar.LENGTH_SHORT).show();
    });

    private void scanQrCode() {
        Intent codeScannerIntent = new Intent(this, CodeScannerActivity.class);
        startActivity(codeScannerIntent);
    }

    private void importProfiles() {
        // Select file from internal storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv"});
        startActivityResultLauncher.launch(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.main_menu_import) {
            importProfiles();
            return true;
        }
        return false;
    }

    private final ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            Snackbar.make(mainLayout, R.string.no_file_selected, Snackbar.LENGTH_SHORT).show();
            return;
        }
        processFile(result.getData().getData());
    });

    private void processFile(Uri fileUri) {
        // Check if file extension is .csv
        if (!Utils.isCSV(this, fileUri)) {
            Snackbar.make(mainLayout, R.string.not_csv_file, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Work in background thread to prevent blocking the UI (main thread)
        new Thread(() -> {
            try {
                // Read the file
                List<String> content = Utils.readFile(MainActivity.this, fileUri);

                // Check if file is empty
                if (content.isEmpty()) {
                    runOnUiThread(() -> Snackbar.make(mainLayout, R.string.file_empty, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Read first line to check if it has CSV content and if columns are the columns needed
                String[] requiredColumns = {"id", "name", "department"};
                String[] columns = content.getFirst().split(",");
                if (columns.length < requiredColumns.length || !Arrays.equals(columns, requiredColumns)) {
                    runOnUiThread(() -> Snackbar.make(mainLayout, R.string.not_csv_file, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Start inserting into database
                insertToDB(content);
            } catch (IOException e) {
                runOnUiThread(() -> Snackbar.make(mainLayout, R.string.error_selecting_file, Snackbar.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void insertToDB(List<String> content) {

    }

}