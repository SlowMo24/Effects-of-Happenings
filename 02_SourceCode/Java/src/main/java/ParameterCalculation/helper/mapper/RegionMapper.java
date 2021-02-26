package ParameterCalculation.helper.mapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import ParameterCalculation.helper.parameters.MappingsByRegion;
import ParameterCalculation.helper.time.AnalysesInterval;

public class RegionMapper extends EventUser {

  private Map<AnalysesInterval, MappingsByRegion> mappedRegions;

  public RegionMapper(Integer id, Integer eventId, LocalDateTime[] eventTime) {
    super(id, eventId, eventTime);
    mappedRegions = new HashMap<>(AnalysesInterval.values().length);
    for (AnalysesInterval ai : AnalysesInterval.values()) {
      mappedRegions.put(ai, new MappingsByRegion());
    }
  }

  public Map<AnalysesInterval, MappingsByRegion> getMappedRegions() {
    return mappedRegions;
  }

  public void addMappedRegion(AnalysesInterval ai, Integer mappedRegion) {
    mappedRegions.computeIfAbsent(ai, ai2 -> new MappingsByRegion()).addMappedRegion(mappedRegion);
  }

  public void setMappedRegion(AnalysesInterval ai, MappingsByRegion mappedRegion) {
    mappedRegions.put(ai, mappedRegion);
  }

}
