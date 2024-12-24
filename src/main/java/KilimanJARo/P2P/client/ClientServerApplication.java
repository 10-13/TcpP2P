package KilimanJARo.P2P.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Stream;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class ClientServerApplication {

    public static void main(String[] args) {
        String[] defArgs = {"--spring.config.name=client_server_private"};
        if (args != null) {
            String[] allArgs = Stream.concat(Arrays.stream(args), Arrays.stream(defArgs)).toArray(String[]::new);
            SpringApplication.run(ClientServerApplication.class, allArgs);
        } else {
            SpringApplication.run(ClientServerApplication.class, defArgs);
        }

    }



    @Bean(name="ClientServerRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${public_properties}")
    private String publicPropertiesPath;

    @Bean(name = "publicProperties")
    public PropertiesFactoryBean centralServerProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(publicPropertiesPath + ".properties"));
        return bean;
    }

    @Value("${spring.config.name}")
    private String serverPropertiesPath;


    @Bean(name = "clientServerProperties")
    public PropertiesFactoryBean serverProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(serverPropertiesPath + ".properties"));
        return bean;
    }

    @Bean(name="clientSecurityChain")
    public SecurityFilterChain securityFilterChainMain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
