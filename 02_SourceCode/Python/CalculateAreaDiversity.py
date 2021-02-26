#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""

@author: Schott
"""

import sqlite3
import math

mapperConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
mapperCur = mapperConn.cursor()

mapperStatement="""
select
mapperid,
happeningid
from aa_HappeningMapperJoin;
"""
mapperCur.execute(mapperStatement)

editAreaFeatureConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
editAreaFeatureCur = editAreaFeatureConn.cursor()

editAreaFeatureStatement="""
select
abstractFeature,
numberOfelements,
(select sum(numberOfelements) from aa_mapperhappeningtimeuniqefeature where mapperid=? and happeningid=? and timeinterval=?) as total
from aa_mapperhappeningtimeuniqefeature
where mapperid=? and happeningid=? and timeinterval=?;
"""



updateConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
updateCur = updateConn.cursor()
updateStatement="Update aa_mapperHappeningTimeMetric set editAreaDiversity=? where mapperid=? and happeningid=? and timeinterval=?;"



for mapper in mapperCur:
    for i in range(-4,5):
        shannon=0.0
        editAreaFeatureCur.execute(editAreaFeatureStatement,(mapper[0],mapper[1],i,mapper[0],mapper[1],i))
        for editAreaFeature in editAreaFeatureCur:
            p=editAreaFeature[1]/editAreaFeature[2]
            shannon+=p*math.log(p)
        shannon=shannon*-1.0
        #print(shannon,mapper[0],mapper[1],i,sep=' ')
        updateCur.execute(updateStatement,(shannon,mapper[0],mapper[1],i))


updateConn.commit()

updateCur.close()
updateConn.close()
mapperCur.close()
mapperConn.close()
editAreaFeatureCur.close()
editAreaFeatureConn.close()