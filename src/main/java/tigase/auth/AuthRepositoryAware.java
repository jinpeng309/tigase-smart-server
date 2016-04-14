package tigase.auth;

import tigase.db.AuthRepository;

import javax.security.auth.callback.CallbackHandler;

/**
 * Interface should be implemented by {@linkplain CallbackHandler} instance if
 * {@linkplain AuthRepository} from session should be injected.
 */
public interface AuthRepositoryAware {

    /**
     * Sets {@linkplain AuthRepository}.
     *
     * @param repo {@linkplain AuthRepository}.
     */
    void setAuthRepository(AuthRepository repo);

}
