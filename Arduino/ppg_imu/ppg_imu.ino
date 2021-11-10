#include <MPU6050_tockn.h>
#include <Wire.h>
#define USE_ARDUINO_INTERRUPTS true    // Set-up low-level interrupts for most acurate BPM math.
#include <PulseSensorPlayground.h>     // Includes the PulseSensorPlayground Library.   

MPU6050 mpu6050(Wire);

long timer = 0;

int Signal;
const int PulseWire = 0;       // PulseSensor PURPLE WIRE connected to ANALOG PIN 0
const int LED13 = 13;          // The on-board Arduino LED, close to PIN 13.
int Threshold = 550;           // Determine which Signal to "count as a beat" and which to ignore.
                               // Use the "Gettting Started Project" to fine-tune Threshold Value beyond default setting.
                               // Otherwise leave the default "550" value. 
PulseSensorPlayground pulseSensor;  // Creates an instance of the PulseSensorPlayground object called "pulseSensor"


void setup() {
  Serial.begin(19200);
  
  // Configure the PulseSensor object, by assigning our variables to it. 
  pulseSensor.analogInput(PulseWire);   
  pulseSensor.blinkOnPulse(LED13);       //auto-magically blink Arduino's LED with heartbeat.
  pulseSensor.setThreshold(Threshold);   

  // Double-check the "pulseSensor" object was created and "began" seeing a signal. 
   if (pulseSensor.begin()) {
    Serial.println("We created a pulseSensor Object !");  //This prints one time at Arduino power-up,  or on Arduino reset.  
  }
  
  Wire.begin();
  mpu6050.begin();
  mpu6050.calcGyroOffsets(true);
  Serial.println("");
}

void loop() {
  mpu6050.update();
  Signal = analogRead(PulseWire); // Read the sensor value
  int myBPM = pulseSensor.getBeatsPerMinute();
  
  if(millis() - timer > 40){ 
    Serial.print(mpu6050.getTemp());Serial.print(",");
    
    Serial.print(mpu6050.getAccX());Serial.print(",");
    Serial.print(mpu6050.getAccY());Serial.print(",");
    Serial.print(mpu6050.getAccZ());Serial.print(",");
    
    Serial.print(mpu6050.getGyroX());Serial.print(",");
    Serial.print(mpu6050.getGyroY());Serial.print(",");
    Serial.print(mpu6050.getGyroZ());Serial.print(",");
    
    Serial.print(mpu6050.getAccAngleX());Serial.print(",");
    Serial.print(mpu6050.getAccAngleY());Serial.print(",");

    Serial.print(mpu6050.getGyroAngleX());Serial.print(",");
    Serial.print(mpu6050.getGyroAngleY());Serial.print(",");
    Serial.print(mpu6050.getGyroAngleZ());Serial.print(",");
    
    Serial.print(mpu6050.getAngleX());Serial.print(",");
    Serial.print(mpu6050.getAngleY());Serial.print(",");
    Serial.print(mpu6050.getAngleZ());Serial.print(",");

    Serial.print(Signal);Serial.print(",");
    Serial.println(myBPM);
    
  }

}
