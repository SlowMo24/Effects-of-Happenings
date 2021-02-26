package ParameterCalculation.helper.time;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;

public enum AnalysesInterval {
  TWO_YEARS_BEFORE(-4),
  ONE_YEAR_BEFORE(-3),
  SIX_MONTH_BEFORE(-2),
  ONE_MONTH_BEFORE(-1),
  DURING(0),
  ONE_MONTH_AFTER(1),
  SIX_MONTH_AFTER(2),
  ONE_YEAR_AFTER(3),
  TWO_YEARS_AFTER(4),
  OUTSIDE(Integer.MAX_VALUE);

  private final int id;

  AnalysesInterval(int id) {
    this.id = id;
  }

  public static AnalysesInterval getForTS(LocalDateTime[] eventTss, OSHDBTimestamp ts) {
    LocalDateTime contribTs = LocalDateTime.ofEpochSecond(ts.getRawUnixTimestamp(), 0,
        ZoneOffset.UTC);
    LocalDateTime start = eventTss[0];
    LocalDateTime end = eventTss[1];
    LocalDateTime minus = start.minusYears(2);
    LocalDateTime plus = end.plusYears(2);

    if (contribTs.compareTo(minus) < 0 || contribTs.compareTo(plus) > 0) {
      return OUTSIDE;
    }
    minus = start.minusYears(1);
    plus = end.plusYears(1);
    if (contribTs.compareTo(minus) < 0) {
      return TWO_YEARS_BEFORE;
    } else if (contribTs.compareTo(plus) > 0) {
      return TWO_YEARS_AFTER;
    }
    minus = start.minusMonths(6);
    plus = end.plusMonths(6);
    if (contribTs.compareTo(minus) < 0) {
      return ONE_YEAR_BEFORE;
    } else if (contribTs.compareTo(plus) > 0) {
      return ONE_YEAR_AFTER;
    }

    minus = start.minusMonths(1);
    plus = end.plusMonths(1);
    if (contribTs.compareTo(minus) < 0) {
      return SIX_MONTH_BEFORE;
    } else if (contribTs.compareTo(plus) > 0) {
      return SIX_MONTH_AFTER;
    } else if (contribTs.compareTo(start) < 0) {
      return ONE_MONTH_BEFORE;
    } else if (contribTs.compareTo(end) > 0) {
      return ONE_MONTH_AFTER;
    }
    return DURING;
  }

  public int getId() {
    return id;
  }

}
