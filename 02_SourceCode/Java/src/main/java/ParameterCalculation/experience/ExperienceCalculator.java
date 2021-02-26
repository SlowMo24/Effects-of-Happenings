/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.experience;

import com.google.common.collect.Iterables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.oshdb.OSHDBHandler;
import ParameterCalculation.helper.tags.TagHandler;
import ParameterCalculation.helper.time.AnalysesInterval;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class ExperienceCalculator {
  
  private static final Logger LOG = LoggerFactory.getLogger(ExperienceCalculator.class);

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    //ExperienceCalculator.evaluateErrors();
    ExperienceCalculator.evaluateExperience();
  }
  
  private static BitSet analyseGeom(Geometry geometryBefore, Geometry geometryAfter) {
    BitSet result = new BitSet(3);
    int deltaVerc = geometryAfter.getCoordinates().length - geometryBefore.getCoordinates().length;
    if (deltaVerc > 0) {
      result.set(0);
    } else if (deltaVerc < 0) {
      result.set(1);
    } else {
      result.set(2);
    }
    
    return result;
  }
  
  private static BitSet compareTags(Iterable<OSHDBTag> before, Iterable<OSHDBTag> after) {
    
    BitSet result = new BitSet(3);
    Map<Integer, Integer> beforeTags = StreamSupport.stream(before.spliterator(), true).collect(
        Collectors.toMap(tag -> tag.getKey(), tag -> tag.getValue()));
    Map<Integer, Integer> afterTags = StreamSupport.stream(after.spliterator(), true).collect(
        Collectors.toMap(tag -> tag.getKey(), tag -> tag.getValue()));
    
    afterTags.forEach((Integer k, Integer v) -> {
      if (beforeTags.containsKey(k)) {
        if (!(Objects.equals(beforeTags.get(k), afterTags.get(k)))) {
          result.set(2);
        }
        beforeTags.remove(k);
      } else {
        result.set(0);
      }
    });
    if (beforeTags.size() > 0) {
      result.set(1);
    }
    
    return result;
  }
  
  private static void evaluateExperience() throws Exception {
    
    Set<Integer> userIds = EventUser.getUserIds();
    Map<Integer, List<EventUser>> users = EventUser.getUsers();
    SortedMap<OSHDBCombinedIndex<EventUser, AnalysesInterval>, ExperienceResult> aggregateBy = null;
    try (OSHDBDatabase oshdb = OSHDBHandler.getOhsdb(true);
        Connection keytablesConn = DbHandler.getKeytables();) {
      
      Map<Integer, Integer> primaryKeys = TagHandler.getPrimaryKeys(keytablesConn);
      Map<OSHDBTag, Integer> relationValues = TagHandler.getRelationValues(keytablesConn);
      
      aggregateBy = OSHDBHandler
          .getDistinctContribView(oshdb, keytablesConn, userIds)
          //.filter(contrib -> contrib.getOSHEntity().getId() == 0)
          .flatMap((OSMContribution contrib) -> {
            List<ExperienceResult> resultList = new ArrayList<>();
            try {
              OSMEntity entityBefore = contrib.getEntityBefore();
              OSMEntity entityAfter = contrib.getEntityAfter();
              
              AbstractEdit ae = new AbstractEdit();
              List<Integer> primKey;
              BitSet tags = new BitSet(3);
              BitSet geoms = new BitSet(3);
              EditComplexity ec = new EditComplexity();
              if (contrib.getContributionTypes().contains(ContributionType.CREATION)) {
                ae.cOrD.set(0);
                primKey = ExperienceCalculator.getPrimKey(primaryKeys, entityAfter.getTags());
                ec.comp = ExperienceCalculator.getComplexity(relationValues, contrib);
              } else if (contrib.getContributionTypes().contains(ContributionType.DELETION)) {
                ae.cOrD.set(2);
                primKey = ExperienceCalculator.getPrimKey(primaryKeys, entityBefore.getTags());
                ec.comp = 0;
              } else {
                ae.cOrD.set(1);
                primKey = ExperienceCalculator.getPrimKey(primaryKeys, entityBefore.getTags());
                tags = ExperienceCalculator.compareTags(entityBefore.getTags(), entityAfter
                    .getTags());
                geoms = ExperienceCalculator.analyseGeom(contrib.getGeometryBefore(), contrib
                    .getGeometryAfter());
                ec.comp = ExperienceCalculator.getComplexity(relationValues, contrib);
              }
              for (int i : primKey) {
                ae.primaryKeyTouched.set(i);
              }
              ae.typeGeomChange.or(geoms);
              ae.typeTagChange.or(tags);
              
              for (EventUser eu : users.get(contrib.getContributorUserId())) {
                ExperienceResult result = new ExperienceResult();
                result.contribCount += 1;
                for (ContributionType ct : contrib.getContributionTypes()) {
                  result.countPerType.put(ct, 1);
                }
                result.ae.put(ae, 1);
                
                result.ec.put(ec, 1);
                result.eu = eu;
                result.ai = AnalysesInterval.getForTS(eu.getEventTime(), contrib.getTimestamp());
                resultList.add(result);
              }
            } catch (Exception ex) {
              LOG.error("Something went terribly wrong with this contrib", ex);
            }
            return resultList;
          })
          .aggregateBy(er -> er.eu)
          .aggregateBy(er -> er.ai)
          .reduce(() -> new ExperienceResult(), (era, erb) -> {
            
            ExperienceResult result = new ExperienceResult();
            
            result.contribCount = era.contribCount + erb.contribCount;
            
            result.ae.putAll(era.ae);
            for (Map.Entry<AbstractEdit, Integer> entr : erb.ae.entrySet()) {
              result.ae.merge(entr.getKey(), entr.getValue(), (v1, v2) -> v1 + v2);
            }
            
            result.countPerType.putAll(era.countPerType);
            for (Map.Entry<ContributionType, Integer> entr : erb.countPerType.entrySet()) {
              result.countPerType.merge(entr.getKey(), entr.getValue(), (v1, v2) -> v1 + v2);
            }
            
            result.ec.putAll(era.ec);
            for (Map.Entry<EditComplexity, Integer> entr : erb.ec.entrySet()) {
              result.ec.merge(entr.getKey(), entr.getValue(), (v1, v2) -> v1 + v2);
            }
            
            if (era.eu != null) {
              result.eu = era.eu;
              result.ai = era.ai;
            } else {
              result.ai = erb.ai;
              result.eu = erb.eu;
            }
            
            return result;
          });
    }
    try (Connection conn = DbHandler.getMAConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE aa_MapperHappeningTimeMetric SET TotalContributions=?,Creations=?,Deletions=?,Tagchanges=?,geometrychanges=? WHERE Mapperid=? and Happeningid=? and TimeInterval=?;");
        PreparedStatement ps2 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeMetric (Mapperid,Happeningid,TimeInterval,TotalContributions,Creations,Deletions,Tagchanges,geometrychanges) VALUES(?,?,?,?,?,?,?,?);");
        PreparedStatement ps3 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeAbstractedit(Mapperid,Happeningid,TimeInterval,AbstractEdit,Count) VALUES(?,?,?,?,?);");
        PreparedStatement ps4 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeComplexedit(Mapperid,Happeningid,TimeInterval,Complexity,Count) VALUES(?,?,?,?,?);");) {
      conn.setAutoCommit(false);
      for (Map.Entry<OSHDBCombinedIndex<EventUser, AnalysesInterval>, ExperienceResult> res : aggregateBy
          .entrySet()) {
        OSHDBCombinedIndex<EventUser, AnalysesInterval> key = res.getKey();
        EventUser firstIndex = key.getFirstIndex();
        AnalysesInterval secondIndex = key.getSecondIndex();
        ExperienceResult value = res.getValue();
        if (value.contribCount > 0) {
          ps.setInt(6, firstIndex.getId());
          ps.setInt(7, firstIndex.getEventId());
          ps.setInt(8, secondIndex.getId());
          ps.setInt(1, value.contribCount);
          ps.setInt(2, value.countPerType.getOrDefault(ContributionType.CREATION, 0));
          ps.setInt(3, value.countPerType.getOrDefault(ContributionType.DELETION, 0));
          ps.setInt(4, value.countPerType.getOrDefault(ContributionType.TAG_CHANGE, 0));
          ps.setInt(5, value.countPerType.getOrDefault(ContributionType.GEOMETRY_CHANGE, 0));
          ps.execute();
          
          if (ps.getUpdateCount() == 0) {
            ps2.setInt(1, firstIndex.getId());
            ps2.setInt(2, firstIndex.getEventId());
            ps2.setInt(3, secondIndex.getId());
            ps2.setInt(4, value.contribCount);
            ps2.setInt(5, value.countPerType.getOrDefault(ContributionType.CREATION, 0));
            ps2.setInt(6, value.countPerType.getOrDefault(ContributionType.DELETION, 0));
            ps2.setInt(7, value.countPerType.getOrDefault(ContributionType.TAG_CHANGE, 0));
            ps2.setInt(8, value.countPerType.getOrDefault(ContributionType.GEOMETRY_CHANGE, 0));
            ps2.execute();
          }
          
          for (Map.Entry<AbstractEdit, Integer> en : value.ae.entrySet()) {
            ps3.setInt(1, firstIndex.getId());
            ps3.setInt(2, firstIndex.getEventId());
            ps3.setInt(3, secondIndex.getId());
            ps3.setString(4, en.getKey().toString());
            ps3.setInt(5, en.getValue());
            ps3.execute();
          }
          
          for (Map.Entry<EditComplexity, Integer> en : value.ec.entrySet()) {
            ps4.setInt(1, firstIndex.getId());
            ps4.setInt(2, firstIndex.getEventId());
            ps4.setInt(3, secondIndex.getId());
            ps4.setInt(4, en.getKey().comp);
            ps4.setInt(5, en.getValue());
            ps4.execute();
          }
        }
      }
      conn.commit();
    }
  }
  
  private static int getComplexity(Map<OSHDBTag, Integer> relationValues, OSMContribution contrib) {
    switch (contrib.getEntityAfter().getType()) {
      case NODE:
        return 1;
      case WAY:
        if (contrib.getGeometryAfter().getCoordinates().length <= 10) {
          return 2;
        } else {
          return 3;
        }
      case RELATION:
        Iterable<OSHDBTag> tags = contrib.getEntityAfter().getTags();
        if (Iterables.any(tags, tag -> relationValues.keySet().contains(tag))) {
          return 5;
        } else {
          return 4;
        }
      default:
        throw new AssertionError(contrib.getEntityAfter().getType().name());
    }
  }
  
  private static List<Integer> getPrimKey(Map<Integer, Integer> primaryKeys, Iterable<OSHDBTag> tags) {
    List<Integer> result = new ArrayList<>();
    for (OSHDBTag tag : tags) {
      if (primaryKeys.keySet().contains(tag.getKey())) {
        result.add(primaryKeys.get(tag.getKey()));
      }
    }
    return result;
  }
  
}
