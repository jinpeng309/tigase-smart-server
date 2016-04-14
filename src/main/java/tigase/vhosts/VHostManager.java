/*
 * VHostManager.java
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



package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.ComponentRepository;
import tigase.db.TigaseDBException;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.ServerComponent;

import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import javax.script.Bindings;

/**
 * Describe class VHostManager here.
 *
 *
 * Created: Fri Nov 21 14:28:20 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostManager
				extends AbstractComponentRegistrator<VHostListener>
				implements VHostManagerIfc, StatisticsContainer {
	/** Field description */
	public static final String VHOSTS_REPO_CLASS_PROP_KEY = "repository-class";

	/** Field description */
	public static final String VHOSTS_REPO_CLASS_PROP_VAL =
			"tigase.vhosts.VHostJDBCRepository";

	/** Field description */
	public static final String  VHOSTS_REPO_CLASS_PROPERTY = "--vhost-repo-class";
	private static final Logger log = Logger.getLogger(VHostManager.class.getName());

	//~--- fields ---------------------------------------------------------------

	private long getComponentsForLocalDomainCalls    = 0;
	private long getComponentsForNonLocalDomainCalls = 0;

	// private ServiceEntity serviceEntity = null;
	private String                       identity_type           = "generic";
	private long                         isAnonymousEnabledCalls = 0;
	private long                         isLocalDomainCalls      = 0;
	private LinkedHashSet<VHostListener> localDomainsHandlers =
			new LinkedHashSet<VHostListener>(10);
	private LinkedHashSet<VHostListener> nonLocalDomainsHandlers =
			new LinkedHashSet<VHostListener>(10);
	private LinkedHashSet<VHostListener> nameSubdomainsHandlers =
			new LinkedHashSet<VHostListener>(10);
	private ConcurrentSkipListSet<String> registeredComponentDomains =
			new ConcurrentSkipListSet<String>();
	protected ComponentRepository<VHostItem> repo = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>VHostManager</code> instance.
	 *
	 */
	public VHostManager() {}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	@Override
	public void addComponentDomain(String domain) {
		registeredComponentDomains.add(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentAdded(VHostListener component) {
		component.setVHostManager(this);
		if (component.handlesLocalDomains()) {
			localDomainsHandlers.add(component);
		}
		if (component.handlesNonLocalDomains()) {
			nonLocalDomainsHandlers.add(component);
		}
		if (component.handlesNameSubdomains()) {
			nameSubdomainsHandlers.add(component);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(VHostListener component) {
		localDomainsHandlers.remove(component);
		nonLocalDomainsHandlers.remove(component);
		nameSubdomainsHandlers.remove(component);
	}

	/**
	 * Initialize a mapping of key/value pairs which can be used in scripts
	 * loaded by the server
	 *
	 * @param binds A mapping of key/value pairs, all of whose keys are Strings.
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	@Override
	public void removeComponentDomain(String domain) {
		registeredComponentDomains.remove(domain);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>List<JID></code>
	 */
	@Override
	public List<JID> getAllVHosts() {
		List<JID> list = new ArrayList<JID>();

		try {
			for (VHostItem item : repo.allItems()) {
				list.add(item.getVhost());
			}
		} catch (TigaseDBException ex) {
			Logger.getLogger(VHostManager.class.getName()).log(Level.SEVERE, null, ex);
		}

		return list;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>ServerComponent[]</code>
	 */
	@Override
	public ServerComponent[] getComponentsForLocalDomain(String domain) {
		++getComponentsForLocalDomainCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {

			// This is not a local domain.
			// Maybe this is a 'name' subdomain: 'pubsub'.domain.name
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String        name       = domain.substring(0, idx);
				String        basedomain = domain.substring(idx + 1);
				VHostListener listener   = components.get(name);

				if ((listener != null) && listener.handlesNameSubdomains() && isLocalDomain(
						basedomain)) {
					return new ServerComponent[] { listener };
				}
			}

			return null;
		} else {

//    // First check whether the domain has special configuration
//    // specifying what components are for this domain:
//    String[] comps = vhost.getComps();
//    if (comps != null && comps.length > 0) {
//      !!
//    }
			// Return all components for local domains and components selected
			// for this specific domain
			LinkedHashSet<ServerComponent> results = new LinkedHashSet<ServerComponent>(10);

			// are there any components explicitly bound to this domain?
			String[] comps = vhost.getComps();

			if ((comps != null) && (comps.length > 0)) {
				for (String name : comps) {
					VHostListener listener = components.get(name);

					if (listener != null) {
						results.add(listener);
					}
				}
			}

			// if not, then add any generic handlers
			if (results.size() == 0) {
				results.addAll(localDomainsHandlers);
			}
			if (results.size() > 0) {
				return results.toArray(new ServerComponent[results.size()]);
			} else {
				return null;
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>ServerComponent[]</code>
	 */
	@Override
	public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		++getComponentsForNonLocalDomainCalls;

		// Return components for non-local domains
		if (nonLocalDomainsHandlers.size() > 0) {
			return nonLocalDomainsHandlers.toArray(
					new ServerComponent[nonLocalDomainsHandlers.size()]);
		} else {
			return null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 *
	 *
	 * @return a value of <code>Map<String,Object></code>
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs       = super.getDefaults(params);
		String              repo_class = (String) params.get(VHOSTS_REPO_CLASS_PROPERTY);

		if (repo_class == null) {
			repo_class = VHOSTS_REPO_CLASS_PROP_VAL;
		}
		defs.put(VHOSTS_REPO_CLASS_PROP_KEY, repo_class);
		try {
			ComponentRepository<VHostItem> repo_tmp = (ComponentRepository<VHostItem>) Class
					.forName(repo_class).newInstance();

			repo_tmp.getDefaults(defs, params);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate VHosts repository for class: " +
					repo_class, e);
		}

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>BareJID</code>
	 */
	@Override
	public BareJID getDefVHostItem() {
		Iterator<VHostItem> vhosts = repo.iterator();

		if ((vhosts != null) && vhosts.hasNext()) {
			return vhosts.next().getVhost().getBareJID();
		}

		return getDefHostName();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoDescription() {
		return "VHost Manager";
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		list.add(getName(), "Number of VHosts", repo.size(), Level.FINE);
		list.add(getName(), "Checks: is local domain", isLocalDomainCalls, Level.FINER);
		list.add(getName(), "Checks: is anonymous domain", isAnonymousEnabledCalls, Level
				.FINER);
		list.add(getName(), "Get components for local domain",
				getComponentsForLocalDomainCalls, Level.FINER);
		list.add(getName(), "Get components for non-local domain",
				getComponentsForNonLocalDomainCalls, Level.FINER);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>VHostItem</code>
	 */
	@Override
	public VHostItem getVHostItem(String domain) {
		return repo.getItem(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean isAnonymousEnabled(String domain) {
		++isAnonymousEnabledCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {
			return false;
		} else {
			return vhost.isAnonymousEnabled();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof VHostListener;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean isLocalDomain(String domain) {
		++isLocalDomainCalls;

		return repo.contains(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean isLocalDomainOrComponent(String domain) {
		boolean result = isLocalDomain(domain);

		if (!result) {
			result = registeredComponentDomains.contains(domain);
		}
		if (!result) {
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String        name       = domain.substring(0, idx);
				String        basedomain = domain.substring(idx + 1);
				VHostListener listener   = components.get(name);

				result = ((listener != null) && listener.handlesNameSubdomains() && isLocalDomain(
						basedomain));
			}
		}

		return result;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);

		String repo_class = (String) properties.get(VHOSTS_REPO_CLASS_PROP_KEY);

		if (repo_class != null && !isInitializationComplete()) {
			try {
				ComponentRepository<VHostItem> repo_tmp = (ComponentRepository<VHostItem>) Class
						.forName(repo_class).newInstance();

				repo_tmp.setProperties(properties);
				repo = repo_tmp;
				log.warning(repo.toString());
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can not create VHost repository instance for class: " +
						repo_class, e);
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/10/05
