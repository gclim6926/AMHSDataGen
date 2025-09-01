package demo.amhsdatagen.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI amhsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AMHS Data Generator API")
                        .description("APIs for AMHS data generation, layout processing, and visualization")
                        .version("v1")
                        .contact(new Contact()
                                .name("AMHS Team")
                                .email("gclim@sk.com"))
                        .license(new License().name("Proprietary").url("https://gitlab.tde.sktelecom.com/DTDB/backend/fontus-hub/amhsdatagen")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repository")
                        .url("https://github.com/gclim6926/AMHSDataGen"));
    }
}


