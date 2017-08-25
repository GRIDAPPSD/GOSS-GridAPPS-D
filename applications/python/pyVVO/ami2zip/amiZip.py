
# coding: utf-8

# In[5]:

import csv
import numpy as np
import pandas as pd
import matplotlib.pyplot as pyplot
import datetime
from scipy.optimize import curve_fit
import collections
import math
from sklearn.metrics import r2_score, mean_squared_error


# Function to convert CSV files to pandas DataFrame. Discarding the description sentences on top of each file.
# 'n' refers to the column or the meter that you want to model.

# In[6]:

def csv2dataframe(fileName):#csv2dataframe(fileName,n):
    n=1
    newdata=[]
    with open(fileName,'r') as fileData:
        rowData = csv.reader(fileData, 
                           delimiter = ',', 
                           quotechar = '"')
        for data in rowData:
            if len(data)>1:
                newdata.append([data[0],data[n]])                
    data_array = np.asarray(newdata)
    
    return convertDateTime(data_array)


# First Column of the array is datetime with 'PST' extension. This function strips timezone and converts it into numpy datetime64 format

# In[7]:

def convertDateTime(data_array):
    
    firstCol=data_array[1:,0]
    stripFirstCol=[ele.rstrip(' PST') for ele in firstCol]
    
    dateTimeFirstCol=[np.datetime64(ele) for ele in stripFirstCol]
    data_array[1:,0]=dateTimeFirstCol
    data_frame=pd.DataFrame(data_array.reshape(len(data_array),-1))
    
    data_frame.index=data_frame[0]
    data_frame=data_frame.drop(data_frame.columns[0],axis=1)


    data_frame.columns=data_frame.iloc[0]
    data_frame=data_frame.drop(data_frame.index[0])
    
    data_frame=data_frame.astype(str).astype(float)
    data_frame.index=pd.to_datetime(data_frame.index)
    data_frame=data_frame.resample('15T').mean()
    data_frame=data_frame[1:]
    return data_frame


# In[8]:

reactivePfile='R3_12_47_1_AMI_residential_phase12_reactive_power.csv'
realPfile='R3_12_47_1_AMI_residential_phase12_real_power.csv'
reactiveVfile='R3_12_47_1_AMI_residential_phase12_reactive_voltage.csv'
realVfile='R3_12_47_1_AMI_residential_phase12_real_voltage.csv'
temperaturefile='R3_12_47_1_climate.csv'


# In[9]:

reactiveP=csv2dataframe(reactivePfile)
realP=csv2dataframe(realPfile)
reactiveV=csv2dataframe(reactiveVfile)
realV=csv2dataframe(realVfile)
amiTemp=csv2dataframe(temperaturefile)


# Given a startDate and endDate for a season, this function generates all the dates within that interval. This dateList is later used to find the weekdays and weekends within that interval

# In[10]:

def getDateList(startDate,endDate):
    startDate=datetime.datetime.strptime(startDate,'%Y-%m-%d')

    endDate=datetime.datetime.strptime(endDate,'%Y-%m-%d')
  
    numDays=endDate-startDate
    numDays=numDays.days

    date_list = [startDate + datetime.timedelta(days=x) for x in range(0, numDays)]
    return date_list


# def splitWDWE(data_frame,weekdays,weekends):
#     weekdayResult=getDays(data_frame,weekdays)
#     weekendResult=getDays(data_frame,weekends)
#     return weekdayResult, weekendResult

# This function extracts only those instance from the data_frame that match in 'days'. Days is either the list of weekdays or weekends

# In[11]:

def getDays(data_frame,days):
    result=data_frame[days[0]]
    for i in range(1,len(days)):
            result=result.append(data_frame[days[i]])
    return result


# This function return the instances for a given hourwindow. If the 'hourwindow' is 1, then it extracts only those instances whose hour in the index is equal to 'hr'. If the 'hourwindow' is greater than 1, then it return all those instance which match the hours in the range 'hr' to 'hr+hourwindow'

# In[12]:

def getHourData(data_frame,hourwindow,hr):
    if hourwindow==1:
        result=data_frame[data_frame.index.hour == hr]
    else:
        result=data_frame[data_frame.index.hour == hr]
        for i in range(1,hourwindow):
            hr=hr+i
            result=result.append(data_frame[data_frame.index.hour == hr])
            
    return result


# In[13]:

def getMinsData(data_frame,minswindow,mins):
    if minswindow==1:
        result=data_frame[data_frame.index.minute == mins]
    else:
        result=data_frame[data_frame.index.minute == mins]
        for i in range(1,minswindow):
            mins=mins+i
            result=result.append(data_frame[data_frame.index.minute == mins])
            
    return result


# This is the function/modelthe curve_fit function would take to find the coefficients.

# In[14]:

def myfunc(independentVar, a, b, c):
    x,t=independentVar
    return (a*(x)**2)+b*x+c   #+t - the r squared value is negative when we include temperature
                                
                                


# 
# def realFunc(independentVar,Z,I,P, Zo,Io,Po):
#     x,t,S=independentVar
#     return (((x**2)*S*Z*(math.cos(Zo)))+(x*S*I*(math.cos(Io)))+(S*P*(math.cos(Po)))+t)

# def reactiveFunc(independentVar,Z,I,P, Zo,Io,Po):
#     x,t,S=independentVar
#     return (((x**2)*S*Z*(math.sin(Zo)))+(x*S*I*(math.sin(Io)))+(S*P*(math.sin(Po)))+t)

# Function to calculate the Root Mean Square Error value

# def RMS(yActual, yPredicted):
#     squares = (yPredicted - yActual) ** 2
#     result=(sum(squares)/len(yActual))**(0.5)
#     return result

# def rSquared(x, yActual, yPredicted):
#     squareError = (yPredicted - yActual) ** 2
#     variationFromMean= (yActual - yActual.mean()) ** 2
#     result = 1 - (sum(squareError)/sum(variationFromMean))
#     return result
#     

# Finds the Nominal Power when the nominal Voltage is between a certain range. voltageSensitivity allows to change the range within which we can set the nominalVoltage to be.

# In[15]:

def getNominalPower(apparentPower, apparentVoltage):#getNominalPower(apparentPower, apparentVoltage, voltageSensitivity):
    voltageSensitivity=3;
    nominalV=apparentVoltage.mean()
    powerArray=[]
    
    for i in range(0,len(apparentPower)):
        
        if ((apparentVoltage.iloc[i].values>= (nominalV-voltageSensitivity)).bool() | (apparentVoltage.iloc[i].values <= (nominalV+voltageSensitivity)).bool()):
            powerArray.extend(apparentPower.iloc[i].values)
    
    return np.mean(powerArray) # or np.median(powerArray)
    


# The coefficients that we get from curve fitting is a profuct of nominalPower(Sn), Z/I/P coefficient and cos or sin of Z/I/P angle for real or reactive Power respectively. This function tries to get the Z, I, P coefficient values from the product.

# def processCoefficients(coefficient1, coefficient2, nominalPower):
#     coefficient1=coefficient1/nominalPower
#     coefficient2=coefficient2/nominalPower
#     
#     for i in range(0,len(coefficient1)):
#             coefficient1[i],coefficient2[i]= splitCoefficient(coefficient1[i],coefficient2[i])
#     
#     return coefficient1, coefficient2

# def splitCoefficient(coefficient1, coefficient2):
#     theta=math.atan(coefficient2/coefficient1)
#     coefficient1=coefficient1/(math.cos(theta))
#     coefficient2=coefficient2/(math.sin(theta))
#     return coefficient1, coefficient2

# TODO- need to create a function that would take the starting and ending date from the dataset and then create seasonDate ranges

# In[16]:

seasonDates=['2013-01-01','2013-03-01','2013-05-15']


# hourWindows over which the coefficients are to be computed. '1' refers to finding coefficients for every hour of the day, while '24' refers to finding coefficients over the entire day.
# 
# '1' would compute {4(numOfSeasons) * 2(weekdays,weekends) * 24 (each hour in a day)} efficients
# '12' would compute {4(numOfSeasons) * 2(weekdays,weekends) * 2 (12 hour window of each day)} efficients
# '24' would compute {4(numOfSeasons) * 2(weekdays,weekends) * 1 (considers the entire day)} efficients

# In[ ]:




# In[23]:

def computeValues(realVoltage, reactiveVoltage, realPower, reactivePower, temperature):
    apparentVoltage=((realVoltage**2)+(reactiveVoltage**2))**0.5
    apparentPower=((realPower**2)+(reactivePower**2))**0.5
    independentVariable=apparentVoltage/apparentVoltage.mean() # or .median()
                
    nominalPower= getNominalPower(apparentPower, apparentVoltage) #Sn value
    
    columnNames=independentVariable.columns
                
    t=temperature.values
    t=np.reshape(t,len(t))
                
    x=independentVariable[columnNames[0]]
    x=x.values
            
    xaxis=apparentVoltage[columnNames[0]] # used for defining the axis in the graph
                
    y1=realPower[columnNames[0]].values
    y2=reactivePower[columnNames[0]].values
            
                                
                #c1, covar = curve_fit(realFunc, (x,t,nominalPower), y1,maxfev=50000)#,method='lm',p0=init_vals)
                #print("NEW FUNCTION1",c1)
                #c2, covar = curve_fit(reactiveFunc, (x,t,nominalPower), y2,maxfev=800000)#,method='lm',p0=init_vals)
                #print("NEW FUNCTION 2",c2)
                
    # REAL COEFFICIENTS
    coefficient1, covar = curve_fit(myfunc, (x,t), y1,maxfev=8000)#,method='lm',p0=init_vals)
                
    rms1 = math.sqrt(mean_squared_error(y1,myfunc((x,t),coefficient1[0],coefficient1[1],coefficient1[2])))
    #rms1=RMS(y1,myfunc((x,t),coefficient1[0],coefficient1[1],coefficient1[2]))
                
    rSqr1=r2_score(y1,myfunc((x,t),coefficient1[0],coefficient1[1],coefficient1[2])) #, multioutput='uniform_average')
    # rSqr1=rSquared(x,y1,myfunc((x,t),coefficient1[0],coefficient1[1],coefficient1[2]))
                
    fid,cx = pyplot.subplots()
    cx.plot(xaxis,y1,'ro',label='Actual')
    cx.plot(xaxis,myfunc((x,t),coefficient1[0],coefficient1[1],coefficient1[2]),'go',label='Predicted')          
    cx.legend()
    cx.set_title('Real Power Consumption')
    cx.set_xlabel('Actual Terminal Voltage')
    cx.set_ylabel('Real Power')
    pyplot.show()
    print("R SQUARED VALUES is", rSqr1)

                
    # REACTIVE COEFFICIENTS
    coefficient2, covar = curve_fit(myfunc, (x,t), y2,maxfev=8000)#,method='lm',p0=init_vals)
    rms2= math.sqrt(mean_squared_error(y2, myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2])))
    #rms2=RMS(y2,myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2]))
    rSqr2 = r2_score(y2,myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2])) #, multioutput='uniform_average')
                                #rSqr2=r2_score(y2,myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2]))
    #rSqr2=rSquared(x,y2,myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2]))
    fid,cx = pyplot.subplots()
    cx.plot(xaxis,y2,'ro',label='Actual')
    cx.plot(xaxis,myfunc((x,t),coefficient2[0],coefficient2[1],coefficient2[2]),'go',label='Predicted')
    cx.legend()
    cx.set_title('Reactive Power Consumption')
    cx.set_xlabel('Actual Terminal Voltage')
    cx.set_ylabel('Reactive Power')
    pyplot.show()
    print("R SQUARED VALUES is", rSqr2)
                
    #finalCoefficients1,finalCoefficients2=processCoefficients(coefficient1,coefficient2,nominalPower)
                
    # Real hour
    hourValuesReal=np.append(coefficient1,rms1)
    hourValuesReal=np.append(hourValuesReal, rSqr1)
    #hourValuesReal=np.append(hourValuesReal, finalCoefficients1)
    #hourValuesReal=np.append(hourValuesReal, sum(finalCoefficients1))
                
    hourValuesReactive=np.append(coefficient2, rms2)
    hourValuesReactive=np.append(hourValuesReactive, rSqr2)
    #hourValuesReactive=np.append(hourValuesReactive, finalCoefficients2)
    #hourValuesReactive=np.append(hourValuesReactive, sum(finalCoefficients2))
    
    return hourValuesReal, hourValuesReactive
                


# In[24]:

hourWindows=[0.5,1,4,12,24] # for minutes window please enter in decimal hours (15 mins- 0.25 hours, 30 mins -0.5 hours)


# Find the Coefficients and RMSE values for different time windows, based on Season, Weekday, Weekend and Hourly

# In[25]:

# TODO- make this a function and call it over each meter

hourWindowDict=collections.OrderedDict()
for hourWindow in hourWindows:
    hourWindowKey="HourWindowOf"+str(hourWindow)
    print("HOUR WINDOW IS",hourWindow)
    
    seasonDict=collections.OrderedDict()
    for j in range(0,len(seasonDates)-1):
        
        # Extract only those instance from the dataframe that are within the range of seasonDates
        realPowerSeason=realP[seasonDates[j]:seasonDates[j+1]]
        realVoltageSeason=realV[seasonDates[j]:seasonDates[j+1]]
        reactivePowerSeason=reactiveP[seasonDates[j]:seasonDates[j+1]]
        reactiveVoltageSeason=reactiveV[seasonDates[j]:seasonDates[j+1]]
        temperatureSeason=amiTemp[seasonDates[j]:seasonDates[j+1]]
    
        # get the dates within the range of dates. This will be used to check which dates within the range are weekdays and weekends
        date_list=getDateList(seasonDates[j],seasonDates[j+1])
        
        print("Season ",j+1)
        print(seasonDates[j])
        print(seasonDates[j+1])
        seasonKey="Season"+str(j+1)
    
        dayTypeDict=collections.OrderedDict()
        
        for i in ('weekdays','weekends'):
            if i=='weekdays':
                dayTypeKey="weekdays"
                
                # extract only those days from the dateList which are weekdays
                days=[(day.strftime('%Y-%m-%d')) for day in date_list if (day.isoweekday()==1 or day.isoweekday()==2 or day.isoweekday()==3 or day.isoweekday()==4 or day.isoweekday()==5)]
                print("Weekdays")
            
            else:
                dayTypeKey="weekends"
                
                # extract only those days from the dateList which are weekends
                days=[(day.strftime('%Y-%m-%d')) for day in date_list if (day.isoweekday()==6 or day.isoweekday()==7)]
                print("Weekends")
        
            
            # get only those instances which weekdays or weekends depending on what value 'days' holds
            totalrealPower=getDays(realPowerSeason,days)
            totalrealVoltage=getDays(realVoltageSeason,days)
            totalreactivePower=getDays(reactivePowerSeason,days)
            totalreactiveVoltage=getDays(reactiveVoltageSeason,days)
            totaltemperature=getDays(temperatureSeason,days)
            
            hourDict=collections.OrderedDict()
            hr=0
            while hr <24:
                
                if hourWindow<1:
                    
                    realPower=getHourData(totalrealPower,1,hr)
                    realVoltage=getHourData(totalrealVoltage,1,hr)
                    reactivePower=getHourData(totalreactivePower,1,hr)
                    reactiveVoltage=getHourData(totalreactiveVoltage,1,hr)
                    temperature=getHourData(totaltemperature,1,hr)
                
                    hourKey=str(hr)+" to "+str(hr+1)
                    print(hourKey)
                    hr=hr+1
                    
                    minsDict=collections.OrderedDict()
                    minsWindow=math.floor(hourWindow*60)
                    mins=0
                    while mins<60:
                        
                        minsRealPower=getMinsData(realPower,minsWindow,mins)
                        minsRealVoltage=getMinsData(realVoltage,minsWindow,mins)
                        minsReactivePower=getMinsData(reactivePower,minsWindow,mins)
                        minsReactiveVoltage=getMinsData(reactiveVoltage,minsWindow,mins)
                        minsTemperature=getMinsData(temperature,minsWindow,mins)
                        
                        minsKey=str(mins)+" to "+str(mins+minsWindow)
                        print(minsKey)
                        
                        mins=mins+ minsWindow
                        
                        
                        minsValuesReal, minsValuesReactive=computeValues(minsRealVoltage, minsReactiveVoltage, minsRealPower, minsReactivePower, minsTemperature)
                        innerDict={'real': pd.DataFrame(np.reshape(minsValuesReal,(1,len(minsValuesReal)))), 'reactive':pd.DataFrame(np.reshape(minsValuesReactive,(1,len(minsValuesReactive))))}
                        df=pd.concat(innerDict)
                        df=df.reset_index(level=1, drop=True) # while creating the dataFrame it adds its own numerical index. So this would drop that numerical index
                        
                        minsDict[minsKey]=df
                    
                    minsDF=pd.concat(minsDict)
                    hourDict[hourKey]=minsDF
                    
                else:
                    minsDict=collections.OrderedDict()
                    # getHourData takes the extracted DataFrame from previous step, hourWindow and the value of hr at that instant
                    realPower=getHourData(totalrealPower,hourWindow,hr)
                    realVoltage=getHourData(totalrealVoltage,hourWindow,hr)
                    reactivePower=getHourData(totalreactivePower,hourWindow,hr)
                    reactiveVoltage=getHourData(totalreactiveVoltage,hourWindow,hr)
                    temperature=getHourData(totaltemperature,hourWindow,hr)
    
                    hourKey=str(hr)+" to "+str(hr+hourWindow)
                    print(hourKey)
                
                    hr=hr+hourWindow
                
                    hourValuesReal, hourValuesReactive=computeValues(realVoltage, reactiveVoltage, realPower, reactivePower, temperature)
                    innerDict={'real': pd.DataFrame(np.reshape(hourValuesReal,(1,len(hourValuesReal)))), 'reactive':pd.DataFrame(np.reshape(hourValuesReactive,(1,len(hourValuesReactive))))}
                    #print("InnerDict", innerDict)
                    df=pd.concat(innerDict)
                    df=df.reset_index(level=1, drop=True) # while creating the dataFrame it adds its own numerical index. So this would drop that numerical index
                
                    # creating a dictionary with hourKey which holds the DataFrame from the previous step as the value. Later this dictionary will be moved to a DataFrame
                    minsKey="0 to " +str(hourWindow*60)
                    minsDict[minsKey]=df
                    
                    minsDF=pd.concat(minsDict)
                    hourDict[hourKey]=minsDF 
                                    
            hourDF= pd.concat(hourDict) #pd.concat(hourDict)#HERE
            dayTypeDict[dayTypeKey]=hourDF
            
        dayTypeDF=pd.concat(dayTypeDict)
        seasonDict[seasonKey]=dayTypeDF
        
    seasonDF=pd.concat(seasonDict)
    hourWindowDict[hourWindowKey]=seasonDF
    
hourWindowDF=pd.concat(hourWindowDict)
columnNames=['Z','I','P','RMSE','rSquared',]
hourWindowDF.columns=columnNames


# Table giving the Coefficients and the the RMSE values for different Time Window Models

# In[26]:

hourWindowDF


# In[ ]:



