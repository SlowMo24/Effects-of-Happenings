package ParameterCalculation.community;

import java.util.HashMap;
import java.util.Map;

public class ChangesetsResult {

  public int countComments = 0;
  public int countChangeset = 0;
  public Map<String, Integer> wordCountComments = new HashMap<>();
  public Map<String, Integer> wordCountChangesetComment = new HashMap<>();
  public int reviewRequestedAnswered = 0;
  public int reviewRequestedIssued = 0;

}
