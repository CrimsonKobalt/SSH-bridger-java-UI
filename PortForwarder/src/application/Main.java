package application;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

import configurations.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static FileHandler logfile;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("Launcher.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setResizable(false);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");  
		LocalDateTime now = LocalDateTime.now();
		
		File appfile = new File(Constants.HOME_DIR + "/.arkserver");
		if(!appfile.isDirectory()) {
			appfile.mkdir();
		}
		
		File file = new File(Constants.HOME_DIR + "/.arkserver/logs");
		if(!file.isDirectory()) {
			file.mkdir();
		}
		
		try {
			logfile = new FileHandler(Constants.HOME_DIR + "/.arkserver/logs/logs_" + dtf.format(now) +".txt");
			logfile.setLevel(Level.ALL);
			logfile.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(logfile);
			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.ALL);
			LOGGER.addHandler(ch);
			LOGGER.setLevel(Level.ALL);
			LOGGER.log(Level.FINE, "Logger initiated.");
		} catch (SecurityException e) {
			e.printStackTrace();
			System.out.println("SecurityViolation detected. Program shutting down...");
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException caught. Program shutting down...");
			System.exit(-1);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override public void run() {
				if(logfile != null) {
					LOGGER.log(Level.FINE, "Program shut down. Closing resources...");
					logfile.close();
				}
			}
		});
		
		cleanLogs();
		launch(args);
	}
	
	public static void cleanLogs() {
		LOGGER.entering(Class.class.getName(), "cleanLogs");
		new Thread() {
			@Override public void run() {
				File file = new File(Constants.HOME_DIR + "/.arkserver/logs");
				
				List<String> files = Arrays.asList(file.list());
				if(files.size() > 30) {
					Comparator<String> reverseSorter = Collections.reverseOrder();
					Collections.sort(files, reverseSorter);
					for(int i=files.size()-1; i>0; i--) {
						if(i>=25) {
							File file_i = new File(files.get(i));
							file_i.delete();
						}
					}
					LOGGER.exiting(this.getClass().getName(), "cleanLogs, logs cleaned.");
					return;
				}
				LOGGER.exiting(this.getClass().getName(), "cleanLogs, no logs cleaned.");
			}
		}.start();
	}
}
