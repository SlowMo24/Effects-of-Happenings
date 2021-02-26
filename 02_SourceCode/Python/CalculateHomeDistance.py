#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jul 23 14:41:43 2019

@author: moritz
"""

import sqlite3

mapperConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
mapperCur = mapperConn.cursor()

mapperStatement="""
select
mapperid,
happeningid,
pop,
economy,
cult,
ecoregion
from aa_HappeningMapperJoin
join aa_parameterregions on homeregion = cat;
"""
mapperCur.execute(mapperStatement)

regionEditConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
regionEditCur = regionEditConn.cursor()

regionEditStatement="""
select
regionid,
nrcontribs,
type
from aa_MapperHappeningregiontime
join aa_parameterProps on regionid=aa_parameterprops.id
where mapperid=? and happeningid=? and timeinterval=?;
"""



updateConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
updateCur = updateConn.cursor()



for mapper in mapperCur:
    homeregion={}
    homeregion["pop"]=mapper[2]
    homeregion["economy"]=mapper[3]
    homeregion["cult"]=mapper[4]
    homeregion["ecoregion"]=mapper[5]
    for i in range(-4,5):
        regiontypes={}
        regionEditCur.execute(regionEditStatement,(mapper[0],mapper[1],i))
        for regionEdit in regionEditCur:
            distdict=regiontypes.get(regionEdit[2],{"dist":0,"n":0})
            distdict["n"]=distdict["n"]+regionEdit[1]
            if regionEdit[0]!=homeregion[regionEdit[2]]:
                distdict["dist"]=distdict["dist"]+regionEdit[1]
            regiontypes[regionEdit[2]]=distdict
        for k,v in regiontypes.items():
            mean=v["dist"]/v["n"]
            #print(mean,"mean"+k+"dist",mapper[0],mapper[1],i,sep=' ')
            updateStatement="Update aa_mapperHappeningTimeMetric set mean"+k+"dist=? where mapperid=? and happeningid=? and timeinterval=?;"
            updateCur.execute(updateStatement,(mean,mapper[0],mapper[1],i))


updateConn.commit()

updateCur.close()
updateConn.close()
mapperCur.close()
mapperConn.close()
regionEditCur.close()
regionEditConn.close()