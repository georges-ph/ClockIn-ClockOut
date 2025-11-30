package ga.jundbits.clock_in_clock_out.models;

import androidx.room.Embedded;
import androidx.room.Relation;

public class ProfileAttendance {

    @Embedded
    public Attendance attendance;
    @Relation(parentColumn = "employeeId", entityColumn = "id")
    public Profile profile;

}
