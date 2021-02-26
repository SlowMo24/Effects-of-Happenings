/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.regions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.geometryhandler.GeometryHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.mapper.RegionMapper;
import ParameterCalculation.helper.oshdb.OSHDBHandler;
import ParameterCalculation.helper.parameters.MappingsByRegion;
import ParameterCalculation.helper.time.AnalysesInterval;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableBinaryOperator;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableSupplier;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class MappedRegions {

  private static final Logger LOG = LoggerFactory.getLogger(MappedRegions.class);

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    MappedRegions.evaluate();
  }

  private static void evaluate() throws Exception {
    Set<Integer> users = EventUser.getUserIds();
    Map<Integer, List<EventUser>> eventUsers = EventUser.getUsers();
    STRtree[] indices = GeometryHandler.getGeometries(HomeRegion.getRegionGeomSQL());
    STRtree countries = indices[0];
    STRtree ocean = null;//indices[1];

    try (OSHDBDatabase oshdb = OSHDBHandler.getOhsdb(true);
        Connection keytablesConn = DbHandler.getKeytables();
        Connection maSqlite = DbHandler.getMAConnection();
        PreparedStatement countryInsertStatement = maSqlite.prepareStatement(
            "INSERT INTO aa_MapperHappeningRegionTime(Mapperid,Happeningid,TimeInterval,RegionId,NrContribs) VALUES(?,?,?,?,?);")) {
      maSqlite.setAutoCommit(false);
      //partition users to not overthrow cluster
      int[] i = {0};
      //max number of runs
      if (i[0] > -1) {
        //MAP HERE!!!!
        SortedMap<Integer, Map<Integer, RegionMapper>> reduce = null;
        try {
          reduce = OSMContributionView.on(oshdb)
              .timestamps("2014-01-01", "2018-07-01")
              .areaOfInterest(new OSHDBBoundingBox(-185, -185, 185, 185))
              .keytables(new OSHDBJdbc(keytablesConn))
              .osmType(OSMType.NODE, OSMType.WAY)
              .filter(contrib -> users.contains(contrib.getContributorUserId()))
              .aggregateBy(contrib -> contrib.getContributorUserId())
              .map(new SerializableFunctionImpl(eventUsers, countries, ocean))
              .reduce(new SerializableSupplierImpl(), new SerializableBinaryOperatorImpl());
        } catch (Exception ex) {
          LOG.error("oshdb query faild for user set" + users, ex);
        }
        //===============================================================================
        reduce.forEach((user, regionUserMap) -> {

          try {
            for (Map.Entry<Integer, RegionMapper> rm : regionUserMap.entrySet()) {
              for (Map.Entry<AnalysesInterval, MappingsByRegion> ai
                  : rm.getValue().getMappedRegions().entrySet()) {
                AnalysesInterval key = ai.getKey();
                MappingsByRegion value = ai.getValue();
                for (Map.Entry<Integer, Integer> regMap : value.getMappingByRegion().entrySet()) {
                  countryInsertStatement.setInt(1, user);
                  countryInsertStatement.setInt(2, rm.getKey());
                  countryInsertStatement.setInt(3, key.getId());
                  countryInsertStatement.setInt(4, regMap.getKey());
                  countryInsertStatement.setInt(5, regMap.getValue());
                  countryInsertStatement.execute();
                }
              }
            }
          } catch (SQLException ex) {
            LOG.error("error on insert", ex);
          }
          i[0] += 1;
        });
        maSqlite.commit();
      }

    }

  }

  private static class SerializableBinaryOperatorImpl implements
      SerializableBinaryOperator<Map<Integer, RegionMapper>> {

    public SerializableBinaryOperatorImpl() {
    }

    @Override
    public Map<Integer, RegionMapper> apply(Map<Integer, RegionMapper> rm1,
        Map<Integer, RegionMapper> rm2) {
      Map<Integer, RegionMapper> resultResult = new HashMap<>();
      Map<Integer, RegionMapper> theRM = rm1;
      Map<Integer, RegionMapper> otherRM = rm2;
      if (rm1.isEmpty()) {
        theRM = rm2;
        otherRM = rm1;
      }
      for (Map.Entry<Integer, RegionMapper> rm : theRM.entrySet()) {
        RegionMapper result = new RegionMapper(rm.getValue().getId(), rm.getValue().getEventId(), rm
            .getValue().getEventTime());

        Map<AnalysesInterval, MappingsByRegion> mappedRegions = otherRM
            .getOrDefault(rm.getKey(), new RegionMapper(rm.getValue().getId(), rm.getValue()
                .getEventId(), rm
                    .getValue().getEventTime())).getMappedRegions();
        //this works because we initialise the maps
        for (Map.Entry<AnalysesInterval, MappingsByRegion> entry
            : rm.getValue().getMappedRegions().entrySet()) {
          //this could be improved but whatever!
          MappingsByRegion merge = MappingsByRegion
              .merge(entry.getValue(), mappedRegions.remove(entry.getKey()));
          result.setMappedRegion(entry.getKey(), merge);
        }
        resultResult.put(rm.getKey(), result);
        assert mappedRegions.isEmpty();
      }

      return resultResult;
    }

  }

  private static class SerializableFunctionImpl implements
      SerializableFunction<OSMContribution, Map<Integer, RegionMapper>> {

    private final STRtree countries;
    private final Map<Integer, List<EventUser>> eventUsers;
    private final STRtree ocean;

    public SerializableFunctionImpl(Map<Integer, List<EventUser>> eventUsers, STRtree countries,
        STRtree ocean) {
      this.eventUsers = eventUsers;
      this.countries = countries;
      this.ocean = ocean;
    }

    @Override
    public Map<Integer, RegionMapper> apply(OSMContribution contrib) {
      List<EventUser> eventUser = eventUsers.get(contrib.getContributorUserId());
      Integer insertGeom = HomeEvaluator.getInsertGeom(contrib, countries, ocean);
      Map<Integer, RegionMapper> resultResult = new HashMap<>();
      for (EventUser eu : eventUser) {
        AnalysesInterval forTS = AnalysesInterval
            .getForTS(eu.getEventTime(), contrib.getTimestamp());

        RegionMapper result = new RegionMapper(
            eu.getId(), eu.getEventId(), eu.getEventTime());
        if (insertGeom == Integer.MIN_VALUE) {
          insertGeom = 100000750;
        }
        if (insertGeom != null) {
          result.addMappedRegion(forTS, insertGeom);
        }
        resultResult.put(eu.getEventId(), result);
      }
      return resultResult;
    }

  }

  private static class SerializableSupplierImpl implements
      SerializableSupplier<Map<Integer, RegionMapper>> {

    public SerializableSupplierImpl() {
    }

    @Override
    public Map<Integer, RegionMapper> get() {
      return new HashMap<>();
    }

  }

}
