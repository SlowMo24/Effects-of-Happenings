package ParameterCalculation.digitalarea;

import com.google.common.collect.Iterables;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import ParameterCalculation.helper.dbhandler.DbHandler;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.oshdb.OSHDBHandler;
import ParameterCalculation.helper.tags.TagHandler;
import ParameterCalculation.helper.time.AnalysesInterval;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableBinaryOperator;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMEntitySnapshotView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.osh.OSHEntities;
import org.heigit.bigspatialdata.oshdb.osh.OSHEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class digitalAreaEval {

  private static final Logger LOG = LoggerFactory.getLogger(digitalAreaEval.class);

  public static void main(String[] args) throws Exception {
    digitalAreaEval.evaluate();

  }

  private static Map<String, Map<DigitalAreaUser, MultiPolygon>> getWeeklyGeoms() throws
      SQLException,
      ClassNotFoundException,
      ParseException {
    Map<Integer, List<EventUser>> eventUsers = EventUser.getUsers();
    Set<Integer> userIds = EventUser.getUserIds();
    Set<Long> uids = new HashSet<>();
    userIds.forEach(id -> uids.add(id.longValue()));//666l));//
    Map<String, Map<DigitalAreaUser, MultiPolygon>> result = new HashMap<>();
    String sql = "select user_id as uid, "
        + "to_char(date_trunc('month',created_at),'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') as d, "
        + "st_asbinary(ST_Multi(st_union(st_buffer(geography(geom),150,'join=mitre quad_segs=1  endcap=square')::geometry))) as thegeom, "
        + "count(r) as nrOfGeoms, "
        + "sum(st_area(st_buffer(geography(geom), 150, 'join=mitre quad_segs=1  endcap=square'))) ar "
        + "from ( "
        + "	select row_number() OVER (PARTITION BY user_id,date_trunc('month',created_at) order by random()) as r, "
        + "	user_id, "
        + "	created_at, "
        + "	geom "
        + "from osm_changeset "
        + "where user_id=ANY(?) and created_at < date '2018-07-01' and created_at > date '2014-07-01' and st_area(geography(geom))<25000000) as ab  "
        + "where r<11 "
        + "group by uid,d;";
    try (Connection conn = DbHandler.getChangesets();
        PreparedStatement stmt = conn.prepareStatement(sql);) {
      Array createArrayOf = conn.createArrayOf("bigint", uids.toArray());
      stmt.setArray(1, createArrayOf);
      try (ResultSet rs = stmt.executeQuery()) {
        WKBReader wkbr = new WKBReader();
        while (rs.next()) {
          Integer uid = rs.getInt(1);
          List<EventUser> get = eventUsers.get(uid);
          String time = rs.getString(2);
          byte[] bytes = rs.getBytes(3);
          double ar = rs.getDouble(5);
          Geometry read = wkbr.read(bytes);
          read.setUserData(ar);
          for (EventUser eu : get) {
            DigitalAreaUser dau = new DigitalAreaUser(eu.getId(), eu.getEventId(), eu.getEventTime());
            result
                .computeIfAbsent(time, uid2 -> new HashMap<>())
                .put(dau, (MultiPolygon) read);
          }
        }
      }

    }
    return result;

  }

  private static void evaluate() throws Exception {
    Map<String, Map<DigitalAreaUser, MultiPolygon>> analysesGeoms = digitalAreaEval.getWeeklyGeoms();

    try (OSHDBDatabase oshdb = OSHDBHandler.getOhsdb(true);
        Connection keytablesConn = DbHandler.getKeytables();
        Connection maSqlite = DbHandler.getMAConnection();
        PreparedStatement countryInsertStatement = maSqlite.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeMetric"
            + "(Mapperid,Happeningid,TimeInterval,AreaEdited,NrOfElements,SumOfTags,SumOfMappers) "
            + "VALUES(?,?,?,?,?,?,?);");
        PreparedStatement abstractObjects = maSqlite.prepareStatement(
            "INSERT INTO aa_MapperHappeningTimeUniqefeature"
            + "(Mapperid,Happeningid,TimeInterval,AbstractFeature,NumberOfElements) "
            + "VALUES(?,?,?,?,?);");
        BufferedWriter writer = new BufferedWriter(new FileWriter("Geoms.wkt"));) {
      writer.write("ID;Geom");
      writer.newLine();
      WKTWriter wktW = new WKTWriter();

      maSqlite.setAutoCommit(false);

      Map<Integer, Integer> primaryKeys = TagHandler.getPrimaryKeys(keytablesConn);

      //partition users to not overthrow cluster
      int[] i = {0};
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'",
          Locale.ENGLISH);
      Map<DigitalAreaUser, Map<AnalysesInterval, DigitalAreaResult>> result = new HashMap<>();
      for (Map.Entry<String, Map<DigitalAreaUser, MultiPolygon>> gemMap : analysesGeoms.entrySet()) {
        System.out.println(i[0] + 1 + " of " + analysesGeoms.size());
        System.out.println(gemMap.getValue().size());
        //max number of runs
        if (i[0] > -1) {
          UnaryUnionOp uuo = new UnaryUnionOp(gemMap.getValue().values());
          Geometry union = uuo.union();
          System.out.println(union.getEnvelopeInternal());

          writer.write(i[0] + ";" + wktW.write(union));
          writer.newLine();

          //CascadedPolygonUnion cpu=new CascadedPolygonUnion(gemMap.getValue().values());
          //Geometry union2 = cpu.union();
          SortedMap<DigitalAreaUser, DigitalAreaResult> reduce = OSMEntitySnapshotView.on(oshdb)
              .osmType(OSMType.NODE, OSMType.WAY)
              .timestamps(gemMap.getKey())
              .keytables(new OSHDBJdbc(keytablesConn))
              .areaOfInterest((Geometry & Polygonal) union)//.getEnvelope())
              .aggregateByGeometry(gemMap.getValue())
              .map(new SerializableFunctionImpl(primaryKeys))
              .reduce(() -> new DigitalAreaResult(),
                  new SerializableBinaryOperatorImpl());

          //===============================================================================
          reduce.forEach((user, interResult) -> {

            interResult.area = (double) gemMap.getValue().get(user).getUserData();

            EventUser eu = user.getEu();
            LocalDateTime date = LocalDateTime.parse(gemMap.getKey(), formatter);
            OSHDBTimestamp oshdbTimestamp = new OSHDBTimestamp(date.toInstant(ZoneOffset.UTC)
                .getEpochSecond());
            AnalysesInterval forTS = AnalysesInterval.getForTS(eu.getEventTime(),
                oshdbTimestamp);
            SerializableBinaryOperatorImpl serializableBinaryOperatorImpl = new SerializableBinaryOperatorImpl();
            result.computeIfAbsent(user, uk ->
                new HashMap<AnalysesInterval, DigitalAreaResult>())
                .merge(forTS, interResult, (resa, resb) -> serializableBinaryOperatorImpl.apply(
                    resa, resb));

          });
          i[0] += 1;
        }
      }
      result.forEach((user, resultMap) -> {
        EventUser eu = user.getEu();
        for (Map.Entry<AnalysesInterval, DigitalAreaResult> entr : resultMap.entrySet()) {
          DigitalAreaResult value = entr.getValue();
          try {
            countryInsertStatement.setInt(1, eu.getId());
            countryInsertStatement.setInt(2, eu.getEventId());
            countryInsertStatement.setInt(3, entr.getKey().getId());
            countryInsertStatement.setDouble(4, value.area);
            countryInsertStatement.setInt(5, value.nrOfElements);
            countryInsertStatement.setInt(6, value.sumOfTags);
            countryInsertStatement.setInt(7, value.sumOfMappers);
            countryInsertStatement.execute();

            for (int j = 0; j < value.nrPrimaryKeys.length; j++) {
              if (value.nrPrimaryKeys[j] > 0) {
                abstractObjects.setInt(1, eu.getId());
                abstractObjects.setInt(2, eu.getEventId());
                abstractObjects.setInt(3, entr.getKey().getId());
                abstractObjects.setInt(4, j);
                abstractObjects.setInt(5, value.nrPrimaryKeys[j]);
                abstractObjects.execute();
              }
            }

            System.out.println(value);
          } catch (SQLException ex) {
            LOG.error("error on insert", ex);
          }
        }

      });
      maSqlite.commit();
    }
  }

  private static class SerializableBinaryOperatorImpl implements
      SerializableBinaryOperator<DigitalAreaResult> {

    public SerializableBinaryOperatorImpl() {
    }

    @Override
    public DigitalAreaResult apply(DigitalAreaResult dar1, DigitalAreaResult dar2) {
      DigitalAreaResult result = new DigitalAreaResult();
      result.nrOfElements = dar1.nrOfElements + dar2.nrOfElements;
      result.sumOfMappers = dar1.sumOfMappers + dar2.sumOfMappers;
      result.sumOfTags = dar1.sumOfTags + dar2.sumOfTags;
      for (int i = 0; i < 27; i++) {
        result.nrPrimaryKeys[i] = dar1.nrPrimaryKeys[i] + dar2.nrPrimaryKeys[i];
      }
      result.area = dar1.area + dar2.area;
      return result;
    }

  }

  private static class SerializableFunctionImpl implements
      SerializableFunction<OSMEntitySnapshot, DigitalAreaResult> {

    Map<Integer, Integer> primaryKeys;

    public SerializableFunctionImpl(Map<Integer, Integer> primaryKeys) {
      this.primaryKeys = primaryKeys;
    }

    @Override
    public DigitalAreaResult apply(OSMEntitySnapshot snap) {
      OSMEntity entity = snap.getEntity();
      OSHEntity oshEntity = snap.getOSHEntity();
      Set<Integer> uniquUsers = new HashSet<>();
      OSHEntities.getModificationTimestamps(oshEntity).stream()
          .parallel()
          .map(oshdbtimestamp -> OSHEntities.getByTimestamp(oshEntity, oshdbtimestamp))
          .forEach(osme -> uniquUsers.add(osme.getUserId()));
      Iterable<OSHDBTag> tags = entity.getTags();
      DigitalAreaResult digitalAreaResult = new DigitalAreaResult();
      digitalAreaResult.nrOfElements += 1;
      digitalAreaResult.sumOfTags += Iterables.size(tags);
      digitalAreaResult.sumOfMappers += uniquUsers.size();
      for (OSHDBTag tag : tags) {
        if (primaryKeys.keySet().contains(tag.getKey())) {
          digitalAreaResult.nrPrimaryKeys[primaryKeys.get(tag.getKey())] += 1;
        }
      }

      return digitalAreaResult;
    }

  }

}
