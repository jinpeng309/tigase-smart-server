/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.db.xml;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.AuthRepository;
import tigase.db.AuthRepositoryImpl;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;

import tigase.xml.db.NodeExistsException;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.XMLDB;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Class <code>XMLRepository</code> is a <em>XML</em> implementation of
 * <code>UserRepository</code>.
 * It uses <code>tigase.xml.db</code> package as repository backend and uses
 * <em>Bridge</em> design pattern to translate <code>XMLDB</code> calls to
 * <code>UserRepository</code> functions.
 *
 * <p>
 * Created: Tue Oct 26 15:27:33 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMLRepository implements AuthRepository, UserRepository {
	private static final String USER_STR = "User: ";
	private static final String NOT_FOUND_STR = " has not been found in repository.";
	private static final Logger log = Logger.getLogger("tigase.db.xml.XMLRepository");

	//~--- fields ---------------------------------------------------------------

	private AuthRepository auth = null;
	private XMLDB xmldb = null;
	private boolean autoCreateUser = false;

	//~--- methods --------------------------------------------------------------

	/**
	 * <code>addDataList</code> method adds mode entries to existing data list
	 * associated with given key in repository under given node path.
	 * This method is very similar to <code>setDataList(...)</code> except it
	 * doesn't remove existing data.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param list a <code>String[]</code> is an array of values to be associated
	 * with the specified key.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void addDataList(BareJID user, final String subnode, final String key,
			final String[] list)
			throws UserNotFoundException, TigaseDBException {
		try {
			String[] old_data = getDataList(user, subnode, key);
			String[] all = null;

			if (old_data != null) {
				all = new String[old_data.length + list.length];
				System.arraycopy(old_data, 0, all, 0, old_data.length);
				System.arraycopy(list, 0, all, old_data.length, list.length);
				xmldb.setData(user.toString(), subnode, key, all);
			} else {
				xmldb.setData(user.toString(), subnode, key, list);
			}    // end of else
		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
		}      // end of try-catch
	}

	/**
	 * This <code>addUser</code> method allows to add new user to repository.
	 * It <b>must</b> throw en exception <code>UserExistsException</code> if such
	 * user already exists because user <b>must</b> be unique within user
	 * repository data base.<br/>
	 * As one <em>XMPP</em> server can support many virtual internet domains it
	 * is required that <code>user</code> id consists of user name and domain
	 * address: <em>username@domain.address.net</em> for example.
	 *
	 * @param user a <code>String</code> value of user id consisting of user name
	 * and domain address.
	 * @exception UserExistsException if user with the same id already exists.
	 */
	@Override
	public synchronized void addUser(BareJID user) throws UserExistsException {
		try {
			xmldb.addNode1(user.toString());
		} catch (NodeExistsException e) {
			throw new UserExistsException(USER_STR + user + " already exists.", e);
		}    // end of try-catch
	}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	@Override
	public synchronized void addUser(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		auth.addUser(user, password);
	}

	/**
	 * Describe <code>digestAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param digest a <code>String</code> value
	 * @param id a <code>String</code> value
	 * @param alg a <code>String</code> value
	 * @return a <code>boolean</code> value
	 *
	 * @throws AuthorizationException
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	@Override
	@Deprecated
	public synchronized boolean digestAuth(BareJID user, final String digest, final String id,
			final String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.digestAuth(user, digest, id, alg);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in given subnode.
	 * If key is not found in repository given default value is returned.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @param def a <code>String</code> value which is returned in case if data
	 * for specified key does not exist in repository.
	 * @return a <code>String</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized String getData(BareJID user, final String subnode, final String key,
			final String def)
			throws UserNotFoundException, TigaseDBException {
		try {
			return (String) xmldb.getData(user.toString(), subnode, key, def);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return (String) xmldb.getData(user.toString(), subnode, key, def);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in given subnode.
	 * If key is not found in repository <code>null</code> value is returned.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @return a <code>String</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public String getData(BareJID user, final String subnode, final String key)
			throws UserNotFoundException, TigaseDBException {
		return getData(user, subnode, key, null);
	}

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in default subnode.
	 * If key is not found in repository <code>null</code> value is returned.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @return a <code>String</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public String getData(BareJID user, final String key)
			throws UserNotFoundException, TigaseDBException {
		return getData(user, null, key, null);
	}

	/**
	 * <code>getDataList</code> method returns array of values associated with
	 * given key or <code>null</code> if given key does not exist for given user
	 * ID in given node path.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed values list is
	 * associated.
	 * @return a <code>String[]</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized String[] getDataList(BareJID user, final String subnode,
			final String key)
			throws UserNotFoundException, TigaseDBException {
		try {
			return xmldb.getDataList(user.toString(), subnode, key);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getDataList(user.toString(), subnode, key);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * <code>getKeys</code> method returns list of all keys stored in given
	 * subnode in user repository.
	 * There is a value (or list of values) associated with each key. It is up to
	 * user (developer) to know what key keeps one value and what key keeps list
	 * of values.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @return a <code>String[]</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized String[] getKeys(BareJID user, final String subnode)
			throws UserNotFoundException, TigaseDBException {
		try {
			return xmldb.getKeys(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getKeys(user.toString(), subnode);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * <code>getKeys</code> method returns list of all keys stored in default user
	 * repository node.
	 * There is some a value (or list of values) associated with each key. It is
	 * up to user (developer) to know what key keeps one value and what key keeps
	 * list of values.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		return getKeys(user, null);
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String getResourceUri() {
		return xmldb.getDBFileName();
	}

	/**
	 * <code>getSubnodes</code> method returns list of all direct subnodes from
	 * given node.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @return a <code>String[]</code> value is an array of all direct subnodes.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized String[] getSubnodes(BareJID user, final String subnode)
			throws UserNotFoundException, TigaseDBException {
		try {
			return xmldb.getSubnodes(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getSubnodes(user.toString(), subnode);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * <code>getSubnodes</code> method returns list of all <em>root</em> nodes for
	 * given user.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value is an array of all <em>root</em>
	 * nodes for given user.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException {
		return getSubnodes(user, null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * 
	 *
	 * @throws TigaseDBException
	 */
	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		return Math.abs(user.hashCode());
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public synchronized List<BareJID> getUsers() {
		List<String> users = xmldb.getAllNode1s();
		List<BareJID> result = new ArrayList<BareJID>();

		for (String usr : users) {
			result.add(BareJID.bareJIDInstanceNS(usr));
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * 
	 */
	@Override
	public synchronized long getUsersCount(String domain) {
		long res = 0;
		List<BareJID> jids = getUsers();

		for (BareJID jid : jids) {
			if (jid.getDomain().equals(domain)) {
				++res;
			}
		}

		return res;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public synchronized long getUsersCount() {
		return xmldb.getAllNode1sCount();
	}

	//~--- methods --------------------------------------------------------------

	// Implementation of tigase.xmpp.rep.UserRepository

	/**
	 * Method description
	 *
	 *
	 * @param file
	 * @param params
	 */
	@Override
	public synchronized void initRepository(String file, Map<String, String> params) {
		String file_name = file;

		try {
			int idx = file.indexOf("?");

			if (idx > 0) {
				file_name = file.substring(0, idx);
			}

			if (file.contains("autoCreateUser=true")) {
				autoCreateUser = true;
			}    // end of if (db_conn.contains())

			auth = new AuthRepositoryImpl(this);
			xmldb = new XMLDB(file_name);
		} catch (Exception e) {
			log.warning("Can not open existing user repository file, creating new one, " + e);
			xmldb = XMLDB.createDB(file_name, "users", "user");
		}      // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public synchronized void logout(BareJID user)
			throws UserNotFoundException, TigaseDBException {
		auth.logout(user);
	}

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	@Override
	public synchronized boolean otherAuth(final Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.otherAuth(props);
	}

	// Implementation of tigase.db.AuthRepository

	/**
	 * Describe <code>plainAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @return a <code>boolean</code> value
	 *
	 * @throws AuthorizationException
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	@Override
	@Deprecated
	public synchronized boolean plainAuth(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.plainAuth(user, password);
	}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 */
	@Override
	public synchronized void queryAuth(Map<String, Object> authProps) {
		auth.queryAuth(authProps);
	}

	/**
	 * <code>removeData</code> method removes pair (key, value) from user
	 * repository in given subnode.
	 * If the key exists in user repository there is always a value
	 * associated with this key - even empty <code>String</code>. If key does not
	 * exist the <code>null</code> value is returned from repository backend or
	 * given default value.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> for which the value is to be removed.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void removeData(BareJID user, final String subnode, final String key)
			throws UserNotFoundException {
		try {
			xmldb.removeData(user.toString(), subnode, key);
		} catch (NodeNotFoundException e) {
			if ( !autoCreateUser) {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * <code>removeData</code> method removes pair (key, value) from user
	 * repository in default repository node.
	 * If the key exists in user repository there is always a value
	 * associated with this key - even empty <code>String</code>. If key does not
	 * exist the <code>null</code> value is returned from repository backend or
	 * given default value.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> for which the value is to be removed.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public void removeData(BareJID user, final String key) throws UserNotFoundException {
		removeData(user, null, key);
	}

	/**
	 * <code>removeSubnode</code> method removes given subnode with all subnodes
	 * in this node and all data stored in this node and in all subnodes.
	 * Effectively it removes entire repository tree starting from given node.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path to subnode which
	 * has to be removed. Node path has the same form as directory path on file
	 * system: <pre>/root/subnode1/subnode2</pre>.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void removeSubnode(BareJID user, final String subnode)
			throws UserNotFoundException {
		try {
			xmldb.removeSubnode(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if ( !autoCreateUser) {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * This <code>removeUser</code> method allows to remove user and all his data
	 * from user repository.
	 * If given user id does not exist <code>UserNotFoundException</code> must be
	 * thrown. As one <em>XMPP</em> server can support many virtual internet
	 * domains it is required that <code>user</code> id consists of user name and
	 * domain address: <em>username@domain.address.net</em> for example.
	 *
	 * @param user a <code>String</code> value of user id consisting of user name
	 * and domain address.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void removeUser(BareJID user) throws UserNotFoundException {
		try {
			xmldb.removeNode1(user.toString());
		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
		}    // end of try-catch
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * <code>setData</code> method sets data value for
	 * given user ID in repository under given node path and associates it with
	 * given key.
	 * If there already exists value for given key in given node, old value is
	 * replaced with new value. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void setData(BareJID user, final String subnode, final String key,
			final String value)
			throws UserNotFoundException, TigaseDBException {
		try {
			xmldb.setData(user.toString(), subnode, key, value);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);
					xmldb.setData(user.toString(), subnode, key, value);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	/**
	 * This <code>setData</code> method sets data value for given user ID
	 * associated with given key in default repository node.
	 * Default node is dependent on implementation and usually it is root user
	 * node. If there already exists value for given key in given node, old value
	 * is replaced with new value. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public void setData(BareJID user, final String key, final String value)
			throws UserNotFoundException, TigaseDBException {
		setData(user, null, key, value);
	}

	/**
	 * <code>setDataList</code> method sets list of values for given user
	 * associated given key in repository under given node path.
	 * If there already exist values for given key in given node, all old values are
	 * replaced with new values. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param list a <code>String[]</code> is an array of values to be associated
	 * with the specified key.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 */
	@Override
	public synchronized void setDataList(BareJID user, final String subnode, final String key,
			final String[] list)
			throws UserNotFoundException, TigaseDBException {
		try {
			xmldb.setData(user.toString(), subnode, key, list);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);
					xmldb.setData(user.toString(), subnode, key, list);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @throws TigaseDBException
	 * @throws UserExistsException
	 */
	@Override
	public synchronized void updatePassword(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		auth.updatePassword(user, password);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * 
	 */
	@Override
	public synchronized boolean userExists(BareJID user) {
		return xmldb.findNode1(user.toString()) != null;
	}
}    // XMLRepository


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
