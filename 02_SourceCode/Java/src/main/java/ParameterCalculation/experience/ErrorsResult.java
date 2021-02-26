package ParameterCalculation.experience;

import ParameterCalculation.helper.mapper.EventUser;
import ParameterCalculation.helper.time.AnalysesInterval;

public class ErrorsResult {

  public EventUser eu;
  public AnalysesInterval inter;
  public int errorDelta = 0;
  public int untouchedErrors = 0;
  public boolean tureThing=false;

  @Override
  public String toString() {
    return "ErrorsResult{" + "eu=" + eu + ", inter=" + inter + ", sum=" + errorDelta + ", untouched=" + untouchedErrors + '}';
  }

}
