package ga.jundbits.clock_in_clock_out.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileId, profileDepartment, profileClocking;
    private ImageView profileQrCode;

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
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Profile profile = Profile.fromJson(getIntent().getStringExtra("profile"));
        if (profile != null) showProfile(profile);
    }

    private void initVars() {
        profileName = findViewById(R.id.profile_name);
        profileId = findViewById(R.id.profile_id);
        profileDepartment = findViewById(R.id.profile_department);
        profileClocking = findViewById(R.id.profile_clocking);
        profileQrCode = findViewById(R.id.profile_qr_code);
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

}
