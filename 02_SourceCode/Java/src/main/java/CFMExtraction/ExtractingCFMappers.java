package CFMExtraction;

import ParameterCalculation.helper.changesetenricher.ChangesetEnricher;
import ParameterCalculation.helper.changesetenricher.OSMChangeset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBIgnite;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestampList;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

public class ExtractingCFMappers {

  private static final Logger LOG = LoggerFactory.getLogger(ExtractingCFMappers.class);

  public static void main(String[] args) throws SQLException, Exception {

    FeatureCollection eventGeoms = ExtractingCFMappers.getEventGeoms();
    Iterator<Feature> iterator = Arrays.asList(eventGeoms.getFeatures()).iterator();
    GeoJSONReader reader = new GeoJSONReader();

    Connection keytablesConn = DriverManager.getConnection(
        "jdbc:postgresql://OSHDBKeytables", "User", "PW");
    Connection changesetConn = DriverManager.getConnection(
        "jdbc:postgresql://ChangesetsDB", "User", "PW");

    ChangesetEnricher ce = new ChangesetEnricher(changesetConn);

    StringJoiner changesets = new StringJoiner(",");
    changesets.add("emptyRow")
        .add("EventID")
        .add("changesetId")
        .add("userId")
        .add("changesetCreateTS")
        .add("changesetSource")
        .add("changesetComment\n");

    GeoJSONWriter writer = new GeoJSONWriter();
    List<Feature> features = new ArrayList<Feature>();

    OSHDBDatabase oshdb;
    oshdb = new OSHDBIgnite(ExtractingCFMappers.class.getResource("/ignite-prod-ohsome-client.xml")
        .getFile());
    oshdb.prefix("global_v4");

    while (iterator.hasNext()) {
      Feature feature = iterator.next();
      Map<String, Object> properties = feature.getProperties();
      Stream<Feature> geomStream = OSMContributionView.on(oshdb)
          .keytables(new OSHDBJdbc(keytablesConn))
          .timestamps(ExtractingCFMappers.getTimeRange(feature))
          .areaOfInterest((Polygon) reader.read(feature.getGeometry()))
          .map(contrib -> {
            Geometry geom = contrib.getGeometryUnclippedAfter();
            HashMap<String, Object> localProps = new HashMap<>(properties);
            localProps.put("contributionTS", contrib.getTimestamp().toString());
            localProps.put("UserIDGeom", contrib.getContributorUserId());
            localProps.put("changesetIDGeom", contrib.getChangesetId());
            localProps.put("OSMType", contrib.getOSHEntity().getType().toString());
            localProps.put("OSMID", contrib.getOSHEntity().getId());
            if (geom != null) {
              return new Feature(writer.write(geom), localProps);
            } else {
              geom = contrib.getGeometryUnclippedBefore();
              if (geom != null) {
                return new Feature(writer.write(geom), localProps);
              } else {
                properties.put("Error", "Error in geometry Construction");
                return new Feature(
                    writer.write(
                        reader.read(feature.getGeometry()).getCentroid()), localProps);
              }
            }
          })
          .stream();

      Set<Long> change = new HashSet<>();
      geomStream.forEach(entry -> {
        Long changesetId = (Long) entry.getProperties().get("changesetIDGeom");
        if (!change.contains(changesetId)) {
          change.add(changesetId);
          try {
            OSMChangeset cs = ce.getChangeset(changesetId);
            changesets.add(properties.get("ID") + "")
                .add(changesetId + "")
                .add(cs.getUserId() + "")
                .add(cs.getCreatedAts().toString())
                .add(cs.getTags().get("source"))
                .add("\'" + cs.getTags().get("comment") + "\'\n");
          } catch (SQLException ex) {
            LOG.error("", ex);
          }
        }
        features.add(entry);
      });

    }
    oshdb.close();

    GeoJSON json = writer.write(features);

    try (PrintWriter pw = new PrintWriter(new File("CFMgeometries.geojson"))) {
      pw.write(json.toString());
    }

    try (PrintWriter pw = new PrintWriter(new File("CFMchangesets.csv"))) {
      pw.write(changesets.toString());
    }

  }

  private static FeatureCollection getEventGeoms() throws FileNotFoundException, IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(
        ".../EventAreas.geojson"))) {
      StringBuilder sb = new StringBuilder();
      br.lines().forEach(line -> sb.append(line));
      String cfmRegions = sb.toString();
      return (FeatureCollection) GeoJSONFactory.create(cfmRegions);
    }
  }

  private static OSHDBTimestampList getTimeRange(Feature feature) {
    Map<String, Object> properties = feature.getProperties();

    //get UTC-end-time
    LocalDateTime utcDateTime = LocalDateTime.parse(
        properties.get("enddate_UTC") + "T" + properties.get("endtime_UTC"));
    //get offset
    ZoneOffset offset = ZoneOffset.ofHours((Integer) properties.get("time_offset"));
    //calculate local date time
    LocalDateTime zoneDateTime = utcDateTime.plusSeconds(offset.getTotalSeconds());
    //create offset date time
    OffsetDateTime offsetDateTime = OffsetDateTime.of(zoneDateTime, offset);
    //add 2 days...
    OffsetDateTime oneDayLater = offsetDateTime.plusDays(2);
    //then got to 00:00 o'clock == next midnight
    OffsetDateTime offsetDate = oneDayLater.truncatedTo(ChronoUnit.DAYS);
    //calculate back to UTC again
    ZonedDateTime endRangeUtc = offsetDate.atZoneSameInstant(ZoneId.of("UTC"));

    return new OSHDBTimestamps(
        properties.get("startdate_UTC") + "T"
        + properties.get("starttime_UTC"),
        endRangeUtc.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

  }

}
