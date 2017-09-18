'''
Created on Sep 11, 2017

@author: thay838
'''
from powerflow import writeCommands
import time
from util import db
from util import gld
import os
print('The time is {}'.format(time.ctime(), flush=True))
# *****************************************************************************
# Stuff to start
timezone= "EST+5EDT"
tz_offset = 10800
starttime= "2009-07-21 00:00:00"
stoptime = "2009-07-21 01:00:00"
tFmt = "%Y-%m-%d %H:%M:%S"
inPath = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/ieee8500_base.glm"
playerFile = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/zipload_schedule.player"
outDir = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output"
numInd = 16 # Best if this is a multiple of num cores.
numGen = 2
numIntervals = 2
tInt = 60 * 15 # 15 minutes
#******************************************************************************
# Modify model. NOTE: Things will be in reverse order intentionally.
# Read the base model as a string.
with open(inPath, 'r') as f1:
    strModel = f1.read()

# Get three filenames and write objects.
# Raw model for comparrison.
rawFile = writeCommands.writeCommands.addFileSuffix(inPath=inPath,
                                                    suffix='raw',
                                                    outDir=outDir)
writeRaw = writeCommands.writeCommands(strModel = strModel,
                                       pathModelOut=rawFile)
# Model which will use database.
dbFile = writeCommands.writeCommands.addFileSuffix(inPath=inPath,
                                                      suffix='db',
                                                      outDir=outDir)
writeDB = writeCommands.writeCommands(strModel = strModel,
                                      pathModelOut=dbFile)
# Model which will use metric_collectors.
metricFile = writeCommands.writeCommands.addFileSuffix(inPath=inPath,
                                                      suffix='metric',
                                                      outDir=outDir)
writeMetric = writeCommands.writeCommands(strModel = strModel,
                                          pathModelOut=metricFile)

# Setup the models.
# Raw model has no frills.
writeRaw.setupModel(starttime=starttime, stoptime=stoptime, timezone=timezone,
                    vSource=69715.065, playerFile=playerFile, dbFlag=False,
                    profiler=1)
# DB model should have a database object
writeDB.setupModel(starttime=starttime, stoptime=stoptime, timezone=timezone,
                    vSource=69715.065, playerFile=playerFile, dbFlag=True,
                    tz_offset=tz_offset, profiler=1)
# Metric model will have metric collectors added later.
writeMetric.setupModel(starttime=starttime, stoptime=stoptime,
                       timezone=timezone, vSource=69715.065,
                       playerFile=playerFile, dbFlag=False, profiler=1)

# All three models should use non-manual control.
writeRaw.switchControl()
writeDB.switchControl()
writeMetric.switchControl()
 
# DB model should record all triplex nodes.
tableDat = writeDB.recordTriplex(suffix='test')
# Get a database connection and cursor - we'll want to truncate this table
# between runs.
cnxn = db.connect()
cursor = cnxn.cursor()

# Metrics model should add meters and metrics collectors
writeMetric.addTriplexMeters()
mFile = writeMetric.addMetricsCollectorWriter(interval=60, suffix='test')
writeMetric.addMetricsCollectors()

# Write each model, run three times, record output.
rawOut = 'rawOut.txt'
dbOut = 'dbOut.txt'
metricOut = 'metricOut.txt'
a = [(writeRaw, rawOut), (writeDB, dbOut),
     (writeMetric, metricOut)]

for el in a:
    print('***Begin runs for {}***'.format(el[1]), flush=True)
    # Write the model
    el[0].writeModel()
    # Open the output file
    f = open(outDir + '/' + el[1], 'w')
    # Run model three times, and write stderr and stdout.
    for n in range(3):
        t0=time.time()
        r = gld.runModel(el[0].pathModelOut)
        t1=time.time()
        print('Run {} complete in {:.2f} seconds.'.format(n+1, t1-t0),
              flush=True)
        f.write('*'*40 + '\n')
        f.write('**RUN {}**'.format(n+1))
        f.write(r.stdout.decode('utf-8'))
        f.write(r.stderr.decode('utf-8'))
        
        # If we're using the DB, truncate the table. It'd be better to drop it,
        # but the mysql connector doesn't want to drop tables...
        if el[1] == dbOut:
            db.truncateTable(cursor=cursor, table=tableDat['table'])
        
    # Close file
    f.close()
    
print('The time is {}'.format(time.ctime(), flush=True))