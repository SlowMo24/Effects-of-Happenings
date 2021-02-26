package ParameterCalculation.digitalarea;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import ParameterCalculation.helper.mapper.EventUser;

class DigitalAreaUser implements Comparable<DigitalAreaUser>, Serializable {

  private DigitalAreaResult dar;
  private final EventUser eu;

  public DigitalAreaUser(Integer id, Integer eventId, LocalDateTime[] eventTime) {
    this.eu = new EventUser(id, eventId, eventTime);
  }

  @Override
  public int compareTo(DigitalAreaUser o) {
    return this.eu.compareTo(o.eu);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DigitalAreaUser other = (DigitalAreaUser) obj;
    if (!Objects.equals(this.eu, other.eu)) {
      return false;
    }
    return true;
  }

  public DigitalAreaResult getDar() {
    return dar;
  }

  public void setDar(DigitalAreaResult dar) {
    this.dar = dar;
  }

  public EventUser getEu() {
    return eu;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 23 * hash + Objects.hashCode(this.eu);
    return hash;
  }

}
