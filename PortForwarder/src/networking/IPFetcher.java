package networking;

import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import application.GuiController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class IPFetcher {
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private GuiController Gui;
	
	public IPFetcher(GuiController Gui) {
		this.Gui = Gui;
	}
	
	public void fetchIP() {
		LOGGER.entering(this.getClass().getName(), "fetchIP");
		new Thread() {		
    		public void run() {
    			Label progress_label = Gui.getProgress_label();
    			Button btn_execute = Gui.getBtn_execute();
    			Button btn_reconnect = Gui.getBtn_reconnect();
        		//try with resource: (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api64.ipify.org").openStream(), "UTF-8").useDelimiter("\\A"))
    			//better not to. This leaks resources you can't close anymore...
    			try {
    				Platform.runLater(new Runnable() {
    					@Override public void run() {
    						progress_label.setTextFill(Color.BLACK);
    						progress_label.setText("Fetching public IP-Address...");
    						Gui.setPublicIP("fetching...");
    						btn_execute.setDisable(true);
    					}
    				});
					URL api = new URL("https://api64.ipify.org");
					InputStream ips = api.openStream();
	    			Scanner sc = new Scanner(ips, "UTF-8");
	    			sc.useDelimiter("\\A");
	    		    final String res = sc.next();
	    		    sc.close();
	    		    ips.close();
	    		    LOGGER.log(Level.FINE, "Public IP fetched: " + res);
	    		    
	    		    Platform.runLater(new Runnable() {
	    	            @Override public void run() {
	    	            	progress_label.setTextFill(Color.GREEN);
	    	                progress_label.setText("Public IP fetched; ready to connect to server!");
	    	                Gui.setPublicIP(res);
	            		    btn_execute.setDisable(false);
	    	            }
	    	        });
	    		    LOGGER.exiting(this.getClass().getName(), "fetchIP");
        		} catch (Exception e) {
        			//fix UI
        			Platform.runLater(new Runnable() {
        				@Override public void run() {
                			btn_reconnect.setDisable(false);
                			btn_reconnect.setVisible(true);
                			Gui.setPublicIP("No connection.");
                			
                			progress_label.setTextFill(Color.RED);
                			progress_label.setText("Reconnect to the net and try again.");
        				}
        			});
        			
        			LOGGER.log(Level.WARNING, "Connection Error, no functionality.", e);
        		    //Display error notification to user & exit
        		    Alert alert = new Alert(AlertType.ERROR);
        			alert.setTitle("Connection Error!");
        			alert.setHeaderText("Connection Error Detected.");
        			alert.setContentText("Error fetching public IP Address, please make sure you are connected to the internet. If this problem persists, notify the server admin.");
        			alert.showAndWait();
        		}
    		}
    	}.start();
    	LOGGER.log(Level.FINER, "IP-fetcher thread launched.");
	}
}
