package vn.iotstar.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger UI) configuration.
 *
 * After starting the app, access the interactive API documentation at:
 *   http://localhost:8080/swagger-ui.html
 *
 * To authenticate in Swagger UI:
 *   1. Login via POST /api/auth/login to receive a JWT token.
 *   2. Click "Authorize" → paste the token as: Bearer <token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("SportShop E-Commerce API")
                .description("""
                        ## 🛒 SportShop — Multi-Role E-Commerce Platform
                        
                        A full-featured e-commerce REST API built with **Spring Boot 3.2**.
                        
                        ### Key Features
                        - 🔐 **Multi-Role Authentication**: ADMIN / VENDOR / SHIPPER / USER
                        - 💳 **Payment Integration**: VNPay, MoMo (sandbox), QR Code, VietQR
                        - 💬 **Real-time Chat**: WebSocket-based chat between buyer and vendor
                        - 📦 **Order Management**: Full lifecycle from creation to delivery
                        - 📊 **Excel Import/Export**: Bulk product management via Apache POI
                        - 📄 **PDF Invoice Generation**: iText-powered invoice export
                        - 🔔 **OTP Email Verification**: Quartz-scheduled token cleanup
                        - 🏷️ **Promotion System**: Discount management per store
                        
                        ### Authentication
                        Use `POST /api/auth/login` to get a JWT token, then click **Authorize** above.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("IoTStar Dev Team")
                        .email("contact@iotstar.vn"))
                .license(new License()
                        .name("Academic Project — HK1 2025-2026")
                        .url("#"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter the JWT token obtained from POST /api/auth/login. Format: Bearer <token>");
    }
}
