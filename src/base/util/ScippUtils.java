/**
 * ScippUtils.java.
 *
 * Created on Aug 28 2014, 01:50 AM
 * Last Edited Apr 5, 2016, 9:11 PM
 * @author Christopher Milke
 * edited by J. Shtalenkova
 * @version 1.0 
 * 
 */
package scipp_ilc.base.util;
import scipp_ilc.base.util.LCIOFileManager;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

public class ScippUtils {
    
    
    //METHODS: TRANSFORM- IN: (px, py, pz), and E
    //                    OUT: (px', py', pz', E')  
    public static double[] transform(double[] p, double E){
        double theta = inc_ang;
        double in_E = E_e;
        double beta = Math.sin(theta);
        double gamma = Math.pow((1-Math.pow(beta, 2)), -0.5);
        double[] cm_Vec = new double[4];

        /*
         *       |gamma         -gamma*beta| |p|                       |p'|
         *       |-gamma*beta         gamma|*|E| = TRANSOFORMED4vector |E'|
         *  
         *      */
    
        cm_Vec[0] = p[0]*gamma - gamma*beta*E;
        cm_Vec[1] = p[1];
        cm_Vec[2] = p[2];
        cm_Vec[3] = E*gamma - gamma*beta*p[0];
        return cm_Vec;
    }
        
    //Get electron signal event and ensure it actually hits the detector
    public static EventHeader getProperEvent(LCIOFileManager mngr) {
        EventHeader signalEvent = mngr.nextEvent();

        if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

        MCParticle electron = getElectron(signalEvent);

        if (electron == null) {
            throw new NextEventException();
        }

        while ( !hitBeamCal(electron) ) {
            signalEvent = mngr.nextEvent();
            if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

            electron = getElectron(signalEvent);
            if (electron == null) {
                throw new NextEventException();
            }
        }
        
        return signalEvent;
    }



    public static EventHeader getEvent(LCIOFileManager mngr) {
        EventHeader signalEvent = mngr.nextEvent();

        if (signalEvent == null) throw new java.lang.RuntimeException("There are no signal events left. Process ending");
        MCParticle electron = getElectron(signalEvent);
        if (electron == null) throw new NextEventException();

        return signalEvent;
    }
    
    
    
    //extracts electron data from signal file
    public static MCParticle getElectron(EventHeader event) {
        MCParticle mcp = null;
        for (MCParticle p : event.getMCParticles()) {
            if (Math.abs(p.getPDGID()) == 11 && p.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                mcp = p;
                break;
            }
        }
        return mcp;
    }
    
    
    
    //ensures the electron hits the detector
    private static boolean hitBeamCal(MCParticle electron) {
        double[] vec = electron.getEndPoint().v();
        double   rorg = Math.hypot(vec[0], vec[1]);

        //below values are VERY approximate and provide a wide range that should usually work
        double beamCalFront = 2000;
        double beamCalRear = 4000;

        //Reject electrons that do not decay at the front of the beamcal
        return (beamCalFront < vec[2] && vec[2] < beamCalRear);
    }

   //global vars
   public static final double inc_ang = 0.007;  //incident angle, rads
   public static final double E_e = 250.0;      //incident electron enegry, GeV
}
