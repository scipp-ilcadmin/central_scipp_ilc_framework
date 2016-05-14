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
            root.init("TH1D", "px", "px", "px", 1000, -10,10);
            root.init("TH1D", "py", "py", "py", 1000, -10,10);
            root.init("TH1D", "pz", "pz", "pz", 1000, -10,10);

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
        int electron = 0;
        double eE = 0;
        int positron = 0;
        double pE = 0;

        for (int particleI = 0; particleI < number_particles; particleI++) {
            int pdgid = event.getIDHEP( particleI );
            double energy = event.getPHEP(particleI,3);
            if (pdgid == Electron_ID && energy > eE) {
                eE = energy;
                electron = particleI;
            }
            if (pdgid == Positron_ID && energy > pE) {
                pE = energy;
                positron = particleI;
            }
        }

        double px = 0;
        double py = 0;
        double pz = 0;

        //px += event.getPHEP(electron, 0); 
        //py += event.getPHEP(electron, 1); 
        //pz += event.getPHEP(electron, 2);
        //px += event.getPHEP(positron, 0); 
        //py += event.getPHEP(positron, 1); 
        //pz += event.getPHEP(positron, 2);


        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP(particleI) != FINAL_STATE ) continue;
            //if ( particleI == electron ) continue;
            //if ( particleI == positron ) continue;
            int pdgid = event.getIDHEP( particleI ) ;

            px += event.getPHEP(particleI, 0); 
            py += event.getPHEP(particleI, 1); 
            pz += event.getPHEP(particleI, 2);
        }

        try {
            root.fill("px", px);
            root.fill("py", py);
            root.fill("pz", pz);

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
