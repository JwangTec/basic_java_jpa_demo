package github.learn666.jpademo.repository;


import github.learn666.jpademo.model.po.Person;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertNull;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;
    private Long id;

    /**
     * 保存person到数据库
     */

    @Before
    public void setUp() {
        Assert.assertNotNull(personRepository);
        Person person = new Person("S1", 23, 1L);
        Person savedPerson = personRepository.saveAndFlush(person);
        id = savedPerson.getId();
    }

    /**
     * jpa自带方法查找person
     */

    @Test
    public void should_get_school() {
        Optional<Person> personOptional = personRepository.findById(id);
        Assert.assertTrue(personOptional.isPresent());
        Assert.assertEquals("S1", personOptional.get().getName());
        Assert.assertEquals(Integer.valueOf(23), personOptional.get().getAge());

        List<Person> personList = personRepository.findByAgeGreaterThan(18);
        Assert.assertEquals(1, personList.size());

        //清空数据库
        personRepository.deleteAll();
    }

    @Test
    public void should_get_person_use_custom_query() {
        // 查找所有字段
        Optional<Person> personOptional = personRepository.findByNameCustomeQuery("S1");
        Assert.assertTrue(personOptional.isPresent());
        Assert.assertEquals(Integer.valueOf(23), personOptional.get().getAge());
        // 查找部分字段
        String personName = personRepository.findPersonNameById(id);
        Assert.assertEquals("S1", personName);
        System.out.println(id);
        // 更新
        personRepository.updatePersonNameById("UpdatedName", id);
        Optional<Person> updatedName = personRepository.findByNameCustomeQuery("UpdatedName");
        Assert.assertTrue(updatedName.isPresent());
        // 清空数据库
        personRepository.deleteAll();
    }
}
