package ParameterCalculation.helper.tags;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.exceptions.OSHDBKeytablesNotFoundException;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

public class TagHandler {

  public static Map<Integer, Integer> getPrimaryKeys(Connection conn)
      throws OSHDBKeytablesNotFoundException {
    Map<Integer, String> primaryKeysMap = getPrimaryKeysMap();
    TagTranslator tt = new TagTranslator(conn);
    Map<Integer, Integer> result = new HashMap<>(27);
    for (Map.Entry<Integer, String> pk : primaryKeysMap.entrySet()) {
      result.put(tt.getOSHDBTagKeyOf(pk.getValue()).toInt(), pk.getKey());
    }

    return result;
  }

  public static Map<OSHDBTag, Integer> getRelationValues(Connection conn)
      throws OSHDBKeytablesNotFoundException {
    Map<Integer, String> primaryKeysMap = getRelationValueMap();
    TagTranslator tt = new TagTranslator(conn);
    Map<OSHDBTag, Integer> result = new HashMap<>(27);
    for (Map.Entry<Integer, String> pk : primaryKeysMap.entrySet()) {
      result.put(tt.getOSHDBTagOf("type", pk.getValue()), pk.getKey());
    }

    return result;
  }

  public static Map<Integer, String> getRelationValueMap() {

    //steht jetzt auch in der sqlite-db und könnte dort gelesen werden...
    Map<Integer, String> result = new HashMap<>();
    result.put(0, "multipolygon");
    result.put(1, "route");
    result.put(2, "boundary");
    result.put(3, "waterway");
    return result;
  }

  public static Map<Integer, String> getPrimaryKeysMap() {

    //steht jetzt auch in der sqlite-db und könnte dort gelesen werden...
    Map<Integer, String> result = new HashMap<>();
    result.put(0, "aerialway");
    result.put(1, "aeroway");
    result.put(2, "amenity");
    result.put(3, "barrier");
    result.put(4, "boundary");
    result.put(5, "building");
    result.put(6, "craft");
    result.put(7, "emergency");
    result.put(8, "geological");
    result.put(9, "highway");
    result.put(10, "historic");
    result.put(11, "landuse");
    result.put(12, "leisure");
    result.put(13, "man_made");
    result.put(14, "military");
    result.put(15, "natural");
    result.put(16, "office");
    result.put(17, "place");
    result.put(18, "power");
    result.put(19, "public_transport");
    result.put(20, "railway");
    result.put(21, "route");
    result.put(22, "shop");
    result.put(23, "sport");
    result.put(24, "telecom");
    result.put(25, "tourism");
    result.put(26, "waterway");
    return result;
  }

}
