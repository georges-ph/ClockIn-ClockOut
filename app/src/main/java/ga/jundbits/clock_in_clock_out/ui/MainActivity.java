package ga.jundbits.clock_in_clock_out.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.data.entity.Profile;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.utils.Utils;

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
        try {
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
        } catch (Exception e) {
            Snackbar.make(mainLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.main_menu_profiles) {
            startActivity(new Intent(this, ProfilesActivity.class));
            return true;
        } else if (item.getItemId() == R.id.main_menu_attendance) {
            startActivity(new Intent(this, AttendanceActivity.class));
            return true;
        }
        return false;
    }

}