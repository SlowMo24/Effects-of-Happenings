#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jul 23 14:41:43 2019

@author: moritz
"""

import sqlite3

conn2 = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
cur2 = conn2.cursor()

select2="""
select
mapperid,
happeningid
from aa_HappeningMapperJoin;
"""

conn = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
cur = conn.cursor()

select="""
select
complexity,
count
from aa_MapperHappeningtimeComplexedit
where mapperid=? and happeningid=? and timeinterval=?
order by complexity asc;
"""


cur2.execute(select2)

conn3 = sqlite3.connect('MastersThesisSchottDataBase.sqlite')
cur3 = conn3.cursor()

select3="""
update aa_mapperHappeningTimeMetric set medianComplexity=? where mapperid=? and happeningid=? and timeinterval=?;
"""

for row in cur2:
    for i in range(-4,5):
        cur.execute(select,(row[0],row[1],i))
        complexDict={0:0,1:0,2:0,3:0,4:0,5:0}
        median=0
        for row2 in cur:
            complexDict[row2[0]]=row2[1]
        total=sum(complexDict.values())
        prev=0
        for k,v in complexDict.items():
            prev+=v
            if total==0:
                median=0
                break
            elif prev==total/2:
                for j in range(k+1,max(complexDict.keys())):
                    if complexDict[j]>0:
                        median=(k+j)/2
                        break
            elif prev>total/2:
                median =k
                break
        cur3.execute(select3,(median,row[0],row[1],i))


conn3.commit()

cur3.close()
conn3.close()
cur2.close()
conn2.close()
cur.close()
conn.close()