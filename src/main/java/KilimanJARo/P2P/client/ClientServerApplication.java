package KilimanJARo.P2P.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Stream;

@SpringBootApplication
@EnableScheduling
public class ClientServerApplication {

    public static void main(String[] args) {
        if (args != null) {
            String[] allArgs = Stream.concat(Arrays.stream(args), Stream.of("--spring.config.name=client_server")).toArray(String[]::new);
            SpringApplication.run(ClientServerApplication.class, allArgs);
        } else {
            SpringApplication.run(ClientServerApplication.class, "--spring.config.name=client_server");
        }

    }



    @Bean(name="ClientServerRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${central_server_properties}")
    private String centralServerPropertiesPath;

    @Bean(name = "centralServerProperties")
    public PropertiesFactoryBean centralServerProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(centralServerPropertiesPath));
        return bean;
    }

    @Value("${spring.config.name}")
    private String serverPropertiesPath;


    @Bean(name = "serverProperties")
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
