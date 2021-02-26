package ParameterCalculation.helper.parameters;

import java.util.Arrays;

public class VirtualLocation {

  //DataRegion
  private double[] meanAttributeDensity;
  private double[] meanAttributeDiversity;
  private double[] meanElementDensity;
  private double[] meanUsersInLast10Days;

  public VirtualLocation() {
    this.meanAttributeDensity = new double[2];
    this.meanAttributeDiversity = new double[2];
    this.meanElementDensity = new double[2];
    this.meanUsersInLast10Days = new double[2];
  }

  public double[] getMeanAttributeDensity() {
    return meanAttributeDensity;
  }

  public void addMeanAttributeDensity(double newMean) {
    this.meanAttributeDensity[1] = calcNewMean(this.meanAttributeDensity, newMean);
  }

  public double[] getMeanAttributeDiversity() {
    return meanAttributeDiversity;
  }

  public void addMeanAttributeDiversity(double newMean) {
    this.meanAttributeDiversity[1] = calcNewMean(this.meanAttributeDiversity, newMean);
  }

  public double[] getMeanElementDensity() {
    return meanElementDensity;
  }

  public void addMeanElementDensity(double newMean) {
    this.meanElementDensity[1] = calcNewMean(this.meanElementDensity, newMean);
  }

  public double[] getMeanUsersInLast10Days() {
    return meanUsersInLast10Days;
  }

  public void addMeanUsersInLast10Days(double newMean) {
    this.meanUsersInLast10Days[1] = calcNewMean(this.meanUsersInLast10Days, newMean);
  }

  @Override
  public String toString() {
    return "VirtualLocation{" + ", meanAttributeDensity=" + Arrays
        .toString(meanAttributeDensity) + ", meanAttributeDiversity=" + Arrays.toString(
        meanAttributeDiversity) + ", meanElementDensity=" + Arrays.toString(meanElementDensity) + ", meanUsersInLast10Days=" + Arrays
        .toString(meanUsersInLast10Days) + '}';
  }

  void merge(VirtualLocation vl, VirtualLocation vl0) {
    this.meanAttributeDensity = calcNewMean2(vl.meanAttributeDensity, vl0.meanAttributeDensity);
    this.meanAttributeDiversity = calcNewMean2(vl.meanAttributeDiversity, vl0.meanAttributeDiversity);
    this.meanElementDensity = calcNewMean2(vl.meanElementDensity, vl0.meanElementDensity);
    this.meanUsersInLast10Days = calcNewMean2(vl.meanUsersInLast10Days, vl0.meanUsersInLast10Days);

  }

  private double calcNewMean(double[] nx, double x1) {
    return ((nx[0] * nx[1] + x1) / (nx[0] + 1));
  }

  private double[] calcNewMean2(double[] nx, double[] ny) {
    double[] result = new double[2];
    result[1] = ((nx[0] * nx[1] + (ny[0] * ny[1])) / (nx[0] + ny[0]));
    result[0] = (nx[0] + ny[0]);
    return result;
  }

}
