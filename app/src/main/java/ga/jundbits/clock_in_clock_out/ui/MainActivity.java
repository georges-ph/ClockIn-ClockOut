package ga.jundbits.clock_in_clock_out.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonSyntaxException;

import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class MainActivity extends AppCompatActivity {

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
        mainScanFab = findViewById(R.id.main_scan_fab);
        mainInButton = findViewById(R.id.main_in_button);
        mainOutButton = findViewById(R.id.main_out_button);
    }

    private void setOnClicks() {

        mainScanFab.setOnClickListener(view -> {

            // Check if camera is available
            if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(this, R.string.camera_missing, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) scanQrCode();
        else Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
    });

    private void scanQrCode() {
        Intent codeScannerIntent = new Intent(this, CodeScannerActivity.class);
        startActivity(codeScannerIntent);
    }

}