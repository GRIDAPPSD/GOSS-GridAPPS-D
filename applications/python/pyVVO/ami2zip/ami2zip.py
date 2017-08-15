'''
Created on Aug 9, 2017

@author: bala256
'''
# In[1]:

import csv
#import sys
#print(sys.version)
#import os

import numpy as np
import pandas as pd
#from datetime import datetime
import matplotlib.pyplot as pyplot
#from numpy import sqrt, pi, exp, linspace, random


# Function to convert CSV files to pandas DataFrame. Discarding the description sentences on top of each file

# In[2]:
#cwd=os.getcwd()
#print(cwd)

def csv2dataframe(fileName):
    newdata=[]
    with open(fileName,'r') as fileData:
        rowData = csv.reader(fileData, 
                           delimiter = ',', 
                           quotechar = '"')
        for data in rowData:
            if len(data)>2:
                newdata.append(data[0:3])
    data_array = np.asarray(newdata)
    return convertDateTime(data_array)



# Fist Column of the array is datetime with 'PST' extension. This function strips timezone and converts it into numpy datetime64 format

# In[3]:

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


# In[ ]:




# In[4]:

reactivePfile='R3_12_47_1_AMI_residential_phase12_reactive_power.csv'
realPfile='R3_12_47_1_AMI_residential_phase12_real_power.csv'
reactiveVfile='R3_12_47_1_AMI_residential_phase12_reactive_voltage.csv'
realVfile='R3_12_47_1_AMI_residential_phase12_real_voltage.csv'


# In[ ]:

reactivePower=csv2dataframe(reactivePfile)
print("Reading Reactive Power")
realPower=csv2dataframe(realPfile)
print("Reading Real Power")
reactiveVoltage=csv2dataframe(reactiveVfile)
print("Reading Reactive Voltage")
realVoltage=csv2dataframe(realVfile)
print("Reading Real Voltage")


# In[ ]:

apparentVoltage=((realVoltage**2)+(reactiveVoltage**2))**0.5
apparentPower=((realPower**2)+(reactivePower**2))**0.5
independentVariable=apparentVoltage/240


# In[ ]:

columnNames=independentVariable.columns

x=independentVariable[columnNames[1]]
x=x.values
xaxis=apparentVoltage[columnNames[1]]
y1=realPower[columnNames[1]]
y1=y1.values
y2=reactivePower[columnNames[1]]


# In[ ]:




# In[ ]:




# Polynomial Curve Fitting

# In[32]:

coeff1=np.polyfit(x,y1,2)
print(coeff1)

coeff2=np.polyfit(x,y2,2)
print(coeff2)


# In[33]:

fig = pyplot.figure(figsize=(10, 8))

xx= np.linspace(x.min(),x.max(),100)
xxaxis=np.linspace(xaxis.min(),xaxis.max(),100)

poly1=np.poly1d(coeff1)
fid,cx = pyplot.subplots()

cx.plot(xaxis,y1,'ro',label='Actual')
cx.plot(xxaxis,np.polyval(coeff1,xx),'go',label='Predicted')

cx.legend()
cx.set_title('Real Power Consumption')
cx.set_xlabel('Actual Terminal Voltage')
cx.set_ylabel('Real Power')
#pyplot.show()
fig.add_subplot(cx)


poly2=np.poly1d(coeff2)
fid,cx = pyplot.subplots()

cx.plot(xaxis,y2,'ro',label='Actual')
cx.plot(xxaxis,poly2(xx),'go',label='Predicted')
cx.legend()
cx.set_title('Reactive Power Consumption')
cx.set_xlabel('Actual Terminal Voltage')
cx.set_ylabel('Reactive Power')
#pyplot.show()
fig.add_subplot(cx)

fig.show()

# In[ ]:




# In[ ]:




# In[ ]:


"""



# Gaussian Function Curve Fitting model

# In[34]:




# In[35]:

def gaussian(x, amp, cen, wid):
    return amp * np.exp(-(x-cen)**2 /wid)
#(amp/(sqrt(2*pi)*wid)) * exp(-(x-cen)**2 /(2*wid**2))
#amp * exp(-(x-cen)**2 /wid)


# In[ ]:




# In[36]:

def func(x, a, b, c):
    return a*np.exp(-b*x) + c


# In[37]:

def residuals(coeffs, y, t):
    return y - model(t, coeffs)


# In[38]:

def model(t, coeffs):
    return coeffs[0] + coeffs[1] * np.exp( - ((t-coeffs[2])/coeffs[3])**2 )


# In[39]:

def myfunc(x,a,b,c):
    return (a*(x)**2)+b*x+c


# In[ ]:


# In[40]:
#import scipy
from scipy.optimize.minpack import curve_fit


# In[41]:

#x = linspace(-10,10, 101)
#ynew = gaussian(x, 2.33, 0.21, 1.51) + random.normal(0, 0.2, len(x))

#init_vals = [1, 0, 1]     # for [amp, cen, wid]
coefficient1, covar = curve_fit(gaussian, x, y1,maxfev=8000)#,method='lm',p0=init_vals)
coefficient2, covar = curve_fit(gaussian, x, y2,maxfev=8000)#,method='lm',p0=init_vals)
print (coefficient1)
print (coefficient2)


# In[42]:

xx= np.linspace(x.min(),x.max(),100)
xxaxis=np.linspace(xaxis.min(),xaxis.max(),100)


polynomial1=np.poly1d(coefficient1)

fid,cx = pyplot.subplots()
cx.plot(xxaxis,polynomial1(xx),'-g',label='Predicted')
cx.plot(xaxis,y1,'-r',label='Actual')
cx.legend()
cx.set_title('Real Power Consumption')
cx.set_xlabel('Actual Terminal Voltage')
cx.set_ylabel('Real Power')
pyplot.show()



polynomial2=np.poly1d(coefficient2)
xx= np.linspace(x.min(),x.max(),100)
xxaxis=np.linspace(xaxis.min(),xaxis.max(),100)

fid,cx = pyplot.subplots()
cx.plot(xxaxis,polynomial2(xx),'-g',label='Predicted')
cx.plot(xaxis,y2,'-r',label='Actual')
cx.legend()
cx.set_title('Reactive Power Consumption')
cx.set_xlabel('Actual Terminal Voltage')
cx.set_ylabel('Reactive Power')
pyplot.show()
"""