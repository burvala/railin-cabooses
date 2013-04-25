/*
 * TrainController.java
 * Description: TrainController can regulate the train to run in legal status,
 * as well as allow operator to operate the train.
 * Author: KE LUO
 * Date Created: 07/04/2013
 * Date Last Updated: 24/04/2013
 * 
 */


package TrainController;

import java.util.ArrayList;

import TrainModel.*;
import TrackModel.Block;

/**
 * Train Controller class contain control info and regulate train moving
 * @author keluo
 *
 */

public class TrainController 
{
	public TrainModel train;		// train model 
	public int trainID;				// train ID
	public double currSpeed;		// current speed	
	public double speedLimit;		// speed limit
	public double setPointSpeed;	// set point speed	
	public int authority;			// authority
	public Boolean lights;	// lights ON: true OFF: false
	public Boolean doors;	// doors  Open: true Close: false
	public int temp;				// temperature
	public Boolean brake;			// brake 
	public Boolean eBrake;			// emergency brake 
	private TNCPanel panel;			// Gui 
	private ArrayList<Block> routeInfo = null;	// route information
	private Block currBlock;		// current clock the train occupied
	private String nextStationName = "N/A";		// station name
	private Block nextStation;			// station block 
	private double distToNextStation;			// distance to next station		
	private double authLen;		// authorized length
	private double eBrakeDist;		// distance to stop with emergency brake
	private Boolean tick;		// tick 
	private int brakeType;		// 0-free 1-near station 2-auth check 3-speed 
								// check 4-manual 5-lower speed setpoint
	private int eBrakeType;		// 0-free 1-auth check 2-manual 
	private int lightType;		// 0-free 1-underground 2-manual
	private int doorType;		// 0-free 1-prohibited 2- manual

	public static final double INTEGRAL_INITIAL = 0.0f;		// integral initial
	public static final double ERROR_INITIAL = 0.0f;		// error initial
	public static final double PROPORTIONAL_GAIN = 10000f;	// set proportional
															// gain 10000
	public static final double INTEGRAL_GAIN = 2f;		// set integral gain 2
	private double integralLast = INTEGRAL_INITIAL;		// track last integral
	private double errorLast = ERROR_INITIAL;				// track last error

	/** 
	 * constructor 
	 * */
	public TrainController(TNCPanel gui){
		panel = gui;						// reference gui
		// initialize train status
		currSpeed = 0;						
		speedLimit = 0;						
		authority = 0;						
		lights = false;						
		doors = false;						
		temp = 0;							
		brake = false;					
		eBrake = false;
		eBrakeDist = 0;
		brakeType = 0;
		eBrakeType = 0;
		authLen = 0;
		currBlock = null;
		nextStationName = "N/A";
		nextStation = null;
		tick = true;
	}

	/**  		
	 * 		set train controller to specific train model
	 * 		@ TrainModel    
	 * 		@ void
	 *  
	 * */
	public void setTrainModel(TrainModel tm)
	{
		train = tm;
		if (train!=null){
			// update train status to specific train model
			trainID = tm.getTrainID();
			currSpeed = tm.getVelocity();
			speedLimit = tm.getSpeedLimit();
			authority = tm.getAuthority();
			lights = tm.getLights();
			doors =tm.getDoors();
			temp = tm.getTemperature();		
			brake = tm.getBrake();
			eBrake = tm.getEmergencyBrake();
//			routeInfo = tm.getRouteInfo();
			
			// add new item into combo box
			panel.comboBox.addItem(tm.getTrainID());
		}

	}

	/** 
	 * 		
	 * 		set train controller to specific train model
	 * 		@ TrainModel    
	 * 		@ void
	 *  
	 * */
	public void setSpeed(double s){
		// if set point speed is over speed limit, set to set point 
		// speed to speed limit
		if (train.getSetpointSpeed()>train.getSpeedLimit()){	
			setPointSpeed = train.getSpeedLimit()/3.6;	 		
		}
		else {
			setPointSpeed = s/3.6;
		}
		// setpoint lower than currspeed
		train.setSetpointSpeed(setPointSpeed*3.6);
	}

	/**
	 * set lights
	 * @param l
	 */
	public void setLights(Boolean l){
		lights = l;
		train.toggleLights();
	}
	
	/******** set doors ***********/
	public void setDoors(Boolean d){
		doors = d;
		train.toggleDoors();
	}

	/******** set temperature ***********/
	public void setTemp(int t){
		temp = t;
		train.setTemperature(temp);
	}

	/******** set brake ***********/
	public void setBrake(Boolean b){
		brake = b;
		train.setBrake(brake);
	}

	/******** set emergency brake ***********/
	public void setEBrake(Boolean eb){
		eBrake = eb;
		train.setPower(0);
		train.toggleEmergencyBrake();
	}

	
	/******** tick action ***********/
	public void tick(double time){
		// tick enable
		if (tick==true){
			// update train attributes 
			currSpeed = train.getVelocity();
			speedLimit = train.getSpeedLimit();
			authority = train.getAuthority();
			
			temp = train.getTemperature();
			currBlock = train.getBlock();
			brake = train.getBrake();
			eBrake = train.getEmergencyBrake();
			routeInfo = train.getRouteInfo();
			
			// door control
			if (currBlock!=null){
				doorControl();
				lightControl();
			}
			
			
			// regulate set point speed
			regulateSpeed();			
			

			// route info reset if no route
			if (routeInfo==null || 
					panel.comboBox.getSelectedItem().equals("Train List")){
				nextStationName = "N/A";
			}

			// update destination
			if (currBlock!=null){
				getNextStation();
			}

			// check authority
			if (checkAuth()){
				// check station approach, and slow down 
				// when approaching a station
				if (currBlock!=null && nextStation!=null){
					stationApproachCheck();
				}
				// speed check make sure speed is legal
				if (speedIsSafe()){
					// initial power
					if (currSpeed == 0 && setPointSpeed > 0 && !brake 
							&& !eBrake){
						train.setPower(
								nextPower(setPointSpeed, currSpeed, time)*0.1);
					}
					// update power 
					else if (!brake && !eBrake){		
						train.setPower(
								nextPower(setPointSpeed, currSpeed, time));
					}
				}
			}
		}
		
		// update gui
			updateGui();
	}

	/******** output power ***********/
	private double nextPower(double setpoint, double currentSpeed, 
			double millis) {

		double integral; // integral used for PID control 
		double error;    // difference between setpoint and currentSpeed 
		double sampleTime; // sample time 
		double powerCommand; // power to provide to engine 
		double POWER_MAX = train.getPowerLimit();	// power limit

		error = setpoint - currentSpeed;

		integral = integralLast + (millis/2f)*(error + errorLast);
		powerCommand = (INTEGRAL_GAIN * integral) + 
				(PROPORTIONAL_GAIN * error);

		if (powerCommand > POWER_MAX) {
			integral = integralLast;
			powerCommand = POWER_MAX;
		}
		else if (powerCommand <= 0.0f) {
			integral = integralLast;
			powerCommand = 0.0f;
		}
		integralLast = integral;
		errorLast = error; 

		return powerCommand;

	}

	/******** get next station ***********/
	private void getNextStation(){
		// get destination
		nextStation = routeInfo.get(routeInfo.size()-1);
		// get destionation name
		nextStationName = nextStation.getStationName();		
	}

	/******** check if approach a station ***********/
	private void stationApproachCheck(){
		// distance needed to make a stop with service brake
		double brakeDownDist =0;		
		getDistToNextStation();		// update distance to nextStation
		// update slown down distance
		brakeDownDist = currSpeed*currSpeed/2/1.2;	

		//if distance to next station <= distance needed for a stop, pull brake
		if (distToNextStation <= brakeDownDist){
			train.setPower(0);		// turn down power
			train.setBrake(true);	// pull brake
			brakeType=1;			// set brake type to 1
		}
	}

	/******** get the distance to next station ***********/
	private void getDistToNextStation(){
		// return distance between current position to middle of station block
		
		// distance between a next block to the block before station black
		double distBetween=0;	

		if (!currBlock.getStationName().equals(nextStation.getStationName())){
			for (int i=routeInfo.indexOf(currBlock); 
					i<routeInfo.indexOf(nextStation)-1; i++){
				distBetween+=routeInfo.get(i+1).getLength();
			}
			distToNextStation = (currBlock.getLength() - 
					train.getPosition()) + distBetween + 
					nextStation.getLength()/2;
		}
		else if(currBlock.getStationName().equals(nextStation.getStationName()) 
				&& train.getPosition()< nextStation.getLength()/2){
			distToNextStation = currBlock.getLength()/2 - train.getPosition();
		}
		else{
			distToNextStation =0;
		}
	}

	/******** get authorized length ***********/
	private void getAuthLen(){
		// length from current position to next block
		authLen = currBlock.getLength() - train.getPosition(); 
		if (authority <= routeInfo.indexOf(nextStation)-
				routeInfo.indexOf(currBlock)){
			for (int i=0; i<authority; i++){
				authLen+=routeInfo.get(routeInfo.
						indexOf(currBlock)+1+i).getLength();
			}
		}
		else{
			for (int i=0; i<routeInfo.indexOf(nextStation)-
					routeInfo.indexOf(currBlock); i++){
				authLen+=routeInfo.get(routeInfo.
						indexOf(currBlock)+1+i).getLength();
			}
		}
	}

	/******** get distance required to make a emergency brake ***********/
	private double getEBrakeDist(){
		double dist;	
		dist = currSpeed*currSpeed/2/2.73;
		return dist;
	}

	/******** check authority ***********/
	private Boolean checkAuth(){
		// if authority goes to 0, pull emergency brake
		if (authority == 0 && 
				!currBlock.getStationName().equals(nextStation.getStationName()) 
				&& eBrakeType!=1 
				&& eBrakeType!=2){
			// set ebrake type to 1, ebrake pull by authority check
			eBrakeType=1;		
			train.setPower(0);
			train.toggleEmergencyBrake();
			return false;
		}
		// emergency pressed
		else if (authority == 0 && 
				!currBlock.getStationName().
				equals(nextStation.getStationName()) && eBrakeType==1){
			return false;
		}
		else if (eBrakeType == 1){
			eBrakeType = 0;
			train.toggleEmergencyBrake();
			train.setSetpointSpeed(train.getSpeedLimit());
		}
		
		return true;
	}

	/******** set tick method ***********/
	public void setTick(Boolean t){
		tick = t;
		// if stop ticking, release brake
		if (!tick){			
			brakeType=0;	// free brake type
//			train.setPower(0);		
			train.setBrake(false);
		}
	}

	/******** check if speed is safe to run ***********/
	private Boolean speedIsSafe(){
		if (currSpeed>(speedLimit/3.6)){
			brakeType=3;	// update brake type
			train.setPower(0);	// stop engine
			train.setBrake(true);
			return false;
		}
		else if (brakeType==3){
			brakeType=0;	// release brake	
			train.setBrake(false);

		}
		return true;
	}

	/******** Update GUI ***********/
	private void updateGui(){
		// update table 
		if (!panel.comboBox.getSelectedItem().equals("Train List")){
			// update current speed
			panel.table.setValueAt(String.format("%3.3f", 
					train.getVelocity()*3.6) + " KPH", 0, 1);
			// update set point speed
			panel.table.setValueAt(String.format("%3.3f", 
					train.getSetpointSpeed()) + " KPH", 1, 1);
			// update authority
			panel.table.setValueAt(authority, 2, 1);
			// update light
			if (lights==true){
				panel.table.setValueAt("On", 3, 1);
			}
			else {
				panel.table.setValueAt("Off", 3, 1);
			}
			// update doors
			if (doors==true ){
				panel.table.setValueAt("Open", 4, 1);
			}
			else {
				panel.doorToggle.setSelected(false);
				panel.table.setValueAt("Close", 4, 1);
			}
			// update temp
			panel.table.setValueAt(train.getTemperature(), 5, 1);
			// update emergency brake
			if (eBrake==true && eBrakeType==1){
				panel.tglbtnSetEmergencyBrake.setSelected(true);
				panel.table.setValueAt("Controller Pull", 6, 1);
			}
			else if (eBrake==true && eBrakeType==2){
				panel.tglbtnSetEmergencyBrake.setSelected(true);
				panel.table.setValueAt("Manually Pull", 6, 1);
			}
			else{
				panel.tglbtnSetEmergencyBrake.setSelected(false);
				panel.table.setValueAt("Release", 6, 1);
			}
			// update brake
			if (brake==true && brakeType==1){
				panel.SetBrake.setSelected(true);
				panel.table.setValueAt("Near Station Pull", 7, 1);
			}
			else if (brake==true && brakeType==2){
				panel.SetBrake.setSelected(true);
				panel.table.setValueAt("Authority Check Pull", 7, 1);
			}
			else if (brake==true && brakeType==3){
				panel.SetBrake.setSelected(true);
				panel.table.setValueAt("Speed Check Pull", 7, 1);
			}
			else if (brake==true && brakeType==4){
				panel.SetBrake.setSelected(true);
				panel.table.setValueAt("Manually Pull", 7, 1);
			}
			else if (brake==true && brakeType==5){
				panel.SetBrake.setSelected(true);
				panel.table.setValueAt("Low setpoint Pull", 7, 1);
			}
			else{
				panel.SetBrake.setSelected(false);
				panel.table.setValueAt("Release", 7, 1);
			}
			// update acceleration
			panel.table.setValueAt(String.format("%3.3f", 
					train.getAcceleration()) + " m/s^2", 8, 1);
			// update power
			panel.table.setValueAt(String.format("%3.3f", 
					train.getPower()) + " W", 9, 1);
			// update speed limit
			panel.table.setValueAt(train.getSpeedLimit() + " KPH", 10, 1);
			// update power limit
			panel.table.setValueAt(train.getPowerLimit() + " W", 11, 1);
			// update station field
			panel.nextStation.setText(nextStationName);
		}
		// reset table if no train is selected
		else {
			for (int i=0; i<12; i++){
				panel.table.setValueAt("N/A", i, 1);
			}
		}
	}
	
	/******** get brake type ***********/
	public int getBrakeType(){
		return brakeType;
	}
	
	/******** get emergency brake type ***********/
	public int getEBrakeType(){
		return eBrakeType;
	}
	
	/******** set brake type ***********/
	public void setBrakeType(int t){
		brakeType = t;
	}
	
	/******** set emergency brake type ***********/
	public void setEBrakeType(int t){
		eBrakeType = t;
	}
	
	/******** get speed limit ***********/
	public double getSpeedLimit(){
		return train.getSpeedLimit();
	}
	
	/******** regulate set point speed ***********/
	public void regulateSpeed(){
		if (train.getSetpointSpeed()<=speedLimit){
			setPointSpeed = train.getSetpointSpeed()/3.6;

		}
		else {
			setPointSpeed = train.getSpeedLimit()/3.6;
			train.setSetpointSpeed(speedLimit);
		}
		if (setPointSpeed<currSpeed){
			brakeType = 5;
			setBrake(true);
		}
		else if (brakeType==5){
			setBrake(false);
		}
	}
	
	private void lightControl(){
		if (currBlock.isUnderground()){
			lights = true;
			lightType = 1;		// underground mode
		}
		// no manual control of lights
		else if (lightType != 2){
			lights = false;
			lightType = 0;		// free mode
		}
	}
	
	private void doorControl(){
		if (currSpeed>0){
			doors = false;
			doorType = 1; // prohibited mode
		}
		// no manual control of doors
		else if (currSpeed<=0 && doorType!=2){
			doorType = 0;	// free mode
		}
	}
	
	public int getLightType(){
		return lightType;
	}
	
	public void setLightType(int lt){
		lightType=lt;
	}
	
	public int getDoorType(){
		return doorType;
	}
	
	public void setDoorType(int dt){
		doorType=dt;
	}
	
}
