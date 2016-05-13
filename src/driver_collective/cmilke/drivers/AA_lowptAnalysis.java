/*
 * aa_lowptAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.PolarCoords;

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

public class AA_lowptAnalysis extends Driver {


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

        try {
            root = new Jroot(jrootFile,root_mode);
            root.init("TH1D", "detect_scalar", "detect_scalar", "detect_scalar", 1000, 0, 1000);
            root.init("TH1D", "detect_vector", "detect_vector", "detect_vector", 1000, 0, 1000);
            root.init("TH1D", "detect_mass", "detect_mass", "detect_mass", 1000, 0, 1000);
            root.init("TH1D", "true_scalar", "true_scalar", "true_scalar", 1000, 0, 1000);
            root.init("TH1D", "true_vector", "true_vector", "true_vector", 1000, 0, 1000);
            root.init("TH1D", "true_mass", "true_mass", "true_mass", 1000, 0, 1000);

            //file process loop
            int total = 0;
            int limit = 1000;
            for(String filename: stdhepfilelist) {
                System.out.println("\n\n\n\n\n\n\nFILENAME = " + filename + "\n\n\n\n\n\n\n\n");
                StdhepReader reader = new StdhepReader(filename);
                for (int i=0;i<reader.getNumberOfEvents();i++) {
                    StdhepRecord record = reader.nextRecord();
                    if (record instanceof StdhepEvent) {
                        StdhepEvent event = (StdhepEvent) record;
                        if (total++ % 10000 == 0) System.out.println("\n\n\n   TOTAL = " + total + "\n\n\n");
                        //do stuff with even
                        analyze(event);
                    }
                    if (total++ > limit) break;
                }
                if (total > limit) break;
            } 
            System.out.println("\n\nFINISHED " + total);
            root.end();

        } catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void analyze(StdhepEvent event){
        int number_particles = event.getNHEP();

        //these two sets of values are witheld from the total
        //until it is determined that the particle they derived
        //their values from was not the initial electron or positron
        //i.e. that particle's energy is not the highest
        double[] init_e = {0,0,0,0,0}; //px,py,pz,E,detectable
        double[] init_p = {0,0,0,0,0};

        //not-detectable  , detectable  : px, py, pz, E, scalar
        double[][] totals = { {0,0,0,0,0}, {0,0,0,0,0} };

        //find initial electron and positron
        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP( particleI ) != FINAL_STATE) continue;

            int pdgid = event.getIDHEP( particleI ) ;

            double mom_x  = event.getPHEP(particleI, 0); 
            double mom_y  = event.getPHEP(particleI, 1); 
            double mom_z  = event.getPHEP(particleI, 2);
            double energy = event.getPHEP(particleI, 3);

            int is_detectable = check_if_detectable(pdgid,mom_x,mom_y,mom_z);

            if ( pdgid == Electron_ID && energy > init_e[3] ) {
                update_totals(totals,init_e[0],init_e[1],init_e[2],init_e[3],(int)init_e[4]);
                update_initial(init_e,mom_x,mom_y,mom_z,energy,(int)is_detectable);

            } else if ( pdgid == Positron_ID && energy > init_p[3] ) {
                update_totals(totals,init_p[0],init_p[1],init_p[2],init_p[3],(int)init_p[4]);
                update_initial(init_p,mom_x,mom_y,mom_z,energy,(int)is_detectable);

            } else {
                update_totals(totals,mom_x,mom_y,mom_z,energy,is_detectable);
            }
        }


        double square_detect_px = Math.pow( totals[1][0], 2 );
        double square_detect_py = Math.pow( totals[1][1], 2 );
        double square_detect_pz = Math.pow( totals[1][2], 2 );
        double square_detect_pE = Math.pow( totals[1][3], 2 );

        double total_detect_scalar = totals[1][4];
        double total_detect_vector = Math.sqrt(square_detect_px + square_detect_py);
        double total_detect_mass   = Math.sqrt( square_detect_pE - square_detect_px - square_detect_py - square_detect_pz );


        double square_true_px = Math.pow( totals[0][0] + totals[1][0], 2 );
        double square_true_py = Math.pow( totals[0][1] + totals[1][1], 2 );
        double square_true_pz = Math.pow( totals[0][2] + totals[1][2], 2 );
        double square_true_pE = Math.pow( totals[0][3] + totals[1][3], 2 );

        double total_true_scalar = totals[0][4] + totals[1][4];
        double total_true_vector = Math.sqrt(square_true_px + square_true_py);
        double total_true_mass   = Math.sqrt( square_true_pE - square_true_px - square_true_py - square_true_pz );

        try {
            root.fill("detect_scalar", total_detect_scalar);
            root.fill("detect_vector", total_detect_vector);
            root.fill("detect_mass", total_detect_mass);
            root.fill("true_scalar", total_true_scalar);
            root.fill("true_vector", total_true_vector);
            root.fill("true_mass", total_true_mass);

        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

    }

    private void update_initial(double[] init_array, double mom_x, double mom_y, 
                                double mom_z, double energy, double is_detectable) {

        init_array[0] = mom_x;
        init_array[1] = mom_y;
        init_array[2] = mom_z;
        init_array[3] = energy;
        init_array[4] = is_detectable;
    }

    private void update_totals( double[][] totals, double mom_x, double mom_y, double mom_z, double energy, int is_detectable ) {
        totals[is_detectable][0] += mom_x;
        totals[is_detectable][1] += mom_y;
        totals[is_detectable][2] += mom_z;
        totals[is_detectable][3] += energy;
        totals[is_detectable][4] += Math.sqrt( mom_x*mom_x + mom_y*mom_y );
    }

    private int check_if_detectable( int id, double mom_x, double mom_y, double mom_z ) {
        boolean is_neutrino = ( id == 12 || id == -12 || 
                                id == 14 || id == -14 ||
                                id == 16 || id == -16 ||
                                id == 18 || id == -18 );

        double mom_t = PolarCoords.CtoP(mom_x,mom_y)[0]; //transverse momentum
        double cos_theta = Math.pow( Math.pow(mom_t/mom_z,2) + 1, -1/2 );
        boolean is_not_forward = ( cos_theta < 0.9 );

        //return 0 (is not detectable) if particle is neutrino or is not forward
        return ( is_neutrino || is_not_forward ) ? 0 : 1;
    }


    // Generator Statuses
    private final int DOCUMENTATION = 3;
    private final int FINAL_STATE = 1;
    private final int INTERMEDIATE = 2;

    // particle IDs
    private final int Electron_ID = +11;
    private final int Positron_ID = -11;

    /*here all the classwide variables are declared*/
    private int eventNumber;

    //xml derived variables
    private String jrootFile = "";
    private ArrayList<String> stdhepfilelist = new ArrayList();

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
}
