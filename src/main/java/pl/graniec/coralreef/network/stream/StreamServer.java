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
package pl.graniec.coralreef.network.stream;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import pl.graniec.coralreef.network.exceptions.NetworkException;
import pl.graniec.coralreef.network.server.ConnectionListener;
import pl.graniec.coralreef.network.server.RemoteClient;
import pl.graniec.coralreef.network.server.Server;

/**
 * Server that uses TCP stream sockets to transfer packets between client
 * and server.
 * 
 * @author Piotr Korzuszek <piotr.korzuszek@gmail.com>
 *
 */
public class StreamServer implements Server {

	private class Listener extends Thread {
		/*
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			
			ServerSocket socket;
			
			while (!isInterrupted()) {
				socket = StreamServer.this.socket;
				
				if (socket == null) {
					return;
				}
				
				try {
					
					final Socket remoteSocket = socket.accept();
					
					
					// this below is all synchronized because remote client can report disconnection
					// before library user can be notified about client connection. Synchronization will
					// guarantee that connection will be notified first, and disconnection later
					synchronized (remoteClients) {
						
						// create new remote client
						final StreamRemoteClient remoteClient = new StreamRemoteClient(StreamServer.this, remoteSocket);
						
						remoteClients.add(remoteClient);
						
						// and report about it
						notifyClientConnected(remoteClient);
						
					}
					
					
				} catch (SocketTimeoutException e) {
					// timeout is expected one
					
				} catch (IOException e) {
					// disconnection occurs
					try {
						if (socket.isBound()) {
							socket.close();
							socket = null;
						}
					} catch (IOException e1) {
					}
				}
			}
		}
	}

	private static final int SO_TIMEOUT = 100;
	
	/** Stream socket */
	private ServerSocket socket;
	/** Remote clients */
	final List<StreamRemoteClient> remoteClients = new LinkedList<StreamRemoteClient>();
	/** New connections listener */
	private Listener acceptListener;
	
	/** Connection listeners */
	private final List<ConnectionListener> connectionListeners = new LinkedList<ConnectionListener>();
	
	/*
	 * @see pl.graniec.coralreef.network.server.Server#addConnectionListener(pl.graniec.coralreef.network.server.ConnectionListener)
	 */
	@Override
	public boolean addConnectionListener(ConnectionListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("given object cannot be null");
		}
		
		synchronized (connectionListeners) {
			return connectionListeners.add(l);
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.Server#close()
	 */
	@Override
	public void close() {
		if (!isOpen()) {
			throw new IllegalStateException("server is not open");
		}
		
		try {
			socket.close();
			socket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// stop the accept listener
		try {
			acceptListener.interrupt();
			acceptListener.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.Server#getPort()
	 */
	@Override
	public int getPort() {
		// this.socket object can change because of threads
		final ServerSocket socket = this.socket;
		
		if (socket != null && socket.isBound()) {
			return socket.getLocalPort();
		} else {
			return 0;
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.Server#isOpen()
	 */
	@Override
	public boolean isOpen() {
		// this.socket object can change because of threads
		final ServerSocket socket = this.socket;
		return socket != null && socket.isBound();
	}

	/*
	 * @see pl.graniec.coralreef.network.server.Server#open(int)
	 */
	@Override
	public void open(int port) throws NetworkException {
		try {
			
			socket = new ServerSocket(port);
			
			// configure socket
			socket.setSoTimeout(SO_TIMEOUT);
			
			// run accept listener
			acceptListener = new Listener();
			acceptListener.start();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			
		} catch (SecurityException e) {
			throw new pl.graniec.coralreef.network.exceptions.SecurityException("not allowed to open server on port " + port);
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if (!socket.isBound()) {
				socket = null;
			}
		}
	}

	/*
	 * @see pl.graniec.coralreef.network.server.Server#removeConnectionListener(pl.graniec.coralreef.network.server.ConnectionListener)
	 */
	@Override
	public boolean removeConnectionListener(ConnectionListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("given object cannot be null");
		}
		
		synchronized (connectionListeners) {
			return connectionListeners.remove(l);
		}
	}
	
	private void notifyClientConnected(RemoteClient client) {
		ConnectionListener[] copy;
		
		synchronized (connectionListeners) {
			copy = connectionListeners.toArray(new ConnectionListener[connectionListeners.size()]);
		}
		
		for (ConnectionListener c : copy) {
			c.clientConnected(client);
		}
	}


}
