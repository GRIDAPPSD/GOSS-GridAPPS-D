'''
Created on Nov 16, 2017

@author: thay838
'''
import pytz
import re

# Consistent date formatting is important. For specifying dates in GridLAB-D,
# use DATE_TZ_FMT. Note that GridLAB-D uses MySQL's TIMESTAMP type, so use 
# DATE_FMT for interacting with the DB. Note - this could cause DST troubles if
# there are multiple rows with the same time - be sure to truncate tables.
DATE_FMT = '%Y-%m-%d %H:%M:%S'
DATE_TZ_FMT = DATE_FMT + ' %Z'
GLD_TIMESTAMP = '# timestamp'

# Map Posix timezones to pytz timezones. NOTE: No '+' here.
TZ = {'EST5EDT':    pytz.timezone('US/Eastern'),
      'CST6CDT':    pytz.timezone('US/Central'),
      'MST7MDT':    pytz.timezone('US/Mountain'),
      'PST8PDT':    pytz.timezone('US/Pacific'),
      'AST9ADT':    pytz.timezone('US/Alaska'),
      'HST10HDT':   pytz.timezone('US/Hawaii')}

# Create regular expression for checking for timezone strings
TZ_EXP = re.compile('[ECMPAH][SD]T')