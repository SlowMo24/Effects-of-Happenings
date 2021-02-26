/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.community;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.time.AnalysesInterval;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class Notes {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    Set<Integer> userIds = EventUser.getUserIds();
    Map<Integer, List<EventUser>> users = EventUser.getUsers();
    Map<EventUser, Map<AnalysesInterval, NotesResult>> result = new HashMap<>();
    try (Connection notesConn = DbHandler.getNotes();
        PreparedStatement ps = notesConn.prepareStatement(
            "select user_id,note_id,action,comment,timestamp from note_comments "
            + " where timestamp > timestamp '2014-01-01 00:00:00' and timestamp < timestamp '2018-07-01 00:00:00' and user_id=ANY(?);");) {

      Array createArrayOf = notesConn.createArrayOf("INTEGER", userIds.toArray());
      ps.setArray(1, createArrayOf);
      try (ResultSet executeQuery = ps.executeQuery()) {
        while (executeQuery.next()) {
          String string = executeQuery.getString("action");
          String string1 = executeQuery.getString("comment");
          LocalDateTime timestamp = executeQuery.getTimestamp("timestamp").toLocalDateTime();
          OSHDBTimestamp ts = new OSHDBTimestamp(timestamp.toEpochSecond(ZoneOffset.UTC));
          for (EventUser eu : users.get(executeQuery.getInt("user_id"))) {
            NotesResult orDefault = result
                .computeIfAbsent(eu, (eu2) -> new HashMap<>())
                .computeIfAbsent(AnalysesInterval.getForTS(eu.getEventTime(), ts), ts2 ->
                    new NotesResult());
            orDefault.count += 1;
            orDefault.actionCount.put(actionConverter(string), orDefault.actionCount.getOrDefault(
                string, 0) + 1);
            for (String part : string1
                .toLowerCase()
                .replaceAll("[\\p{P}]", " ")
                .trim()
                .split(" ")) {

              if (!part.isEmpty()) {
                orDefault.wordCount.put(part, orDefault.wordCount.getOrDefault(part, 0) + 1);
              }
            }
          }
        }
      }

    }
    try (Connection conn = DbHandler.getMAConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE aa_MapperHappeningTimeMetric SET NotesCount=?, distinctwordnotes=? WHERE Mapperid=? and Happeningid=? and TimeInterval=?;");
        PreparedStatement ps2 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeMetric (Mapperid,Happeningid,TimeInterval,NotesCount,distinctwordnotes) VALUES(?,?,?,?,?);");
        PreparedStatement ps3 = conn.prepareStatement(
            "INSERT INTO aa_NoteActionTime(Mapperid,Happeningid,TimeInterval,ActionType,NrOfTouches) VALUES(?,?,?,?,?)");) {
      conn.setAutoCommit(false);
      for (Map.Entry<EventUser, Map<AnalysesInterval, NotesResult>> entry : result.entrySet()) {
        EventUser key = entry.getKey();
        for (Map.Entry<AnalysesInterval, NotesResult> res : entry.getValue().entrySet()) {
          ps.setInt(3, key.getId());
          ps.setInt(4, key.getEventId());
          ps.setInt(5, res.getKey().getId());
          ps.setInt(1, res.getValue().count);
          ps.setInt(2, res.getValue().wordCount.size());
          ps.execute();
          if (ps.getUpdateCount() == 0) {
            ps2.setInt(1, key.getId());
            ps2.setInt(2, key.getEventId());
            ps2.setInt(3, res.getKey().getId());
            ps2.setInt(4, res.getValue().count);
            ps2.setInt(5, res.getValue().wordCount.size());
            ps2.execute();
          }
          for (Map.Entry<Integer, Integer> entr : res.getValue().actionCount.entrySet()) {
            ps3.setInt(1, key.getId());
            ps3.setInt(2, key.getEventId());
            ps3.setInt(3, res.getKey().getId());
            ps3.setInt(4, entr.getKey());
            ps3.setInt(5, entr.getValue());
            ps3.execute();
          }
        }
      }
      conn.commit();
    }
  }

  private static int actionConverter(String str) {
    switch (str) {
      case "opened":
        return 1;
      case "closed":
        return 2;
      case "reopened":
        return 3;
      case "commented":
        return 4;
      case "hidden":
        return 5;
    }
    return 0;
  }

}
