source('LoadData.R')


## ---- factors
library(dplyr)
library(stats)
with( uniqueMappers , aggregate( MapperChangesetsAtStart , by=list(HappeningType) , FUN=summary))
with( uniqueMappers , aggregate( MapperChangesetsAtStart , by=list(HappeningType) , FUN=sd))


count(independentVnewcomer.mappers,HappeningType)
count(independentVnewcomer.mappers,HappeningType)$n/4
count(independentV.mappers,HappeningType)$n/4



count(uniqueMappers,pop)
count(uniqueMappers,economy)
count(uniqueMappers,cult)
count(uniqueMappers,ecoregion)
summary(uniqueMappers$MapperChangesetsAtStart)
count(uniqueMappers,daysRegistered)