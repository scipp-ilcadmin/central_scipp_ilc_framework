/*
 * BeamParameterDetermination_V2.java
 *
 * This driver looks to measure observables
 * in the BeamCalorimeter, and will later
 * reconstruct beam parameters from these.
 * 
 * OBSERVABLES measured:
 * L-R, T-D asymms, baricenter ( [<x>, <y>, <z>] )
 * Thrust and thrust axis from baricenter, total
 * energy deposited
 * 
 * Works in progress: Fix data output for easier analysis (sort of done)
 * To start: Mode-based center, thrust, thrust axis
 *
 * Basis driver:
 * --> BeamcalEnergyDep.java
 *
 * Last edited on May 8, 2016, 3:54 PM
 * @Author: Luc d'Hauthuille, ljdhauth@ucsc.edu 
 *
 * ~ Template based on EventAnalysis written by Christopher Milke et al. ~
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

//import hep.physics.jet.EventShape;
import scipp_ilc.drivers.EventShape; 
//Import the EventShape class, customized to allow for larger list
import hep.physics.particle.Particle;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;
import java.util.Arrays;

import java.lang.String;

public class BeamParameterDetermination_V2 extends Driver {
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT
    public void //setVariablename(variable type) { 
    //the first letter after "set" must be uppercase but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
	this.jrootFile = s;
    }
    //END DEFINE XML FUNCTIONS

    //This function is called when program is started,
    //initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "UPDATE";
	try {
            root = new Jroot(jrootFile,root_mode);

	    String plotName = "hit_vectors_Mean";
	    String plotName2 = "X Y Positions of Hits from Mean (Collapsed Bcal)";
	    root.init("TH2D",plotName, plotName,plotName2, 350, -175, 175, 350, -175, 175);	    
	    String plot2Name = "hit_vectors_Max";
            String plot2Name2 = "X Y Positions of Hits from Max (Collapsed Bcal)";
            root.init("TH2D",plot2Name, plot2Name,plot2Name2, 350, -175, 175, 350, -175, 175);
	}catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    // This function is called after all file runs have finished,
    // and clo. Prints final statements.
    public void endOfData(){
        try {
            root.end();
	    //Prints involving all events:
	    System.out.println("Over all events, Total energy deposited (P) = " + 
			       energyDepOverEvents +" GeV");
	    //System.out.println("Average per event = " +energyDepOverEvents/numberOfEvents);
	    
//for(Integer i: layersHit){System.out.println(i);}	    

	    //Arrays of observables, for different events.
	    
	    System.out.println("E deposited (P): " + eDep_p.toString());
	    System.out.println("E deposited (N): " + eDep_n.toString());

	    System.out.println("meanDepth (P): " + meanDepth_p.toString());
	    System.out.println("meanDepth (N): " + meanDepth_n.toString());
	    
	    System.out.println("x_avgs (P): " + x_avgs_pos.toString());
	    System.out.println("y_avgs (P): " + y_avgs_pos.toString());
	    System.out.println("x_avgs (N): " + x_avgs_neg.toString());
	    System.out.println("y_avgs (N): " + y_avgs_neg.toString());
	    	    
	    System.out.println("LR asyms (P): " + LR_asym_pos.toString());
	    System.out.println("TD asyms (P): " + TD_asym_pos.toString());
	    System.out.println("LR asyms (N): " + LR_asym_neg.toString());
	    System.out.println("TD asyms (N): " + TD_asym_neg.toString());

	    System.out.println("ThrustAxes_x (P): " + thrustAxis_x_p.toString());
	    System.out.println("ThrustAxes_y (P): " + thrustAxis_y_p.toString());
	    System.out.println("ThrustAxes_x (N): " + thrustAxis_x_n.toString());
	    System.out.println("ThrustAxes_y (N): " + thrustAxis_y_n.toString());

	    System.out.println("Thrust values (P):" +
			       thrust_pos.toString());
	    System.out.println("Thrust values (N): " +
			       thrust_neg.toString());
	    System.out.println("*************************************************************" + 
			       "*************\n");
	   	} catch (java.io.IOException e){
	    System.out.println(e);
	    System.exit(1);
        }
    }



    //************** STATS *****************//
    static double[] rms_error(List<Double> observableList, double mean){
	double diffMeanSquared = 0;
	int count = 0;
	for(Double obs: observableList){
	    diffMeanSquared += (obs - mean)*(obs - mean);
	    count++;
	}
	double rms = Math.sqrt(diffMeanSquared/(count-1)); //Take the sqrt of (diffMeanSquared/9) => rms
	double[] rms_error = {rms, rms/(Math.sqrt(count))}; 
	return rms_error; // returns the rms and error
    }

    static public double[] calculateAverage(List<Double> observableList){
	double sumOfObs = 0;
	int count = 0;
	for(Double obs: observableList){
	    sumOfObs += obs;
	    count++;
	}
	double mean = sumOfObs/count;
	double[] rms_error = rms_error(observableList, mean);
	double[] mean_RMS_error = {mean,rms_error[0], rms_error[1]};
	return mean_RMS_error; //returns the mean, the rms, and the error 
    }
    //************* END STATS *****************//




    //******************************Beam Parameter Functions***************************//

    public double maxPixelE(double currMax,double energy){
	double newMax = currMax;
	if(energy > currMax)  newMax = energy;
	return newMax;
    }

    double sum_LR_E = 0;
    double sum_TD_E = 0;
    double sum_LR_Hits = 0;
    double sum_TD_Hits = 0;

    //Compute mean shower depth (NOTE: Layers start at 1, instead of the usual 0 here)
    public double[][] compute_meanDepth(List<SimCalorimeterHit> hits){
	
	double[] eDep = new double[51]; int[] hitLayerArray = new int[51];
	//Positive Bcal variables
	double energyDepth_productSum_p = 0; double energyDepthL_productSum_p = 0; // sum of the products of energy*depth
	double sumOfEnergy_p = 0; int hitnum_p = 0;
	//Negative Bcal variables
	double energyDepth_productSum_n = 0; double energyDepthL_productSum_n = 0; // sum of the products of energy*depth
	double sumOfEnergy_n = 0; int hitnum_n = 0;
	
	
	for(SimCalorimeterHit hit: hits){	    
	    double[] vec = hit.getPosition();
	    double[] transformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
	    double energy = hit.getRawEnergy();
	    if(vec[2] > 0){
		sumOfEnergy_p += energy;
		//eDep[hit.getLayerNumber()+1] += hit.getRawEnergy(); //hitLayerArray[hit.getLayerNumber()+1]++;
		energyDepth_productSum_p += energy*transformed_Vec[2]; // Does this change if don't trans-vec?
		energyDepthL_productSum_p += energy*(hit.getLayerNumber()+1);
	    }else{/*working with negative hits*/
                sumOfEnergy_n += energy;
                //eDep[hit.getLayerNumber()+1] += hit.getRawEnergy(); //hitLayerArray[hit.getLayerNumber()+1]++;
		energyDepth_productSum_n += energy*transformed_Vec[2]; // Does this change if don't trans-vec?
		energyDepthL_productSum_n += energy*(hit.getLayerNumber()+1);

	    }
	}
	//System.out.println("Hits by layer" + Arrays.toString(hitLayerArray));
	
	double meanDepth_Z_p = energyDepth_productSum_p/sumOfEnergy_p;
	double meanDepth_Layer_p = energyDepthL_productSum_p/sumOfEnergy_p;
	
	double meanDepth_Z_n = energyDepth_productSum_n/sumOfEnergy_n;
	double meanDepth_Layer_n = energyDepthL_productSum_n/sumOfEnergy_n;

	double[] depth_L_E_pos = {meanDepth_Z_p, meanDepth_Layer_p, sumOfEnergy_p};
	double[] depth_L_E_neg = {meanDepth_Z_n, meanDepth_Layer_n, sumOfEnergy_n};
	
	double[][] depth_L_E = {depth_L_E_pos,depth_L_E_neg};

	//Obligatory post-method prints!
	System.out.println("E total deposited (P): " + sumOfEnergy_p);
	System.out.println("E total deposited (N): " + sumOfEnergy_n);
	System.out.println("Mean depth, Layer (P) is (" + meanDepth_Z_p + ", " + meanDepth_Layer_p + ")");	
	System.out.println("Mean depth, Layer (N) is (" + meanDepth_Z_n + ", " + meanDepth_Layer_n + ")");	
	return depth_L_E;
    }

    //Computes Baricenter AND assymetries for positive and negative BCal
    public double[][] compute_Baricenter(List<SimCalorimeterHit> hits, EventHeader event) {

	double eSum_pos = 0; double eSum_neg = 0;

	double x_sum_pos = 0; double y_sum_pos = 0; //WEIGHTED by Energy, i.e. Sum(x_i*E_i)
	double x_sum_neg = 0; double y_sum_neg = 0;

	double xMag_sum_pos = 0; double yMag_sum_pos = 0; //WEIGHTED by Energy, s.t.  Sum(|x_i|*E_i)
        double xMag_sum_neg = 0; double yMag_sum_neg = 0;

	for(SimCalorimeterHit hit: hits){
	    BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
	    BasicHep3Vector aMag = new BasicHep3Vector(hit.getPosition());

	    double Energy = hit.getRawEnergy();
	    a.setV(Energy*a.x(), Energy*a.y(),0);
	    aMag.setV(Energy*Math.abs(aMag.x()),Energy*Math.abs(aMag.y()), 0);
	    if(hit.getPosition()[2] > 0){//for positive hits
		eSum_pos += Energy;
		x_sum_pos += a.x();
		y_sum_pos += a.y();
		xMag_sum_pos += aMag.x();
		yMag_sum_pos += aMag.y();
	    }else{
		eSum_neg += Energy;
		x_sum_neg += a.x();
		y_sum_neg += a.y();
		xMag_sum_neg += aMag.x();
		yMag_sum_neg += aMag.y();
	    }
	}
	//These are for Baricenter of Pos/Neg BCal
	double x_avg_pos = x_sum_pos/eSum_pos;//weight by Energy
	double y_avg_pos = y_sum_pos/eSum_pos;
	double x_avg_neg = x_sum_neg/eSum_neg;
	double y_avg_neg = y_sum_neg/eSum_neg;
	//There are the asymmetries of Pos/Neg BCal
	double LR_asym_pos = x_sum_pos/xMag_sum_pos;
	double TD_asym_pos = y_sum_pos/yMag_sum_pos;
	double LR_asym_neg = x_sum_neg/xMag_sum_neg;
	double TD_asym_neg = y_sum_neg/yMag_sum_neg;

	double[] baricenters = {x_avg_pos,y_avg_pos,x_avg_neg,y_avg_neg};
	double[] asymetries = {LR_asym_pos,TD_asym_pos,LR_asym_neg,TD_asym_neg};
	double[][] bari_asym = {baricenters,asymetries};

	//Prints!
	System.out.println("X_avg, Y_avg for positive BeamCal is " +
			   x_avg_pos + ", " + y_avg_pos);
	System.out.println("X_avg, Y_avg for negative BeamCal is " + 
			   x_avg_neg + ", " + y_avg_neg);
	System.out.println("Asymmetries (+) LR/TD: " + 
			   LR_asym_pos + ", " + TD_asym_pos);
	System.out.println("Asymmetries (-) LR/TD: " +
			   LR_asym_neg + ", " + TD_asym_neg);

	return bari_asym;
	
    }
    
    //Split this into 2 functions here... SPLIT COMPLETE (05/01/16 - see original)
    //public void compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event) 
    // throws java.io.IOException{
    
    public double[][] compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event,double[] loc)
	throws java.io.IOException{	
	
	//****Testing****//
	double x_avg_pos = loc[0]; double y_avg_pos = loc[1];
	double x_avg_neg = loc[2]; double y_avg_neg = loc[3];
	//Thrust quantities computed from Calorimeter hits (energy-weighted)
	EventShape es_hits_pos = new EventShape();
	EventShape es_hits_neg = new EventShape();		
	//Now run through these again and plot from mean
	List<BasicHep3Vector> vecs_fromMean_pos = new ArrayList<BasicHep3Vector>();
	List<BasicHep3Vector> vecs_fromMean_neg = new ArrayList<BasicHep3Vector>();		
	int hit_c_pos = 0; int hit_c_neg = 0;
	boolean overLimit = false;

        for(SimCalorimeterHit hit: hits){
	    BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
	    double Energy = hit.getRawEnergy(); //will be weighting by Energy		
	    if(hit.getPosition()[2] > 0){
		//root.fill("hit_vectors_Mean",a.x(),a.y());// Plot by number of hits(occup)
		//weight by energy for thrust algorithm
		//root.fill("hit_vectors_Mean",(a.x() - x_avg_pos), (a.y() - y_avg_pos),Energy);
		hit_c_pos++;
		a.setV(Energy*(a.x() - x_avg_pos), Energy*(a.y() - y_avg_pos),0);
		vecs_fromMean_pos.add(a);

		//What if plot: root.fill("hit_vectors_Mean",a.x(),a.y(),Energy);
	    }else{ //For negative
		hit_c_neg++;
		//if(hit_c_neg % 1000 == 0) root.proc("printf(\"test00\");\n"); //overLimit = true;
		if(hit_c_neg % 200000 == 0) root.proc("f.Write();\n"); //overLimit = true;
		//if (overLimit==false){}
		
		//Wanna plot?
		//root.fill("hit_vectors_Mean",(a.x() - x_avg_neg), (a.y() - y_avg_neg), 
		//	  Energy);
				
		a.setV(Energy*(a.x() - x_avg_neg), Energy*(a.y() - y_avg_neg),0);
                vecs_fromMean_neg.add(a);
	    }
        }
	//Get EventShape based on Energy weighted hit vectors 
	es_hits_pos.setEvent(vecs_fromMean_pos);
	es_hits_neg.setEvent(vecs_fromMean_neg);
	//Get thrustAxis and thrust value
	Hep3Vector thrustAxis_hits_p = es_hits_pos.thrustAxis();
	BasicHep3Vector hopefulthrust_Ebased_p = es_hits_pos.thrust();
	Hep3Vector thrustAxis_hits_n = es_hits_neg.thrustAxis();
        BasicHep3Vector hopefulthrust_Ebased_n = es_hits_neg.thrust();
	
	//The following is antiquated. Nowadays, observables are always added to their
	//global list in the process function...
	//thrust_pos.add(hopefulthrust_Ebased_p.x()); //Print thrust value
	//thrust_neg.add(hopefulthrust_Ebased_n.x());
	
	//Printing stuff
	System.out.println("Positive BeamCal ");
	System.out.println("Total hits:" + hit_c_pos);
	System.out.println("Thrust from hits(E-weighted) is " + hopefulthrust_Ebased_p.x());
	System.out.println("ThrustAxis: " + thrustAxis_hits_p);
	
	System.out.println("-------------------------");
	

        System.out.println("Negative BeamCal");
	System.out.println("Total hits: " + hit_c_neg);
        System.out.println("Thrust from hits(E-weighted) is " + hopefulthrust_Ebased_n.x());
	System.out.println("ThrustAxis: " + thrustAxis_hits_n.toString());
	//Change from Hep3Vector to array
	double[] thrustAxis_p = {thrustAxis_hits_p.x(),thrustAxis_hits_p.y(),thrustAxis_hits_p.z()};
	double[] thrustAxis_n = {thrustAxis_hits_n.x(),thrustAxis_hits_n.y(),thrustAxis_hits_n.z()};
	double[] thrusts = {hopefulthrust_Ebased_p.x(),hopefulthrust_Ebased_n.x()};
	double[][] thrustAxes = {thrustAxis_p, thrustAxis_n, thrusts};//thrust axis in pos, neg and both thrusts 
	
	return thrustAxes;

        //Thrust quantities computed from final state particles(traditional jets?) were here, check V0
	//System.out.println("The number of f_Particles in this event is " + countfinalParticles);
	
	}
    

    //Observables//(These are function-wide variables, for the moment)
    //public double meanDepth_Z = 0;
    //public double meanDepth_Layer = 0;


    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
	//set assymetries to 0 again
	//sum_LR_E = 0; sum_TD_E = 0; sum_LR_Hits = 0; sum_TD_Hits = 0;//UNUSED?

	super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");

	System.out.println("************************************START***" + 
			   "***********************************\n");
	System.out.println("|-_-OBSERVABLES-_-|");
	//initialize event variables
	int check_layer = 0;
        boolean reject_negative = true; // reject negative beamcal?
        int hit_count = 0;
	eventNumber++;
	boolean funcs_only = true;
	double sumOfEnergy = 0;
	double maxPixelEnergy = 0;
	int layerOfMaxE = 0;
	try {	    

	    //Event-wide obserables are added to lists here
	    double[][] bari_asym = compute_Baricenter(hits, event);
	    double loc[] = bari_asym[0];    //Baricenters!
	    double[] asyms = bari_asym[1];  //Assymetries!
	    //Add observables
	    //Add energy-weighted avgs from this event to lists
	    x_avgs_pos.add(loc[0]);
	    y_avgs_pos.add(loc[1]);
	    x_avgs_neg.add(loc[2]);
	    y_avgs_neg.add(loc[3]);
	    //Add asymmetries for this event to lists
	    LR_asym_pos.add(asyms[0]);
	    TD_asym_pos.add(asyms[1]);
	    LR_asym_neg.add(asyms[2]);
	    TD_asym_neg.add(asyms[3]);
	    double[][] thrustAxes = compute_Thrust(hits,event, loc);
	    thrustAxis_x_p.add(thrustAxes[0][0]);
	    thrustAxis_y_p.add(thrustAxes[0][1]);
	    thrustAxis_x_n.add(thrustAxes[1][0]);
	    thrustAxis_y_n.add(thrustAxes[1][1]);	    
	    double[][] depth_L_E = compute_meanDepth(hits); //[[mDepth_z_p,mDepth_L_p,sumOfE_p],[negativs]]
	    //Add meanDepths for this event to lists
	    meanDepth_p.add(depth_L_E[0][0]);
	    meanDepth_n.add(depth_L_E[1][0]);
	    eDep_p.add(depth_L_E[0][2]);
	    eDep_n.add(depth_L_E[1][2]);
	    

	    //WORK IN PROGRESS (A)//	    

	    thrust_pos.add(thrustAxes[2][0]);
	    thrust_neg.add(thrustAxes[2][1]);

	    //PROGRESS (A) ENDS HERE//
	    
	    //WORK IN PROGRESS (B)//	    

	    //Now take averages, since every n events, we change the scenario.
	    //i.e. For Scenarios 1-4, 10 events for Scenario 1, 10 for S2...
	    if(eventNumber % numberOfEventstoAvg == 0){
		System.out.println("--------~?----------");
		System.out.println("*** Averaged observables ***");

		eDep_pList.add(calculateAverage(eDep_p)[0]); 
		System.out.println(eDep_p.toString()); System.out.println("Energy Dep (+): " + eDep_pList.toString());
		eDep_pError.add(calculateAverage(eDep_p)[2]);
                System.out.println("Errors: " + eDep_pError.toString());
                eDep_p.clear();

		eDep_nList.add(calculateAverage(eDep_n)[0]);
                System.out.println(eDep_n.toString()); System.out.println("Energy Dep (-): " + eDep_nList.toString());
		eDep_nError.add(calculateAverage(eDep_n)[2]);
                System.out.println("Errors: " + eDep_nError.toString());
                eDep_n.clear();

		meanDepth_pList.add(calculateAverage(meanDepth_p)[0]);
		System.out.println(meanDepth_p.toString()); System.out.println("mean Depth (+): " + meanDepth_pList.toString());
		meanDepth_pError.add(calculateAverage(meanDepth_p)[2]);
                System.out.println("Errors: " + meanDepth_pError.toString());
		meanDepth_p.clear();

                meanDepth_nList.add(calculateAverage(meanDepth_n)[0]);
		System.out.println(meanDepth_n.toString()); System.out.println("mean Depth (-): " + meanDepth_nList.toString());
		meanDepth_nError.add(calculateAverage(meanDepth_n)[2]);
                System.out.println("Errors: " + meanDepth_nError.toString());
		meanDepth_n.clear();

		thrust_posList.add(calculateAverage(thrust_pos)[0]);
                System.out.println(thrust_pos.toString()); System.out.println("Thrust Value (+): " + thrust_posList.toString());
		thrust_posError.add(calculateAverage(thrust_pos)[2]);
                System.out.println("Errors: " + thrust_posError.toString());
		thrust_pos.clear();

		thrust_negList.add(calculateAverage(thrust_neg)[0]);
                System.out.println(thrust_neg.toString()); System.out.println("Thrust Value (-): " + thrust_negList.toString());
		thrust_negError.add(calculateAverage(thrust_neg)[2]);
                System.out.println("Errors: " + thrust_negError.toString());
                thrust_neg.clear();


		x_avgs_posList.add(calculateAverage(x_avgs_pos)[0]);
                System.out.println(x_avgs_pos.toString()); System.out.println("x_avg (+): " + x_avgs_posList.toString());
		x_avgs_posError.add(calculateAverage(x_avgs_pos)[2]);
                System.out.println("Errors: " + x_avgs_posError.toString());
                x_avgs_pos.clear();

		y_avgs_posList.add(calculateAverage(y_avgs_pos)[0]);
		System.out.println(y_avgs_pos.toString()); System.out.println("y_avg (+): " + y_avgs_posList.toString());
		y_avgs_posError.add(calculateAverage(y_avgs_pos)[2]);
                System.out.println("Errors: " + y_avgs_posError.toString());
		y_avgs_pos.clear();

		x_avgs_negList.add(calculateAverage(x_avgs_neg)[0]);
		System.out.println(x_avgs_neg.toString()); System.out.println("x_avg (-): " + x_avgs_negList.toString());
		x_avgs_negError.add(calculateAverage(x_avgs_neg)[2]);
                System.out.println("Errors: " + x_avgs_negError.toString());
		x_avgs_neg.clear();
		
                y_avgs_negList.add(calculateAverage(y_avgs_neg)[0]);
		System.out.println(y_avgs_neg.toString()); System.out.println("y_avg (-): " + y_avgs_negList.toString());
		y_avgs_negError.add(calculateAverage(y_avgs_neg)[2]);
                System.out.println("Errors: " + y_avgs_negError.toString());
		y_avgs_neg.clear();

		LR_asym_posList.add(calculateAverage(LR_asym_pos)[0]);
		System.out.println(LR_asym_pos.toString()); System.out.println("LR asym (+): " + LR_asym_posList.toString());
		LR_asym_posError.add(calculateAverage(LR_asym_pos)[2]);
                System.out.println("Errors: " + LR_asym_posError.toString());
		LR_asym_pos.clear();

		TD_asym_posList.add(calculateAverage(TD_asym_pos)[0]);
		System.out.println(TD_asym_pos.toString()); System.out.println("TD asym (+): " + TD_asym_posList.toString());
		TD_asym_posError.add(calculateAverage(TD_asym_pos)[2]);
                System.out.println("Errors: " + TD_asym_posError.toString());
		TD_asym_pos.clear();
		
		LR_asym_negList.add(calculateAverage(LR_asym_neg)[0]);
		System.out.println(LR_asym_neg.toString()); System.out.println("LR asym (-): " + LR_asym_negList.toString());
		LR_asym_negError.add(calculateAverage(LR_asym_neg)[2]);
                System.out.println("Errors: " + LR_asym_negError.toString());
                LR_asym_neg.clear();

		TD_asym_negList.add(calculateAverage(TD_asym_neg)[0]);
		System.out.println(TD_asym_neg.toString()); System.out.println("TD asym (-): " + TD_asym_negList.toString());
		TD_asym_negError.add(calculateAverage(TD_asym_neg)[2]);
		System.out.println("Errors: " + TD_asym_negError.toString());
                TD_asym_neg.clear();
		
		System.out.println("--------~?----------");

	    }

	    //PROGRESS (B) ENDS HERE//

	    // loop through the List of <SimCalHits..> Check V0 if you want this.
	    //********Power lied here. See (old) Graveyard. {4/12/16}
	    //c. 03/31/16
	}catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	
	// Post-processing print statements:

        System.out.println("finished event "+ eventNumber);        
	System.out.println("Total energy deposited across BeamCal: " + sumOfEnergy);
	energyDepOverEvents += sumOfEnergy;
	System.out.println("******************************************" + 
			   "******************************\n");
	/* //OUT OF SCOPE/Discontinued
	   System.out.println("L_R assym number for this event, weighted by E:" + (sum_LR_E/sumOfEnergy));
	   System.out.println("L_R assym number for this event, weighted by hits:" +
	   (sum_LR_Hits/hit_count));
	   System.out.println("T_D assym number for this event, weighted by E:" + (sum_TD_E/sumOfEnergy));
	   System.out.println("T_D assym number for this event, weighted by hits:" + 
	   (sum_TD_Hits/hit_count));
	*/
    }//End Process
    




    
    /*here all the classwide variables are declared*/
    private int numberOfEventstoAvg = 8; //This is used to get average over some event.
    private int eventNumber=0;
    private double runTemp = 15;
    private boolean wholeBcal = true;
    private boolean layersBcal = true;
    private double energyDepOverEvents = 0;

    //Beam Parameter, Event-Based Observable
    private List<Double> thrustAxis_x_p = new ArrayList<Double>();
    private List<Double> thrustAxis_x_n = new ArrayList<Double>();
    private List<Double> thrustAxis_y_p = new ArrayList<Double>();
    private List<Double> thrustAxis_y_n = new ArrayList<Double>();

    private List<Double> eDep_p = new ArrayList<Double>();
    private List<Double> eDep_n = new ArrayList<Double>();

    private List<Double> meanDepth_p = new ArrayList<Double>();
    private List<Double> meanDepth_n = new ArrayList<Double>();

    private List<Double> x_avgs_pos = new ArrayList<Double>();
    private List<Double> y_avgs_pos = new ArrayList<Double>();
    private List<Double> x_avgs_neg = new ArrayList<Double>();
    private List<Double> y_avgs_neg = new ArrayList<Double>();

    private List<Double> LR_asym_pos = new ArrayList<Double>();
    private List<Double> TD_asym_pos = new ArrayList<Double>();
    private List<Double> LR_asym_neg = new ArrayList<Double>();
    private List<Double> TD_asym_neg = new ArrayList<Double>();

    private List<Double> thrust_pos = new ArrayList<Double>();
    private List<Double> thrust_neg = new ArrayList<Double>();

    //Beam Parameter-Based Averaged Observables, over n events
    //along with their errors. I know this is "too many" lists.
    private List<Double> eDep_pList = new ArrayList<Double>();
    private List<Double> eDep_nList = new ArrayList<Double>();
    private List<Double> eDep_pError = new ArrayList<Double>();
    private List<Double> eDep_nError = new ArrayList<Double>();

    private List<Double> meanDepth_pList = new ArrayList<Double>();
    private List<Double> meanDepth_nList = new ArrayList<Double>();
    private List<Double> meanDepth_pError = new ArrayList<Double>();
    private List<Double> meanDepth_nError = new ArrayList<Double>();

    private List<Double> x_avgs_posList = new ArrayList<Double>();
    private List<Double> y_avgs_posList = new ArrayList<Double>();
    private List<Double> x_avgs_negList = new ArrayList<Double>();
    private List<Double> y_avgs_negList = new ArrayList<Double>();
    private List<Double> x_avgs_posError = new ArrayList<Double>();
    private List<Double> y_avgs_posError = new ArrayList<Double>();
    private List<Double> x_avgs_negError = new ArrayList<Double>();
    private List<Double> y_avgs_negError = new ArrayList<Double>();

    private List<Double> LR_asym_posList = new ArrayList<Double>();
    private List<Double> TD_asym_posList = new ArrayList<Double>();
    private List<Double> LR_asym_negList = new ArrayList<Double>();
    private List<Double> TD_asym_negList = new ArrayList<Double>();
    private List<Double> LR_asym_posError = new ArrayList<Double>();
    private List<Double> TD_asym_posError = new ArrayList<Double>();
    private List<Double> LR_asym_negError = new ArrayList<Double>();
    private List<Double> TD_asym_negError = new ArrayList<Double>();

    private List<Double> thrust_posList = new ArrayList<Double>();
    private List<Double> thrust_negList = new ArrayList<Double>();
    private List<Double> thrust_posError = new ArrayList<Double>();
    private List<Double> thrust_negError = new ArrayList<Double>();
    /*
    private List<Double> thrustAxis_x_p = new ArrayList<Double>();
    private List<Double> thrustAxis_x_n = new ArrayList<Double>();
    private List<Double> thrustAxis_y_p = new ArrayList<Double>();
    private List<Double> thrustAxis_y_n = new ArrayList<Double>();
    */
    


    private HashSet<Integer> layersHit = new HashSet<Integer>();
    private HashSet<Integer> layersHit2 = new HashSet<Integer>();
    //xml derived variables
    private String jrootFile = "";
 
    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    private boolean aligned;
}


//The Graveyard

//if( layer< 25) eDep[layer] += energy; 

//All Layers. Set wholeBcal to true at bottom
//if(wholeBcal == true){/*Select Layers*/}
//if(layersBcal && layer <= 15){}
//power[layer] += powerDraw(radDosage(energy/numberOfEvents),runTemp);//Divide by Event# for avg/event.
/*Plot radDosage
  root.fill("heatmap"+layer,vec[0],vec[1],radDosage(energy/numberOfEvents));//mW/cm^2
  root.fill("histE"+layer,radDosage(energy/numberOfEvents)); //in mW(each pixel's)*/
/* Plots (Energy Deposited)*#ofCrossingsPerYear*3			
   root.fill("heatmap"+layer,vec[0],vec[1],energy*125*Math.pow(10,9)*3);
   root.fill("histE"+layer,energy*125*Math.pow(10,9)*3); */
    
