package ga.jundbits.clock_in_clock_out.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import ga.jundbits.clock_in_clock_out.enums.Clocking;

@Entity
public class Attendance {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int employeeId;
    private Clocking clocking;
    private long timestamp;

    public Attendance() {

    }

    public Attendance(int employeeId, Clocking clocking, long timestamp) {
        this.employeeId = employeeId;
        this.clocking = clocking;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Clocking getClocking() {
        return clocking;
    }

    public void setClocking(Clocking clocking) {
        this.clocking = clocking;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", employeeId=" + employeeId +
                ", clocking=" + clocking +
                ", timestamp=" + timestamp +
                '}';
    }
}
