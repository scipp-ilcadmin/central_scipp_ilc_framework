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
import java.io.FileWriter;

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
      //String root_mode = "NEW";
      System.out.println("\n\nHELLOOOOO START\n\n");
        
      try {
         //root = new Jroot(jrootFile,root_mode);

         // Resultant Momentum (Pr) vs. Momentum Transfer ( sqrt(Q^2) )
         //root.init("TH2D","prVQ","prVQ","P#_{r} (y) vs. #sqrt{Q#^{2} } (x)", 200, 0, 3, 200, 0, 5 );
         //root.init("TH2D","prVeQ","prVeQ","P#_{r} (y) vs. #sqrt{Q#^{2} } (x)", 200, 0, 3, 200, 0, 5 );
         //root.init("TH2D","prVpQ","prVpQ","P#_{r} (y) vs. #sqrt{Q#^{2} } (x)", 200, 0, 3, 200, 0, 5 );
       
         /* Differentiate output by 
          * 1) which HE particle measuring Q of 
          *    both: "prVQ", e-: "prVeQ", e+: "prVpQ
          * 2) Total invariant mass (M)
          *    >= 2GeV: "hiM", <2GeV: "loM"
          */
         String path = jrootFile;        
 
         fw_hiM = new FileWriter(path+"_prVQ_hiM"); 
         efw_hiM = new FileWriter(path+"_prVeQ_hiM");    
         pfw_hiM = new FileWriter(path+"_prVpQ_hiM");    
         fw_loM = new FileWriter(path+"_prVQ_loM");
         efw_loM = new FileWriter(path+"_prVeQ_loM");
         pfw_loM = new FileWriter(path+"_prVpQ_loM");

         //file process loop
         int total = 0;
         int limit = 2000000;
         for(String filename: stdhepfilelist) {
            StdhepReader reader = new StdhepReader(filename);
            for (int i=0;i<reader.getNumberOfEvents();i++) {
               StdhepRecord record = reader.nextRecord();
               if (record instanceof StdhepEvent) {
                  StdhepEvent event = (StdhepEvent) record;
                  //do stuff with event
                  analyze(event);
               }
               if (total++ > limit) break;
            }
            if (total > limit) break;
         } 
         //root.end();
         fw_loM.close();
         efw_loM.close();
         pfw_loM.close();
         fw_hiM.close();
         efw_hiM.close();
         pfw_hiM.close();
         System.out.println("MaxQ: "+maxQ+", MaxR: "+maxR);

      } catch (java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public void analyze(StdhepEvent event){
      int n = event.getNHEP();
      // Values of Momentum Transfer for e- (Q) and e+ (R)
      double Q = 0; double R = 0;
      // Array to hold perpindicular momenta
      double x_tot=0; double y_tot=0; double z_tot=0;
      // Checking for P conservation
      double px_tot, py_tot, pz_tot;
      px_tot = 0; py_tot = 0; pz_tot = 0;
      double En_tot = 0;
      try{
         //System.out.println("\n=======================================\n");
         //System.out.println("\n"+n+" particle event\n");
         for ( int p = 0; p<n; p++) {
            int ID = event.getIDHEP( p );                           
            boolean fin_st = ( event.getISTHEP( p ) == FINAL_STATE);
            
            // Momentum and energy values
            double x = event.getPHEP(p, 0); 
            double y = event.getPHEP(p, 1); 
            double z = event.getPHEP(p, 2);
            double u = event.getVHEP(p, 0);
            double v = event.getVHEP(p, 1);
            double w = event.getVHEP(p, 2);
            double En = event.getPHEP(p, 3);
            // Running Energy sum of partcles not HE e-||e+
 
            boolean neutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            
            // Include or exclude neutrinos
            boolean filterNeut = ( !neutrino);
            if( filterNeut && fin_st ){
               // Want max of Positron and Electron Q
               if( ID==11 && En>100.0 )
                  Q = getQ( x, y, z, En);
                  if (Q>maxQ) maxQ = Q;
               if( ID==-11 && En>100.0 )
                  R = getQ( x, y, z, En);
                  if (R>maxR) maxR = R;
               // Vector summmation of all resultant particle perp momentum
               else {
                  x_tot+=x; y_tot+=y; z_tot+=z;
                  En_tot+=En;
               }
               // Sum of momenta to chcek P conservation
               px_tot+=x; py_tot+=y; pz_tot+=z;
            }
         }
         double mag_pr = Math.sqrt( x_tot*x_tot + y_tot*y_tot );
         double P = Q;
         
         // Relativity-invariant mass of particles not HE e-||e+
         double M =  Math.sqrt( En_tot*En_tot - 
                     (x_tot*x_tot + y_tot*y_tot + z_tot*z_tot) );
         if (R>Q) P = R;
         
         if( M>= 2.0){
            fw_hiM.write(P+" "+mag_pr+";\n");
            efw_hiM.write(Q+" "+mag_pr+";\n");
            pfw_hiM.write(R+" "+mag_pr+";\n");
         } else {
            fw_loM.write(P+" "+mag_pr+";\n");
            efw_loM.write(Q+" "+mag_pr+";\n");
            pfw_loM.write(R+" "+mag_pr+";\n");
         }         

         // P Conservation test
         if ( pConservationViolated(px_tot, py_tot, pz_tot) ){
            System.out.println("Event "+n+" violates momentum conservation:");
            System.out.println("P: "+px_tot+", "+py_tot+", "+pz_tot);
         }
      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   // Calculaes quantity Q, momentum transfer. I dont know what that means
   public static double getQ( double x, double y, double z, double En ){
      double r = Math.sqrt( x*x + y*y + z*z );
      double P = r;
      double Q2 = 2*En*P*(1 - ( Math.abs(z)/r ) );
      //System.out.println("Q in function: "+Q2 );
      return Math.sqrt( Q2 );
   }

   // checks if the particle momentum can even
   public static boolean pConservationViolated(double x, double y, double z){
      if( Math.abs(x) > 0.0001 ) return true;
      if( Math.abs(y) > 0.0001 ) return true;
      if( Math.abs(z) > 0.0001 ) return true; 
      return false;
   }

   // Generator Statuses
   public static final int DOCUMENTATION = 3;
   public static final int FINAL_STATE = 1;
   public static final int INTERMEDIATE = 2;
 
   double maxQ = 0;
   double maxR = 0; 

   /*here all the classwide variables are declared*/
   private int eventNumber;

   //xml derived variables
   private String jrootFile = "";
   private ArrayList<String> stdhepfilelist = new ArrayList();

   //variables for jroot file construction and background/signal file reading
   private Jroot root;
   private FileWriter fw_loM; 
   private FileWriter efw_loM; 
   private FileWriter pfw_loM;
   private FileWriter fw_hiM; 
   private FileWriter efw_hiM; 
   private FileWriter pfw_hiM;

}
