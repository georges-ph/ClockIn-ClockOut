package ga.jundbits.clock_in_clock_out.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import ga.jundbits.clock_in_clock_out.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.ProfileAttendance;


public class AttendanceActivity extends AppCompatActivity {

    private TextView attendanceNoAttendance;
    private ListView attendanceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendance);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.attendance_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.attendance);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initVars();
        setupAdapter();
    }

    private void initVars() {
        attendanceNoAttendance = findViewById(R.id.attendance_no_attendances);
        attendanceListView = findViewById(R.id.attendance_list_view);
    }

    private void setupAdapter() {
        new Thread(() -> {
            // Get attendance list from database
            List<ProfileAttendance> attendanceList = AppDatabase.getInstance(this).attendanceDao().getAll();

            runOnUiThread(() -> {
                // Show no attendance text if list is empty
                if (attendanceList.isEmpty()) attendanceNoAttendance.setVisibility(View.VISIBLE);

                // Setup adapter with simple_list_item_2: First line for profile ID, Name and Department, Second line for clocking and timestamp
                BaseAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, attendanceList) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = LayoutInflater.from(AttendanceActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);

                        ProfileAttendance profileAttendance = attendanceList.get(position);

                        TextView text1 = view.findViewById(android.R.id.text1);
                        TextView text2 = view.findViewById(android.R.id.text2);
                        text1.setText(profileAttendance.profile.getId() + " - " + profileAttendance.profile.getName() + " - " + profileAttendance.profile.getDepartment());
                        text2.setText(profileAttendance.attendance.getClocking().name() + " - " + Utils.formatDate(profileAttendance.attendance.getTimestamp()));

                        int color = profileAttendance.attendance.getClocking() == Clocking.IN ? R.color.green : R.color.red;
                        view.setBackgroundColor(ColorUtils.setAlphaComponent(getColor(color), 64));

                        return view;
                    }
                };
                attendanceListView.setAdapter(adapter);
            });
        }).start();
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