package ParameterCalculation.experience;

import java.util.HashMap;
import java.util.Map;
import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.time.AnalysesInterval;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class ExperienceResult {
  public EventUser eu;
  public AnalysesInterval ai;

  public int contribCount = 0;
  public final Map<ContributionType, Integer> countPerType = new HashMap<>();
  public final Map<AbstractEdit,Integer> ae=new HashMap<>();
  public final Map<EditComplexity,Integer> ec=new HashMap<>();

}
