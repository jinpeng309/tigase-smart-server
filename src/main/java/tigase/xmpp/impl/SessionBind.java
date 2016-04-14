/*
 * SessionBind.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Describe class SessionBind here.
 *
 *
 * Created: Mon Feb 20 22:43:59 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionBind
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	private static final String     SESSION_KEY = "Session-Set";
	private static final String     XMLNS       = "urn:ietf:params:xml:ns:xmpp-session";
	private static final Logger     log = Logger.getLogger(SessionBind.class.getName());
	private static final String     ID          = XMLNS;
	private static final String[][] ELEMENTS    = {
		{ Iq.ELEM_NAME, "session" }
	};
	private static final String[]   XMLNSS      = { XMLNS };
	private static final Element[]  FEATURES = { new Element("session", new String[] {
			"xmlns" }, new String[] { XMLNS }) };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMLNS }) };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws XMPPException
	 */
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			results.offer(session.getAuthState().getResponseMessage(packet,
					"Session is not yet authorized.", false));

			return;
		}    // end of if (!session.isAuthorized())

		// Element request = packet.getElement();
		StanzaType type = packet.getType();

		switch (type) {
		case set :
			session.putSessionData(SESSION_KEY, "true");
			results.offer(packet.okResult((String) null, 0));

			break;

		default :
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"Session type is incorrect", false));

			break;
		}    // end of switch (type)
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session != null) && (session.getSessionData(SESSION_KEY) == null) && session
				.isAuthorized()) {
			return FEATURES;
		} else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}
}    // SessionBind


//~ Formatted in Tigase Code Convention on 13/03/12
