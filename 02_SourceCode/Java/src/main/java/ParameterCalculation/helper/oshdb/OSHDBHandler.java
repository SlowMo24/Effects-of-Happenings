package ParameterCalculation.helper.oshdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBIgnite;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMEntitySnapshotView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;

public class OSHDBHandler {

  public static OSHDBDatabase getOhsdb(boolean ignite) throws SQLException, ClassNotFoundException {
    OSHDBDatabase oshdb;
    if (ignite) {
      oshdb = new OSHDBIgnite(OSHDBHandler.class.getResource("/ignite-dev-ohsome-client.xml")
          .getFile()).computeMode(OSHDBIgnite.ComputeMode.ScanQuery);
      oshdb.prefix("global_v5");

    } else {
      oshdb = new OSHDBH2("H2TestDB").multithreading(true);
    }
    return oshdb;
  }

  public static MapReducer<OSMContribution> getBasicContribView(
      OSHDBDatabase oshdb,
      Connection keytablesConn) {
    return OSMContributionView
        .on(oshdb)
        .timestamps("1970-01-01T00:00:00", "2019-06-21T13:00:00")
        //.areaOfInterest(new OSHDBBoundingBox(-90.7620867, 14.5245151, -90.7616048, 14.5248775))
        .areaOfInterest(new OSHDBBoundingBox(-185, -185, 185, 185))
        .keytables(new OSHDBJdbc(keytablesConn));
  }

  public static MapReducer<OSMEntitySnapshot> getBasicSanpshotView(
      OSHDBDatabase oshdb,
      Connection keytablesConn) {
    return OSMEntitySnapshotView
        .on(oshdb)
        .timestamps("2019-06-21T13:00:00")
        //.areaOfInterest(new OSHDBBoundingBox(-90.7620867, 14.5245151, -90.7616048, 14.5248775))
        .areaOfInterest(new OSHDBBoundingBox(-185, -185, 185, 185))
        .keytables(new OSHDBJdbc(keytablesConn));
  }

  public static MapReducer<OSMContribution> getDistinctContribView(
      OSHDBDatabase oshdb,
      Connection keytablesConn,
      Set<Integer> userIds) {
    return OSMContributionView
        .on(oshdb)
        .timestamps("2014-01-01", "2018-07-01")
        .areaOfInterest(new OSHDBBoundingBox(-185, -185, 185, 185))
        .osmType(OSMType.NODE, OSMType.WAY)//, OSMType.RELATION)
        .keytables(new OSHDBJdbc(keytablesConn))
        .filter(contrib -> userIds.contains(contrib.getContributorUserId()));
  }

}
