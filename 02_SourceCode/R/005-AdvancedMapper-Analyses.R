source("002-LoadData.R")

library(stats)
library(forcats)
library(effsize)
library(clinfun)
library(ggplot2)
library(reshape2)



digiArea <- c("element_density","tag_density","user_density","area_diversity")
contribIndepVar <- c("quantity","discussion_size","notes_size")

## statistics on seniors
for (variableName in names(dependentV.changes)){
  print("")
  print("========================================================================")
  print(paste("RESULTS FOR VARIABLE",variableName))
  print("========================================================================")
  print("")
  print("")
  
  jitterplot <- ggplot(dependentV.changes,aes(x=independentV.mappers$timeRange,
                                              y=dependentV.changes[,variableName]))+
    geom_count(position="jitter",aes(color=independentV.mappers$HappeningType))+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(jitterplot)
  
  print("")
  
  lineplot <- ggplot(dependentV.changes,aes(x=independentV.mappers$timeRange,
                                            group=interaction(independentV.mappers$Mapperid,independentV.mappers$Happeningid),
                                            y=dependentV.changes[,variableName]))+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    geom_line(aes(color=independentV.mappers$HappeningType))+
    scale_y_continuous(trans='pseudo_log')
  print(lineplot)
  
  print("")
  print("Boxplot on contributors only: ")
  
  boxplot <- ggplot(dependentV.changes[independentV.mappers$contribBefore>0&
                                         independentV.mappers$contribAfter>0,],
                    aes(x=independentV.mappers[independentV.mappers$contribBefore>0&
                                                 independentV.mappers$contribAfter>0,"timeRange"],
                        color=independentV.mappers[independentV.mappers$contribBefore>0&
                                                     independentV.mappers$contribAfter>0,"HappeningType"],
                        y=dependentV.changes[independentV.mappers$contribBefore>0&
                                               independentV.mappers$contribAfter>0,variableName]))+
    geom_boxplot()+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(boxplot)
  
  print("")
  
  for(i in levels(independentV.mappers$timeRange)){
    
    #skip forbidden variables
    if(i=="one month"&any(digiArea==variableName)){
      next
    }
    
    #get data for time interaval and variable
    if(any(contribIndepVar==variableName)){
      variable <- dependentV.changes[independentV.mappers$timeRange==i,variableName]
      classes <- independentV.mappers[independentV.mappers$timeRange==i,"HappeningType"]
    }else{
      variable <- dependentV.changes[independentV.mappers$timeRange==i&
                                       independentV.mappers$contribBefore>0&
                                       independentV.mappers$contribAfter>0,variableName]
      classes <- independentV.mappers[independentV.mappers$timeRange==i&
                                        independentV.mappers$contribBefore>0&
                                        independentV.mappers$contribAfter>0,"HappeningType"]
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
    if(kruskal$p.value<=0.05){

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
          variable1 <- dependentV.changes[independentV.mappers$timeRange==i&
                                          independentV.mappers$HappeningType=="CFM",variableName]
          #skill
	        classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
	                                   independentV.mappers$HappeningType=="CFM","skill"]
        }else{
          variable1 <- dependentV.changes[independentV.mappers$timeRange==i&
                                            independentV.mappers$contribBefore>0&
                                            independentV.mappers$contribAfter>0&
                                            independentV.mappers$HappeningType=="CFM",variableName]
          #skill
          classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
                                             independentV.mappers$contribBefore>0&
                                             independentV.mappers$contribAfter>0&
                                             independentV.mappers$HappeningType=="CFM","skill"]
        }
        print("N:")
        print(length(classes1))
        model <- lm(variable1~classes1)
        if(any(summary(model)$coefficients[2,4]<=0.05)){
          print("Analyses of the effect of skill for CFM")
          { sink("/dev/null"); print(plot(classes1,variable1,log="x",main="The skill has an effect",
                                          sub=paste("on this variable (",variableName,") for CFM"))); sink(); }
          print(abline(model))
          print(plot.new())
          print(summary(model))
          print("")
        }
        if(any(contribIndepVar==variableName)){
        # economic status
        classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
                                           independentV.mappers$HappeningType=="CFM","economic_status"]
        }else{
          # economic status
          classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
                                             independentV.mappers$contribBefore>0&
                                             independentV.mappers$contribAfter>0&
                                             independentV.mappers$HappeningType=="CFM","economic_status"]
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
        classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
                                           independentV.mappers$HappeningType=="CFM","culture"]
	      }else{
	        classes1 <- independentV.mappers[independentV.mappers$timeRange==i&
	                                           independentV.mappers$contribBefore>0&
	                                           independentV.mappers$contribAfter>0&
	                                           independentV.mappers$HappeningType=="CFM","culture"]
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
          if(kruskal2$p.value<=0.05){
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
	        #skill
          variable2 <- dependentV.changes[independentV.mappers$timeRange==i&
                                          independentV.mappers$HappeningType=="CRM",variableName]
          classes2 <- independentV.mappers[independentV.mappers$timeRange==i&
                                           independentV.mappers$HappeningType=="CRM","skill"]
        }else{
          #skill
          variable2 <- dependentV.changes[independentV.mappers$timeRange==i&
                                            independentV.mappers$contribBefore>0&
                                            independentV.mappers$contribAfter>0&
                                            independentV.mappers$HappeningType=="CRM",variableName]
          classes2 <- independentV.mappers[independentV.mappers$timeRange==i&
                                             independentV.mappers$contribBefore>0&
                                             independentV.mappers$contribAfter>0&
                                             independentV.mappers$HappeningType=="CRM","skill"]
        }
        print("N:")
        print(length(classes2))
        print("")
        model <- lm(variable2~classes2)
        if(any(summary(model)$coefficients[2,4]<=0.05,na.rm = TRUE)){
          print("Analyses of the effect of skill for CRM")
          { sink("/dev/null"); print(plot(classes2,variable2,log="x",main="The skill has an effect",
                                          sub=paste("on this variable (",variableName,") for CRM"))); sink(); }
          print(abline(model))
          print(plot.new())
          print(summary(model))
          print("")
        }

        if(any(contribIndepVar==variableName)){
          ## distance during event
          classes2 <- independentV.mappers[independentV.mappers$timeRange==i&
                                           independentV.mappers$HappeningType=="CRM","event_mapping_distance"]
        }else{
          ## distance during event
          classes2 <- independentV.mappers[independentV.mappers$timeRange==i&
                                             independentV.mappers$contribBefore>0&
                                             independentV.mappers$contribAfter>0&
                                             independentV.mappers$HappeningType=="CRM","event_mapping_distance"]
        }
        print("N:")
        print(length(classes2))
        print("")
        model <- lm(variable2~classes2)
        if(any(summary(model)$coefficients[2,4]<=0.05)){
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
      if(wilcox$p.value["CRM","CFM"]<=0.05){
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

#rmarkdown::render("052-Seniors-Analyses.R", "html_document")
