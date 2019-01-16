package com.maveric.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import javax.sql.DataSource;

@EnableResourceServer
@Configuration
public class ResourcesServerConfiguration extends ResourceServerConfigurerAdapter {

    @Bean
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource oauthDataSource(){
    	return DataSourceBuilder.create().build();
    	}

    @Bean
    public TokenStore tokenStore() {
        return new JdbcTokenStore(oauthDataSource());
    }

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) {
		resources.tokenStore(tokenStore()).resourceId("mvapp");
	}
	@Override
	public void configure(HttpSecurity http) throws Exception {
		
		http.authorizeRequests().antMatchers("/")
		.permitAll()
		.anyRequest()
		.authenticated();
	}
}

class FixedSerialVersionUUIDJdbcTokenStore extends JdbcTokenStore {
    public FixedSerialVersionUUIDJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected OAuth2Authentication deserializeAuthentication(byte[] authentication) {
        return deserialize(authentication);
    }

    @Override
    protected OAuth2AccessToken deserializeAccessToken(byte[] token) {
        return deserialize(token);
    }

    @Override
    protected OAuth2RefreshToken deserializeRefreshToken(byte[] token) {
        return deserialize(token);
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] authentication) {
        try {
            return (T) super.deserializeAuthentication(authentication);
        } catch (Exception e) {
            try (ObjectInputStream input = new FixSerialVersionUUID(authentication)) {
                return (T) input.readObject();
            } catch (IOException | ClassNotFoundException e1) {
                throw new IllegalArgumentException(e1);
            }
        }
    }
}

class FixSerialVersionUUID extends ObjectInputStream {

    public FixSerialVersionUUID(byte[] bytes) throws IOException {
        super(new ByteArrayInputStream(bytes));
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        if (resultClassDescriptor.getName().equals(SimpleGrantedAuthority.class.getName())) {
            ObjectStreamClass mostRecentSerialVersionUUID = ObjectStreamClass.lookup(SimpleGrantedAuthority.class);
            return mostRecentSerialVersionUUID;
        }

        return resultClassDescriptor;
    }
}        
