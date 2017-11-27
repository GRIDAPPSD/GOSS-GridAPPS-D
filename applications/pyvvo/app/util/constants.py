'''
Created on Nov 16, 2017

@author: thay838
'''
import dateutil.tz
import re

# Consistent date formatting is important. For specifying dates in GridLAB-D,
# use DATE_TZ_FMT. Note that GridLAB-D uses MySQL's TIMESTAMP type, so use 
# DATE_FMT for interacting with the DB. Note - this could cause DST troubles if
# there are multiple rows with the same time - be sure to truncate tables.
DATE_FMT = '%Y-%m-%d %H:%M:%S'
DATE_TZ_FMT = DATE_FMT + ' %Z'
GLD_TIMESTAMP = '# timestamp'

# Map Posix timezones to Olson database timezones. NOTE: No '+' here.
TZ = {'EST5EDT':    dateutil.tz.gettz('US/Eastern'),
      'CST6CDT':    dateutil.tz.gettz('US/Central'),
      'MST7MDT':    dateutil.tz.gettz('US/Mountain'),
      'PST8PDT':    dateutil.tz.gettz('US/Pacific'),
      'AST9ADT':    dateutil.tz.gettz('US/Alaska'),
      'HST10HDT':   dateutil.tz.gettz('US/Hawaii')}

# Create regular expression for checking for timezone strings
TZ_EXP = re.compile('[ECMPAH][SD]T')