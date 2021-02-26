package ParameterCalculation.helper.josm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ParameterCalculation.helper.enhancedTT.EnhancedTagTranslator;
import org.heigit.bigspatialdata.oshdb.osh.OSHEntities;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMMember;
import org.heigit.bigspatialdata.oshdb.osm.OSMNode;
import org.heigit.bigspatialdata.oshdb.osm.OSMRelation;
import org.heigit.bigspatialdata.oshdb.osm.OSMWay;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMRole;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JosmTransformer {

  private static final Logger LOG = LoggerFactory.getLogger(JosmTransformer.class);

  public static <T extends OSMEntity> OsmPrimitive transform(T ent, OSHDBTimestamp ts,
      EnhancedTagTranslator tt, DataSet ds) {
    long changesetId = ent.getChangesetId();
    long id = ent.getId();
    Iterable<OSHDBTag> tags = ent.getTags();
    Map<String, String> keys = new HashMap<>();
    for (OSHDBTag tag : tags) {
      OSMTag osmTagOf;
      try {
        osmTagOf = tt.getOSMTagOf(tag);
      } catch (Exception ex) {
        //LOG.error("", ex);
        osmTagOf = new OSMTag("", "");

      }
      keys.put(osmTagOf.getKey(), osmTagOf.getValue());
    }
    OSHDBTimestamp timestamp = ent.getTimestamp();
    Date date = timestamp.toDate();
    int userId = ent.getUserId();
    User u = User.createOsmUser(userId, "");
    int version = ent.getVersion();
    boolean visible = ent.isVisible();

    OsmPrimitive p = null;
    switch (ent.getType()) {
      case NODE:
        p = new Node();

        OSMNode OsmNode = (OSMNode) ent;
        EastNorth en = new EastNorth(OsmNode.getLongitude(), OsmNode.getLatitude());
        ((Node) p).setEastNorth(en);
        break;
      case WAY:
        p = new Way();
        List<Node> nodes = new ArrayList<>();
        ((OSMWay) ent).getRefEntities(ts)//.parallel()
            .forEach(no -> {
              Node name = (Node) JosmTransformer.transform(no, ts, tt, ds);
              nodes.add(name);
            });
        ((Way) p).setNodes(nodes);
        break;
      case RELATION:
        p = new Relation();
        List<RelationMember> memberList = new ArrayList<>();
        OSMMember[] members = ((OSMRelation) ent).getMembers();
        Arrays.stream(members)//.parallel()
            .forEach(mem -> {
              //System.out.println(mem.getType() + "   " + mem.getEntity() + "   " + mem.getId() + "   " + ent.getId());
              if (mem.getEntity() != null) {
                OSMEntity byTimestamp = OSHEntities.getByTimestamp(mem.getEntity(), ts);
                if (byTimestamp != null) {
                  OsmPrimitive transform = JosmTransformer.transform(byTimestamp, ts, tt, ds);
                  OSMRole osmRoleOf = tt.getOSMRoleOf(mem.getRoleId());
                  RelationMember rm = new RelationMember(osmRoleOf.toString(), transform);
                  memberList.add(rm);
                }
              }
            });
        ((Relation) p).setMembers(memberList);
        break;
    }
    p.setOsmId(id, version);
    p.setChangesetId((int) changesetId);
    p.setTimestamp(date);
    p.setUser(u);
    p.setVisible(visible);
    p.setKeys(keys);

    //synchronized (JosmTransformer.class) {
    OsmPrimitive primitiveById = ds.getPrimitiveById(p);
    if (primitiveById != null) {
      return primitiveById;
    }
    ds.addPrimitive(p);
    //}
    /*
    try {
      ds.addPrimitive(p);
    } catch (Exception ex) {
      LOG.error("", ex);
    }*/
    return p;
  }

}
