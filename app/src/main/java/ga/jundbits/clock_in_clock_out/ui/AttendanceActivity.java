package ga.jundbits.clock_in_clock_out.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ga.jundbits.clock_in_clock_out.data.database.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.utils.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.data.entity.ProfileAttendance;


public class AttendanceActivity extends AppCompatActivity {

    private ConstraintLayout attendanceLayout;
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
        attendanceLayout = findViewById(R.id.attendance_layout);
        attendanceNoAttendance = findViewById(R.id.attendance_no_attendances);
        attendanceListView = findViewById(R.id.attendance_list_view);
    }

    private void setupAdapter() {
        new Thread(() -> {
            // Get attendance list from database
            List<ProfileAttendance> attendanceList = AppDatabase.getInstance(this).attendanceDao().getAllByTimestamp();

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.attendance_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.attendance_menu_export) {
            exportAttendance();
            return true;
        }
        return false;
    }

    private void exportAttendance() {
        new Thread(() -> {
            try {
                // Load data from database
                List<ProfileAttendance> attendances = AppDatabase.getInstance(this).attendanceDao().getAllById();
                if (attendances.isEmpty()) {
                    runOnUiThread(() -> Snackbar.make(attendanceLayout, R.string.no_attendance, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Build CSV
                StringBuilder builder = new StringBuilder();
                builder.append("attendance ID,employee ID,name,department,clocking,timestamp\n");
                for (ProfileAttendance profileAttendance : attendances) {
                    builder.append(profileAttendance.attendance.getId()).append(",");
                    builder.append(profileAttendance.profile.getId()).append(",");
                    builder.append(profileAttendance.profile.getName()).append(",");
                    builder.append(profileAttendance.profile.getDepartment()).append(",");
                    builder.append(profileAttendance.attendance.getClocking().name()).append(",");
                    builder.append(profileAttendance.attendance.getTimestamp()).append("\n");
                }

                // Create and save the file to storage
                File file = Utils.createFile(this, "attendance_" + Utils.getCurrentFormattedDate() + ".csv", builder.toString());
                runOnUiThread(() -> Snackbar.make(attendanceLayout, getString(R.string.file_saved, file.getAbsolutePath()), Snackbar.LENGTH_SHORT).show());

                // Reference the file URI
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

                // Open share intent
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.setDataAndType(fileUri, getContentResolver().getType(fileUri));
                runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, null)));
            } catch (IOException e) {
                runOnUiThread(() -> Snackbar.make(attendanceLayout, R.string.error_creating_file, Snackbar.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(attendanceLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

}