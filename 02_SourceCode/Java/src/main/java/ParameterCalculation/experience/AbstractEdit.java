package ParameterCalculation.experience;

import java.util.BitSet;
import java.util.Objects;

class AbstractEdit {

  //se primary keys
  public final BitSet primaryKeyTouched = new BitSet(27);
  //0 #vertices increased, 1 #vertices decreased, 2 #verices equal
  public final BitSet typeGeomChange = new BitSet(3);
  //0 add, 1 remove, 2 change
  public final BitSet typeTagChange = new BitSet(3);
  //0 c, 1 other, 2 d
  public final BitSet cOrD = new BitSet(3);

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
    final AbstractEdit other = (AbstractEdit) obj;
    if (!Objects.equals(this.primaryKeyTouched, other.primaryKeyTouched)) {
      return false;
    }
    if (!Objects.equals(this.typeGeomChange, other.typeGeomChange)) {
      return false;
    }
    if (!Objects.equals(this.typeTagChange, other.typeTagChange)) {
      return false;
    }
    if (!Objects.equals(this.cOrD, other.cOrD)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 53 * hash + Objects.hashCode(this.primaryKeyTouched);
    hash = 53 * hash + Objects.hashCode(this.typeGeomChange);
    hash = 53 * hash + Objects.hashCode(this.typeTagChange);
    hash = 53 * hash + Objects.hashCode(this.cOrD);
    return hash;
  }

  @Override
  public String toString() {
    return "AbstractEdit{" + "primaryKeyTouched=" + primaryKeyTouched + ", typeGeomChange=" + typeGeomChange + ", typeTagChange=" + typeTagChange + ", cOrD=" + cOrD + '}';
  }

}
