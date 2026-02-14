package br.com.sawcunhaos.foundation.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@AutoConfigurationPackage
@SpringBootApplication
@ComponentScan({"*ignore*", "br.com.sawcunhaos.foundation.audit", "br.com.sawcunhaos.foundation.utils.configuration.hibernate"})
public class AuditTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditTestApplication.class, args);
	}

}
