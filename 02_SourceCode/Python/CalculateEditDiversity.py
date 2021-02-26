#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jul 23 14:41:43 2019

@author: moritz
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

editTypesConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
editTypesCur = editTypesConn.cursor()

editTypesStatement="""
select
abstractEdit,
count,
(select sum(count) from aa_mapperhappeningtimeabstractedit where mapperid=? and happeningid=? and timeinterval=?) as total
from aa_MapperHappeningtimeabstractedit
where mapperid=? and happeningid=? and timeinterval=?;
"""



updateConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
updateCur = updateConn.cursor()
updateStatement="Update aa_mapperHappeningTimeMetric set editDiversity=? where mapperid=? and happeningid=? and timeinterval=?;"



for mapper in mapperCur:
    for i in range(-4,5):
        shannon=0.0
        editTypesCur.execute(editTypesStatement,(mapper[0],mapper[1],i,mapper[0],mapper[1],i))
        for editType in editTypesCur:
            p=editType[1]/editType[2]
            shannon+=p*math.log(p)
        shannon=shannon*-1.0
        #print(shannon,mapper[0],mapper[1],i,sep=' ')
        updateCur.execute(updateStatement,(shannon,mapper[0],mapper[1],i))


updateConn.commit()

updateCur.close()
updateConn.close()
mapperCur.close()
mapperConn.close()
editTypesCur.close()
editTypesConn.close()