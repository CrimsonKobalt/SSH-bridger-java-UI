package networking;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import com.jcraft.jsch.*;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/**
 * @author Beheerder
 *
 */
public class SSHManager {
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private JSch jschSSHChannel;
	private Session session;
	private int timeout;
	
	private String connectionIP;
	private int connectionPort;
	
	private RemoteUserInfo rui;
	
	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName, int connectionPort, int timeout) throws JSchException {
		LOGGER.entering(this.getClass().getName(), "constructor");
		jschSSHChannel = new JSch();
		
		jschSSHChannel.setKnownHosts(knownHostsFileName);

		this.rui = new RemoteUserInfo(userName, password);
		this.connectionIP = connectionIP;
		this.connectionPort = connectionPort;
		this.timeout = timeout;
		
		LOGGER.exiting(this.getClass().getName(), "constuctor");
	}
	
	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName, int connectionPort) throws JSchException {
		this(userName, password, connectionIP, knownHostsFileName, connectionPort, 60000);
	}
	
	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName) throws JSchException {
		this(userName, password, connectionIP, knownHostsFileName, 22, 60000);
	}
	
	public void connect() throws JSchException {
		LOGGER.entering(this.getClass().getName(), "connect");
		
		this.session = jschSSHChannel.getSession(this.rui.getUser(), this.connectionIP, this.connectionPort);
		this.session.setPassword(this.rui.getPassword());
		
		//for testing only
		//this.session.setConfig("StrictHostKeyChecking", "no");
		this.session.setUserInfo(rui);
		this.session.connect(this.timeout);
		LOGGER.exiting(this.getClass().getName(), "connect");
	}
	
	/**
	 * (port_to_bind_locally, service_ip_relative_to_ssh_target, service_port_remote)
	 * @param local_port::"port to bind on local machine"
	 * @param service_ip
	 * @param service_port
	 */
	public void forwardPort(int local_port, String service_ip, int service_port) throws JSchException {
		LOGGER.entering(this.getClass().getName(), "createtunnel");
		this.session.setPortForwardingL(local_port, service_ip, service_port);
		LOGGER.exiting(this.getClass().getName(), "createtunnel");
	}
	
	/**
	 * Send a single command over ssh & return the normal output
	 * @param String
	 * @return
	 */
	public String sendCommand(String command) throws Exception {
		LOGGER.entering(this.getClass().getName(), "sendCommand");
		StringBuilder sb = new StringBuilder();
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		InputStream commandOutput = channel.getInputStream();
		channel.connect();
		int readByte = commandOutput.read();
		while (readByte != 0xffffffff) {
			sb.append((char) readByte);
			readByte = commandOutput.read();
		}
		channel.disconnect();
		LOGGER.exiting(this.getClass().getName(), "sendCommand");
		return sb.toString();
	}
	
	public boolean isOpen() {
		return this.session.isConnected();
	}
	
	public void close() {
		this.session.disconnect();
	}
}

class RemoteUserInfo implements UserInfo {
	String usn;
	String password;
	
	private Boolean trust = false;
	private Boolean prompt = false;
	
	public RemoteUserInfo(String usn, String passwd) {
		this.usn = usn;
		this.password = passwd;
	}

	@Override
	public boolean promptPassword(String arg0) {
		return true;
	}

	@Override
	public String getPassword() {
		return password;
	}
	
	public String getUser() {
		return this.usn;
	}

	@Override
	public boolean promptPassphrase(String arg0) {
		return false;
	}
	
	@Override
	public String getPassphrase() {
		return null;
	}
	
	@Override
	public boolean promptYesNo(String arg0) {
		final CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				System.out.println("locked on response -- runlater-thread");
				Alert alert = new Alert(AlertType.WARNING, "First time connecting to this host. Are you sure?",
						ButtonType.OK, ButtonType.CANCEL);
				alert.setTitle("New host detected!");
				Optional<ButtonType> result = alert.showAndWait();

				if (result.get() == ButtonType.OK) {
					prompt = true;
					latch.countDown();
				} else {
					prompt = false;
					latch.countDown();
				}
			}
		});
		
		try {
			latch.await();
		} catch (InterruptedException ie) {
			Platform.exit();
		}
		
		return prompt;
	}

	@Override
	public void showMessage(String arg0) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Server response:");
		alert.setContentText(arg0);
		alert.showAndWait();
	}
	
}