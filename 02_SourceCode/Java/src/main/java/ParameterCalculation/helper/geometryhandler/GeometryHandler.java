package ParameterCalculation.helper.geometryhandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ParameterCalculation.helper.dbhandler.DbHandler;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

public class GeometryHandler {

  public static STRtree[] getGeometries(String sql)
      throws SQLException, ParseException, ClassNotFoundException {
    STRtree index = new STRtree();
    STRtree indexOcean = new STRtree();

    try (Connection conn = DbHandler.getMAConnection()) {

      try (Statement stmt = conn.createStatement()) {
        // set timeout to 30 sec.
        stmt.setQueryTimeout(30);
        // load SpatiaLite
        stmt.execute("SELECT load_extension('mod_spatialite')");
      }
      WKBReader wkbReader = new WKBReader();
      try (Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
          byte[] geomBytes = rs.getBytes(3);
          int id = rs.getInt(1);
          int theID=rs.getInt(2);

          Geometry geometry = wkbReader.read(geomBytes);
          for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry geometryN = geometry.getGeometryN(i);
            if (id != 555) {
              index.insert(geometryN.getEnvelopeInternal(), new Object[]{theID, geometryN});
            } else {
              indexOcean.insert(geometryN.getEnvelopeInternal(), new Object[]{theID, geometryN});
            }
          }
        }
      }
    }
    index.build();
    return new STRtree[]{index, indexOcean};
  }

}
