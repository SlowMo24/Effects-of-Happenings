/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.community;

import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.time.AnalysesInterval;
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
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class ChangesetComments {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    Set<Integer> userIds = EventUser.getUserIds();
    Map<Integer, List<EventUser>> users = EventUser.getUsers();
    Map<EventUser, Map<AnalysesInterval, ChangesetsResult>> result = new HashMap<>();
    try (Connection changesetConn = DbHandler.getChangesets();
        PreparedStatement csComments = changesetConn.prepareStatement(
            "select * from osm_changeset_comment "
            + " where comment_date > timestamp '2014-01-01 00:00:00' and comment_date < timestamp '2018-07-01 00:00:00' and comment_user_id=ANY(?);");
        PreparedStatement cs = changesetConn.prepareStatement(
            "select user_id,tags->'comment' as c,tags->'review_requested' as r,created_at from osm_changeset "
            + " where created_at > timestamp '2014-01-01 00:00:00' and created_at < timestamp '2018-07-01 00:00:00' and user_id=ANY(?);");
        PreparedStatement getCS = changesetConn.prepareStatement(
            "SELECT * from osm_changeset where id=? and tags->'review_requested' is not null;");) {
      Array createArrayOf = changesetConn.createArrayOf("INTEGER", userIds.toArray());
      csComments.setArray(1, createArrayOf);
      cs.setArray(1, createArrayOf);
      try (ResultSet executeQuery = csComments.executeQuery()) {
        while (executeQuery.next()) {
          String string = executeQuery.getString("comment_text");
          LocalDateTime timestamp = executeQuery.getTimestamp("comment_date").toLocalDateTime();
          OSHDBTimestamp ts = new OSHDBTimestamp(timestamp.toEpochSecond(ZoneOffset.UTC));
          getCS.setInt(1, executeQuery.getInt("comment_changeset_id"));
          boolean a = false;
          try (ResultSet executeQuery2 = getCS.executeQuery();) {
            a = executeQuery2.next();
          }
          for (EventUser eu : users.get((int) executeQuery.getLong("comment_user_id"))) {
            ChangesetsResult orDefault = result
                .computeIfAbsent(eu, (eu2) -> new HashMap<>())
                .computeIfAbsent(AnalysesInterval.getForTS(eu.getEventTime(), ts), ts2 ->
                    new ChangesetsResult());
            orDefault.countComments += 1;
            for (String part : string
                .toLowerCase()
                .replaceAll("[\\p{P}]", " ")
                .trim()
                .split(" ")) {
              if (!part.isEmpty()) {
                orDefault.wordCountComments.put(part, orDefault.wordCountComments.getOrDefault(part,
                    0) + 1);
              }
            }
            if (a) {
              orDefault.reviewRequestedAnswered += 1;
            }
          }
        }

      }
      try (ResultSet executeQuery = cs.executeQuery()) {
        while (executeQuery.next()) {
          String comment = executeQuery.getString("c");
          String review = executeQuery.getString("r");
          LocalDateTime timestamp = executeQuery.getTimestamp("created_at").toLocalDateTime();
          OSHDBTimestamp ts = new OSHDBTimestamp(timestamp.toEpochSecond(ZoneOffset.UTC));
          for (EventUser eu : users.get((int) executeQuery.getLong("user_id"))) {
            ChangesetsResult orDefault = result
                .computeIfAbsent(eu, (eu2) -> new HashMap<>())
                .computeIfAbsent(AnalysesInterval.getForTS(eu.getEventTime(), ts), ts2 ->
                    new ChangesetsResult());
            orDefault.countChangeset += 1;
            if (review != null) {
              orDefault.reviewRequestedIssued += 1;
            }
            if (comment != null) {
              for (String part : comment
                  .toLowerCase()
                  .replaceAll("[\\p{P}]", " ")
                  .trim()
                  .split(" ")) {
                if (!part.isEmpty()) {
                  orDefault.wordCountChangesetComment.put(part, orDefault.wordCountChangesetComment
                      .getOrDefault(part,
                          0) + 1);
                }
              }
            }
          }
        }

      }

    }
    try (Connection conn = DbHandler.getMAConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE aa_MapperHappeningTimeMetric SET ChangesetComments=?, Changesets=?,reviewRequestedAnswered=?,reviewRequestedIssued=?,commentDistinctWords=?,ChangesetDistinctWords=? WHERE Mapperid=? and Happeningid=? and TimeInterval=?;");
        PreparedStatement ps2 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeMetric (Mapperid,Happeningid,TimeInterval,ChangesetComments, Changesets,reviewRequestedAnswered,reviewRequestedIssued,commentDistinctWords,ChangesetDistinctWords) VALUES(?,?,?,?,?,?,?,?,?);");) {
      conn.setAutoCommit(false);
      for (Map.Entry<EventUser, Map<AnalysesInterval, ChangesetsResult>> entry : result.entrySet()) {
        EventUser key = entry.getKey();
        for (Map.Entry<AnalysesInterval, ChangesetsResult> res : entry.getValue().entrySet()) {
          ps.setInt(7, key.getId());
          ps.setInt(8, key.getEventId());
          ps.setInt(9, res.getKey().getId());
          ps.setInt(1, res.getValue().countComments);
          ps.setInt(2, res.getValue().countChangeset);
          ps.setInt(3, res.getValue().reviewRequestedAnswered);
          ps.setInt(4, res.getValue().reviewRequestedIssued);
          ps.setInt(5, res.getValue().wordCountComments.size());
          ps.setInt(6, res.getValue().wordCountChangesetComment.size());
          ps.execute();
          if (ps.getUpdateCount() == 0) {
            ps2.setInt(1, key.getId());
            ps2.setInt(2, key.getEventId());
            ps2.setInt(3, res.getKey().getId());
            ps2.setInt(4, res.getValue().countComments);
            ps2.setInt(5, res.getValue().countChangeset);
            ps2.setInt(6, res.getValue().reviewRequestedAnswered);
            ps2.setInt(7, res.getValue().reviewRequestedIssued);
            ps2.setInt(8, res.getValue().wordCountComments.size());
            ps2.setInt(9, res.getValue().wordCountChangesetComment.size());
            ps2.execute();
          }
        }
      }
      conn.commit();
    }
  }

}
