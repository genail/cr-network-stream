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
package pl.graniec.coralreef.network.client.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import pl.graniec.coralreef.network.PacketListener;
import pl.graniec.coralreef.network.client.Client;
import pl.graniec.coralreef.network.client.ConnectionListener;
import pl.graniec.coralreef.network.exceptions.NetworkException;

/**
 * @author Piotr Korzuszek <piotr.korzuszek@gmail.com>
 *
 */
public class StreamClient implements Client {

	/** Client socket */
	private Socket socket;
	
	/** Output stream */
	private ObjectOutputStream oos;
	/** Input stream */
	private ObjectInputStream ois;
	
	/** Packet listeners */
	private final Set<PacketListener> packetListeners = new HashSet<PacketListener>();
	/** Connection listeners */
	private final Set<ConnectionListener> connectionListeners = new HashSet<ConnectionListener>();
	
	/*
	 * @see pl.graniec.coralreef.network.client.Client#addConnectionListener(pl.graniec.coralreef.network.client.ConnectionListener)
	 */
	@Override
	public boolean addConnectionListener(ConnectionListener l) {
		synchronized (connectionListeners) {
			return connectionListeners.add(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#addPacketListener(pl.graniec.coralreef.network.PacketListener)
	 */
	@Override
	public boolean addPacketListener(PacketListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("cannot take null values");
		}
		
		synchronized (packetListeners) {
			return packetListeners.add(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#connect(java.lang.String, int)
	 */
	@Override
	public void connect(String host, int port) throws NetworkException {
		if (host == null) {
			throw new IllegalArgumentException("parameters cannot be null");
		}
		
		if (isConnected()) {
			throw new IllegalStateException("client is already connected");
		}
		
		try {
			
			socket = new Socket(host, port);
			
			// create streams
			final OutputStream os = socket.getOutputStream();
			oos = new ObjectOutputStream(os);
			
			final InputStream is = socket.getInputStream();
			ois = new ObjectInputStream(is);
			
		} catch (UnknownHostException e) {
			throw new NetworkException(e);
		} catch (IOException e) {
			throw new NetworkException(e);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#disconnect()
	 */
	@Override
	public void disconnect() {
		if (!isConnected()) {
			throw new IllegalStateException("client is not connected");
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			// ignore this exception
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return socket.isConnected();
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#removeConnectionListener(pl.graniec.coralreef.network.client.ConnectionListener)
	 */
	@Override
	public boolean removeConnectionListener(ConnectionListener l) {
		synchronized (connectionListeners) {
			return connectionListeners.remove(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#removePacketListener(pl.graniec.coralreef.network.PacketListener)
	 */
	@Override
	public boolean removePacketListener(PacketListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("parameters cannot be null");
		}
		
		synchronized (packetListeners) {
			return packetListeners.remove(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.client.Client#send(java.lang.Object)
	 */
	@Override
	public void send(Object data) throws NotSerializableException, NetworkException {
		if (!isConnected()) {
			throw new IllegalStateException("not connected");
		}
		
		try {
			oos.writeObject(data);
		} catch (IOException e) {
			// probably disconnected
			if (!isConnected()) {
				// FIXME: notify disconnection
			} else {
				e.printStackTrace();
			}
		}
	}

}
