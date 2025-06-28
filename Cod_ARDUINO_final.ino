#include <Wire.h>
#include <RTClib.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Servo.h>

// ──────────────────────────────────────────────────────────────────────────────
// SENSOR CONFIGURATION
// ──────────────────────────────────────────────────────────────────────────────

const int tempSensorReadPin   = A3;
OneWire oneWire(tempSensorReadPin);
DallasTemperature tempSensor(&oneWire);
String temperatureString      = "N/A";

// Water-level sensor now powered permanently from 5 V.
// We just read A0 directly.


const int waterLevelReadPin   = A0;
String waterLevelString       = "N/A";


RTC_DS3231 rtc;
String timeString             = "N/A";
String ipString               = "N/A";

const int tdsPin               = A1;
String tdsString              = "N/A";


const int tdsThreshold        = 500;   // ppm

// ──────────────────────────────────────────────────────────────────────────────
// ACTUATORS & SCHEDULE
// ──────────────────────────────────────────────────────────────────────────────

Servo feederServo;
const int servoPin            = 7;

const int extractionPumpPin   = 8;
const int addingPumpPin       = 9;

const int lightPin            = 10;
bool lightIsOn                = false;

int feedingHour               = 12;
int feedingMinute             = 0;
bool fedToday                 = false;
unsigned long lastFeedingCheck= 0;

int  portions                 = 1;
int  feedCountSinceRefill     = 0;
const int FEED_EMPTY_THRESHOLD= 30;

// Autonomous water-change params
const int  LEVEL_LOW          = 330;  // <25%
const int  LEVEL_HIGH         = 600;  // ~75%
enum Mode { MANUAL, AUTO };
Mode currentMode              = MANUAL;
bool  tdsAlarmed              = false;

// throttle TDS alerts to once per 20 s


unsigned long lastTdsAlertMillis = 0;
const unsigned long TDS_ALERT_COOLDOWN = 20UL * 1000UL; //20 sec

// once‐only TDS alert guard
bool alertedBadWater = false;

int tdsValue = 0;          // latest TDS in ppm, always ≥ 0


// … up near the top with your other pins:

const int motorRelayPin = 11;  // Relay IN pin for your DC motor


// Fill/drain durations (seconds)

const unsigned long FILL_DURATION_SEC  = 60;    // e.g. 5min
const unsigned long DRAIN_DURATION_SEC = 60;    // e.g. 3min

// Fill/drain tracking

DateTime fillStartTime;
DateTime drainStartTime;
bool     isFilling  = false;
bool     isDraining = false;

                                                       // Helper: seconds since a DateTime
long secondsSince(const DateTime& then) {
  return rtc.now().unixtime() - then.unixtime();
}



// ──────────────────────────────────────────────────────────────────────────────
// SETUP
// ──────────────────────────────────────────────────────────────────────────────
void setup(){
  Serial.begin(9600);
  Wire.begin();

  // RTC init
  if (!rtc.begin()) { while(1); }
  if (rtc.lostPower())
    rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));

  // Temperature sensor
  tempSensor.begin();

    // Now that RTC is ready, capture initial timestamps
  fillStartTime  = rtc.now();
  drainStartTime = rtc.now();

  // Feeder
  feederServo.attach(servoPin);
  feederServo.write(0);

  // Pumps & light


  pinMode(extractionPumpPin, OUTPUT); digitalWrite(extractionPumpPin, LOW);
  pinMode(addingPumpPin,     OUTPUT); digitalWrite(addingPumpPin,     LOW);


  pinMode(lightPin,          OUTPUT); digitalWrite(lightPin,          LOW);

    // Motor relay


  pinMode(motorRelayPin, OUTPUT);
  digitalWrite(motorRelayPin, HIGH);  // start “off”

  delay(500);
  Serial.println("System Initialized");

  // get IP from ESP (via the serial-to-WiFi link)
  ipString = updateIP();
  Serial.print("ESP IP: "); Serial.println(ipString);

  lastFeedingCheck = millis();
}

// ──────────────────────────────────────────────────────────────────────────────
// MAIN LOOP
// ──────────────────────────────────────────────────────────────────────────────
void loop(){
  updateTemperature();
  updateTime();
  updateWaterLevel();
  updateTDS();


  // Parse the PPM out of the string once
  int tdsValue = tdsString.toInt();

  // Get current millis
  unsigned long now = millis();

  // If we’re over threshold AND our cooldown has passed, fire once


  if (tdsValue >= tdsThreshold
      && (now - lastTdsAlertMillis) > TDS_ALERT_COOLDOWN)
  {
    Serial.println("!ALERT:TDS_HIGH!");                      // this shows the dialog in your app
    lastTdsAlertMillis = now;                                // reset the cooldown
    if (currentMode == AUTO) {
      performAutoWaterChange();                               // only if you’re in auto‐mode
    }
  }

  //checkTDSThreshold();


  checkWiFiRequest();


  // feeding schedule every minute

  if (millis() - lastFeedingCheck > 60000){
    lastFeedingCheck = millis();
    DateTime now = rtc.now();
    if (now.hour()==feedingHour && now.minute()==feedingMinute   && !fedToday){                                
      triggerFeeding();
      fedToday = true;
    }
   if (now.hour()==0 && now.minute()==0) fedToday = false;
  }

  delay(200);
}

// ──────────────────────────────────────────────────────────────────────────────
// SENSOR UPDATES
// ──────────────────────────────────────────────────────────────────────────────

void updateTemperature(){
  tempSensor.requestTemperatures();
  double t = tempSensor.getTempCByIndex(0);
  temperatureString = String(t,1) + " C";
}

void updateTime(){
  DateTime now = rtc.now();
  char buf[6];
  sprintf(buf, "%02d:%02d", now.hour(), now.minute());
  timeString = String(buf);
}

void checkTDSThreshold() {
  int tdsValue = tdsString.toInt();

  // clamp negatives up to 0
  if (tdsValue < 0) tdsValue = 0;

  // only alert when truly above threshold
  if (tdsValue >= tdsThreshold && !alertedBadWater) {
    Serial.println("!ALERT:TDS_HIGH!");
    alertedBadWater = true;
  }
  // reset the alert when it falls back under
  if (tdsValue < tdsThreshold) {
    alertedBadWater = false;
  }
}


void updateWaterLevel(){
  
  int raw = analogRead(waterLevelReadPin);
  if      (raw < LEVEL_LOW)   waterLevelString = "<25%";
  else if (raw < LEVEL_HIGH)  waterLevelString = "~50%";
  else                        waterLevelString = "~75%";
}

void updateTDS()
{
  int raw = analogRead(tdsPin);

  // convert ADC reading → voltage (5 V reference assumed)
  float voltage = raw * (5.0 / 1023.0);

    // voltage → EC (mS/cm)  — same polynomial you already had
  float ec = 133.42 * pow(voltage, 3)
           - 255.86 * pow(voltage, 2)
           + 857.39 * voltage;

     // EC → ppm  (0.5 conversion factor, then to ppm)
  int ppm = int(ec * 0.5 * 1000.0);

       // ─── NEW: clamp negative results to zero ──────────────
  if (ppm < 0) ppm = 0;

         // store the numeric value for the alarm test
  tdsValue = ppm;

           // and build the printable string for the app
  tdsString = String(ppm) + "ppm";
}


// ──────────────────────────────────────────────────────────────────────────────
// COMMUNICATION & COMMAND PARSING
// ──────────────────────────────────────────────────────────────────────────────
void checkWiFiRequest(){
  if (!Serial.available()) return;
  String raw = Serial.readStringUntil('\n');
  String cmd = cleanUp(raw);

       if (cmd.startsWith("MODE:M")) { currentMode = MANUAL;  Serial.println("Mode=Manual"); }
  else if (cmd.startsWith("MODE:A")) { currentMode = AUTO;    Serial.println("Mode=Auto");
                                         performAutoWaterChange();
                                         tdsAlarmed = false;
                                       }

  else if (cmd.startsWith("F:"))     updateFeedingSchedule(cmd);

  else if (cmd.startsWith("PE:"))    { digitalWrite(extractionPumpPin, cmd.endsWith("ON")?HIGH:LOW);
                                       isDraining = (cmd.substring(3) == "ON");
                                       if (isDraining) drainStartTime = rtc.now();
                                       Serial.println("Extraction Pump "+cmd.substring(3)); }
  else if (cmd.startsWith("PA:"))    { digitalWrite(addingPumpPin, cmd.endsWith("ON")?HIGH:LOW);
                                       isFilling = (cmd.substring(3) == "ON");
                                       if (isFilling) fillStartTime = rtc.now();
                                       Serial.println("Adding Pump "+cmd.substring(3)); }

  else if (cmd.startsWith("PL:"))    { lightIsOn = cmd.endsWith("ON");
                                       digitalWrite(lightPin, lightIsOn?HIGH:LOW);
                                       Serial.println("Light "+cmd.substring(3)); }

  else if (cmd.startsWith("Q:"))     { portions = cmd.substring(2).toInt();
                                       Serial.print("Portions=");Serial.println(portions); }

  else if (cmd.startsWith("MTR:")) {
    String s = cmd.substring(4);                  // pulls “ON” or “OFF”
    digitalWrite(motorRelayPin, 
                 (s == "ON") ? LOW : HIGH);
    Serial.println("Motor turned " + s);
  }   

  else if (cmd == "RESET_FOOD"){
    feedCountSinceRefill = 0;
    Serial.println("!INFO:FOOD_RESET!");
  }
    else if (cmd == "RESET_FILL") {
    isFilling     = false;
    fillStartTime = rtc.now();
    Serial.println("!INFO:FILL_RESET!");
  }
  else if (cmd == "RESET_DRAIN") {
    isDraining     = false;
    drainStartTime = rtc.now();
    Serial.println("!INFO:DRAIN_RESET!");
  }

  else if (cmd == "U:ALL")           
  Serial.println(buildStatus());

}

String cleanUp(const String& s){
  int b1 = s.indexOf('!');
  int b2 = s.indexOf('!', b1+1);
  if (b1>=0 && b2>b1) return s.substring(b1+1,b2);
  return s;
}

// ──────────────────────────────────────────────────────────────────────────────
// FEEDER, LIGHT, SCHEDULE HELPERS
// ──────────────────────────────────────────────────────────────────────────────

void updateFeedingSchedule(const String& c){
  int p = c.indexOf(':',2);
  feedingHour   = c.substring(2,p).toInt();
  feedingMinute = c.substring(p+1).toInt();
  fedToday      = false;
  Serial.print("Feeding at "); Serial.print(feedingHour);
  Serial.print(":"); Serial.println(feedingMinute);
}

void triggerFeeding(){
  Serial.println("Feeding…");
  for(int i=0;i<portions;i++){
    feederServo.write(90);
    delay(1000);
    feederServo.write(0);
    delay(500);
    feedCountSinceRefill++;
    if(feedCountSinceRefill>=FEED_EMPTY_THRESHOLD)
      Serial.println("!ALERT:FOOD_EMPTY!");
  }
  Serial.println("Feeding done.");
}

// ──────────────────────────────────────────────────────────────────────────────
// AUTO WATER-CHANGE CYCLE
// ──────────────────────────────────────────────────────────────────────────────

void performAutoWaterChange(){
  Serial.println("Auto-change started");

                                                                  // DRAIN until below LEVEL_LOW
  digitalWrite(extractionPumpPin, HIGH);
  while (analogRead(waterLevelReadPin) > LEVEL_LOW) {
    delay(20);                                                              // poll every 20ms
  }
  digitalWrite(extractionPumpPin, LOW);

                                                                      // FILL until above LEVEL_HIGH
  digitalWrite(addingPumpPin, HIGH);
  while (analogRead(waterLevelReadPin) < LEVEL_HIGH) {
    delay(20);
  }
  digitalWrite(addingPumpPin, LOW);

  Serial.println("Auto-change complete");

                                                                      // **NEW**: go back to MANUAL so we only run one cycle
  currentMode = MANUAL;
  Serial.println("Mode=Manual (auto cycle finished)");
}

// ──────────────────────────────────────────────────────────────────────────────
// BUILD STATUS STRING
// ──────────────────────────────────────────────────────────────────────────────

String buildStatus(){
  String r = "!";

  r += "T:"; r += temperatureString;
  r += ",W:"; r += waterLevelString;
  r += ",R:"; r += timeString;

  // Lumini LED
  r += ",L:"; 
  r += (lightIsOn ? "ON" : "OFF");

  // Food-empty alert
  if (feedCountSinceRefill >= FEED_EMPTY_THRESHOLD) {
    r += ",ALERT:FOOD_EMPTY";
  }

  // TDS
  r += ",D:"; 
  r += tdsString;

  //dc motor
  r += ",M:"; 
  r += ( digitalRead(motorRelayPin)==HIGH ? "ON" : "OFF" );

  // Fill-full alert
  long elapsedFill = secondsSince(fillStartTime);
  if (isFilling && elapsedFill >= FILL_DURATION_SEC) {
    r += ",ALERT:FILL_FULL";
  }
  // Drain-empty alert
  long elapsedDrain = secondsSince(drainStartTime);
  if (isDraining && elapsedDrain >= DRAIN_DURATION_SEC) {
    r += ",ALERT:DRAIN_EMPTY";
  }

  r += "!";
  return r;
}

// ──────────────────────────────────────────────────────────────────────────────
// GET IP FROM ESP
// ──────────────────────────────────────────────────────────────────────────────
String updateIP(){
  Serial.print("getip");
  while(!Serial.available()) delay(10);
  String raw = Serial.readString();
  return cleanUp(raw);
}


