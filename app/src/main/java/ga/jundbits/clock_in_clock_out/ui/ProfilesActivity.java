package ga.jundbits.clock_in_clock_out.ui;

import android.app.Activity;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ga.jundbits.clock_in_clock_out.data.database.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.utils.Utils;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.data.entity.Profile;

public class ProfilesActivity extends AppCompatActivity {

    private ConstraintLayout profilesLayout;
    private ProgressBar profilesProgressBar;
    private TextView profilesNoProfiles;
    private ListView profilesListView;

    private List<Profile> profiles = new ArrayList<>();
    private final List<Profile> filteredProfiles = new ArrayList<>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profiles);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profiles_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.profiles));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initVars();
        setupAdapter();
    }

    private void initVars() {
        profilesLayout = findViewById(R.id.profiles_layout);
        profilesProgressBar = findViewById(R.id.profiles_progress_bar);
        profilesNoProfiles = findViewById(R.id.profiles_no_profiles);
        profilesListView = findViewById(R.id.profiles_list_view);
    }

    private void setupAdapter() {
        new Thread(() -> {
            // Get all profiles from database and store them in variable for later filtering
            profiles = AppDatabase.getInstance(this).profileDao().getAll();
            filteredProfiles.addAll(profiles);

            runOnUiThread(() -> {
                // Show no profiles text if list is empty
                if (filteredProfiles.isEmpty()) profilesNoProfiles.setVisibility(View.VISIBLE);

                // Setup adapter with simple_list_item_2: First line for profile ID and Name, Second line for profile department
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, filteredProfiles) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = LayoutInflater.from(ProfilesActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
                        Profile profile = filteredProfiles.get(position);
                        TextView text1 = view.findViewById(android.R.id.text1);
                        TextView text2 = view.findViewById(android.R.id.text2);
                        text1.setText(profile.getId() + " - " + profile.getName());
                        text2.setText(profile.getDepartment());
                        return view;
                    }
                };
                profilesListView.setAdapter(adapter);

                // Open profile activity when clicking a list item
                profilesListView.setOnItemClickListener((adapterView, view, i, l) -> {
                    Profile profile = filteredProfiles.get(i);
                    profile.setClocking(Clocking.IN);
                    Intent profileIntent = new Intent(ProfilesActivity.this, ProfileActivity.class);
                    profileIntent.putExtra("profile", profile.toJson());
                    startActivity(profileIntent);
                    finish();
                });
            });

        }).start();
    }

    private void filterList(String text) {
        // Clear the filtered list for fresh results
        filteredProfiles.clear();

        // Iterate through the original list to search for profile ID, name or department and add to filtered list
        for (Profile profile : profiles) {
            if (String.valueOf(profile.getId()).contains(text) ||
                    profile.getName().toLowerCase().contains(text) ||
                    profile.getDepartment().toLowerCase().contains(text)) {
                filteredProfiles.add(profile);
            }
        }

        // Notify the adapter that the data has changed
        adapter.notifyDataSetChanged();

        // Show no profiles text when filtered list is empty and hide it otherwise
        if (filteredProfiles.isEmpty()) profilesNoProfiles.setVisibility(View.VISIBLE);
        else profilesNoProfiles.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profiles_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.profiles_menu_search).getActionView();
        if (searchView == null) return false;

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String text) {
                filterList(text.toLowerCase());
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String text) {
                filterList(text.toLowerCase());
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.profiles_menu_import) {
            importProfiles();
            return true;
        }
        return false;
    }

    private void importProfiles() {
        // Select file from internal storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv"});
        startActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            Snackbar.make(profilesLayout, R.string.no_file_selected, Snackbar.LENGTH_SHORT).show();
            return;
        }
        processFile(result.getData().getData());
    });

    private void processFile(Uri fileUri) {
        // Check if file extension is .csv
        if (!Utils.isCSV(this, fileUri)) {
            Snackbar.make(profilesLayout, R.string.not_csv_file, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Work in background thread to prevent blocking the UI (main thread)
        new Thread(() -> {
            try {
                // Read the file
                List<String> content = Utils.readFile(ProfilesActivity.this, fileUri);

                // Check if the file is completely empty or if there is only the headers row
                if (content.isEmpty() || content.size() == 1) {
                    runOnUiThread(() -> Snackbar.make(profilesLayout, R.string.file_empty, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Read first line to check if it has CSV content and if columns are the columns needed
                String[] requiredColumns = {"id", "name", "department"};
                String[] columns = content.get(0).split(",");
                if (columns.length != requiredColumns.length || !Arrays.equals(columns, requiredColumns)) {
                    runOnUiThread(() -> Snackbar.make(profilesLayout, R.string.not_csv_file, Snackbar.LENGTH_SHORT).show());
                    return;
                }

                // Remove headers row
                content.remove(0);
                // Start inserting into database
                insertToDB(content);
            } catch (IOException e) {
                runOnUiThread(() -> Snackbar.make(profilesLayout, R.string.error_selecting_file, Snackbar.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(profilesLayout, e.getMessage() != null ? e.getMessage() : getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void insertToDB(List<String> lines) {
        // Show the progress bar
        runOnUiThread(() -> {
            // Setup progress bar
            profilesProgressBar.setVisibility(View.VISIBLE);
            profilesProgressBar.setMax(lines.size());
            profilesProgressBar.setProgress(0);
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
                runOnUiThread(() -> profilesProgressBar.setProgress(profiles.size()));
            }

            AppDatabase.getInstance(this).runInTransaction(() -> {
                // Empty profile table
                AppDatabase.getInstance(getApplicationContext()).profileDao().deleteAll();
                // Insert list into database
                AppDatabase.getInstance(ProfilesActivity.this).profileDao().insertAll(profiles);
            });

            runOnUiThread(() -> {
                // Add profiles to list
                filteredProfiles.clear();
                filteredProfiles.addAll(profiles);
                adapter.notifyDataSetChanged();
                // Show success message
                Snackbar.make(profilesLayout, getString(R.string.import_message, profiles.size(), lines.size()), Snackbar.LENGTH_SHORT).show();
            });
        } catch (NumberFormatException e) {
            runOnUiThread(() -> Snackbar.make(profilesLayout, R.string.error_parsing_numbers, Snackbar.LENGTH_SHORT).show());
        } catch (Exception e) {
            runOnUiThread(() -> Snackbar.make(profilesLayout, R.string.error_importing_profiles, Snackbar.LENGTH_SHORT).show());
        } finally {
            // Hide progress bar
            runOnUiThread(() -> profilesProgressBar.setVisibility(View.GONE));
        }
    }

}