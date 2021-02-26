package ParameterCalculation.experience;

class EditComplexity {

  //0 deletion, 1 point, 2 line <10 vert , 3 line >10 vert, 4 realtion, 5 multipolygon, route, boundary, waterway
  int comp;

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
    final EditComplexity other = (EditComplexity) obj;
    if (this.comp != other.comp) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 53 * hash + this.comp;
    return hash;
  }

  @Override
  public String toString() {
    return "EditComplexity{" + "comp=" + comp + '}';
  }

}
