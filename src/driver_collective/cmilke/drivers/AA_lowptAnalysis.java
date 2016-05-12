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

            // Ocurrences of #P_r
            root.init("TH1D","timesVpr","timesVpr","Occurences of #P_r of Resultant Particles", 200, 0, 1.8 );

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
                    //if (total++ > limit) break;
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

    public void analyze(StdhepEvent event){
        int n = event.getNHEP();
        for ( int p = 0; p<n; p++) {
            int ID = event.getIDHEP( p );                           
            boolean fin_st = ( event.getISTHEP( p ) == FINAL_STATE);

        }
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
