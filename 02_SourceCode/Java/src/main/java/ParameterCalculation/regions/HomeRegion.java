/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ParameterCalculation.regions;

import ParameterCalculation.regions.HomeEvaluator;
import java.sql.SQLException;
import org.locationtech.jts.io.ParseException;

/**
 *
 * @author Moritz Schott <m.schott@stud.uni-heidelberg.de>
 */
public class HomeRegion {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
      throws SQLException, ParseException, ClassNotFoundException, Exception {
    HomeEvaluator.evaluate(HomeRegion.getRegionGeomSQL(),
        "Insert into  aa_MapperRegionRelation(mapperId,regionId,nredits) values(?,?,?);");
  }

  public static String getRegionGeomSQL() {
    return "SELECT ogc_fid,cat,ST_AsBinary(geometry) FROM aa_parameterregions;";
  }

}
