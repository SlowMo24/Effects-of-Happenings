#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jul 25 08:43:46 2019

@author: moritz
"""


import sqlite3
import psycopg2
from datetime import datetime

mapperConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
mapperCur = mapperConn.cursor()

mapperStatement="""
select
mapperid,
happeningid,
startdate_UTC||' '||starttime_UTC
from aa_HappeningMapperJoin;
"""

mapperCur.execute(mapperStatement)

changesetsConn = psycopg2.connect(host="host",database="changesets", user="user", password="pw")
changesetsCurser = changesetsConn.cursor()

changesetsQuery="""
select
count(*)
 from osm_changeset
 where created_at< %s and user_id=%s;
"""

updateConn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
updateCur = updateConn.cursor()
updateStatement="Update aa_happeningmapperrelation set mapperchangesetsatstart=? where mapperid=? and happeningid=?;"


for mapper in mapperCur:
    startObject = datetime.strptime(mapper[2], '%Y-%m-%d %H:%M:%S')
    changesetsCurser.execute(changesetsQuery,(startObject,mapper[0]))
    summe=changesetsCurser.fetchall();
    print(summe[0][0])
    updateCur.execute(updateStatement,(summe[0][0],mapper[0],mapper[1]))


updateConn.commit()

updateCur.close()
updateConn.close()
changesetsCurser.close()
changesetsConn.close()
mapperCur.close()
mapperConn.close()
