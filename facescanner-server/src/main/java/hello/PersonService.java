package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);
    private final PersonRepository repo;

    @Autowired
    public PersonService(PersonRepository repo) {
        this.repo = repo;
    }

    public Optional<Person> findById(UUID id) {
        return repo.findById(id);
    }

    public Iterable<Person> findByName(String name) {
        return repo.findByName(name);
    }

    public Iterable<Person> findAll() {
        return repo.findAll();
    }

    public Iterable<Person> findAllById(Iterable<UUID> uuids) {
        return repo.findAllById(uuids);
    }

    public Optional<Person> getOne() {
        return Optional.ofNullable(findAll().iterator().next());
    }

    @Transactional
    public Person save(Person person) {
        return repo.save(person);
    }

    @Transactional
    public void delete(Person person) {
        repo.delete(person);
    }
}
