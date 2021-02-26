package ParameterCalculation.helper.parameters;

import java.util.HashMap;
import java.util.Map;

public class MappingsByRegion {

//Distance to Phys
  private Map<Integer, Integer> mappingByRegion;

  public MappingsByRegion() {
    this.mappingByRegion = new HashMap<>(35);
  }

  public static MappingsByRegion merge(MappingsByRegion vl, MappingsByRegion vl0) {
    MappingsByRegion result = new MappingsByRegion();
    result.mappingByRegion.putAll(vl.mappingByRegion);
    if (vl0 != null) {
      vl0.mappingByRegion.forEach((inta, intb) -> {
        result.mappingByRegion.merge(inta, intb, (intc, intd) -> intc + intd);
      });
    }
    return result;
  }

  public void addMappedRegion(Integer mappedRegion) {
    int ecor = mappedRegion / 1000000;
    this.mappingByRegion.put(ecor * 1000000, 1);
    int cult = (mappedRegion - ecor * 1000000) / 100;
    this.mappingByRegion.put(cult * 100, 1);
    int eco = (mappedRegion - ecor * 1000000 - cult * 100) / 10;
    this.mappingByRegion.put(eco * 10, 1);
    int urb = (mappedRegion - ecor * 1000000 - cult * 100 - eco * 10);
    this.mappingByRegion.put(urb, 1);
  }

  public Map<Integer, Integer> getMappingByRegion() {
    return mappingByRegion;
  }

  @Override
  public String toString() {
    return "MappingsByRegion{" + "mappingByRegion=" + mappingByRegion + '}';
  }

}
