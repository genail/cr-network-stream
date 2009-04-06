/**
 * Copyright (c) 2009, Coral Reef Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of the Coral Reef Project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pl.graniec.coralreef.network.stream.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

import pl.graniec.coralreef.network.DisconnectReason;
import pl.graniec.coralreef.network.PacketListener;
import pl.graniec.coralreef.network.server.RemoteClient;

/**
 * @author Piotr Korzuszek <piotr.korzuszek@gmail.com>
 *
 */
public class StreamRemoteClient implements RemoteClient {

	private class Listener extends Thread {

		/*
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			
			Object object;
			
			while (!isInterrupted()) {
				
				try {
					
					if (ois == null) {
						// get input
						final InputStream is = socket.getInputStream();
						// just now because constructor of object input stream waits
						// for header from other side
						ois = new ObjectInputStream(is);
					}
					
					object = ois.readObject();
					notifyPacketReceived(object);
					
				} catch (SocketTimeoutException e) {
					// thats fine
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// this probably means a disconnection
					notifyClientDisconnected(reason, e.getMessage());
					ois = null;
					
					break;
				}
				
			}
		}
	}
	
	private static final int SO_TIMEOUT = 100;

	/** Parent Server */
	private final StreamServer parent;
	/** Socket of this client */
	private final Socket socket;
	
	/** Output */
	ObjectOutputStream oos;
	/** Input */
	ObjectInputStream ois;
	/** The listener */
	private final Listener listener = new Listener();
	
	/** Disconnection reason if should be notified */
	private int reason = DisconnectReason.Reset;
	
	/** Packet listeners */
	private final Set<PacketListener> packetListeners = new HashSet<PacketListener>();

	public StreamRemoteClient(StreamServer parent, Socket socket) throws IOException {
		this.parent = parent;
		this.socket = socket;
		
		// socket configuration
		socket.setSoTimeout(SO_TIMEOUT);
		
		// output
		final OutputStream os = socket.getOutputStream();
		oos = new ObjectOutputStream(os);
		
		// run the listener thread
		listener.start();
	}
	
	/*
	 * @see pl.graniec.coralreef.network.server.RemoteClient#addPacketListener(pl.graniec.coralreef.network.PacketListener)
	 */
	@Override
	public boolean addPacketListener(PacketListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("given object cannot be null");
		}
		
		synchronized (packetListeners) {
			return packetListeners.add(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.RemoteClient#disconnect()
	 */
	@Override
	public void disconnect() {

		if (!isConnected()) {
			throw new IllegalStateException("client not connected");
		}
		
		try {
			reason = DisconnectReason.UserAction;
			socket.close();
		} catch (IOException e) {
			// ignore the socket closing exception
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.RemoteClient#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return socket.isConnected();
	}

	private void notifyClientDisconnected(int reason, String reasonString) {
		
		// this synchronization is because the disconnection can be reported
		// earlier that client connection. This prevents that situation.
		synchronized (parent.remoteClients) {
			parent.notifyClientDisconnected(this, reason, reasonString);
		}
	}

	private void notifyPacketReceived(Object data) {
		PacketListener[] copy;
		
		synchronized (packetListeners) {
			copy = packetListeners.toArray(new PacketListener[packetListeners.size()]);
		}
		
		for (PacketListener l : copy) {
			l.packetReceiver(data);
		}
	}
	
	/*
	 * @see pl.graniec.coralreef.network.server.RemoteClient#removePacketListener(pl.graniec.coralreef.network.PacketListener)
	 */
	@Override
	public boolean removePacketListener(PacketListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("given object cannot be null");
		}
		
		synchronized (packetListeners) {
			return packetListeners.remove(l);
		}
	}
	
	/*
	 * @see pl.graniec.coralreef.network.server.RemoteClient#send(java.lang.Object)
	 */
	@Override
	public void send(Object data) throws NotSerializableException {
		
		if (data == null) {
			throw new IllegalArgumentException("data cannot be null");
		}
		
		if (!isConnected()) {
			throw new IllegalStateException("client is not connected");
		}
		
		try {
			oos.writeObject(data);
			oos.flush();
		} catch (InvalidClassException e) {
			// this is exception that user should know about
			e.printStackTrace();
		} catch (NotSerializableException e) {
			throw e;
		} catch (IOException e) {
			// this probably means the disconnection
			if (!isConnected()) {
				notifyClientDisconnected(reason, e.getMessage());
			} else {
				// if still connected then we have worst problem
				// let the user know about it
				e.printStackTrace();
			}
		}
	}

}
