package com.github.michael79bxl.zbtprinter;

import java.io.IOException;
import java.io.File;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.util.Base64;

import android.os.Looper;

import com.zebra.android.discovery.*;
import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.*;
import com.zebra.sdk.graphics.*;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.device.*;

public class ZebraBluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    //String mac = "AC:3F:A4:1D:7A:5C";

    public ZebraBluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("print")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendData(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
		if (action.equals("batch")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendBatch(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("find")) {
            try {
                findPrinter(callbackContext);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
	    if (action.equals("image")) {
            try {
                String mac = args.getString(0);
                String name = args.getString(1);
				String img = args.getString(2);
                sendImage(callbackContext, mac, name, img);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    
    public void findPrinter(final CallbackContext callbackContext) {
      try {
          BluetoothDiscoverer.findPrinters(this.cordova.getActivity().getApplicationContext(), new DiscoveryHandler() {

              public void foundPrinter(DiscoveredPrinter printer) {
                  if(printer instanceof DiscoveredPrinterBluetooth) {
                     JSONObject printerObj = new JSONObject();
                     try {
                       printerObj.put("address", printer.address);
                       printerObj.put("friendlyName", ((DiscoveredPrinterBluetooth) printer).friendlyName);
                       callbackContext.success(printerObj);
                     } catch (JSONException e) {
                     }
                  } else {              
                    String macAddress = printer.address;
                    //I found a printer! I can use the properties of a Discovered printer (address) to make a Bluetooth Connection
                    callbackContext.success(macAddress);
                  }
              }

              public void discoveryFinished() {
                  //Discovery is done
				   callbackContext.error("discoveryDone");
              }

              public void discoveryError(String message) {
                  //Error during discovery
                  callbackContext.error(message);
              }
          });
      } catch (Exception e) {
          e.printStackTrace();
      }      
    }

	
	/*
     * This will send an image to the bluetooth printer
     */  	 
    void sendImage(final CallbackContext callbackContext, final String mac, final String imgName, final String imgData) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {                             	
					byte[] imageData = Base64.decode(imgData, 0);					
					Connection thePrinterConn = new BluetoothConnection(mac);	
					
					   // Initialize
                     Looper.prepare();
					 
                    if (isPrinterReady(thePrinterConn, PrinterLanguage.ZPL)) {						 
						 try {
								thePrinterConn.open();										  
								final ZebraPrinter printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, thePrinterConn);
								Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);	
								final int maxSize = 300;
								int outWidth;
								int outHeight;
								int inWidth = bitmap.getWidth();
								int inHeight = bitmap.getHeight();
								if(inWidth > inHeight){
									outWidth = maxSize;
									outHeight = (inHeight * maxSize) / inWidth; 
								} else {
									outHeight = maxSize;
									outWidth = (inWidth * maxSize) / inHeight; 
								}
								Bitmap signatureBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);													
								
									 String init = "! U1 SETVAR \"device.languages\" \"zpl\"\r\n! U1 JOURNAL\r\n! U1 SETFF 1 1\r\n! U1 setvar \"ezpl.media_type\" \"continuous\"\r\n! U1 setvar \"zpl.label_length\" \"180\"\r\n" +
														"^XA^POI";  
														
										if (imgName != "") {										
											 init += "^CFD,26,10^FO0,160^FB792,1,,C^CI27^FH^FD" + imgName + "^FS";										
										}		
									
									 thePrinterConn.write(init.getBytes("ISO-8859-1"));															
								
									 printer.printImage(new ZebraImageAndroid(signatureBitmap), 250, 0, -1, -1, true);
									 
									 String close = "^XZ\r\n! U1 SETVAR \"device.languages\" \"line_print\"\r\n";
														  
									thePrinterConn.write(close.getBytes("ISO-8859-1"));	

							} catch (ConnectionException e) {									
								callbackContext.error(e.getMessage());										
							}  catch (Exception e) {									
								callbackContext.error(e.getMessage());										
							} 
							Thread.sleep(500);
							thePrinterConn.close();		
							Looper.myLooper().quit();							
							callbackContext.success("Done");								
					  
                    } else {
					callbackContext.error("Printer is not ready");
					}
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }
	
	 /*
     * This will process a batch with data and images and send to be printed by the bluetooth printer
     */
    void sendBatch(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    //Connection thePrinterConn = new BluetoothConnectionInsecure(mac);
					Connection thePrinterConn = new BluetoothConnection(mac);
		      
					// Initialize
                     Looper.prepare();
					 
                    // Verify the printer is ready to print
                     if (isPrinterReady(thePrinterConn, PrinterLanguage.LINE_PRINT)) {

                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();
			 						 
						JSONArray aryJSONStrings = new JSONArray(msg);
						for (int i=0; i<aryJSONStrings.length(); i++) {
							   String sTyp = aryJSONStrings.getJSONObject(i).getString("typ");
							   String sString = aryJSONStrings.getJSONObject(i).getString("string");
							   
							   if (sTyp.equals("data")) {
									thePrinterConn.write(sString.getBytes("ISO-8859-1"));
							   }
							   
							   if (sTyp.equals("image")) {
									String sTitle = aryJSONStrings.getJSONObject(i).getString("title");
									byte[] imageData = Base64.decode(sString, 0);	
									
									final ZebraPrinter printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, thePrinterConn);
									
									//Prepare Image
									Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);	
									final int maxSize = 300;
									int outWidth;
									int outHeight;
									int inWidth = bitmap.getWidth();
									int inHeight = bitmap.getHeight();
									if(inWidth > inHeight){
										outWidth = maxSize;
										outHeight = (inHeight * maxSize) / inWidth; 
									} else {
										outHeight = maxSize;
										outWidth = (inWidth * maxSize) / inHeight; 
									}
									Bitmap signatureBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);													
									
									//Prepare Print
									String init = "! U1 SETVAR \"device.languages\" \"zpl\"\r\n! U1 JOURNAL\r\n! U1 SETFF 1 1\r\n! U1 setvar \"ezpl.media_type\" \"continuous\"\r\n! U1 setvar \"zpl.label_length\" \"180\"\r\n" +
														"^XA^POI";  
														
									if (sTitle != "") {										
										 init += "^CFD,26,10^FO0,160^FB792,1,,C^CI27^FH^FD" + sTitle + "^FS";										
									}		
									
									thePrinterConn.write(init.getBytes("ISO-8859-1"));															
									
									printer.printImage(new ZebraImageAndroid(signatureBitmap), 250, 0, -1, -1, true);
										 
									String close = "^XZ\r\n! U1 SETVAR \"device.languages\" \"line_print\"\r\n";															  
									thePrinterConn.write(close.getBytes("ISO-8859-1"));		
							   }	
						}
		
						// Make sure the data got to the printer before closing the connection
						Thread.sleep(500);	

                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                   						
						Looper.myLooper().quit();
						
						callbackContext.success("Done");
							 
                    } else {
						callbackContext.error("Printer is not ready");
				}
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }
	
    /*
     * This will send data to be printed by the bluetooth printer
     */
    void sendData(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    //Connection thePrinterConn = new BluetoothConnectionInsecure(mac);
					Connection thePrinterConn = new BluetoothConnection(mac);
		      
					// Initialize
                     Looper.prepare();
					 
                    // Verify the printer is ready to print
                     if (isPrinterReady(thePrinterConn, PrinterLanguage.LINE_PRINT)) {

                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();

                        // Send the data to printer as a byte array.
						// thePrinterConn.write("^XA^FO0,20^FD^FS^XZ".getBytes());
                        thePrinterConn.write(msg.getBytes("ISO-8859-1"));

                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);

                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                   						
						Looper.myLooper().quit();
						
						callbackContext.success("Done");
							 
                    } else {
						callbackContext.error("Printer is not ready");
				}
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    private Boolean isPrinterReady(Connection connection, PrinterLanguage printerLanguage) throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Boolean isOK = false;
        connection.open();
        // Creates a ZebraPrinter object to use Zebra specific functionality like getCurrentStatus()
        ZebraPrinter printer = ZebraPrinterFactory.getInstance(printerLanguage,connection);
        
        // Creates a LinkOsPrinter object to use with newer printer like ZQ520 
        ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);
            
        //PrinterStatus printerStatus = printer.getCurrentStatus();
        PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();
             
        if (printerStatus.isReadyToPrint) {
            isOK = true;
        } else if (printerStatus.isPaused) {
            throw new ConnectionException("Cannot print because the printer is paused");
        } else if (printerStatus.isHeadOpen) {
            throw new ConnectionException("Cannot print because the printer media door is open");
        } else if (printerStatus.isPaperOut) {
            throw new ConnectionException("Cannot print because the paper is out");
        } else {
            throw new ConnectionException("Cannot print");
        }
        
        connection.close();
        return isOK;
        
    }
}

