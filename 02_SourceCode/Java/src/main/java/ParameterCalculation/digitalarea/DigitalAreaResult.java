package ParameterCalculation.digitalarea;

import java.util.Arrays;

class DigitalAreaResult {

  public int nrOfElements = 0;
  public int sumOfTags = 0;
  public final int[] nrPrimaryKeys = new int[27];
  public int sumOfMappers = 0;
  public double area = 0.0;

  public void addNrPrimaryKeys(int[] nrPrimaryKeys) {
    for (int i = 0; i < 27; i++) {
      this.nrPrimaryKeys[i] += nrPrimaryKeys[i];
    }
  }

  @Override
  public String toString() {
    return "DigitalAreaResult{" + "nrOfElements=" + nrOfElements + ", sumOfTags=" + sumOfTags + ", nrPrimaryKeys=" + Arrays.toString(nrPrimaryKeys) + ", sumOfMappers=" + sumOfMappers + ", area=" + area + '}';
  }

}
