#include <ESP8266WiFi.h>

//GPIO 
const int ledPin = 2; 


//WIFI
WiFiServer server(80); 
const char* ssid = "HUAWEI-sdDf";
const char* password = "GqY7Cyip";

//WIFI Book keeping
String currlocalIP; 
WiFiClient client; 






// GPIO2 of ESP8266 - LED used to indicate the status of the module
//Initialize a server
//type your ssid
//type your password
//stores the local IP of the wifi module (assignned by the router)
//To store a connection to the client

 


/**
 * setup() function
 * Called once when the WiFi module boots up
 * 
*/
void setup() {

  //LED: Initialize LED.
  pinMode(ledPin, OUTPUT);


  //LED: Turn LED ON when below initializing process begins
  digitalWrite(ledPin, HIGH);

  //Begin serial for communication with arduino
  Serial.begin(9600);
  delay(500);

  //Begin WiFi connection
  WiFi.begin(ssid, password);
  delay(500);
  while (WiFi.status() != WL_CONNECTED)//Connect to Wifi
  {
    delay(500); //wait till the WiFi is connected.
  }
  delay(500);
  
  //Start server on ESP8266
  server.begin();

  //LED: Turn LED OFF when initializing is done.
  digitalWrite(ledPin, LOW);

  Serial.flush();
}


/**
 * loop() function
 * This is function is called continuously by the arduino
 * Statements in here get executed like they are in an inifinite loop; 
*/
void loop() 
{

  //Check if there is a client request
  String clientRequest = onClientRequest();
  if(clientRequest != "N/A")
  {
    //send response to client
    String cleanClientRequest = cleanUpRequestString(clientRequest);
    String response = generateClientResponse(cleanClientRequest);
    sendToClient(response);
    return;
  }

  //Check if there is a response from the arduino
  String arduinoRequest = onArduinoRequest();
  if(arduinoRequest != "N/A")
  {
    
    //send response to arduino
    if(arduinoRequest.indexOf("getip") != -1)
    sendToArduino( "!"+  WiFi.localIP().toString() + "!");
    
    return;
  }
}


/**
 * cleanUpRequestString()
 * 
 * This function cleans up any noise from the string.
 * The information we need is enclosed in "!" symbols.
 * 
 * So a message from the the client to turn OFF the lights looks like !L:OFF!
 *  and to turn ON the client will send !L:ON!"
 * 
 * This function cleans up anything recieved before and after the opening and closing "!"
*/
String cleanUpRequestString(String request)
{
  String clean = "";
  boolean append = false;
  
  for(int i = 0; i < request.length(); i ++)
  {
    if(request.charAt(i) == '!' && append == false)
    {
      append = true;
      }

    else if(request.charAt(i) == '!' && append == true)
     {
      append = false;
      clean = clean + request.charAt(i);
      break;
      }

     if(append == true)
     {
      clean = clean + request.charAt(i);
      }
    
    }
    return clean;
  }


/**
 * generateClientResponse()
 * 
 * This client takes in a client request string and generates 
 * the response for the client
*/

String generateClientResponse(String request)
{

  String response = getArduinoResponseToRequest(request);
  return response;
}



/**
 * generateArduinoResponse()
 * 
 * This function is called when the arduino is the requestor for some
 * information from the Wi-Fi mmodule.
 * 
 * Information such as the local IP on the WiFi
*/

String generateArduinoResponse(String request)
{
  //Daca request-ul este de a lua IP-ul 
  if(request.indexOf("getip") != -1)
  {
    return WiFi.localIP().toString();
  }

  else return  "N/A";
}



/**
 * onClientRequest()
 * 
 * -Checks if there is a client
 *    -If there is, then we get that request and return it.
 *    -If there is no client, we simply return "N/A" as a string.
*/

String onClientRequest()
{
  //Ia client instance
  client = server.available();
  
  //Verifica daca exista client 
  if (!client) {
    client.flush();
    return "N/A"; //returneaza "N/A" ca indicator ca nu este client
  }

  //Daca este client, asteapta pana trimite date
  while(!client.available()){
    delay(1);
  }

  //Citeste datele
  String clientRequest = client.readStringUntil('\r');
  client.flush();

  //Returneaza data ca si string.
  return clientRequest;
 }




String onArduinoRequest()
{
  //Verifica daca Arduino incearca sa trimita date
  if(!Serial.available())
  {
    Serial.flush();
    return "N/A"; //Daca nu, return "N/A" 
  }

  //Ia datele de pe serial
  String arduinoRequest = (String) Serial.readString();
  Serial.flush();

  //Returneaza datele ca string
  return arduinoRequest;
 }



void sendToArduino(String out)
{
  Serial.println(out);
}

/**
 * sendToClient()
 * 
 * Sends data to the client.
 * This is the reponse to the http get request that the clients 
 * make (From the android app)
*/


void sendToClient(String out)
{
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text");
  client.println(""); 
  client.println("");
  client.print(out);
  
}

/**
 * getArduinoResponseToRequest()
 * 
 * This function is called when the client request if for the arduino.
 * 
*/

String getArduinoResponseToRequest(String request)
{
 
  //Print request catre Arduino
  Serial.println(request);

  //Asteapta pana cand Arduino este pregatit pentru a trimite raspuns
  while(!Serial.available()){
    digitalWrite(ledPin, HIGH); //porneste led, pentru a se indica starea de not ready
    }

  //Ia raspunsul de la Arduino
  String arduinoResponse = Serial.readString();
  digitalWrite(ledPin, LOW); //opreste led, pentru a indica ca am primit raspunsul
  
  return arduinoResponse; //returneaza string-ul raspuns
}