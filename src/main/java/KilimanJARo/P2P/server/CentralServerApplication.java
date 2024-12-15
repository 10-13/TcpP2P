package KilimanJARo.P2P.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Stream;

@SpringBootApplication
public class CentralServerApplication {
    public static void main(String[] args) {
        String[] defArgs = {"--spring.config.name=central_server_private"};
        if (args != null) {
            String[] allArgs = Stream.concat(Arrays.stream(args), Arrays.stream(defArgs)).toArray(String[]::new);
            SpringApplication.run(CentralServerApplication.class, allArgs);
        } else {
            SpringApplication.run(CentralServerApplication.class, defArgs);
        }
    }

    @Bean(name="CentralServerRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${public_properties}")
    private String publicPropertiesPath;

    @Bean(name = "publicProperties")
    public PropertiesFactoryBean publicProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(publicPropertiesPath + ".properties"));
        return bean;
    }

    @Value("${spring.config.name}")
    private String serverPropertiesPath;


    @Bean(name = "centralServerProperties")
    public PropertiesFactoryBean serverProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(serverPropertiesPath + ".properties"));
        return bean;
    }

    @Bean(name="centralSecurityChain")
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
