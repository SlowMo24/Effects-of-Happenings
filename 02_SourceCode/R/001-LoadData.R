## ---- prepare
library(DBI)
library(ggplot2)
library(stats)

#connect to db
con  <-  dbConnect(RSQLite::SQLite(), "MastersThesisSchottDatabase.sqlite")

# read data
valuesPerMapper <- dbReadTable(con,"aa_MapperTimeParameter")
valuesPerNewcomer <- dbReadTable(con,"aa_NewcomerTimeParameter")
valuesDuring <- dbReadTable(con,"aa_DuringTimeParameter")

dbDisconnect(con)

# clean Data
## subset
validMappers <- subset(valuesPerMapper,fullTimeRegistered==1)
newcomers <- subset(valuesPerNewcomer,fullTimeRegistered==0&timeRange==1)
newcomers <- subset(valuesPerNewcomer,match(interaction(Mapperid,Happeningid),interaction(newcomers$Mapperid,newcomers$Happeningid),nomatch = 0)>0)


## create factors
validMappers$timeRange <-factor(ordered=TRUE,levels=c("1","2","3","4"),validMappers$timeRange,labels=c("one month","six months","one year","two years"))
validMappers$HappeningType <- relevel(factor(validMappers$HappeningType,labels=c("CFM","CRM","CG")),"CG")
validMappers$economic_status <-factor(ordered=TRUE,validMappers$economic_status,levels=c("low income","lower middle income","upper middle income","high income"))
validMappers$culture <- factor(validMappers$culture)
validMappers$population_density <- factor(ordered=TRUE,validMappers$population_density,levels=c("sparsely populated","rural areas","urban clusters","urban centres"))
validMappers$physical_geography <- factor(validMappers$physical_geography)

newcomers$timeRange <-factor(ordered=TRUE,levels=c("1","2","3","4"),newcomers$timeRange,labels=c("one month","six months","one year","two years"))
newcomers$HappeningType <- relevel(factor(newcomers$HappeningType,labels=c("CFM","CRM","CG")),"CG")
newcomers$economic_status <-factor(ordered=TRUE,newcomers$economic_status,levels=c("low income","lower middle income","upper middle income","high income"))
newcomers$culture <- factor(newcomers$culture)
newcomers$population_density <- factor(ordered=TRUE,newcomers$population_density,levels=c("sparesely populated","rural areas","urban clusters","urban centres"))
newcomers$physical_geography <- factor(newcomers$physical_geography)

valuesDuring$timeRange <-factor(ordered=TRUE,levels=c("0"),valuesDuring$timeRange,labels=c("during"))
valuesDuring$HappeningType <- relevel(factor(valuesDuring$HappeningType,labels=c("CFM","CRM","CG")),"CG")
valuesDuring$economic_status <-factor(ordered=TRUE,valuesDuring$economic_status,levels=c("low income","lower middle income","upper middle income","high income"))
valuesDuring$culture <- factor(valuesDuring$culture)
valuesDuring$population_density <- factor(ordered=TRUE,valuesDuring$population_density,levels=c("sparesely populated","rural areas","urban clusters","urban centres"))
valuesDuring$physical_geography <- factor(valuesDuring$physical_geography)

valuesDuring$event_mapping_distance <- valuesDuring$population_density_distance + valuesDuring$economic_distance + valuesDuring$cultural_distance + valuesDuring$physical_geography_distance
validMappers$event_mapping_distance <-  merge(validMappers,valuesDuring[,c(1:2,25)])$event_mapping_distance

newcomers$event_mapping_distance <-  merge(newcomers,valuesDuring[,c(1:2,25)])$event_mapping_distance


## ---- outliers

############# ===> newcomers werden beschnitten!
newcomers <- subset(newcomers,daysRegistered<=3,skill<=10)

## Newcomer die nicht wiederkehren filtern
reccurring<- aggregate(newcomers$quantity,by=list(newcomers$Mapperid,newcomers$Happeningid),FUN=sum)
reccurring <- reccurring[reccurring$x>0,]

nonrecurringNewcomers <- subset(subset(newcomers,match(interaction(Mapperid,Happeningid),interaction(reccurring$Group.1,reccurring$Group.2),nomatch = 0)==0),timeRange=="one month")

newcomers <- subset(newcomers,match(interaction(Mapperid,Happeningid),interaction(reccurring$Group.1,reccurring$Group.2),nomatch = 0)>0)

## ---- seperateData
library(forcats)
dependentV.changes <- validMappers[,-c(1:13,32)]
independentV.mappers <- cbind(validMappers[,c(1:3,5:13,32)])
uniqueMappers <- unique(independentV.mappers[,c(1,4:10)])


dependentVnewcomer.changes <- newcomers[,-c(1:11,30)]
independentVnewcomer.mappers <- cbind(newcomers[,c(1:3,5:11,30)])
uniqueNewcomers <- unique(independentVnewcomer.mappers[,c(1,4:10)])

dependentVduring.changes <- valuesDuring[,-c(1:10,25)]
independentVduring.mappers <- cbind(valuesDuring[,c(1:10,25)])