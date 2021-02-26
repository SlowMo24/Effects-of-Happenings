package ParameterCalculation.regions;

import com.google.common.collect.Iterables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.geometryhandler.GeometryHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.oshdb.OSHDBHandler;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapAggregator;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(HomeEvaluator.class);

  public static void evaluate(String sql, String insert) throws SQLException, ClassNotFoundException,
      ParseException, Exception {
    Set<Integer> users = EventUser.getUserIds();
    STRtree[] indices = GeometryHandler.getGeometries(sql);
    STRtree countries = indices[0];
    STRtree ocean = null;//indices[1];
    try (OSHDBDatabase oshdb = OSHDBHandler.getOhsdb(true);
        Connection keytablesConn = DbHandler.getKeytables();
        Connection maSqlite = DbHandler.getMAConnection();
        PreparedStatement countryInsertStatement = maSqlite.prepareStatement(insert)) {
      maSqlite.setAutoCommit(false);

      //partition users to not overthrow cluster
      Iterable<List<Integer>> subSet = Iterables.partition(users, 2000);
      int[] i = {0};
      subSet.forEach(userList -> {
        //max number of runs
        if (i[0] > -1) {
          SortedMap<OSHDBCombinedIndex<Integer, Integer>, Integer> count = null;
          try {
            count = HomeEvaluator.getregionEvalMapReducer(
                oshdb, keytablesConn, userList, countries, ocean)
                .count();
          } catch (Exception ex) {
            LOG.error("oshdb query faild for user set" + userList, ex);
          }
          //===============================================================================
          SortedMap<Integer, SortedMap<Integer, Integer>> nest = OSHDBCombinedIndex.nest(count);
          nest.forEach((user, map) -> {
            map.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                //order is important here!
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                //only get the first x countries
                //.limit(50)
                .forEach(entry -> {
                  try {
                    countryInsertStatement.setInt(1, user);
                    countryInsertStatement.setInt(2, entry.getKey());
                    countryInsertStatement.setInt(3, entry.getValue());
                    countryInsertStatement.executeUpdate();
                  } catch (SQLException ex) {
                    LOG.error("error on insert", ex);
                  }
                });
          });
          try {
            maSqlite.commit();
          } catch (SQLException ex) {
            LOG.error("", ex);
          }
          i[0] += 1;
        }
      });
    }

  }

  public static Integer getInsertGeom(
      OSMContribution contrib, STRtree countries, STRtree ocean) {
    Geometry geometry;
    if (contrib.getContributionTypes().contains(ContributionType.DELETION)) {
      geometry = contrib.getGeometryBefore();
    } else {
      geometry = contrib.getGeometryAfter();
    }
    if (geometry.isEmpty()) {
      LOG.error(
          "emptyGeom: " + contrib.getOSHEntity().getType() + "  ---  " + contrib
          .getOSHEntity().getId());
    }
    Point centroid = geometry.getCentroid();
    @SuppressWarnings("unchecked")
    List<Object[]> query = countries.query(centroid.getEnvelopeInternal());

    Stream<Object[]> parallelStream = query
        .parallelStream();
    if (query.size() > 1) {
      parallelStream = parallelStream.filter(entry -> ((Polygon) entry[1]).contains(geometry));
    }
    Optional<Integer> findFirst = parallelStream
        .map(entry -> ((int) entry[0]))
        .findAny();
    if (findFirst.isPresent()) {
      return findFirst.get();
    }

    //fall back to nearest neighbour
    ItemDistance itemDistance = new ItemDistance() {
      @Override
      public double distance(ItemBoundable item1, ItemBoundable item2) {
        //geometry distance nur für unsere Items

        Geometry g1 = (Geometry) ((Object[]) item1.getItem())[1];
        Geometry g2 = (Geometry) ((Object[]) item2.getItem())[1];
        try {
          //could be done on client but ignite serialisation problems
          
          CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
          CoordinateReferenceSystem eckert4 = CRS.parseWKT(
              "PROJCS[\"World_Eckert_IV\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Eckert_IV\"],PARAMETER[\"Central_Meridian\",0],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"54012\"]]");
          MathTransform transform = CRS.findMathTransform(wgs84, eckert4);
          g1 = JTS.transform(g1, transform);
          g2 = JTS.transform(g2, transform);
        } catch (FactoryException | TransformException ex) {
          LOG.error("", ex);
        }
        return g1.distance(g2);
      }

    };

    Object nearestNeighbour = countries.nearestNeighbour(centroid.getEnvelopeInternal(),
        new Object[]{null, centroid},
        itemDistance);
    if (nearestNeighbour != null) {
      return (int) ((Object[]) nearestNeighbour)[0];
    }
    /*
    @SuppressWarnings("unchecked")
    List<Object[]> theobs = ocean.query(centroid.getEnvelopeInternal());
    if (theobs.size() > 0) {
      return Integer.MIN_VALUE;
    }*/
    LOG.error(
        centroid.toText() + " point in no geometry from changeset: " + contrib
        .getChangesetId() + " and entity " + contrib.getOSHEntity().getId());
    return null;
  }

  private static MapAggregator<OSHDBCombinedIndex<Integer, Integer>, OSMContribution> getregionEvalMapReducer(
      OSHDBDatabase oshdb, Connection keytablesConn, List<Integer> userList, STRtree countries,
      STRtree ocean) throws FactoryException {
    return OSHDBHandler.getBasicContribView(oshdb, keytablesConn)
        .osmType(OSMType.NODE, OSMType.WAY)
        .filter(contrib -> userList.contains(contrib.getContributorUserId()))
        .aggregateBy(contrib -> contrib.getContributorUserId())
        .aggregateBy((OSMContribution contrib) -> {
          Integer insertGeom = HomeEvaluator.getInsertGeom(contrib, countries, ocean);
          if (insertGeom == null) {
            return -1;
          } /*else if (insertGeom == Integer.MIN_VALUE) {
            //return 555;
            LOG.error("THIS SHOULD NOT HAPPEN!");
            return 100000750;
          }*/ else {
            return insertGeom;
          }
        });
  }

  public static MathTransform getTransformEckIV() {
    try {
      CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
      CoordinateReferenceSystem eckert4 = CRS.parseWKT(
          "PROJCS[\"World_Eckert_IV\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Eckert_IV\"],PARAMETER[\"Central_Meridian\",0],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"54012\"]]");
      return CRS.findMathTransform(wgs84, eckert4);
    } catch (FactoryException ex) {
      LOG.error("", ex);
      return null;
    }
  }

}
