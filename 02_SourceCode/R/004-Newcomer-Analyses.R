source("002-LoadData.R")

library(stats)
library(forcats)
library(effsize)
library(clinfun)
library(ggplot2)
library(reshape2)
library(forcats)

#newcomer recurring, non recurring
print("Newcomers over time:")
timelyAgg <- aggregate(newcomers$quantity[order(fct_rev(newcomers$timeRange))],
                       by=list(newcomers$Mapperid[order(fct_rev(newcomers$timeRange))],
                               newcomers$Happeningid[order(fct_rev(newcomers$timeRange))],
                               newcomers$HappeningType[order(fct_rev(newcomers$timeRange))]),
                       FUN=cumsum)

newcomersOverTime <- aggregate( timelyAgg$x>0, by=list(timelyAgg$Group.3) , FUN=sum)
newcomersOverTime$during <- summary(nonrecurringNewcomers$HappeningType)+newcomersOverTime$V4

1-newcomersOverTime[,-1]/newcomersOverTime[,6]


newcomerPlot <- ggplot(melt(newcomersOverTime),aes(x=rev(variable),group=Group.1,color=Group.1,y=value))+
  geom_line()+
  theme_minimal()+ 
  theme(axis.text.x = element_text(angle = 45, vjust = 1,size = 10, hjust = 1),
        axis.text.y=element_text(size=10))+
  scale_x_discrete(labels=c("during","one month","six months","one year","two years"))+
  scale_colour_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
  labs(x = "Time interval of last contribution",y="Nr. of newcomers contributing",color="Happening type")
print(newcomerPlot)
  
ggsave(path="/home/moritz/Schreibtisch/Masterarbeit/02_Text/figures",
       filename = "newcomerPlot.eps",
       plot=newcomerPlot,
       units="cm",
       width = 16,
       height = 12)

print("for recurring newcomers only: ")

digiArea <- c("element_density","tag_density","user_density","area_diversity")
contribIndepVar <- c("quantity","discussion_size","notes_size")

## statistics on newcomers
for (variableName in names(dependentVnewcomer.changes)){
  print("")
  print("========================================================================")
  print(paste("RESULTS FOR VARIABLE",variableName))
  print("========================================================================")
  print("")
  print("")
  
  jitterplot <- ggplot(dependentVnewcomer.changes,aes(x=independentVnewcomer.mappers$timeRange,
                                                      y=dependentVnewcomer.changes[,variableName]))+
    geom_count(position="jitter",aes(color=independentVnewcomer.mappers$HappeningType))+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(jitterplot)
  
  print("")
  
  lineplot <- ggplot(dependentVnewcomer.changes,aes(x=independentVnewcomer.mappers$timeRange,
                                                    group=interaction(independentVnewcomer.mappers$Mapperid,independentVnewcomer.mappers$Happeningid),
                                                    y=dependentVnewcomer.changes[,variableName]))+
    geom_line(aes(color=independentVnewcomer.mappers$HappeningType))+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(lineplot)
  
  print("")
  print("Boxplot on contributors only: ")
  
  boxplot <- ggplot(dependentVnewcomer.changes[dependentVnewcomer.changes$quantity>0,],
                    aes(x=independentVnewcomer.mappers[dependentVnewcomer.changes$quantity>0,"timeRange"],
                        color=independentVnewcomer.mappers[dependentVnewcomer.changes$quantity>0,"HappeningType"],
                        y=dependentVnewcomer.changes[dependentVnewcomer.changes$quantity>0,variableName]))+
    geom_boxplot()+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(boxplot)
  
  print("")
  
  
  for(i in levels(independentVnewcomer.mappers$timeRange)){
    
    #skip forbidden variables
    if(i=="one month"&any(digiArea==variableName)){
      next
    }
    
    #get data for time interaval and variable
    if(any(contribIndepVar==variableName)){
      variable <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i,variableName]
      classes <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i,"HappeningType"]
    }else{
      variable <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i&dependentVnewcomer.changes$quantity>0,variableName]
      classes <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&dependentVnewcomer.changes$quantity>0,"HappeningType"]
    }

    
    print("")
    print(paste("Time frame: ",i))
    print("")
    print("N:")
    print(summary(classes))
    print("")
    print(paste("Summary Statistics for variable ",variableName))
    print(aggregate(variable,by=list(classes),FUN=summary))
    print("")
    
    #kruskal test
    kruskal <- kruskal.test(x=variable,g=classes)
    if(!is.nan(kruskal$p.value) & kruskal$p.value<=0.05){

      print("")
      print(paste(variableName,"is significantly influenced by an event for",i))
      print("")
      print("")
      
      # wilcox pariwise post hoc
      #http://www.sthda.com/english/wiki/kruskal-wallis-test-in-r
      wilcox <- pairwise.wilcox.test(x=variable,g=classes,p.adjust.method = "BH")
      print("Pairwise comparison: ")
      print(wilcox$p.value)
      
      
      # mode IVs for CFM
      if(wilcox$p.value["CFM","CG"]<=0.05){
        print(paste("Cohends d for effect size of the CFM on",variableName,":"))
        print(cohen.d(d=variable[classes!="CRM"],f=fct_drop(classes[classes!="CRM"])))
        print("")
        
        if(any(contribIndepVar==variableName)){
          variable1 <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i&
                                                    independentVnewcomer.mappers$HappeningType=="CFM",variableName]
          # economic status
	        classes1 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
	                                                   independentVnewcomer.mappers$HappeningType=="CFM","economic_status"]
        }else{
          variable1 <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i&
                                                    dependentVnewcomer.changes$quantity>0&
                                                    independentVnewcomer.mappers$HappeningType=="CFM",variableName]
          # economic status
          classes1 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
                                                     dependentVnewcomer.changes$quantity>0&
                                                     independentVnewcomer.mappers$HappeningType=="CFM","economic_status"]
        }
        
        
	      jockey <- jonckheere.test(variable1,classes1,nperm=1000)
	      print("N:")
	      print(summary(classes1))
	      print("")
	      print("Summary Statistics: ")
	      print(aggregate(variable1,by=list(classes1),FUN=summary))
	      print("")
        if(jockey$p.value<=0.05){
          wilcox2 <- pairwise.wilcox.test(x=variable1,g=classes1,p.adjust.method = "BH")
          if(any(wilcox2$p.value<=0.05,na.rm=TRUE)){
            print("Pairwise comparison: ")
            print(wilcox2$p.value)
            print(paste("Analyses of the effect of the economic status for CFM with a p-value of",jockey$p.value))
            { sink("/dev/null"); print(plot(x=classes1,
                                            y=variable1,
                                            main="The economic status has an effect",
                                            sub=paste("on this variable (",variableName,") for CFM"))); 
              sink(); }
            print(plot.new())
            print("")
          }
        }
        
        #culture
	      if(any(contribIndepVar==variableName)){
          classes1 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
                                                   independentVnewcomer.mappers$HappeningType=="CFM","culture"]
	      }else{
	        classes1 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
	                                                   dependentVnewcomer.changes$quantity>0&
	                                                   independentVnewcomer.mappers$HappeningType=="CFM","culture"]
	      }
        classes1 <- fct_drop(fct_lump_min(classes1,min=10))

        print("N:")
        print(summary(classes1))
        print("")
        print("Summary Statistics: ")
        print(aggregate(variable1,by=list(classes1),FUN=summary))
        print("")
        
        if(length(levels(classes1))>1){
          kruskal2 <- kruskal.test(x=variable1,g=classes1)
          if(!is.nan(kruskal2$p.value) & kruskal2$p.value<=0.05){
            wilcox2 <- pairwise.wilcox.test(x=variable1,g=classes1,p.adjust.method = "BH")
            if(any(wilcox2$p.value<=0.05,na.rm=TRUE)){
              print("")
              print(paste(variableName,"is significantly influenced by at least one culture for CFM with p=",kruskal2$p.value))
              print("")
              print("Pairwise comparison: ")
              print(wilcox2$p.value)
              { sink("/dev/null"); print(plot(classes1,variable1,main="The culture has an effect",
                                              sub=paste("on this variable (",variableName,") for CFM"))); sink(); }
              print(plot.new())
              print("")
            }
          }
        }
        
        
        
        
      }
      
      # other IVs for CRM
      if(wilcox$p.value["CRM","CG"]<=0.05){
        print(paste("Cohends d for effect size of the CRM on",variableName,":"))
        print(cohen.d(d=variable[classes!="CFM"],f=fct_drop(classes[classes!="CFM"])))
        print("")
        
        if(any(contribIndepVar==variableName)){
        variable2 <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i&
                                                  independentVnewcomer.mappers$HappeningType=="CRM",variableName]
        
        ## distance during event
        classes2 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
                                                   independentVnewcomer.mappers$HappeningType=="CRM","event_mapping_distance"]
        }else{
          variable2 <- dependentVnewcomer.changes[independentVnewcomer.mappers$timeRange==i&
                                                    dependentVnewcomer.changes$quantity>0&
                                                    independentVnewcomer.mappers$HappeningType=="CRM",variableName]
          
          ## distance during event
          classes2 <- independentVnewcomer.mappers[independentVnewcomer.mappers$timeRange==i&
                                                     dependentVnewcomer.changes$quantity>0&
                                                     independentVnewcomer.mappers$HappeningType=="CRM","event_mapping_distance"]
        }
        print("N:")
        print(length(classes2))
        print("")
        model <- lm(variable2~classes2)
        if(any(summary(model)$coefficients[2,4]<=0.05,na.rm = TRUE)){
          print("Analyses of the effect of the distance to the Region mapped during the event for CRM")
          { sink("/dev/null"); print(plot(classes2,variable2,log="x",main="The event mapping distance has an effect",
                                          sub=paste("on this variable (",variableName,") for CRM"))); sink(); }
          print(abline(model))
          print(plot.new())
          print(summary(model))
          print("")
        }
      }
      
      # between happenings
      if(!is.nan(wilcox$p.value["CRM","CFM"]) & wilcox$p.value["CRM","CFM"]<=0.05){
        print(paste("Cohends d for effect size between CRM and CFM on",variableName,":"))
        print(cohen.d(d=variable[classes!="CG"],f=fct_drop(classes[classes!="CG"])))
        print("")
      }
    }
    print("")
    print("")
    print("--------------------------------------------------------------------------------")
    print("")
    print("")
  }
}

#rmarkdown::render("051-Newcomer-Analyses.R", "html_document")

