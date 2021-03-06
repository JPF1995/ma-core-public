/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig.SerialPortTypes;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.io.serial.CommPortConfigException;
import com.serotonin.m2m2.Common;

import jssc.SerialNativeInterface;
import jssc.SerialPortList;


/**
 * @author Terry Packer
 *
 */
public class SerialPortManager {

	private final Log LOG = LogFactory.getLog(SerialPortManager.class);

	public static final String VIRTUAL_SERIAL_PORT_KEY = "comm.virtual.serial";
	
	private final ReadWriteLock lock;
    private final List<SerialPortIdentifier> freePorts;
    private final List<SerialPortIdentifier> ownedPorts;
    private boolean initialized;
    
    
    public SerialPortManager(){
    	lock = new ReentrantReadWriteLock();
    	freePorts = new CopyOnWriteArrayList<SerialPortIdentifier>();
    	ownedPorts = new CopyOnWriteArrayList<SerialPortIdentifier>();
    	initialized = false;
    }
    
    
    
    /**
     * Get a list of all free Comm Ports
     * @return
     * @throws CommPortConfigException
     */
    public List<SerialPortIdentifier> getFreeCommPorts() throws SerialPortConfigException {
    	if(!initialized){
	    	this.lock.writeLock().lock();
	    	try{
	    		initialize(false);
	    	}finally{
	    		this.lock.writeLock().unlock();
	    	}
    	}
    	return freePorts;
    }

    /**
     * Get a list of all free Comm Ports
     * @return
     * @throws CommPortConfigException
     */
    public List<SerialPortIdentifier> getAllCommPorts() throws SerialPortConfigException {
    	if(!initialized){
    		this.lock.writeLock().lock();
        	try{
        		initialize(false);
        	}finally{
        		this.lock.writeLock().unlock();
        	}
    	}
    	List<SerialPortIdentifier> allPorts = new ArrayList<SerialPortIdentifier>(freePorts);
    	allPorts.addAll(ownedPorts);
    	return allPorts;
    }
    
    
    /**
     * Refresh the list of available Comm Ports
     * @return
     * @throws CommPortConfigException
     */
    public void refreshFreeCommPorts() throws SerialPortConfigException {
    	this.lock.writeLock().lock();
    	try{
    		freePorts.clear();
    		initialize(false);
    	}finally{
    		this.lock.writeLock().unlock();
    	}
    }   

    /**
     * 
     * @param safe
     * @throws SerialPortConfigException
     */
    public void initialize(boolean safe) throws SerialPortConfigException {
 
    	if(safe)
    		return;
    	
    	try {
            String[] portNames;
            
            switch(SerialNativeInterface.getOsType()){
            	case SerialNativeInterface.OS_LINUX:
            		portNames = SerialPortList.getPortNames(Common.envProps.getString("serial.port.linux.path","/dev/"),
            				Pattern.compile(Common.envProps.getString("serial.port.linux.regex", "((cu|ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO|COM)[0-9]{1,3}|rs(232|485)-[0-9])")));
                break;
            	case SerialNativeInterface.OS_MAC_OS_X:
                    portNames = SerialPortList.getPortNames(Common.envProps.getString("serial.port.osx.path","/dev/"),
                    		Pattern.compile(Common.envProps.getString("serial.port.osx.regex","(cu|tty)..*"))); //Was "tty.(serial|usbserial|usbmodem).*")
                break;
            	case SerialNativeInterface.OS_WINDOWS:
                    portNames = SerialPortList.getPortNames(Common.envProps.getString("serial.port.windows.path",""),
                    		Pattern.compile(Common.envProps.getString("serial.port.windows.regex","")));
            	default:
                	 portNames = SerialPortList.getPortNames();
                break;
            }
            
            for (String portName : portNames) {
                freePorts.add(new SerialPortIdentifier(portName, SerialPortTypes.JSSC));
            }
            
            //Collect any Virtual Comm Ports from the DB and load them in
			List<VirtualSerialPortConfig> list = VirtualSerialPortConfigDao.instance.getAll();
            if (list != null){
            	for(VirtualSerialPortConfig config : list){
            		freePorts.add(new VirtualSerialPortIdentifier(config));
            	}
            }
            initialized = true;
        }
        catch (UnsatisfiedLinkError e) {
            throw new SerialPortConfigException(e.getMessage());
        }
        catch (NoClassDefFoundError e) {
            throw new SerialPortConfigException(
                    "Comm configuration error. Check that your serial port DLL or libraries have been correctly installed. " + e.getMessage() );
        }

    }

	/**
	 * Is a port currently in use by Mango?
	 * 
	 * @param commPortId
	 * @return
	 */
	public boolean portOwned(String commPortId) {

		//Check to see if the port is currently in use.
		for(SerialPortIdentifier id : ownedPorts){
			if(id.getName().equalsIgnoreCase(commPortId)){
				return true;
			}
		}
		
		return false;
	}
	
	public String getPortOwner(String commPortId) {
		
		//Get serial port owner
		for(SerialPortIdentifier id : ownedPorts){
			if(id.getName().equalsIgnoreCase(commPortId))
				return id.getCurrentOwner();
		}
		
		return null;
	}

	public boolean isPortNameRegexMatch(String portName) {
		switch(SerialNativeInterface.getOsType()){
    	case SerialNativeInterface.OS_LINUX:
    		return Pattern.matches(Common.envProps.getString("serial.port.linux.regex", "((cu|ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO|COM)[0-9]{1,3}|rs(232|485)-[0-9])"), portName);
    	case SerialNativeInterface.OS_MAC_OS_X:
            return Pattern.matches(Common.envProps.getString("serial.port.osx.regex","(cu|tty)..*"), portName); //Was "tty.(serial|usbserial|usbmodem).*")
    	case SerialNativeInterface.OS_WINDOWS:
            return Pattern.matches(Common.envProps.getString("serial.port.windows.regex",""), portName);
    	default:
        	 return false;
		}
	}

	/**
	 * @param string
	 * @param commPortId
	 * @param baudRate
	 * @param flowControlIn
	 * @param flowControlOut
	 * @param dataBits
	 * @param stopBits
	 * @param parity
	 * @param parity2
	 * @return
	 */
	public SerialPortProxy open (
			String ownerName, String commPortId, int baudRate, int flowControlIn,
			int flowControlOut, int dataBits, int stopBits, int parity) throws SerialPortException {

		this.lock.writeLock().lock();
		try{
			if(!initialized)
				initialize(false);

			//Check to see if the port is currently in use.
			for(SerialPortIdentifier id : ownedPorts){
				if(id.getName().equalsIgnoreCase(commPortId)){
					throw new SerialPortException("Port " + commPortId + " in use by " + id.getCurrentOwner());
				}
			}
			
			//Get the Free port
			SerialPortIdentifier portId = null;
			for(SerialPortIdentifier id : freePorts){
				if(id.getName().equalsIgnoreCase(commPortId)){
					portId = id;
					break;
				}
			}
			
			//Did we find it?
			if(portId == null){
				throw new SerialPortException("Port " + commPortId + " does not exist.");
			}
		
			//Try to open the port
			SerialPortProxy port;
			switch(portId.getType()){
			case SerialPortTypes.JSSC:
				port = new JsscSerialPortProxy(portId, baudRate, flowControlIn, flowControlOut, dataBits, stopBits, parity);
				break;
			case SerialPortTypes.SERIAL_SOCKET_BRIDGE:
			case SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE:
				VirtualSerialPortIdentifier bridgeId = (VirtualSerialPortIdentifier)portId;
				port = bridgeId.getProxy();
				break;
			default:
				throw new SerialPortException("Uknown port type " + portId.getType());
			}
					
			port.open();
			//Port is open move to owned list
			freePorts.remove(portId);
			portId.setCurrentOwner(ownerName);
			portId.setPort(port);
			ownedPorts.add(portId);
			
			return port;
		}catch (Exception e) {
            // Wrap all exceptions
            if (e instanceof SerialPortException)
                throw (SerialPortException) e;
            throw new SerialPortException(e);
        }finally{
			lock.writeLock().unlock();
		}
	}


	//TODO Run a periodic task to check when the port was last accessed and if past some time then assume it is not owned anymore?

	/**
	 * @param port
	 */
	public void close(SerialPortProxy port) throws SerialPortException {
		
		lock.writeLock().lock();
		
		try{
			if(!initialized)
				initialize(false);

			//Close the port
			if(port == null)
				return; //Can't close a non existent port
			
			port.close();

			SerialPortIdentifier id = port.getSerialPortIdentifier();
			if(this.ownedPorts.remove(id)){
				id.setCurrentOwner("");
				id.setPort(null);
				this.freePorts.add(id);
			}else{
				//It must have already been free?
				if(!this.freePorts.contains(id)){
					throw new SerialPortException("Port " + id.getName() + " was already free?");
				}
			}
			
		}catch(Exception e){
            // Wrap all exceptions
            if (e instanceof SerialPortException)
                throw (SerialPortException) e;
            throw new SerialPortException(e);
		}finally{
			lock.writeLock().unlock();
		}
		
	}

	/**
	 * Ensure all ports are closed
	 */
	public void terminate() throws Exception{
		
		try{
			lock.writeLock().lock();

			//Close all the ports if they are open
			for(SerialPortIdentifier id : ownedPorts){
				try{
					id.getPort().close();
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
				}
			}
			
			ownedPorts.clear();
		}catch(Exception e){
			throw e;
		}finally{
			lock.writeLock().unlock();
		}
		
	}
	
}
