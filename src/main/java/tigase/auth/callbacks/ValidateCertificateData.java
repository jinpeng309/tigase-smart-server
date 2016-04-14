package tigase.auth.callbacks;

import tigase.xmpp.BareJID;

import javax.security.auth.callback.Callback;

public class ValidateCertificateData implements Callback {

    private boolean authorized;

    private String authorizedID;

    private BareJID defaultAuthzid;

    public ValidateCertificateData(BareJID jid) {
        setDefaultAuthzid(jid);
    }

    public String getAuthorizedID() {
        return authorizedID;
    }

    public BareJID getDefaultAuthzid() {
        return defaultAuthzid;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public void setAuthorizedID(String authorizedID) {
        this.authorizedID = authorizedID;
    }

    public void setDefaultAuthzid(BareJID defaultAuthzid) {
        this.defaultAuthzid = defaultAuthzid;
    }

}
