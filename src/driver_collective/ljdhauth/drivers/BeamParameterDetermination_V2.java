/*
 * BeamParameterDetermination_V2.java
 *
 * This driver looks to measure observables
 * in the BeamCalorimeter, and will later
 * reconstruct beam parameters from these.
 * 
 * OBSERVABLES to be measured: L-R , T-D,
 * & diagonal asymmetries, Thrust axis, total
 * deposition, ...
 *
 * Works in progress: LR & TD assymetry
 * The next thing to start: thrust axis?
 *
 * Basis driver:
 * --> BeamcalEnergyDep.java
 *
 * Last edited on May 8, 2016, 3:54 PM
 * @Author: Luc d'Hauthuille 
 *
 * ~ Based on EventAnalysis written by Christopher Milke et al. ~
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
        String root_mode = "NEW";
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
	    System.out.println("Over all events, total energy deposited = " + 
			       energyDepOverEvents +" GeV");
	    System.out.println("Average per event = " +energyDepOverEvents/numberOfEvents);
	    //for(Integer i: layersHit){System.out.println(i);}	    
	    System.out.println("Array of x_avgs (P BCal): " + x_avgs_pos.toString());
	    System.out.println("Array of y_avgs (P BCal): " + y_avgs_pos.toString());
	    System.out.println("Array of x_avgs (N BCal): " + x_avgs_neg.toString());
	    System.out.println("Array of y_avgs (N BCal): " + y_avgs_neg.toString());
		    
	    System.out.println("Array of LR assyms for positive BeamCal:" + LR_asym_pos.toString());
	    System.out.println("Array of TD assyms for positive BeamCal:" + TD_asym_pos.toString());
	    System.out.println("Array of LR assyms for negative BeamCal:" + LR_asym_neg.toString());
	    System.out.println("Array of TD assyms for negative BeamCal:" + TD_asym_neg.toString());
	    
	    System.out.println("Array of thrust values for positive BeamCal:" +
			       thrust_pos.toString());
	    System.out.println("Array of thrust values for negative BeamCal:" + 
			       thrust_neg.toString());
	    System.out.println("*************************************************************" + 
			       "*************");
	   	}
        catch (java.io.IOException e) {
	    System.out.println(e);
	    System.exit(1);
        }
    }

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
    public double[] compute_meanDepth(List<SimCalorimeterHit> hits){
	double[] eDep = new double[51]; int[] hitLayerArray = new int[51];
	double energyDepth_productSum = 0; double energyDepthL_productSum = 0; // sum of the products of energy*depth
	double hitDepthL = 0;
	double sumOfEnergy = 0;	int hitnum = 0;
	for(SimCalorimeterHit hit: hits){	    
	    double[] vec = hit.getPosition();
	    double[] transformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
	    double energy = hit.getRawEnergy();
	    if(vec[2] > 0){
		sumOfEnergy += energy;

		//eDep[hit.getLayerNumber()+1] += hit.getRawEnergy();
		//hitLayerArray[hit.getLayerNumber()+1]++;
		energyDepth_productSum += energy*transformed_Vec[2];
		energyDepthL_productSum += energy*(hit.getLayerNumber()+1);
		hitDepthL += hit.getLayerNumber()+1;
	    }else{/*working with negative hits?*/

	    }
	}
	
	hitDepthL = hitDepthL/hitnum;
	System.out.println("E total deposited" + sumOfEnergy);	
	//Layer Arrays
	//System.out.println("E deposited" + Arrays.toString(eDep));
	//System.out.println("Hits by layer" + Arrays.toString(hitLayerArray));
	//System.out.println("Hit number based: " + hitDepthL);
	
	meanDepth_Z = energyDepth_productSum/sumOfEnergy;
	meanDepth_Layer = energyDepthL_productSum/sumOfEnergy;
	System.out.println("Energy based:");
	System.out.println("Mean depth, Layer is (" + meanDepth_Z + ", " + meanDepth_Layer + ")");	
	
	double[] depth = {meanDepth_Z, meanDepth_Layer};
	return (depth);
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
	//There are the assymetries of Pos/Neg BCal
	double LR_asym_pos =x_sum_pos/xMag_sum_pos;
	double TD_asym_pos =y_sum_pos/yMag_sum_pos;
	double LR_asym_neg =x_sum_neg/xMag_sum_neg;
	double TD_asym_neg =y_sum_neg/yMag_sum_neg;

	double[] baricenters = {x_avg_pos,y_avg_pos,x_avg_neg,y_avg_neg};
	double[] asymetries = {LR_asym_pos,TD_asym_pos,LR_asym_neg,TD_asym_neg};
	double[][] bari_asym = {baricenters,asymetries};
	System.out.println("X_avg, Y_avg for positive BeamCal is" +
			   x_avg_pos + ", " + y_avg_pos);
	System.out.println("X_avg, Y_avg for negative BeamCal is" + 
			   x_avg_neg + ", " + y_avg_neg);
	System.out.println("DEBUGGING assymetries: X_sum_pos, xMag_sum_pos is" + 
			   x_sum_pos + ", " + xMag_sum_pos);


	return bari_asym;
	
    }
    
    //Split this into 2 functions here... SPLIT COMPLETE (05/01/16 - see original)
    //public void compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event) 
    // throws java.io.IOException{
    
    public void compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event,double[] loc)
	throws java.io.IOException{	
	
	//******Test CASES*******//
	//*****END TEST CASES****// 
	double x_avg_pos = loc[0];
	double y_avg_pos = loc[1];
	double x_avg_neg = loc[2];
	double y_avg_neg = loc[3];
	//Thrust quantities computed from Calorimeter hits (energy-weighted)
	EventShape es_hits_pos = new EventShape();
	EventShape es_hits_neg = new EventShape();		
	//Now run through these again and plot from mean
	List<BasicHep3Vector> vecs_fromMean_pos = new ArrayList<BasicHep3Vector>();
	List<BasicHep3Vector> vecs_fromMean_neg = new ArrayList<BasicHep3Vector>();		
	int hit_c_pos = 0;
	int hit_c_neg = 0;
	    
        for(SimCalorimeterHit hit: hits){
	    BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
	    double Energy = hit.getRawEnergy(); //will be weighting by Energy		
	    if(hit.getPosition()[2] > 0){
		//root.fill("hit_vectors_Mean",a.x(),a.y());// Plot by number of hits(occup)
		//weight by energy for thrust algorithm
		//root.fill("hit_vectors_Mean",(a.x() - x_avg_pos), (a.y() - y_avg_pos),Energy);
		hit_c_pos++;
		a.setV(Energy*(a.x() - x_avg_pos), Energy*(a.y() - y_avg_pos),0);
		root.fill("hit_vectors_Mean",(a.x() - x_avg_neg), (a.y() - y_avg_neg), 
			  Energy);		
		vecs_fromMean_pos.add(a);

		//What if plot: root.fill("hit_vectors_Mean",a.x(),a.y(),Energy);
	    }else{ //For negative
		hit_c_neg++;
		//if(hit_c_neg % 50000 == 0) root.proc("f.write()");

		a.setV(Energy*(a.x() - x_avg_neg), Energy*(a.y() - y_avg_neg),0);
                vecs_fromMean_neg.add(a);
	    }
        }
	
	es_hits_pos.setEvent(vecs_fromMean_pos);
	es_hits_neg.setEvent(vecs_fromMean_neg);
	System.out.println("The total hits on positive BCAL:" + hit_c_pos);
	System.out.println("The total hits on negative BCAL:" + hit_c_neg);
	//Printing stuff
	Hep3Vector thrustAxis_hits_p = es_hits_pos.thrustAxis();
	BasicHep3Vector hopefulthrust_Ebased_p = es_hits_pos.thrust();
	thrust_pos.add(hopefulthrust_Ebased_p.x());
	System.out.println("For the positive BeamCal");
	System.out.println("The thrust from hits is " + thrustAxis_hits_p.magnitude());
	System.out.println("The thrust(diff-corr method) from hits(E-weighted) is " + hopefulthrust_Ebased_p.toString());
	System.out.println("The thrustAxis from hits is " + thrustAxis_hits_p.toString());
	System.out.println("----------");
	Hep3Vector thrustAxis_hits_n = es_hits_neg.thrustAxis();
        BasicHep3Vector hopefulthrust_Ebased_n = es_hits_neg.thrust();
	thrust_neg.add(hopefulthrust_Ebased_n.x());
        System.out.println("For the negative BeamCal");
        System.out.println("The thrust from hits is " + thrustAxis_hits_n.magnitude());
        System.out.println("The thrust(diff-corr method) from hits(E-weighted) is " + hopefulthrust_Ebased_n.toString());
	System.out.println("The thrustAxis from hits is " + thrustAxis_hits_n.toString());
	
	/*
	//Thrust quantities computed from final state particles(traditional jets?)
	EventShape es_fParticles = new EventShape();
	List<MCParticle> final_Particles = new ArrayList<MCParticle>();
	int counter = 0;
	for(MCParticle p: event.getMCParticles()){
	    if(p.getGeneratorStatus() == MCParticle.FINAL_STATE){
		final_Particles.add(p);
	    }
	    if(counter > 100000) break; //Don't go over maximum
	    counter++;
	}
	
	List<Hep3Vector> momentaList = new ArrayList<Hep3Vector>();
	for(MCParticle p: final_Particles){ momentaList.add(p.getMomentum());}
	es_fParticles.setEvent(momentaList);
	Hep3Vector thrust = es_fParticles.thrustAxis();
	System.out.println("The thrust from final state particles is " + thrust.magnitude());
	//System.out.println("The number of hits in this event is " + countHits);
	//System.out.println("The number of f_Particles in this event is " + countfinalParticles);
	*/
	}
    

    //Observables//
    public double meanDepth_Z = 0;
    public double meanDepth_Layer = 0;


    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
	//set assymetries to 0 again
	sum_LR_E = 0;
        sum_TD_E = 0;
	sum_LR_Hits = 0;
	sum_TD_Hits = 0;
	super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");

	System.out.println("************************************START***" + 
			   "***********************************");
	//initialize event variables
	int check_layer = 0;
        boolean reject_negative = true; // reject negative beamcal?
        int hit_count = 0;
	boolean funcs_only = true;
	double sumOfEnergy = 0;
	double maxPixelEnergy = 0;
	int layerOfMaxE = 0;
	try {	    
	    double[] depth = compute_meanDepth(hits);
	    double[][] bari_asym = compute_Baricenter(hits, event);
	    double loc[] = bari_asym[0];    //Baricenters!
	    double[] asyms = bari_asym[1];  //Assymetries!
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

	    compute_Thrust(hits,event, loc);
	    // loop through the List of <SimCalHits..>
	    if(!funcs_only){
		for (SimCalorimeterHit hit : hits) {
		    double[] vec = hit.getPosition();
		    double[] trformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
		    int layer = hit.getLayerNumber();
		    if ( reject_negative && (vec[2]<0) ){ layersHit2.add(layer);} //pass over event
		    else{		    
			double energy = hit.getRawEnergy();
			sumOfEnergy += energy;
			//root.fill("heatmap",vec[0],vec[1]);// default to 1 
			//assym_Analysis(vec,energy);
			hit_count++;
			//********Power lied here. See (old) Graveyard. {4/12/16}
			//c. 03/31/16
		    }
		}
	    }	    
	}catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	
	// Post-processing print statements:
        System.out.println("finished event "+ ++eventNumber);        
	System.out.println("Total energy deposited across BeamCal: " + sumOfEnergy);
	energyDepOverEvents += sumOfEnergy;
	System.out.println("******************************************" + 
			   "******************************");
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
    private int numberOfEvents = 10; //This is used to get averages per event.
    private int eventNumber;
    private double runTemp = 15;
    private boolean wholeBcal = true;
    private boolean layersBcal = true;
    private double energyDepOverEvents = 0;

    //Beam Parameter Observable
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
    
