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
package pl.graniec.coralreef.network.server.stream;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.graniec.coralreef.network.DisconnectReason;
import pl.graniec.coralreef.network.exceptions.NetworkException;
import pl.graniec.coralreef.network.server.ConnectionListener;
import pl.graniec.coralreef.network.server.RemoteClient;
import pl.graniec.coralreef.network.server.Server;

/**
 * @author Piotr Korzuszek <piotr.korzuszek@gmail.com>
 *
 */
public class StreamRemoteClientTest {

	final Mockery context = new JUnit4Mockery();
	
	final Server server = new StreamServer();
	Socket client;
	RemoteClient remoteClient;
	
	@Before
	public void setUp() throws Exception {
		server.open(0);
		
		server.addConnectionListener(new ConnectionListener() {

			@Override
			public void clientConnected(RemoteClient client) {
				remoteClient = client;
			}

			@Override
			public void clientDisconnected(RemoteClient client,
					DisconnectReason reason, String reasonString) {
				
			}
			
		});
		
		client = new Socket("localhost", server.getPort());
		
		Thread.sleep(50);
	}

	@After
	public void tearDown() throws Exception {
		if (server.isOpen()) {
			server.close();
		}
	}

	/**
	 * Test method for {@link pl.graniec.coralreef.network.server.stream.StreamRemoteClient#disconnect()}.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testDisconnect() throws IOException, InterruptedException {
		final ConnectionListener serverConnectionListener = context.mock(ConnectionListener.class);
		
		context.checking(new Expectations(){{
			oneOf(serverConnectionListener).clientDisconnected(with(remoteClient), with(DisconnectReason.Reset), with(any(String.class)));
		}});
		
		server.addConnectionListener(serverConnectionListener);
		
		client.close();
		
		Thread.sleep(50);
		
		context.assertIsSatisfied();
	}

	/**
	 * Test method for {@link pl.graniec.coralreef.network.server.stream.StreamRemoteClient#send(java.lang.Object)}.
	 * @throws IOException 
	 * @throws NetworkException 
	 * @throws ClassNotFoundException 
	 */
	@Test
	public void testSend() throws IOException, NetworkException, ClassNotFoundException {
		final String data = "test string";
		
		final InputStream is = client.getInputStream();
		final ObjectInputStream ois = new ObjectInputStream(is);
		
		remoteClient.send(data);
		
		Object received = ois.readObject();
		
		assertEquals(data, received);
		
	}

}
