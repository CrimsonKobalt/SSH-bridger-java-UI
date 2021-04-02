module PortForwarder {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires java.logging;
	requires javafx.base;
	requires jsch;
	requires java.prefs;
	
	opens application to javafx.graphics, javafx.fxml;
}
