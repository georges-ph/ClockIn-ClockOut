package ga.jundbits.clock_in_clock_out.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonSyntaxException;

import ga.jundbits.clock_in_clock_out.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Attendance;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private ConstraintLayout profileLayout;
    private TextView profileName, profileId, profileDepartment, profileClocking;
    private ImageView profileQrCode;
    private Button profileAttendButton;

    private Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.profile_details));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initVars();

        // Load profile from intent
        if (!getIntent().hasExtra("profile")) {
            Toast.makeText(this, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            profile = Profile.fromJson(getIntent().getStringExtra("profile"));
            if (profile == null) return;
            showProfile(profile);
        } catch (JsonSyntaxException e) {
            Toast.makeText(this, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If there are profiles imported, it's a scanner device. Don't show the button
        new Thread(() -> {
            try {
                int count = AppDatabase.getInstance(this).profileDao().count();
                runOnUiThread(() -> {
                    if (count == 0) profileAttendButton.setVisibility(View.INVISIBLE);
                });
            } catch (Exception e) {
                Snackbar.make(profileLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show();
            }
        }).start();

        // Set button onClick to insert a new attendance with scanned profile data along with scanner timestamp
        profileAttendButton.setOnClickListener(this);
    }

    private void initVars() {
        profileLayout = findViewById(R.id.profile_layout);
        profileName = findViewById(R.id.profile_name);
        profileId = findViewById(R.id.profile_id);
        profileDepartment = findViewById(R.id.profile_department);
        profileClocking = findViewById(R.id.profile_clocking);
        profileQrCode = findViewById(R.id.profile_qr_code);
        profileAttendButton = findViewById(R.id.profile_attend_button);
    }

    private void showProfile(Profile profile) {
        profileName.setText(profile.getName());
        profileId.setText(String.valueOf(profile.getId()));
        profileDepartment.setText(profile.getDepartment());
        profileClocking.setText(profile.getClocking().name());
        profileClocking.setTextColor(getColor(profile.getClocking() == Clocking.IN ? R.color.green : R.color.red));
        Bitmap code = Utils.generateCode(this, profile.toJson());
        if (code != null) profileQrCode.setImageBitmap(code);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        // No need to check for button ID as it's the only button here

        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);

                // Check if profile exists in database before inserting attendance
                Profile dbProfile = database.profileDao().getById(profile.getId());
                if (dbProfile == null) {
                    runOnUiThread(() -> Snackbar.make(profileLayout, R.string.profile_not_found, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Prevent duplicate clocking
                Attendance lastAttendance = database.attendanceDao().getLast(profile.getId());
                if (lastAttendance != null && lastAttendance.getClocking() == profile.getClocking()) {
                    runOnUiThread(() -> Snackbar.make(profileLayout, getString(R.string.clocking_twice, profile.getClocking().name()), Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Insert the attendance to the database
                database.attendanceDao().insert(new Attendance(profile.getId(), profile.getClocking(), System.currentTimeMillis()));

                // Show success toast message and close activity
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.attended_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(profileLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

}
