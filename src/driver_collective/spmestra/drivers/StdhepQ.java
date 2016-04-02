/*
 * EventAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;

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
import java.io.BufferedReader;
import java.io.FileReader;

import hep.io.stdhep.*;
import hep.io.xdr.XDRInputStream;

public class StdhepQ extends Driver {
    
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void //setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

   public void setOutputfile(String s) {
      this.jrootFile = s;
   }

   public void setStdhepfilelist(String filename) {
      try {
         BufferedReader br = new BufferedReader(new FileReader(filename));
         String line;
         while ((line = br.readLine()) != null) {
            this.stdhepfilelist.add(line);
         }
      }
      catch (java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

    
   //END DEFINE XML FUNCTIONS




   //This function is called when the program is first started
   //and initializes all persistent data
   public void startOfData() {
      eventNumber = 0;
      String root_mode = "NEW";
      System.out.println("\n\nHELLOOOOO START\n\n");
        
      try {
         root = new Jroot(jrootFile,root_mode);

         // Resultant Momentum (Pr) vs. Momentum Transfer ( sqrt(Q^2) )
         root.init("TH2D","prVQsquare","prVQsquare","P#{_r} (y) vs. #sqrt{Q#{^2} } (x)", 200, 0, 3, 200, 0, 1.8 );
         // Ocurrences of #P_r
         root.init("TH1D","timesVpr","timesVpr","Occurences of #P_r of Resultant Particles", 200, 0, 1.8 );

         //file process loop
         int total = 0;
         int limit = 500000;
         for(String filename: stdhepfilelist) {
            if (total %1000==0) System.out.println(total);
            StdhepReader reader = new StdhepReader(filename);
            for (int i=0;i<reader.getNumberOfEvents();i++) {
               StdhepRecord record = reader.nextRecord();
               if (record instanceof StdhepEvent) {
                  //System.out.println(total+"th event");
                  StdhepEvent event = (StdhepEvent) record;
                  //do stuff with even
                  analyze(event);
               }
               if (total++ > limit) break;
            }
            if (total > limit) break;
         } 
         root.end();

      } catch (java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public void analyze(StdhepEvent event){
      int n = event.getNHEP();
      //System.out.println(n+" particle");
      double Q = 0;
      // Array to hold perpindicular momenta
      double x_tot = 0; double y_tot = 0;
      try{
         for ( int p = 0; p<n; p++) {
            int ID = event.getIDHEP( p );                           
            //System.out.println( p+" is a "+ID ); 
            boolean fin_st = ( event.getISTHEP( p ) == FINAL_STATE);
            // Momentum and energy values
            double x = event.getPHEP(p, 0); 
            double y = event.getPHEP(p, 1); 
            double z = event.getPHEP(p, 2);
            double En = event.getPHEP(p, 3);
            //boolean primary = ( event.getJMOHEP(p, 0)==0 );
            boolean primary = ( (ID==11 || ID==-11)
                                 && event.getJMOHEP(p, 1)==0 );
            //System.out.println(ID);
            //System.out.println( "   Parnet: "+event.getJMOHEP(p, 0) );
            //System.out.println( "   Parnet: "+event.getJMOHEP(p, 1) ); 
            //System.out.println("   (x ,y ,z, E) = ("+x+", "+y+", "+z+", "+En+")");
            boolean neutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            if( fin_st && !neutrino ){
               // Want max of Positron and Electron Q
               if( primary ){
                 //System.out.println( event.toString() );
                 //System.out.println("(x ,y ,z, E) = ("+x+", "+y+", "+z+", "+En+")");
                 //System.out.println( "Parnet: "+event.getJMOHEP(p, 0) );
                 //System.out.println( "Parnet: "+event.getJMOHEP(p, 1) ); 
                  double R = getQ(x, y, z, En);
                  if ( R>Q ) Q = R;
               }
               // Vector summmation of all resultant particle perp momentum
               else {
                  x_tot+=x; y_tot+=y;
               }
            }
         }
         double mag_pr = Math.sqrt( x_tot*x_tot + y_tot*y_tot );
         //System.out.println("Q: "+Q );
         //System.out.println("P_t: "+mag_pr+"\n");
         root.fill("timesVpr", mag_pr );
         root.fill("prVQsquare", Q, mag_pr);
      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public static double getEn(double P, double m){
      double c = 298000000;
      return Math.sqrt( P*P*c*c + m*m*Math.pow(c, 4) );
   }

   // Calculaes quantity Q, momentum transfer. I dont know what that means
   public static double getQ( double x, double y, double z, double En ){
      double r = Math.sqrt( x*x + y*y + z*z );
      double P = r;
      double Q2 = 2*En*P*(1 - ( Math.abs(z)/r ) );
      //System.out.println("Q in function: "+Q2 );
      return Math.sqrt( Q2 );
   }

   // Generator Statuses
   public static final int DOCUMENTATION = 3;
   public static final int FINAL_STATE = 1;
   public static final int INTERMEDIATE = 2;
  
   /*here all the classwide variables are declared*/
   private int eventNumber;

   //xml derived variables
   private String jrootFile = "";
   private ArrayList<String> stdhepfilelist = new ArrayList();

   //variables for jroot file construction and background/signal file reading
   private Jroot root;
}
