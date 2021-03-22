# Community Happening analyses in OSM

Supplementary materials for the publication

---
**Schott, M.; Grinberger, A. Y.; Lautenbach, S. & Zipf, A.** (2021) _The Impact of Community Happenings in OpenStreetMap – Establishing a Framework for Online Community Member Activity Analyses_ In: ISPRS Int. J. Geo-Inf. 10, no. 3: 164. https://doi.org/10.3390/ijgi10030164

---

and the Masters Thesis

___
**Schott, M.** (2019) _Unter the Spell of Cummunity Happenings - Analysing the Effects of Mapping Events on OpenStreetMap Contributors_ (available from the author)
___

## 01_InputData

 - Happenings.gpkg
    - Central information collected on the Happenings
 - ParameterRegions.gpkg
    - The combined dataset for the remoteness analyses.
 - Mapper Data
    - The mapper data potentially contains personal information. It is therefore available on request from the authors only.
    
For the orignial Processing all this data was combined in a SQLITE-Database. This database also contains the raw data for each mapper. As this may contain personal information it is only available on request from the author.

## 02-SourceCode
The directory contains the different scripts used to calculate the analysed metrics divided by tool used.

### GRASS
A GRASS GIS Bash-Script that states all steps taken to preprocess the Physical Location datasets

### Java
A Maven project holding a package for the CFM user extraction and another package for the mapper metric calculation. The metric calculation package is subdivided by the respective Influence Group and a helper package. The Main-Classes indicate the entry point for the code providing script run on the OSHDB.

### Python
Holds five scripts that were used to calculate certain user metrics after the data was collected using the Java-code.

### R
Holds the five scripts used to evaluate the above mentioned SQLite database using statistical tests. There are separate scripts for analyses during events, for newcomers and for advanced mappers.

### SQL
Holds a SQL script describing how the home region was elected.

## Directory 03-Results 
It contains the complete set of statistical results in .html format. They form the 1:1 result of the R-scripts described above using R-Knitr.

The boxplots created during this evaluation can be found in the subfolder ResultFigures. Please note that this includes boxplots for analyses dimensions that were statisitcally insignificant.
