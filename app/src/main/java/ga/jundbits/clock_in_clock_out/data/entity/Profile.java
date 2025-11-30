package ga.jundbits.clock_in_clock_out.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;

import ga.jundbits.clock_in_clock_out.enums.Clocking;

@Entity
public class Profile {

    private static final Gson gson = new Gson();

    @PrimaryKey
    private int id;
    private String name;
    private String department;
    @Ignore
    private Clocking clocking;

    public Profile() {

    }

    public Profile(int id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Clocking getClocking() {
        return clocking;
    }

    public void setClocking(Clocking clocking) {
        this.clocking = clocking;
    }

    public boolean isNull() {
        return name == null || id == 0 || department == null || clocking == null;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Profile fromJson(String json) {
        return gson.fromJson(json, Profile.class);
    }

    @NonNull
    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", clocking=" + clocking +
                '}';
    }
}
