package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import configurations.Constants;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.paint.Color;
import networking.IPFetcher;
import networking.SSHManager;

public class GuiController {
	private Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private IPFetcher ipf;
	private SSHManager instance;
	private String hostsfilepath;

    @FXML
    private TextField public_ip_label;

    @FXML
    private TextField ip_addr_label;

    @FXML
    private TextField usn_label;

    @FXML
    private TextField ssh_auth_label;

    @FXML
    private Button btn_execute;
    
    @FXML
    private Button btn_reconnect;
    
    @FXML
    private Button btn_disconnect;

    @FXML
    private Label progress_label;
    
    @FXML
    public void initialize() {
    	LOGGER.entering(this.getClass().getName(), "initialize");
    	
    	//some configurations for startup
    	btn_execute.setDisable(true);
    	
    	//make sure only IP Addresses can be entered in this field
    	String regex = Constants.getPartialIPRegex();
    	final UnaryOperator<Change> ipAddressFilter = c -> {
    		String text = c.getControlNewText();
    		if (text.matches(regex)) {
    			return c;
    		} else {
    			return null;
    		}
    	};
    	ip_addr_label.setTextFormatter(new TextFormatter<>(ipAddressFilter));
    	
    	//fetch IP
    	ipf = new IPFetcher(this);
    	ipf.fetchIP();
    	
    	//get hostfilepath
    	if(Constants.isWindows) {
    		hostsfilepath = Constants.HOME_DIR+"\\.ssh\\known_hosts";
    	} else {
    		hostsfilepath = Constants.HOME_DIR+"/.ssh/known_hosts";
    	}
    	LOGGER.log(Level.FINER, "Assumed hostfilepath: " + hostsfilepath);
    	File file = new File(hostsfilepath);
    	if(!file.exists()) {
    		try {
				file.createNewFile();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Couldn't make a new file on filesystem. Proceeding regardless...");
			}
    	}
    	
    	LOGGER.exiting(this.getClass().getName(), "initialize");
    }

    //this is used when there is no internet connection detected on program boot
    @FXML
    void reconnect(ActionEvent event) {
    	if(btn_reconnect.isDisabled()) {
    		return;
    	}
    	btn_reconnect.setDisable(true);
    	LOGGER.entering(this.getClass().getName(), "reconnect");
    	btn_reconnect.setVisible(false);
    	ipf.fetchIP();
    	LOGGER.exiting(this.getClass().getName(), "reconnect");
    }

    @FXML
    void connect(ActionEvent event) {
    	
    	if(btn_execute.isDisabled()) {
    		return;
    	}
    	//gui
    	btn_execute.setDisable(true);
    	LOGGER.entering(this.getClass().getName(), "connect");
    	
    	//fetch inputs & trim whitespaces
    	final String target = ip_addr_label.getText().trim();
    	final String usn = usn_label.getText().trim();
    	final String sshAuth = ssh_auth_label.getText().trim();
    	//also validate them
    	if(!validateInputs(target, usn, sshAuth)) {
    		btn_execute.setDisable(false);
    		return;
    	}
    	
    	new Thread() {
    		@Override public void run() {
		    	try {
		    		instance = new SSHManager(usn, sshAuth, target, hostsfilepath, Constants.GATEWAY_PORT);
		    		instance.connect();
		    		instance.forwardPort(Constants.LOCAL_PORT, Constants.SERVICE_IP, Constants.SERVICE_PORT);
		    	} catch (Exception e) {
		    		LOGGER.log(Level.WARNING, "Connection Error detected", e);
		    		Platform.runLater(new Runnable() {
		    			@Override public void run() {
				    		progress_label.setTextFill(Color.RED);
				    		progress_label.setText("Error connecting to host. Please try again.");
				    		
				    		
				    		Alert alert = new Alert(AlertType.ERROR);
							alert.setTitle("Fatal Error");
							alert.setHeaderText("ConnectionError encountered.");
							alert.setContentText(
									"Could not access the server. Check the logs and contact the host if this problem persists.");
							alert.showAndWait();
							
							
							btn_execute.setDisable(false);
		    			}
		    		});
					return;
		    	} finally {
		    		Runtime.getRuntime().addShutdownHook(new Thread() {
		    			@Override public void run() {
		    				if(instance != null && instance.isOpen()) {
		    					LOGGER.log(Level.FINE, "Channel closed by shutdownhook.");
		    					instance.close();
		    				}
		    			}
		    		});
		    	}
		    	
		    	//connection is succesful!
		    	LOGGER.log(Level.INFO, "Connection made with Service " + Constants.SERVICE_IP + ":" + Constants.SERVICE_PORT + " on " + target + "@" + Constants.GATEWAY_PORT);
		    	LOGGER.log(Level.INFO, "Service will now be accessible on local port " + Constants.LOCAL_PORT);
		    	
		    	Platform.runLater(new Runnable() {
		    		@Override public void run() {
				    	progress_label.setTextFill(Color.GREEN);
				    	progress_label.setText("Connection now available at localhost:" + Constants.LOCAL_PORT);
				    	
				    	//successful exit: connection is made, prepare the disconnect button
				    	btn_execute.setVisible(false);
				    	btn_disconnect.setDisable(false);
				    	btn_disconnect.setVisible(true);
				    	
				    	LOGGER.exiting(this.getClass().getName(), "connect, successful");
		    		}
		    	});
    		}
    	}.start();
    	
    	LOGGER.log(Level.FINER, "");
    }
    
    @FXML
    void disconnect(ActionEvent event) {
    	if(btn_disconnect.isDisabled()) {
    		return;
    	}
    	btn_disconnect.setDisable(true);
    	LOGGER.entering(this.getClass().getName(), "disconnect");
    	
    	progress_label.setTextFill(Color.BLACK);
    	progress_label.setText("disconnected from remote service. Thanks for playing!");
    	ip_addr_label.setText("");
    	usn_label.setText("");
    	ssh_auth_label.setText("");
    	
    	this.instance.close();
    	
    	//successful exit: connection interrupted, prepare connect button
    	btn_disconnect.setVisible(false);
    	btn_execute.setVisible(true);
    	btn_execute.setDisable(false);
    	LOGGER.exiting(this.getClass().getName(), "disconnect");
    }

	public Button getBtn_execute() {
		return btn_execute;
	}

	public Button getBtn_reconnect() {
		return btn_reconnect;
	}

	public Label getProgress_label() {
		return progress_label;
	}
    
    public void setPublicIP(String ip) {
    	this.public_ip_label.setText(ip);
    }
    
    private boolean validateInputs(String target, String usn, String sshAuth) {
    	//validate target ip
    	String[] testArray = target.split("\\.");
    	boolean isCorrectIP = true;
    	if(testArray.length != 4) {
    		isCorrectIP = false;
    	} else {
        	for(String s : testArray) {
    			if(s.equals("")) {
    				isCorrectIP = false;
    			}
    		}
    	}
    	if(!isCorrectIP) {
    		progress_label.setTextFill(Color.RED);
			progress_label.setText("Please Fill out the correct IPv4 address and run the script again.");
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Validation Error");
			alert.setHeaderText("Please enter a valid IPv4 address");
			alert.setContentText("A valid IPv4 address is of the form contains 4 fields separated by '.', none can be empty. eg.: 192.168.0.1");
			alert.showAndWait();
			LOGGER.log(Level.INFO, "Non-complete IP-Address detected, rejecting inputs...");
			return false;
    	}
    	
    	
    	//validate usn && sshAuth: only allow whitelisted characters + blacklist "sudo" and "null"
    	List<String> toValidate = new ArrayList<>();
    	toValidate.add(usn);
    	toValidate.add(sshAuth);
    	
    	for(String item : toValidate) {
    		if(item.isEmpty() | item.toLowerCase().matches(Constants.BLACKLIST_REGEX)) {
    			progress_label.setTextFill(Color.RED);
    			progress_label.setText("Please Fill out the form correctly and run the script again.");
    			Alert alert = new Alert(AlertType.WARNING);
    			alert.setTitle("Validation Error");
    			alert.setHeaderText("Please fill out the form correctly.");
    			alert.setContentText("Please make sure you are not leaving empty fields and remove special characters (eg.: no ~, /, \\, @,...)");
    			alert.showAndWait();
    			LOGGER.log(Level.INFO, "Potential malicious intent detected, rejecting inputs...");
    			return false;
    		}
    	};
    	   	
    	LOGGER.log(Level.INFO, "Inputs successfully validated for " + usn + "@" + target);
    	progress_label.setTextFill(Color.BLACK);
    	progress_label.setText("Connecting to remote server...");
    	return true;
    }
}
