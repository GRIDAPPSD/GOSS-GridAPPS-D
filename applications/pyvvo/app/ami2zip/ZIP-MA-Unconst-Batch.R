# analyze AMI data
# fitting a ZIP model
# using an unconstrianed moving average method
# this version is fixed at using two weeks worth of data
# unconstrained: i.e., Z% + I% + P% = 1  is not enforced
# run in batch mode: 
#   Rscript ZIP-MA-Unconst-Batch.R n1 n2
#      where n1 and n2 is a test feature for a range of loads to process (default is all)

# possible libraries we may use 
# install.packages('Rsolnp')
library(Rsolnp)
# install.packages('alabama')
# library(alabama)

# read control file
cmd <- readLines("ZIP.cmd")

# figure out the data subset to read
rec.day = 4*24       # number of records per day = (4 records per hour) * (24 hours)
first.day = as.Date(unlist(read.csv(cmd[2],skip=9,nrows=1,header=F)[1]))   # first day in dataset
Start.day = as.Date(cmd[6]) ; End.day = as.Date(cmd[7])

# need to skip the header and the first record, cuz first record is all zeros for all files
SKIP  = 10 + (as.numeric(Start.day - first.day)-1)*96  ; if (SKIP<10) SKIP = 10
NROWS = (as.numeric(End.day   - Start.day)+4)*96

# where are we putting the new files
chead = paste(cmd[5], '/ZIP', sep='') ; ctail = '.csv'

# read load names
load.names <- unlist(read.csv(cmd[2], skip=8, nrows=1, header=F)[-1])
# default loads to process
num.loads = length(load.names)  ;  load1 = 1  ;  load2 = num.loads

# see if we are running a subset of loads (from command line)
args <- commandArgs(TRUE)
if (length(args)>0) {
   load1 = 1 ; load2 = as.numeric(args[1])
   if (length(args)>1) {load1 = as.numeric(args[1]) ; load2 = as.numeric(args[2])}
}
paste('load range: (', load1, ', ', load2, ')', sep='')
paste('number of loads:', (load2-load1+1))

# read in data (voltage, Real Power, Reactive Power)
Volt.mag   <- read.csv(cmd[2], skip=SKIP, header=F, nrows=NROWS)
Power.real <- read.csv(cmd[3], skip=SKIP, header=F, nrows=NROWS)
Power.reac <- read.csv(cmd[4], skip=SKIP, header=F, nrows=NROWS)
paste('Dataset range:', as.Date(head(Volt.mag[,1],1)), '--', as.Date(tail(Volt.mag[,1],1)))

# dim(Volt.mag) ; dim(Power.real) ; dim(Power.reac)

###################################################################
# subsetting

Dtime = as.POSIXlt(Volt.mag[,1])-15*60
Ctime <- as.character(Dtime) ; Drange = as.Date(range(Ctime))

# find index to split data into weekdays and weekends
T15.day <- weekdays((Dtime), abbreviate=T)
week.end <- which(T15.day=='Sat' | T15.day=='Sun')		# weekend
week.day <- which(T15.day!='Sat' & T15.day!='Sun')		# weekday
Weekday  <- rep(0, length(T15.day)) ; Weekday[week.day] = 1

# need to use this version, cuz leap year screws up the previous calc
N = length(Dtime)  #  need to parse date/time to get hour (2016-01-01 00:00:00 PST)
Hour <- as.numeric(unlist(strsplit(unlist(strsplit(as.character(Dtime), " "))[(1:N)*2], ':'))[(1:N)*3-2])

############################################################################################
# Fit ZIP coefficients on previous (e.g., 2 week) data
# want 
# 1 file each hour (24)
# each file contains model info for each load

# objective and constraint functions
Object <- function(Params) {
   a1 = Params[1] ; a2 = Params[2] ; a3 = Params[3]
   b1 = Params[4] ; b2 = Params[5] ; b3 = Params[6]
   sum((Pbar - (a1*Vbar*Vbar + a2*Vbar + a3))^2 + (Qbar - (b1*Vbar*Vbar + b2*Vbar + b3))^2)/length(Vbar)
}
Constrain <- function(Params) {
   a1 = Params[1] ; a2 = Params[2] ; a3 = Params[3]
   b1 = Params[4] ; b2 = Params[5] ; b3 = Params[6]
   sqrt(a1*a1 + b1*b1) + sqrt(a2*a2 + b2*b2) + sqrt(a3*a3 + b3*b3) - 1
}

# get indices for data: data for MA (icum) and data for estimating ZIP coefficients (iday)
Calc.day = as.Date(cmd[8]) ; Start.day = as.Date(cmd[6]) ; End.day = as.Date(cmd[7])
icum <- which(as.Date(Ctime)>=Start.day & as.Date(Ctime)<=End.day)
iday <- which(as.Date(Ctime)==Calc.day)
weekdays(Calc.day)

# separate if on the weekend (0) or a weekday
iw = unique(Weekday[iday])
if (length(iw)>1) write('Error: Day contains both weekend and weekday???', file='error.out')
w.ind <- which(Weekday[icum]==iw)  #  0 --> weekend

# constants
Vn = 240.0   	              # place holder for nominal terminal voltage
par0  <- rep(sqrt(1/18),6)    # initial parameters for ZIP model
Frac <- rep(0,3) ; Cos <- rep(0,3)

# loop over hour of the day
for (hr in 0:(length(unique(Hour))-1)) {
   # hour index into data # hr=0  #  hr=hr+1
   h.ind <- which(Hour[icum][w.ind]==hr)

   nfile = paste(paste(chead, hr, sep='_'), ctail, sep='')  # hour = 0, 1, ..., 23
   write('# ZIP model results', nfile, ncolumns=1)
   write(paste('# date .........',date()), nfile, ncolumns=1, append=TRUE)
   write(paste('# Start.Date ...',Start.day), nfile, ncolumns=1, append=TRUE)
   write(paste('# End.Date .....',End.day), nfile, ncolumns=1, append=TRUE)
   write(paste('# Calc.Date ....',Calc.day), nfile, ncolumns=1, append=TRUE)

   # one Sn, Vn per file
   Vn = 240.0   	              # place holder for nominal terminal voltage
   write(paste('# Vn ...........',Vn), nfile, ncolumns=1, append=TRUE)
   a = paste('# load','impedance_pf','impedance_fraction','current_pf','current_fraction','power_pf','power_fraction', 'base_power',sep=', ')
   write(a, nfile, ncolumns=1, append=TRUE)

   # now loop over each load
   for (load in load1:load2) {
      V <- Volt.mag[icum,load+1][w.ind][h.ind]
      P <- Power.real[icum,load+1][w.ind][h.ind] ; Q <- Power.reac[icum,load+1][w.ind][h.ind]

      # scale voltage by nominal volt. (Vn) and the power by the base power (Sn) 
      Sn = median(sqrt(P*P + Q*Q)) 
      Vbar <- V/Vn ; Pbar <- P/Sn ; Qbar  <- Q/Sn

      # fit ZIP model
      # opti <- constrOptim.nl(par=par0,fn=Object,gr=NULL,heq=Constrain,control.outer=list(trace=F))
      # Opti = 'constrOptim.nl' ; constr = Constrain(opti$par) ; Model = 1 ; coe <- opti$par
      opti <- solnp(par0, fun=Object, eqfun=Constrain, eqB=1.0, control=list(trace=F)) 
      if (abs(Constrain(opti$par))>3.0) {
        opti <- solnp(par0, fun=Object, eqfun=Constrain, eqB=2.0, control=list(trace=F)) 
      }

      # convert back to original scale
      # opti$par[1] = Z%*cos(Zo), opti$par[2] = I%*cos(Io), opti$par[3] = P%*cos(Po)
      # opti$par[4] = Z%*sin(Zo), opti$par[5] = I%*sin(Io), opti$par[6] = P%*sin(Po)
      p <- opti$par[1:3]  ;  q <- opti$par[4:6]
        
      for (i in 1:3) {
         Frac[i] = sqrt(p[i]*p[i] + q[i]*q[i]) 
         Cos[i]  = 1.0 ; if (Frac[i]!=0) Cos[i] = abs(p[i]/Frac[i])
         # match what is done in Gridlab-D
         if (p[i]>0 & q[i]<0) Cos[i]  = -Cos[i] ;  
         if (p[i]<0 & q[i]<0) Frac[i] = -Frac[i]
         if (p[i]<0 & q[i]>0) { Cos[i]  = -Cos[i] ; Frac[i] = -Frac[i] }
      }
      a = paste(load.names[load], 
                round(Cos[1],6), round(Frac[1],6), 
                round(Cos[2],6), round(Frac[2],6), 
                round(Cos[3],6), round(Frac[3],6), round(Sn,6), sep=', ')
      write(a, nfile, ncolumns=1, append=TRUE)
   }
}
