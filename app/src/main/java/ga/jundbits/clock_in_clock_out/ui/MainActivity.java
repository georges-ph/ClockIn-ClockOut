package ga.jundbits.clock_in_clock_out.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ga.jundbits.clock_in_clock_out.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private ProgressBar mainProgressBar;
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
        mainProgressBar = findViewById(R.id.main_progress_bar);
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
                showProfile(Clocking.IN);
            } catch (JsonSyntaxException e) {
                Snackbar.make(mainLayout, R.string.invalid_qr_code, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            }
        });

        mainOutButton.setOnClickListener(view -> {
            try {
                showProfile(Clocking.OUT);
            } catch (JsonSyntaxException e) {
                Snackbar.make(mainLayout, R.string.invalid_qr_code, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    private void showProfile(Clocking clocking) {
        // Load profile from preferences
        Profile profile = Utils.readProfile(this);

        // Profile not saved and should be QR scanned
        if (profile == null) {
            Snackbar.make(mainLayout, R.string.profile_not_found, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Set clocking
        profile.setClocking(clocking);

        // Navigate to ProfileActivity and show the profile
        Intent profileIntent = new Intent(this, ProfileActivity.class);
        profileIntent.putExtra("profile", profile.toJson());
        startActivity(profileIntent);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) scanQrCode();
        else
            Snackbar.make(mainLayout, R.string.camera_permission_denied, Snackbar.LENGTH_SHORT).setAction(R.string.settings, view -> {
                Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
            }).show();
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
        } else if (item.getItemId() == R.id.main_menu_profiles) {
            startActivity(new Intent(this, ProfilesActivity.class));
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

                // Check if the file is completely empty or if there is only the headers row
                if (content.isEmpty() || content.size() == 1) {
                    runOnUiThread(() -> Snackbar.make(mainLayout, R.string.file_empty, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Read first line to check if it has CSV content and if columns are the columns needed
                String[] requiredColumns = {"id", "name", "department"};
                String[] columns = content.get(0).split(",");
                if (columns.length != requiredColumns.length || !Arrays.equals(columns, requiredColumns)) {
                    runOnUiThread(() -> Snackbar.make(mainLayout, R.string.not_csv_file, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Remove headers row
                content.remove(0);
                // Start inserting into database
                insertToDB(content);
            } catch (IOException e) {
                runOnUiThread(() -> Snackbar.make(mainLayout, R.string.error_selecting_file, Snackbar.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void insertToDB(List<String> lines) {
        // Show the progress bar
        runOnUiThread(() -> {
            // Setup progress bar
            mainProgressBar.setVisibility(View.VISIBLE);
            mainProgressBar.setMax(lines.size());
            mainProgressBar.setProgress(0);
            // Disable buttons to prevent messing with the import
            mainInButton.setEnabled(false);
            mainOutButton.setEnabled(false);
            mainScanFab.setEnabled(false);
        });

        try {
            List<Profile> profiles = new ArrayList<>();

            // Iterate through each line in the file
            for (String line : lines) {
                // Skip empty lines
                if (line.isBlank()) {
                    // Add 2 to the index instead of 1. List is zero based and the headers line is removed
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.invalid_profile_with_line, lines.indexOf(line) + 2), Toast.LENGTH_SHORT).show());
                    continue;
                }

                // Split line to get columns
                String[] parts = line.split(",");

                // Skip lines that don't have 3 columns and skip any entry that has an empty column
                if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                    // Add 2 to the index instead of 1. List is zero based and the headers line is removed
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.invalid_profile_with_line, lines.indexOf(line) + 2), Toast.LENGTH_SHORT).show());
                    continue;
                }

                // Map to profile object and add to list
                Profile profile = new Profile(Integer.parseInt(parts[0].trim()), parts[1].trim(), parts[2].trim());
                profiles.add(profile);

                // Update progress bar to current profiles list size
                runOnUiThread(() -> mainProgressBar.setProgress(profiles.size()));
            }

            AppDatabase.getInstance(this).runInTransaction(() -> {
                // Empty profile table
                AppDatabase.getInstance(getApplicationContext()).profileDao().deleteAll();
                // Insert list into database
                AppDatabase.getInstance(MainActivity.this).profileDao().insertAll(profiles);
            });

            // Show success message
            runOnUiThread(() -> Snackbar.make(mainLayout, getString(R.string.import_message, profiles.size(), lines.size()), Snackbar.LENGTH_SHORT).show());
        } catch (NumberFormatException e) {
            runOnUiThread(() -> Snackbar.make(mainLayout, R.string.error_parsing_numbers, Snackbar.LENGTH_SHORT).show());
        } catch (Exception e) {
            runOnUiThread(() -> Snackbar.make(mainLayout, R.string.error_importing_profiles, Snackbar.LENGTH_SHORT).show());
        } finally {
            runOnUiThread(() -> {
                // Hide progress bar
                mainProgressBar.setVisibility(View.GONE);
                // Reset buttons state
                mainInButton.setEnabled(true);
                mainOutButton.setEnabled(true);
                mainScanFab.setEnabled(true);
            });
        }
    }

}