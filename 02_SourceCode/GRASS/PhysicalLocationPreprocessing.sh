#!/bin/sh


#general
grass -c epsg:4326 Path/to/Grassdb

#worldborders
v.in.ogr --overwrite --verbose input=/path/to/worldborders/ne_10m_admin_0_countries.shp output=worldBorders snap=0.001

#ClipLayer
v.dissolve --overwrite --verbose input=worldBorders column=featurecla output=continentalCrust

#economy
v.db.update --verbose map=worldBorders column=INCOME_GRP value='1. High income: OECD' where='INCOME_GRP like "2. High income: nonOECD"'
v.dissolve --overwrite --verbose input=worldBorders column=INCOME_GRP output=economy_dis

#ecoregions
v.in.ogr input=/path/to/ecoregions/wwf_terr_ecos.shp output=ecoregions snap=0.001
v.db.addcolumn --verbose map=ecoregions columns="biome2 integer"
v.db.update --verbose map=ecoregions column=biome2 query_column="CAST(BIOME AS integer)"
v.dissolve --overwrite --verbose input=ecoregions column=biome2 output=ecoregions_dis


#Eckert IV
#g.proj -c location=EckertIV proj4='+proj=eck4 +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs'
#g.mapset mapset=PERMANENT location=EckertIV

#proj
#v.proj --overwrite --verbose location=WGS84 mapset=PERMANENT input=continentalCrust
#v.proj --overwrite --verbose location=WGS84 mapset=PERMANENT input=economy_dis
#v.proj --overwrite --verbose location=WGS84 mapset=PERMANENT input=ecoregions_dis
#v.proj --overwrite --verbose location=WGS84 mapset=PERMANENT input=worldBorders

#urbanisation
#gdalwarp -overwrite -t_srs '+proj=eck4 +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs' -r near -of GTiff GHS_SMOD_POP2015_GLOBE_R2016A_54009_1k_v1_0.tif pop_proj.tif
gdalwarp -overwrite -t_srs EPSG:4326 -r near -of GTiff /path/to/popdensity/GHS_SMOD_POP2015_GLOBE_R2016A_54009_1k_v1_0.tif ./pop_proj.tif
r.in.gdal --overwrite --verbose input=./pop_proj.tif output=pop
g.region raster=pop
r.mask --overwrite --verbose vector=continentalCrust

#"culture"
#v.in.ogr input=CulturalAreas.shp output=cult
v.in.ogr input=/path/to/huntington/CulturalAreas_WGS84.shp output=cult snap=0.001
v.clean -c --overwrite --verbose input=cult output=cult_clean error=cult_clean_err tool=rmarea threshold=5000000000
v.generalize --overwrite --verbose input=cult_clean output=cult_clean_gen method=reduction threshold=0.2

#121km²
r.neighbors -c --overwrite --verbose input=pop output=pop_mod method=mode size=11
r.to.vect -v --overwrite --verbose input=pop_mod output=pop_resamp type=area
#rm area ca Heidelberg
v.clean -c --overwrite --verbose input=pop_resamp output=pop_resamp_clean error=pop_resamp_clean_err tool=rmarea threshold=100000000
v.generalize --overwrite --verbose input=pop_resamp_clean output=pop_resamp_clean_gen method=reduction threshold=0.2



#rm area
v.clean -c --overwrite --verbose input=ecoregions_dis output=ecoregions_dis_clean error=ecoregions_dis_clean_err tool=rmarea threshold=5000000000
v.generalize --overwrite --verbose input=ecoregions_dis_clean output=ecoregions_dis_clean_gen method=reduction threshold=0.2
#see ecoregions
v.clean -c --overwrite --verbose input=economy_dis output=economy_dis_clean error=economy_dis_clean_err tool=rmarea threshold=5000000000
v.generalize --overwrite --verbose input=economy_dis_clean output=economy_dis_clean_gen method=reduction threshold=0.2




#intersect
v.overlay --overwrite --verbose ainput=economy_dis_clean_gen binput=cult_clean_gen operator=or output=overlayA snap=0.01
v.overlay --overwrite --verbose ainput=overlayA binput=ecoregions_dis_clean_gen operator=or output=overlayB snap=0.0001
v.overlay --overwrite --verbose ainput=overlayB binput=pop_resamp_clean_gen operator=or output=overlayC snap=0.0001



# rename columns
v.db.renamecolumn --verbose map=overlayC column=a_a_a_cat,economyRank
v.db.renamecolumn --verbose map=overlayC column=a_a_b_cat,culture
v.db.renamecolumn --verbose map=overlayC column=a_b_cat,ecoregion
v.db.renamecolumn --verbose map=overlayC column=b_cat,popRank

#
v.db.dropcolumn --verbose map=overlayC columns=a_cat,a_a_cat,a_a_a_INCOME_GRP,a_a_b_culturalAr,a_a_b_culArNum,b_label

v.edit --verbose map=overlayC tool=delete where='economyRank IS NULL OR ecoregion IS NULL OR culture IS NULL OR popRank IS NULL'


#
#v.db.update --verbose map=overlayC column=popRank value=0 where="popRank IS NULL"
# concat string from columns
v.db.addcolumn --verbose map=overlayC columns="combine integer"
v.db.update --verbose map=overlayC column=combine query_column="1000000 * ecoregion + 100 * culture + 10 * economyRank + popRank"

v.dissolve --overwrite --verbose input=overlayC column=combine output=overlayC_dis

v.clean -c --overwrite --verbose input=overlayC_dis output=overlayC_dis_clean tool=rmarea threshold=10000000
v.generalize --overwrite --verbose input=overlayC_dis_clean output=overlayC_dis_clean_gen method=reduction threshold=0.2

g.region n=90 s=-90 e=180 w=-180
v.in.region --overwrite --verbose output=globalRegion
v.overlay --overwrite --verbose ainput=overlayC_dis_clean_gen binput=globalRegion operator=xor output=ocean snap=0.00001
v.overlay --overwrite --verbose ainput=overlayC_dis_clean_gen binput=ocean operator=or output=finalDataset snap=0.00001

v.db.renamecolumn --verbose map=finalDataset column=a_cat,region
v.db.renamecolumn --verbose map=finalDataset column=a_area,area
v.db.dropcolumn --verbose map=finalDataset columns=b_cat,b_a_cat,b_b_cat

v.db.update --verbose map=finalDataset column=region query_column="1000000 * 100 + 100 * 10" where="region IS NULL"

v.dissolve --overwrite --verbose input=finalDataset column=region output=finalDataset_dis

v.out.ogr -m --verbose input=finalDataset_dis output='FinalOverlay.sqlite' format=SQLite output_layer="parameterRegions" dsco='SPATIALITE=yes'

v.out.ogr -a -u -m --verbose input=finalDataset_dis output='OutDatabase.sqlite' format=SQLite output_layer="aa_parameterRegions" dsco='SPATIALITE=yes'

