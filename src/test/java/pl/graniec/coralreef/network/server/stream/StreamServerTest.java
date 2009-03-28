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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
public class StreamServerTest {

	final Server server = new StreamServer();
	final Mockery context = new JUnit4Mockery();
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (server.isOpen()) {
			server.close();
		}
	}

	/**
	 * Test method for {@link pl.graniec.coralreef.network.server.stream.StreamServer#addConnectionListener(pl.graniec.coralreef.network.server.ConnectionListener)}.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws NetworkException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testAddConnectionListener() throws UnknownHostException, IOException, NetworkException, InterruptedException {
		
		final ConnectionListener connectionListener = context.mock(ConnectionListener.class);
		
		context.checking(new Expectations(){{
			oneOf(connectionListener).clientConnected(with(any(RemoteClient.class)));
		}});
		
		server.open(0);
		server.addConnectionListener(connectionListener);
		
		final Socket client = new Socket("localhost", server.getPort());
		
		Thread.sleep(50);
		
		context.assertIsSatisfied();
		
		context.checking(new Expectations(){{
			oneOf(connectionListener).clientDisconnected(with(any(RemoteClient.class)), with(any(DisconnectReason.class)), with(any(String.class)));
		}});
		
		client.close();
		
		Thread.sleep(50);
		
		context.assertIsSatisfied();
	}

	/**
	 * Test method for {@link pl.graniec.coralreef.network.server.stream.StreamServer#open(int)}.
	 * @throws NetworkException 
	 */
	@Test
	public void testOpen() throws NetworkException {
		assertFalse(server.isOpen());
		assertEquals(0, server.getPort());
		
		server.open(0);
		
		assertTrue(server.isOpen());
		assertTrue(server.getPort() != 0);
		
		server.close();
		
		assertFalse(server.isOpen());
		assertEquals(0, server.getPort());
	}

}
