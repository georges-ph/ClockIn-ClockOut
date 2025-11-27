package ga.jundbits.clock_in_clock_out.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import ga.jundbits.clock_in_clock_out.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Attendance;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class ProfileActivity extends AppCompatActivity {

    private ConstraintLayout profileLayout;
    private TextView profileName, profileId, profileDepartment, profileClocking;
    private ImageView profileQrCode;
    private Button profileAttendButton;

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
            Snackbar.make(profileLayout, R.string.error_loading_profile, Snackbar.LENGTH_SHORT).show();
            finish();
            return;
        }
        Profile profile = Profile.fromJson(getIntent().getStringExtra("profile"));
        if (profile == null) return;
        showProfile(profile);

        // If there are profiles imported, it's a scanner device. Don't show the button
        new Thread(() -> {
            int count = AppDatabase.getInstance(this).profileDao().count();
            runOnUiThread(() -> {
                if (count == 0) profileAttendButton.setVisibility(View.INVISIBLE);
            });
        }).start();

        // Set button onClick to insert a new attendance with scanned profile data along with scanner timestamp
        profileAttendButton.setOnClickListener(view -> {
            Attendance attendance = new Attendance(profile.getId(), profile.getClocking(), System.currentTimeMillis());
            new Thread(() -> {
                AppDatabase.getInstance(this).attendanceDao().insert(attendance);
                runOnUiThread(this::finish);
            }).start();
        });
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

}
