package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "hello")
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // at Bean
    public CommandLineRunner demo(PersonService service) {
        return (args) -> {
            // save a couple of customers
            service.save(new Person("Jackson"));
            service.save(new Person("Jose"));
            service.save(new Person("Marcos"));
            service.save(new Person("David"));
            service.save(new Person("Michele"));

            // fetch all customers
            log.info("Customers found with findAll():");
            log.info("-------------------------------");
            for (Person person : service.findAll()) {
                log.info(person.toString());
            }
            log.info("");

            // fetch an individual customer by ID
            service.getOne()
                    .ifPresent(person -> {
                        log.info("Person found with findById(1L):");
                        log.info("--------------------------------");
                        log.info(person.toString());
                        log.info("");
                    });

            // fetch customers by last name
            log.info("Person found with findByLastName('Bauer'):");
            log.info("--------------------------------------------");
            service.findByName("Jack").forEach(person -> {
                log.info(person.toString());
            });
            // for (Person bauer : repository.findByLastName("Bauer")) {
            // 	log.info(bauer.toString());
            // }
            log.info("");
        };
    }

}
