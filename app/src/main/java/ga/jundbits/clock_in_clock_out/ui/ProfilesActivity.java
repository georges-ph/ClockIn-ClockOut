package ga.jundbits.clock_in_clock_out.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import ga.jundbits.clock_in_clock_out.AppDatabase;
import ga.jundbits.clock_in_clock_out.R;
import ga.jundbits.clock_in_clock_out.enums.Clocking;
import ga.jundbits.clock_in_clock_out.models.Profile;

public class ProfilesActivity extends AppCompatActivity {

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
        }
        return false;
    }

}