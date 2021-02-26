source("002-LoadData.R")

library(stats)
library(forcats)
library(effsize)
library(clinfun)
library(ggplot2)
library(reshape2)




distance <- c("economic_distance","cultural_distance","population_density_distance","physical_geography_distance")

## statistics on during
for (variableName in names(dependentVduring.changes)){
  print("")
  print("========================================================================")
  print(paste("RESULTS FOR VARIABLE",variableName))
  print("========================================================================")
  print("")
  print("")
  
  jitterplot <- ggplot(dependentVduring.changes,aes(x=independentVduring.mappers$timeRange,
                                                    y=dependentVduring.changes[,variableName]))+
    geom_count(position="jitter",aes(color=independentVduring.mappers$HappeningType))+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(jitterplot)
  
  print("")
  
  boxplot <- ggplot(dependentVduring.changes,aes(x=independentVduring.mappers$timeRange,
                                                 color=independentVduring.mappers$HappeningType,
                                                 y=dependentVduring.changes[,variableName]))+
    geom_boxplot()+
    labs(title=variableName)+
    scale_color_manual(values=c("#8c510a","#01665e","#5ab4ac"))+
    theme(legend.title = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y=element_blank())+
    scale_y_continuous(trans='pseudo_log')
  print(boxplot)
  
  print("")
  
  
  for(i in levels(independentVduring.mappers$timeRange)){
    
    #get data for time interaval and variable
    variable <- dependentVduring.changes[independentVduring.mappers$timeRange==i,variableName]
    classes <- independentVduring.mappers[independentVduring.mappers$timeRange==i,"HappeningType"]
    
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
        
        variable1 <- dependentVduring.changes[independentVduring.mappers$timeRange==i&
                                                independentVduring.mappers$HappeningType=="CFM",variableName]
        #skill
	      classes1 <- independentVduring.mappers[independentVduring.mappers$timeRange==i&
	                                               independentVduring.mappers$HappeningType=="CFM","skill"]
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
        
        # economic status
        classes1 <- independentVduring.mappers[independentVduring.mappers$timeRange==i&
                                                 independentVduring.mappers$HappeningType=="CFM","economic_status"]
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
        classes1 <- independentVduring.mappers[independentVduring.mappers$timeRange==i&
                                                 independentVduring.mappers$HappeningType=="CFM","culture"]
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
        
	      #skill
        variable2 <- dependentVduring.changes[independentVduring.mappers$timeRange==i&
                                                independentVduring.mappers$HappeningType=="CRM",variableName]
        classes2 <- independentVduring.mappers[independentVduring.mappers$timeRange==i&
                                                 independentVduring.mappers$HappeningType=="CRM","skill"]
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

      
        ## distance during event
        classes2 <- independentVduring.mappers[independentVduring.mappers$timeRange==i&
                                                 independentVduring.mappers$HappeningType=="CRM","event_mapping_distance"]
        print("N:")
        print(length(classes2))
        print("")
        model <- lm(variable2~classes2)
        if(!any(distance==variableName)&any(summary(model)$coefficients[2,4]<=0.05,na.rm = TRUE)){
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

#rmarkdown::render("004-During-Analyses.R", "html_document")
