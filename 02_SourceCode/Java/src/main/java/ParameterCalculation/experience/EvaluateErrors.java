/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.experience;

import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.enhancedTT.EnhancedTagTranslator;
import ParameterCalculation.helper.josm.JosmTransformer;
import ParameterCalculation.helper.josm.JOSMTests.OpeningHourTestPatch;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.oshdb.OSHDBHandler;
import ParameterCalculation.helper.time.AnalysesInterval;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Territories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class EvaluateErrors {
  
  private static final Logger LOG = LoggerFactory.getLogger(EvaluateErrors.class);

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    EvaluateErrors.evaluateErrors();
  }
  
  private static void evaluateErrors() throws Exception {
    Set<Integer> userIds = EventUser.getUserIds();
    Map<Integer, List<EventUser>> users = EventUser.getUsers();
    
    Preferences preferences = new Preferences();
    Config.setPreferencesInstance(preferences);
    Projection p = new CustomProjection("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
    ProjectionRegistry.setProjection(p);
    Territories.initialize();
    RightAndLefthandTraffic.initialize();
    final DataSet ds = new DataSet();
    Layer layer = new OsmDataLayer(ds, "L1", null);
    MainApplication.getLayerManager().addLayer(layer);
    
    List<String> undesired = new ArrayList<>();
    undesired.add("ApiCapabilitiesTest");
    undesired.add("CrossingWays");
    undesired.add("DuplicateNode");
    undesired.add("DuplicateRelation");
    undesired.add("DuplicateWay");
    undesired.add("OverlappingWays");
    undesired.add("SimilarNamedWays");
    undesired.add("UnconnectedWays");
    undesired.add("WayConnectedToArea");
    undesired.add("OpeningHourTest");
    
    SortedMap<String, Test> allTestsMap = OsmValidator.getAllTestsMap();
    Map<String, Test> desired = allTestsMap
        .entrySet()
        .parallelStream()
        .filter(entr -> !undesired.stream().parallel().anyMatch(s -> entr.getKey().contains(s)))
        .collect(Collectors.toMap(entr -> entr.getKey(), entr2 -> entr2.getValue()));
    desired.put("OpeningHourTestPatch", new OpeningHourTestPatch());
    
    for (Test t : desired.values()) {
      t.initialize();
    }
    SortedMap<OSHDBCombinedIndex<EventUser, AnalysesInterval>, ErrorsResult> reduce = null;

    //ERROR! global Keytables stimmen nicht für lokalen extract!
    try (OSHDBDatabase oshdb = OSHDBHandler.getOhsdb(false);
        //OSHDBDatabase oshdb2 = OSHDBHandler.getOhsdb(false);
        Connection keytablesConn = DbHandler.getLocalKeytables();) {//.getKeytables();){//
      /* Set<OSHDBTag> reduce1 = OSHDBHandler.getDistinctContribView(oshdb2, keytablesConn, userIds)
          .map((OSMContribution contrib) -> {
            Set<OSHDBTag> result = new HashSet<>();
            OSMEntity before = contrib.getEntityBefore();
            OSMEntity after = contrib.getEntityAfter();
            if (before != null) {
              before.getTags().forEach(tag -> result.add(tag));

              if (before instanceof OSMWay) {
                ((OSMWay) before).getRefEntities(contrib.getTimestamp())
                    .forEach(osment -> osment.getTags().forEach(tag -> result.add(tag)));
              } else if (before instanceof OSMRelation) {
                ((OSMRelation) before).getMemberEntities(contrib.getTimestamp())
                    .forEach(osment -> {
                      osment.getTags().forEach(tag -> result.add(tag));
                      if (osment.getType() == OSMType.WAY) {
                        ((OSMWay) osment).getRefEntities(contrib.getTimestamp())
                            .forEach(osment2 -> osment2.getTags().forEach(tag2 -> result.add(
                                tag2)));
                      }
                    });
              }
            }
            after.getTags().forEach(tag -> result.add(tag));
            return result;
          })
          .reduce(() -> new HashSet<>(), (seta, setb) -> {
            HashSet<OSHDBTag> result = new HashSet<>(seta.size());
            result.addAll(seta);
            result.addAll(setb);
            return result;
          });*/
      
      EnhancedTagTranslator tt = new EnhancedTagTranslator(keytablesConn);
      //tt.fetchAll(reduce1);
      tt.fetchAll();
      System.out.println("feched");
      
      final int[] j = new int[]{0};
      reduce = OSHDBHandler.getDistinctContribView(oshdb, keytablesConn, userIds)
          .flatMap((OSMContribution contrib) -> {
            OSMEntity entityBefore = contrib.getEntityBefore();
            OSMEntity entityAfter = contrib.getEntityAfter();
            List<ErrorsResult> result = new ArrayList<>();
            
            int errBefore = 0;
            int errAfter = 0;
            try {
              synchronized (EvaluateErrors.class) {
                
                if (entityBefore != null) {
                  OsmPrimitive before = JosmTransformer
                      .transform(contrib.getEntityBefore(), contrib.getTimestamp(), tt, ds);
                  errBefore = EvaluateErrors.test(before, desired.values());
                  
                  ds.clear();
                  /*for (OsmPrimitive prim : before) {
                  prim = null;
                }*/
                }
                
                if (entityAfter.isVisible()) {
                  OsmPrimitive after = JosmTransformer
                      .transform(contrib.getEntityAfter(), contrib.getTimestamp(), tt, ds);
                  errAfter = EvaluateErrors.test(after, desired.values());
                  
                  ds.clear();
                  /*for (OsmPrimitive prim : after) {
                  prim = null;
                }*/
                }
                /* //the lack of the following may have caused out of memeory...
              ProjectionRegistry.clearProjectionChangeListeners();

              //MainApplication.getLayerManager().resetState();
              
              ds = null;
              layer = null;*/
                
              }
            } catch (Exception ex) {
              LOG.error("Had to skip this entity! ", ex);
            }
            
            for (EventUser eu : users.get(contrib.getContributorUserId())) {
              ErrorsResult tmp = new ErrorsResult();
              tmp.eu = eu;
              tmp.errorDelta = errAfter - errBefore;
              if (tmp.errorDelta > 0) {
                tmp.untouchedErrors = errBefore;
              } else {
                tmp.untouchedErrors = errBefore + tmp.errorDelta;
              }
              tmp.inter = AnalysesInterval.getForTS(eu.getEventTime(), contrib.getTimestamp());
              tmp.tureThing = true;
              result.add(tmp);
            }
            
            return result;
          })
          .map((ErrorsResult res) -> {
            j[0] += 1;
            if (j[0] != 0 && j[0] % 100000 == 0) {
              LOG.info("Processed " + j[0] + "/12'000'000 contributions");
            }
            return res;
          })
          .aggregateBy(er -> er.eu)
          .aggregateBy(er -> er.inter)
          .reduce(() -> new ErrorsResult(), (ErrorsResult era, ErrorsResult erb) -> {
            ErrorsResult result = new ErrorsResult();
            if (era.eu != null) {
              result.eu = era.eu;
              result.inter = era.inter;
              result.tureThing = era.tureThing;
            } else {
              result.inter = erb.inter;
              result.eu = erb.eu;
              result.tureThing = erb.tureThing;
            }
            result.errorDelta = era.errorDelta + erb.errorDelta;
            result.untouchedErrors = era.untouchedErrors + erb.untouchedErrors;
            return result;
          });
    }
    try (Connection conn = DbHandler.getMAConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE aa_MapperHappeningTimeMetric SET JOSMErrorsDelta=?, JOSMErrorsUntouched=? WHERE Mapperid=? and Happeningid=? and TimeInterval=?;");
        PreparedStatement ps2 = conn.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeMetric (Mapperid,Happeningid,TimeInterval,JOSMErrorsDelta,JOSMErrorsUntouched) VALUES(?,?,?,?,?);");) {
      conn.setAutoCommit(false);
      for (Map.Entry<OSHDBCombinedIndex<EventUser, AnalysesInterval>, ErrorsResult> res : reduce
          .entrySet()) {
        OSHDBCombinedIndex<EventUser, AnalysesInterval> key = res.getKey();
        EventUser firstIndex = key.getFirstIndex();
        AnalysesInterval secondIndex = key.getSecondIndex();
        ErrorsResult value = res.getValue();
        if (value.tureThing) {
          ps.setInt(3, firstIndex.getId());
          ps.setInt(4, firstIndex.getEventId());
          ps.setInt(5, secondIndex.getId());
          ps.setInt(1, value.errorDelta);
          ps.setInt(2, value.untouchedErrors);
          ps.execute();
          if (ps.getUpdateCount() == 0) {
            ps2.setInt(1, firstIndex.getId());
            ps2.setInt(2, firstIndex.getEventId());
            ps2.setInt(3, secondIndex.getId());
            ps2.setInt(4, value.errorDelta);
            ps2.setInt(5, value.untouchedErrors);
            ps2.execute();
          }
        }
      }
      conn.commit();
    }
  }
  
  private static int test(OsmPrimitive prim, Collection<Test> desired) {
    int i = 0;
    for (Test t : desired) {
      try {
        t.startTest(null);
      } catch (Exception ex) {
        LOG.error("", ex);
      }
      prim.accept(t);
      i += t.getErrors().size();
      try {
        t.endTest();
      } catch (Exception ex) {
        LOG.error("Coult not end Test", ex);
      }
    }
    return i;
  }
  
}
