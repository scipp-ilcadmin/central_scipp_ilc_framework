/*
 * Checksum.java
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

public class Checksum extends Driver {


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
        logCount1 = 0;
        logCount2 = 0;
        String root_mode = "UPDATE";

        try {
            root = new Jroot(jrootFile,root_mode);
            root.init("TH1D", "gt_pt_initial", "gt_pt_initial", "gt_pt_initial", 1000, -10,10);
            root.init("TH1D", "gt_pt_hadronic", "gt_pt_hadronic", "gt_pt_hadronic", 1000, -10,10);
            root.init("TH1D", "gt_pt_all", "gt_pt_all", "gt_pt_all", 1000, -10,10);
            root.init("TH1D", "lt_pt_initial", "lt_pt_initial", "lt_pt_initial", 1000, -10,10);
            root.init("TH1D", "lt_pt_hadronic", "lt_pt_hadronic", "lt_pt_hadronic", 1000, -10,10);
            root.init("TH1D", "lt_pt_all", "lt_pt_all", "lt_pt_all", 1000, -10,10);
            root.init("TH1D", "all_pt_initial", "all_pt_initial", "all_pt_initial", 1000, -10,10);
            root.init("TH1D", "all_pt_hadronic", "all_pt_hadronic", "all_pt_hadronic", 1000, -10,10);
            root.init("TH1D", "all_pt_all", "all_pt_all", "all_pt_all", 1000, -10,10);

            //file process loop
            int total = 0;
            //int limit = 10000;
            for(String filename: stdhepfilelist) {
                System.out.println("FILENAME = " + filename);
                StdhepReader reader = new StdhepReader(filename);
                for (int i=0;i<reader.getNumberOfEvents();i++) {
                    StdhepRecord record = reader.nextRecord();
                    if (record instanceof StdhepEvent) {
                        StdhepEvent event = (StdhepEvent) record;
                        if (++total % 1000 == 0) {
                            System.out.println("    TOTAL = " + total);
                            root.proc("f.Write()");
                        }
                        //do stuff with even
                        analyze(event);
                    }
                    //if (total > limit) break;
                }
                //if (total > limit) break;
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

        int electronI = -1;
        int positronI = -1;
        double electronEnergy = 0;
        double positronEnergy = 0;

        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP(particleI) != FINAL_STATE ) continue;

            int pdgid = event.getIDHEP( particleI ) ;
            double energy = event.getPHEP(particleI, 3);

            if ( pdgid == Electron_ID && energy > electronEnergy ) {
                electronI = particleI;
                electronEnergy = energy;
            }
            if ( pdgid == Positron_ID && energy > positronEnergy ) {
                positronI = particleI;
                positronEnergy = energy;
            }
        }




        //these two sets of values are witheld from the total
        //until it is determined that the particle they derived
        //their values from was not the initial electron or positron
        //i.e. that particle's energy is not the highest


        //not-detectable  , detectable  : px, py, pz, E, scalar
        double[] totals = {0,0,0,0};

        //find initial electron and positron
        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP(particleI) != FINAL_STATE ) continue;
            if ( particleI == electronI ) continue;
            if ( particleI == positronI ) continue;
            int pdgid = event.getIDHEP( particleI ) ;

            double mom_x  = event.getPHEP(particleI, 0); 
            double mom_y  = event.getPHEP(particleI, 1); 
            double mom_z  = event.getPHEP(particleI, 2);
            double energy = event.getPHEP(particleI, 3);

            totals[0] += mom_x;
            totals[1] += mom_y;
            totals[2] += mom_z;
            totals[3] += energy;
        }

        double square_px = Math.pow( totals[0], 2 );
        double square_py = Math.pow( totals[1], 2 );
        double square_pz = Math.pow( totals[2], 2 );
        double square_pE = Math.pow( totals[3], 2 );
        double hadronic_mass_squared = square_pE - square_px - square_py - square_pz;
        if ( (-1e-9) < hadronic_mass_squared && hadronic_mass_squared < 0 ) hadronic_mass_squared = 0.0;
        double hadronic_mass = Math.sqrt(hadronic_mass_squared);


        double init_mom_x  = event.getPHEP(electronI, 0);
        double init_mom_y  = event.getPHEP(electronI, 1);
        init_mom_x  += event.getPHEP(positronI, 0);
        init_mom_y  += event.getPHEP(positronI, 1);
        double init_mom_transverse = Math.sqrt(init_mom_x*init_mom_x + init_mom_y*init_mom_y);
        
        double hadronic_transverse_momentum = Math.sqrt( square_px + square_py );

        double total_mom_x = init_mom_x + totals[0];
        double total_mom_y = init_mom_y + totals[1];
        double total_mom_transverse = Math.sqrt(total_mom_x*total_mom_x + total_mom_y*total_mom_y);


        try {
            double hadronic_mass_cut = 2.0;
            if ( hadronic_mass > hadronic_mass_cut ) {
                root.fill("gt_pt_hadronic", hadronic_transverse_momentum);
                root.fill("gt_pt_initial", init_mom_transverse);
                root.fill("gt_pt_all", total_mom_transverse);
            } else if ( hadronic_mass < hadronic_mass_cut ) {
                root.fill("lt_pt_hadronic", hadronic_transverse_momentum);
                root.fill("lt_pt_initial", init_mom_transverse);
                root.fill("lt_pt_all", hadronic_transverse_momentum + init_mom_transverse);
            }
            root.fill("all_pt_hadronic", hadronic_transverse_momentum);
            root.fill("all_pt_initial", init_mom_transverse);
            root.fill("all_pt_all", hadronic_transverse_momentum + init_mom_transverse);

        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

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
    private int logCount1;
    private int logCount2;

    //xml derived variables
    private String jrootFile = "";
    private ArrayList<String> stdhepfilelist = new ArrayList();

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
}
