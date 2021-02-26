package ParameterCalculation.helper.mapper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import ParameterCalculation.helper.dbhandler.DbHandler;

public class EventUser implements Comparable<EventUser>,Serializable {

  private final LocalDateTime[] eventTime;
  private final Integer id;
  private final Integer eventId;

  public EventUser(Integer id, Integer eventId, LocalDateTime[] eventTime) {
    this.id = id;
    this.eventId = eventId;
    this.eventTime = eventTime;
  }

  public static Map<Integer, List<EventUser>> getUsers() throws SQLException, ClassNotFoundException {
    Map<Integer, List<EventUser>> users = new HashMap<>();
    try (Connection maSqlite = DbHandler.getMAConnection()) {
      String mappersSelect
          = "select mapperid, happeningid,  startdate_utc|| 'T' ||starttime_utc as start, enddate_utc|| 'T' ||endtime_utc as end "
          + "    	from aa_happeningmapperjoin;";
      try (Statement stmt = maSqlite.createStatement();
          ResultSet rs = stmt.executeQuery(mappersSelect)) {
        while (rs.next()) {
          LocalDateTime start = LocalDateTime.parse(rs.getString("start"));
          LocalDateTime end = LocalDateTime.parse(rs.getString("end"));
          EventUser eventUser = new EventUser(rs.getInt("MapperId"), rs.getInt("HappeningId"),
              new LocalDateTime[]{start, end});
          users
              .computeIfAbsent(eventUser.getId(), k -> new ArrayList<>())
              .add(eventUser);
        }
      }

    }
    return users;

  }

  public static Set<Integer> getUserIds() throws SQLException, ClassNotFoundException {
    return EventUser.getUsers().keySet();
  }

  @Override
  public int compareTo(EventUser o) {
    int idComp = this.getId() - o.getId();
    if (idComp != 0) {
      return idComp;
    } else {
      return this.eventId - o.eventId;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EventUser other = (EventUser) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    } else if (!this.eventId.equals(other.eventId)) {
      return false;
    }
    return true;
  }

  public Integer getEventId() {
    return eventId;
  }

  public LocalDateTime[] getEventTime() {
    return eventTime;
  }

  public Integer getId() {
    return id;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 79 * hash + Objects.hashCode(this.eventId);
    hash = 79 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public String toString() {
    return "EventUser{" + "eventTimestamp=" + Arrays.toString(eventTime) + ", id=" + id + "}";
  }

}
