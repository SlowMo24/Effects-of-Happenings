# The Impact of Community Happenings in OpenStreetMap – Establishing a Framework for Online Community Member Activity Analyses
M. Schott, A. Y. Grinberger, S. Lautenbach and A. Zipf

## Supplementary Materials

### 01_InputData

 - Happenings.gpkg
    - Central information collected on the Happenings
 - ParameterRegions.gpkg
    - The combined dataset for the remoteness analyses.
 - Mapper Data
    - The mapper data potentially contains personal information. It is therefore available on request from the authors only.
    
For the orignial Processing all this data was combined in a SQLITE-Database. This database also contains the raw data for each mapper. As this may contain personal information it is only available on request from the author.

### 02-SourceCode
The directory contains the different scripts used to calculate the analysed metrics divided by tool used.

#### GRASS
A GRASS GIS Bash-Script that states all steps taken to preprocess the Physical Location datasets

#### Java
A Maven project holding a package for the CFM user extraction and another package for the mapper metric calculation. The metric calculation package is subdivided by the respective Influence Group and a helper package. The Main-Classes indicate the entry point for the code providing script run on the OSHDB.

#### Python
Holds five scripts that were used to calculate certain user metrics after the data was collected using the Java-code.

#### R
Holds the five scripts used to evaluate the above mentioned SQLite database using statistical tests. There are separate scripts for analyses during events, for newcomers and for advanced mappers.

#### SQL
Holds a SQL script describing how the home region was elected.

### Directory 03-Results 
It contains the complete set of statistical results in .html format. They form the 1:1 result of the R-scripts described above using R-Knitr.

The boxplots created during this evaluation can be found in the subfolder ResultFigures. Please note that this includes boxplots for analyses dimensions that were statisitcally insignificant.
