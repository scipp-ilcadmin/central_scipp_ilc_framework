/*
 * MomentumTransferAnalysis.java
 *
 * Created on 21 Feb 16
 * @author Spenser Estrada
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;

public class MomentumTransferAnalysis extends Driver {
    
    
   //DEFINE XML FUNCTIONS
   //These functions are specially formatted functions to pull variable data
   //from the xml file
   /**************************************************************************
      XML FUNCTION FORMAT

   public void //setVariablename(variable type) { 
      //the first letter after "set" must be uppercase
      //but can (must?) be lowercase in xml file
      set variable here; 
   }
   **************************************************************************/

   public void setOutputfile(String s) {
      this.jrootFile = s;
   }
 
   public void setBeamOutAligned(boolean algn) {
      aligned = algn;
   }
     
   //END DEFINE XML FUNCTIONS

   //This function is called when the program is first started
   //and initializes all persistent data
   public void startOfData() {
      eventNumber = 0;
      String root_mode = "NEW";
        
      try {
         root = new Jroot(jrootFile,root_mode);
         // Resultant Momentum (Pr) vs. Momentum Transfer ( sqrt(Q^2) )
         root.init("TH2D","prVQsquare","prVQsquare","#P_r vs. #sqrt{Q^2}", 350, 1, 3, 350, 0, 1.8 );
         // Ocurrences of #P_r
         root.init("TH1D","timesVpr","timesVpr","Occurences of #P_r of Resultant Particles", 350, 0, 1.8 );
       } catch(java.io.IOException e) {
          System.out.println(e);
          System.exit(1);
       }
    }

    // This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        try {
            root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

   // PROCESS FUNCTION
   public void process( EventHeader event ) { 
      super.process( event );
      int p_count_limit = 1000;
      boolean use_limit = false;
      int p_count = 0;
      double [] pr = {0,0};
      double Q = 0;
      double elec_theta = 0; double posi_theta = 0;
      try {
         for (MCParticle p : event.getMCParticles() ) {
            int ID = p.getPDGID();
            boolean fin_st =(p.getGeneratorStatus()==MCParticle.FINAL_STATE);
            boolean primary = ( p.getParents().size()==0 ||
                                p.getParents().size()==2 );
            boolean neutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            if( fin_st && !neutrino ){
               // Want max of Positron and Electron Q
               if( primary ){
                  double R = getQ(p);
                  if ( R>Q ) Q = R;
               }
               // Vector summmation of all resultant particle perp momentum
               else {
                  addPperp( pr, p );
               }
            }
            p_count++;
            if ( use_limit && (p_count++ > p_count_limit) ) break;    
         }
         double mag_pr = Math.sqrt( Math.pow(pr[0], 2)+ Math.pow(pr[1], 2 ) );
         root.fill("timesVpr", mag_pr );
         root.fill("prVQsquare", Q, mag_pr);

      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      } 
      System.out.println("finished event "+ ++eventNumber);        

   }//End Process

   public static double magPxPy(MCParticle p){
      double px = p.getPX(); double py = p.getPY();
      double sum = Math.pow(px, 2) + Math.pow(py, 2);
      return Math.pow(sum, .5);
   }
   
   public static double getQ(MCParticle p){
      double En = p.getEnergy();
      double P = p.getMomentum().magnitude();
      double x = p.getPX(); double y = p.getPY(); double z = p.getPZ();
      double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2) );
      double Q = 2*En*P*(1 - ( Math.abs(z)/r ) );
      return Math.sqrt( Q );
   }
   
   // Vector addition of perpindicular momentum to existing vector
   public static void addPperp( double[] v, MCParticle p ){
      double px = p.getPX(); double py = p.getPY();
      v[0]+=px; v[1]+=py;
   }

   public static double getTheta(MCParticle p){
      double x = p.getPX(); double y = p.getPY(); double z = p.getPZ();
      double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2) );
      return Math.abs( Math.acos(z/r) ) ;
   }


   // tests if particle final position is more than a certain cos(theta)
   // away from the center line (z axis)
   public static boolean cosTheta(MCParticle p, double cos_t ){
      double x = p.getPX(); double y = p.getPY(); double z = p.getPZ();
      double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2) );
      System.out.println("z: "+z+" r: "+r+" z/r: "+(z/r) );
      return ( Math.abs(z/r) < cos_t );
   }
    

   /*here all the classwide variables are declared*/
   private int eventNumber;

   //xml derived variables
   private String jrootFile = "";

   //variables for jroot file construction and background/signal file reading
   private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
   private Jroot root;
   private boolean aligned;
}
