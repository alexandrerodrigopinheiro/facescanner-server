package hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import hello.service.FaceRecognitionService;
import hello.service.RecognizedFace;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PersonController.class)
public class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FaceRecognitionService faceService;

    @MockBean
    private PersonService personService;

    @Autowired
    private PersonController controller;

    @Value("classpath:webcam.jpg")
    private Resource resource;

    private UUID uuid;
    private String name;
    private byte[] photo;
    private String photoBase64;
    private Person person;
    private List<Person> people;

    @BeforeClass
    public static void before() {
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:");
    }

    @Before
    public void setUp() throws Exception {
        name = "test name";
        uuid = new UUID(1L, 1L);

        InputStream is = resource.getInputStream();
        photo = StreamUtils.copyToByteArray(is);
        photoBase64 = Base64Utils.encodeToString(photo);

        person = new Person();
        person.setId(uuid);
        person.setName(name);

        people = new ArrayList<>();
        people.add(person);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void contexLoads() throws Exception {
        assertThat(controller).isNotNull();
    }

    @Test
    public void testCreate() {
        when(personService.save(any())).thenReturn(person);
        when(personService.findAll()).thenReturn(people);

        person.setPhotoBase64(photoBase64);

        Person savedPerson = controller.create(person);

        assertEquals(uuid, savedPerson.getId());
        assertEquals(name, savedPerson.getName());
        assertThat(photo).isEqualTo(savedPerson.getPhoto());
    }

    @Test
    public void testFindAll() throws Exception {
        when(personService.findAll()).thenReturn(people);

        String peopleJson = this.mockMvc.perform(
                get("/face-recognition/people"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(name)))
                .andReturn().getResponse().getContentAsString();

        Person[] people1 = objectMapper.readValue(peopleJson, Person[].class);

        assertThat(people1[0].getId()).isEqualTo(uuid);
        assertThat(people1[0].getName()).isEqualTo(name);
    }

    public void testDelete() throws Exception {
        when(personService.findById(uuid)).thenReturn(java.util.Optional.ofNullable(person));

        this.mockMvc.perform(
                delete("/face-recognition/people/" + uuid.toString()))
                .andDo(print());

        String stringJson = this.mockMvc.perform(
                get("/face-recognition/people"))
                .andReturn().getResponse().getContentAsString();
        Person[] people1 = objectMapper.readValue(stringJson, Person[].class);

        assertEquals(0, people1.length);
    }

    public void testFindFaces() throws Exception {
        RecognizedFace face = new RecognizedFace(1, 2, 3, 4, uuid);
        List<RecognizedFace> faces = new ArrayList<>();
        faces.add(face);

        when(faceService.findAllInImage(any())).thenReturn(faces);
        when(personService.findById(uuid)).thenReturn(java.util.Optional.ofNullable(person));

        String stringJson = this.mockMvc.perform(
                post("/find-faces")
                        .content(photoBase64)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        FacePhoto people1 = objectMapper.readValue(stringJson, FacePhoto.class);

        assertEquals(1, people1.getFaces().size());
    }
}
