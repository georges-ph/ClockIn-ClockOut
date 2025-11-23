package ga.jundbits.clock_in_clock_out.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonSyntaxException;

import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class CodeScannerActivity extends AppCompatActivity {

    private FrameLayout codeScannerLayout;
    private CodeScanner codeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_code_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.code_scanner_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.scan_qr_code));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        codeScannerLayout = findViewById(R.id.code_scanner_layout);

        // Setup CodeScanner
        CodeScannerView codeScannerView = findViewById(R.id.code_scanner_view);
        codeScanner = new CodeScanner(this, codeScannerView);

        // Handle errors related to the camera from the library
        codeScanner.setErrorCallback(thrown -> runOnUiThread(() -> {
            Snackbar.make(codeScannerLayout, thrown.getMessage() != null ? thrown.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            finish();
        }));

        // Handle and validate the scanned QR code before showing profile
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            try {
                // Check if QR code is a valid JSON
                Profile profile = Profile.fromJson(result.getText().trim());

                // Check if decoded JSON has the Profile class structure
                if (profile.isNull()) throw new Exception(getString(R.string.invalid_profile));

                // TODO: 23-Nov-25 Check if device has imported data; if so, don't save profile 
                // Save profile to preferences only if not a scanner device
                Utils.saveProfile(this, profile);

                // Navigate to ProfileActivity and show the profile
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                profileIntent.putExtra("profile", result.getText().trim());
                startActivity(profileIntent);
                finish();
            } catch (JsonSyntaxException e) {
                Snackbar.make(codeScannerLayout, R.string.invalid_qr_code, Snackbar.LENGTH_SHORT).show();
                codeScanner.startPreview();
            } catch (Exception e) {
                Snackbar.make(codeScannerLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
                codeScanner.startPreview();
            }
        }));

    }

    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
}