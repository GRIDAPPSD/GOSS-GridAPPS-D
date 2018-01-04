#include "klu.h"
#include "cs.h"

#include <complex>
#include <string>

#define uint unsigned int

#include <array>
#define DARY(len) std::array<double,len>
#define UARY(len) std::array<unsigned int,len>
#define SARY(len) std::array<std::string,len>


#include <vector>
#define DVEC std::vector<double>
//#define UVEC std::vector<unsigned int>
#define CVEC std::vector<std::complex<double>>
#define SVEC std::vector<std::string>


/*
// Address (i,j) of a matrix
#include <tuple>
#define MADR std::tuple<unsigned int,unsigned int>
*/


// Hash address (i,j) to the index of a sparse matrix vector
#include <unordered_map>
#define UMAP std::unordered_map<unsigned int,unsigned int>
#define MMAP std::unordered_map<unsigned int,UMAP>


/*
#include <array>
// #define DARY = std::array<double>
*/


// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// USE SPARSE MATRICES EVERYWHERE
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------


int main(void) {
	// ------------------------------------------------------------------------
	// INITIALIZE
	// ------------------------------------------------------------------------
	
	
	// READ CONFIGURATOIN FILE
	//  - Determine mode
	//  - Determine 
	
	// vector of busnames
	SVEC busnames;
	// map busname -> position
	
	
	// Initialize state vector
	DVEC xV;	// vector of voltage magnitude states
	DVEC xT;	// vector of voltage angle states
	// for ( /* numbuses/numbranches */ ) {
		// xV.append();
		// xT.append();
	// }
	int xqty = xV.size() + xT.size();
	
	
	// query database for branches
	// vectors of: bus1, bus2, impedance parameters
	
	
	
	// ------------------------------------------------------------------------
	// BUILD TOPOLOGY
	// ------------------------------------------------------------------------
	// Build the adjacency matrix
	std::vector<std::vector<uint>> A;
	// outer vector has an element for every row
	// inner vector contains the indices of adjacent nodes
	// To add an adjacent pair of indices (i,j):
	//	-- if ( i > A.size() ) { append empty vector until A.size() == i }
	//  -- A[i].append(j);
	
	// Build the Admittance Matrix Y
	MMAP Ym;
	CVEC Y;
	// To append an element:
	//	-- Ym[i][j] = Y.size();
	//	-- Y.append(yij);
	// G, B, g, and b are derived from Y:
	//	-- Gij = std::real(Ym[i][j]);
	//	-- Bij = std::imag(Ym[i][j]);
	//	-- gij = std::real(-1.0*Ym[i][j]);
	//	-- bij = std::imag(-1.0*Ym[i][j]);

	
	// Initialize Measurement Vector z
	// Determine the size of the measurement vector
	DVEC sense;
	DVEC ssigs;
	SVEC sname;
	// for ( /* sensor objects */ ) {
		// z.append(0.0);
		// sigs.append(sensor.std_dev());
		// zn.append(/*JSON ADDRESS OF MEASUREMENT*/);
	// }
	int zqty = sense.size();
	
	
	// Initialize Measurement Function h(x) and its jacobian H(x)
	enum hx_t {
		Pij ,
		Qij ,
		Pi ,
		Qi };
	DVEC hx;
	std::vector<hx_t> thx;
	std::vector<uint> hxi;
	std::vector<std::vector<uint>> hxj;
	for ( int ii = 0 ; ii < zqty ; ii++ ) {
		// for each measurement:
		// hx.append(initial value)
		// thx.append(type [hx_t])
		// if ( branch ) {
			// hxi.append( i );
			// hxj.append( (vector)(j) );
		// }
		// if ( bus ) {
			// hxi.append( i );
			// hxj.append( A[i] ); // from adjacency matrix
		// }
		// we should actually probably store the nodename or xidx
	}
	
	
	enum Hx_t {
		dPijdVi , dPijdVj , dPijdTi , dPijdTj , 	
		dQijdVi , dQijdVj , dQijdTi , dQijdTj , 
		dPidVi  , dPidVj  , dPidTi  , dPidTj  ,
		dQidVi  , dQidVj  , dQidTi  , dQidTj  };
	DVEC Hx;
	std::vector<Hx_t> tHx;
	std::vector<uint> Hxi;
	std::vector<std::vector<uint>> Hxj;
	for ( int ii = 0 ; ii < zqty ; ii++ ) {
		// for each measurement function:
		for ( int jj = 0 ; jj < xqty ; jj ++ ) {
			// We might want to have established a unified state vector by now
			// establish the derivetive with respect to each state
			// Hx.append(initial value)
			// tHx.append(type [Hx_t])
			// i???
			// j???
			// rows correspond to measurements
			// columns correspond to derivatives with respect to states
		}
	}
	
	// Handoff from the topology processor to the state estimator
	// ------------------------------------------------------------------------
	// INTERNAL VARIABLE INITIALIZATION
	// ------------------------------------------------------------------------
	// Initialize State Vector x
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *xraw = cs_spalloc(0,0,xqty,1,1);
	for ( int ii = 0 ; ii < xV.size() ; ii++ )
		cs_entry(xraw,ii,0,xV[ii]);
	for ( int ii = 0 ; ii < xT.size() ; ii++ )
		cs_entry(xraw,xV.size()+ii,0,xT[ii]);
	cs *x = cs_compress(xraw); cs_spfree(xraw);
	
	// Initialize measurement covariance matrix R
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *Rraw = cs_spalloc(0,0,zqty,1,1);
	for ( int ii = 0 ; ii < zqty ; ii++ )
		cs_entry(Rraw,ii,ii,ssigs[ii]);
	cs *R = cs_compress(Rraw); cs_spfree(Rraw);
	
	// State transition matrix F
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *Fraw = cs_spalloc(0,0,xqty,1,1);
	for ( int ii = 0 ; ii < xqty ; ii++ )
		cs_entry(Fraw,ii,ii,1.0);
	cs *F = cs_compress(Fraw); cs_spfree(Fraw);
	
	// Process noise covariance matrix Q
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *Qraw = cs_spalloc(0,0,xqty,1,1);
	for ( int ii = 0 ; ii < xqty ; ii++ )
		cs_entry(Qraw,ii,ii,0.04*sqrt(1.0/4));
	cs *Q = cs_compress(Qraw); cs_spfree(Qraw);
	
	// Identity matrix of dimention of x eyex
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *eyexraw = cs_spalloc(0,0,xqty,1,1);
	for ( int ii = 0 ; ii < xqty ; ii++ )
		cs_entry(eyexraw,ii,ii,1.0);
	cs *eyex = cs_compress(eyexraw); cs_spfree(eyexraw);
	
	// Identity matrix of dimention of z eyez
	// cs_spalloc(m,n,nzmax,values,triplet)
	cs *eyezraw = cs_spalloc(0,0,zqty,1,1);
	for ( int ii = 0 ; ii < zqty ; ii++ )
		cs_entry(eyexraw,ii,ii,1.0);
	cs *eyez = cs_compress(eyezraw); cs_spfree(eyezraw);
	
	// Initialized error covariance matrix P
	cs *Praw = cs_spalloc(0,0,xqty*xqty,1,1);
	// initialize to zero on a cold start?
	cs *P = cs_compress(Praw); cs_spfree(Praw);
	
	
	
	// ------------------------------------------------------------------------
	// STATE ESTIMATOR LOOP
	// ------------------------------------------------------------------------
	bool quit = false;
	while(!quit) {
		// --------------------------------------------------------------------
		// Check for New Measurements
		// --------------------------------------------------------------------
		while ( 0 /* check for new measurements */ ) {
			// #include <chrono>
			// #include <thread>
			// std::this_thread::sleep_for(std::chrono::milliseconds(1000));
		}
		
		
		// --------------------------------------------------------------------
		// Read Measurements
		// --------------------------------------------------------------------
		for( int ii = 0 ; ii < sense.size() ; ii++ ) {
			// if ( /* sname[ii] in json */ ) {
				// sense[ii] = json[sname[ii]];
			// }
		}
		
		// --------------------------------------------------------------------
		// Update Measurement Function h(x)
		// --------------------------------------------------------------------
		for ( int idx = 0 ; idx < hx.size() ; idx++ ) {
			uint i = hxi[idx];
			std::vector<uint> js = hxj[idx];
			if ( Pij == thx[idx] ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				hx[idx] = xV[i]*xV[i]*gij - xV[i]*xV[j] * 
					( gij*cos(Tij) + bij*sin(Tij) );
			}
			else if ( thx[idx] == Qij ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				hx[idx] = -1.0*xV[i]*xV[i]*bij - xV[i]*xV[j] * 
					( gij*sin(Tij) - bij*cos(Tij) );
			}
			else if ( thx[idx] == Pi ) {
				double h = 0;
				for ( uint jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[j] * ( Gij*cos(Tij) + Bij*sin(Tij) );
				}
				hx[idx] = h * xV[i];
			}
			else if ( thx[idx] == Qi ) {
				double h = 0;
				for ( unsigned int jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[j] * ( Gij*sin(Tij) - Bij*cos(Tij) );
				}
				hx[idx] = h * xV[i];
			}
		}
		
		
		// --------------------------------------------------------------------
		// Update Measurement Jacobian H(x)
		// --------------------------------------------------------------------
		for ( int idx = 0 ; idx < Hx.size() ; idx++ ) {
			uint i = Hxi[idx];
			std::vector<uint> js = Hxj[idx];
			// ----------------------------------------------------------------
			// Partial derivatives of real power flow measurements
			// ----------------------------------------------------------------
			if ( tHx[idx] == dPijdVi ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] =  -1.0*xV[j] * ( gij*cos(Tij) + bij*sin(Tij) ) + 2*gij*xV[i];
			}
			else if ( tHx[idx] == dPijdVj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = -1.0*xV[i] * ( gij*cos(Tij) + bij*sin(Tij) );
			}
			else if ( tHx[idx] == dPijdTi ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = xV[i]*xV[j] * ( gij*sin(Tij) - bij*cos(Tij) );
			}
			else if ( tHx[idx] == dPijdTj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = -1.0*xV[i]*xV[j] * ( gij*sin(Tij) - bij*cos(Tij) );
			}
			// ----------------------------------------------------------------
			// Partial derivatives of reactive power flow measurements
			// ----------------------------------------------------------------
			else if ( tHx[idx] == dQijdVi ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = -1.0*xV[j] * ( gij*sin(Tij) - bij*cos(Tij) ) - 2.0*xV[i]*bij;
			}
			else if ( tHx[idx] == dQijdVj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = -1.0*xV[i] * ( gij*sin(Tij) - bij*cos(Tij) );
			}
			else if ( tHx[idx] == dQijdTi ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = -1.0*xV[i]*xV[j] * ( gij*cos(Tij) + bij*sin(Tij) );
			}
			else if ( tHx[idx] == dQijdTj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double gij = std::real(-1.0*Ym[i][j]);
				double bij = std::imag(-1.0*Ym[i][j]);
				Hx[idx] = xV[i]*xV[j] * ( gij*cos(Tij) + bij*sin(Tij) );
			}
			// ----------------------------------------------------------------
			// Partial derivatives of real power injection measurements
			// ----------------------------------------------------------------
			else if ( tHx[idx] == dPidVi ) {
				double h = 0;
				for ( int jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[j] * ( Gij*cos(Tij) + Bij*sin(Tij) );
				}
				Hx[idx] = h + xV[i]*std::real(Ym[i][i]);
			}
			else if ( tHx[idx] == dPidVj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double Gij = std::real(Ym[i][j]);
				double Bij = std::imag(Ym[i][j]);
				Hx[idx] = xV[i] * ( Gij*cos(Tij) + Bij*sin(Tij) );
			}
			else if ( tHx[idx] == dPidTi ) {
				double h = 0;
				for ( int jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[i]*xV[j]*( -1.0*Gij*sin(Tij) + Bij*cos(Tij) );
				}
				Hx[idx] = h - xV[i]*xV[i]*std::imag(Ym[i][i]);
			}
			else if ( tHx[idx] == dPidTj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double Gij = std::real(Ym[i][j]);
				double Bij = std::imag(Ym[i][j]);
				Hx[idx] = xV[i]*xV[j] * ( Gij*sin(Tij) - Bij*cos(Tij) );
			}
			// ----------------------------------------------------------------
			// Partial derivatives of reactive power injection measurements
			// ----------------------------------------------------------------
			else if ( tHx[idx] == dQidVi ) {
				double h = 0;
				for ( int jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[j] * ( Gij*sin(Tij) - Bij*cos(Tij) );
				}
				Hx[idx] =  h - xV[i]*std::imag(Ym[i][i]);
			}
			else if ( tHx[idx] == dQidVj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double Gij = std::real(Ym[i][j]);
				double Bij = std::imag(Ym[i][j]);
				Hx[idx] = xV[i] * ( Gij*sin(Tij) - Bij*cos(Tij) );
			}
			else if ( tHx[idx] == dQidTi ) {
				double h = 0;
				for ( int jdx = 0 ; jdx < js.size() ; jdx++ ) {
					uint j = js[jdx];
					double Tij = xT[i] - xT[j];
					double Gij = std::real(Ym[i][j]);
					double Bij = std::imag(Ym[i][j]);
					h += xV[i]*xV[j] * ( Gij*cos(Tij) + Bij*sin(Tij) );
				}
				Hx[idx] = h - xV[i]*xV[i]*std::real(Ym[i][i]);
			}
			else if ( tHx[idx] == dQidTj ) {
				uint j = js[0];
				double Tij = xT[i] - xT[j];
				double Gij = std::real(Ym[i][j]);
				double Bij = std::imag(Ym[i][j]);
				Hx[idx] =  xV[i]*xV[j] * ( -1.0*Gij*cos(Tij) - Bij*sin(Tij) );
			}
		}
		
		// --------------------------------------------------------------------
		// Estimate State
		// --------------------------------------------------------------------
		
		
		
		// --------------------------------------------------------------------
		// Setup variables
		// --------------------------------------------------------------------
		// Some variables are established outside of the loop
		// z
		// Note: to improve efficiency, initialize a csc before loop and
		//		insert new measurements directly into the csc
		cs *zraw = cs_spalloc(0,0,zqty,1,1);
		for ( int ii = 0 ; ii < zqty ; ii++ )
			cs_entry(zraw,ii,0,sense[ii]);
		cs *z = cs_compress(zraw); cs_spfree(zraw);
		
		// h
		// Note: to improve efficiency, initialize a csc before loop and
		//		insert new values computed above directly into the csc
		cs *hraw = cs_spalloc(0,0,zqty,1,1);
		for ( int ii = 0 ; ii < zqty ; ii++ )
			cs_entry(hraw,ii,0,hx[ii]);
		cs *h = cs_compress(hraw); cs_spfree(hraw);
		
		// H
		// Note: to improve efficiency, initialize a csc before loop and
		//		insert new values computed above directly into the csc
		cs *Hraw = cs_spalloc(0,0,xqty*zqty,1,1);
		// for ( int ii = 0 ; ii < Hx.size() ; ii ++
		cs *H = cs_compress(Hraw); cs_spfree(Hraw);
		
		
		
		// --------------------------------------------------------------------
		// Predict Step
		// --------------------------------------------------------------------
		// -- compute x_predict = F*x
		cs *xpre = cs_multiply(F,x);
		// -- compute p_predict = F*P*F'+Q
		cs *P1 = cs_transpose(F,1);
		cs *P2 = cs_multiply(P,P1); cs_spfree(P1);
		cs *P3 = cs_multiply(F,P2); cs_spfree(P2);
		cs *Ppre = cs_add(P3,Q,1,1); cs_spfree(P3);
		// clean up
		// cs_spfree(xc);
		// cs_spfree(Fc);
		// cs_spfree(Qc);
		
		// --------------------------------------------------------------------
		// Update Step
		// --------------------------------------------------------------------
		// -- compute y = H*x_predict + z
		cs *y1 = cs_multiply(H,xpre);
		cs *yupd = cs_add(z,y1,1,-1); cs_spfree(y1);
		// -- compute S = H*P_predict*H' + R
		cs *S1 = cs_transpose(H,1);
		cs *S2 = cs_multiply(Ppre,S1); cs_spfree(S1);
		cs *S3 = cs_multiply(H,S2); cs_spfree(S2);
		cs *Supd = cs_add(R,S3,1,1); cs_spfree(S3);
		// -- compute K = P_predict*H'*S^-1
		cs *K1 = cs_transpose(H,1);
		cs *K2 = cs_multiply(Ppre,K1); cs_spfree(K1);
			// cs *K3 = invertcs(Supd);
			// Initialize klusolve variables
			klu_symbolic *klusym;
			klu_numeric *klunum;
			klu_common klucom;
			if (!klu_defaults(&klucom)) throw "klu_defaults failed";
			klusym = klu_analyze(Supd->m,Supd->p,Supd->i,&klucom);
			if (!klusym) throw "klu_analyze failed in se_bcse_kfe";
			klunum = klu_factor(Supd->p,Supd->i,Supd->x,klusym,&klucom);
			if (!klunum) throw "klu_factor failed in se_bcse_kfe";
			// Initialize an identiy right-hand size
			double *rhs = new double[zqty*zqty];
			for ( int ii = 0 ; ii < zqty*zqty ; ii++ )
				rhs[ii] = ii/zqty == ii%zqty ? 1 : 0;
			klu_solve(klusym,klunum,Supd->m,Supd->n,rhs,&klucom);
			// Convert the inverted result to cs*
			cs *K3raw = cs_spalloc(0,0,zqty*zqty,1,1);
			for ( int ii = 0 ; ii < zqty ; ii++ )
				for ( int jj = 0 ; jj < zqty ; jj++ )
					if (rhs[ii+zqty*jj])
						cs_entry(K3raw,ii,jj,rhs[ii+zqty*jj]);
			delete rhs;
		cs *K3 = cs_compress(K3raw); cs_spfree(K3raw);
		cs *Kupd = cs_multiply(K2,K3); cs_spfree(K2); cs_free(K3);
		
		
		cs *x1 = cs_multiply(Kupd,yupd);
		cs *xupd = cs_add(xpre,x1,1,1); cs_spfree(x1);
		
		// -- compute P = (K*H+I)*P_predict
		cs *P4 = cs_multiply(Kupd,H);
		cs *P5 = cs_add(eyex,P4,1,-1); cs_spfree(P4);
		cs *Pupd = cs_multiply(P5,Ppre); cs_spfree(P5);
		// Cleanup
		cs_spfree(xpre);
		cs_spfree(Ppre);
		cs_spfree(yupd);
		cs_spfree(Supd);
		cs_spfree(Kupd);
		// cs_spfree(Pupd);
		// cs_spfree(z);
		// cs_spfree(H);
		// cs_spfree(R);
		
		
		// Shift updated variables
		cs_spfree(P); P = Pupd; // delete Pupd; //cs_spfree(Pupd);
		// cs_spfree(yint); y = yupd; // delete yupd; //cs_spfree(yupd);
		
		
		// Compute Full State
		
		
		
		// Publish State
		
		
		
		quit = true;
	}
	
	
	
	return 0;
}